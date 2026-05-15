import glob
import os
import sys
import logging
from typing import Any

import joblib
import numpy as np
import uvicorn
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from sklearn.isotonic import IsotonicRegression
from http import HTTPStatus

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from shared.schemas import (
    FraudPredictionRequest,
    FraudPredictionResponse,
    RiskLevel,
    ErrorResponse,
    ModelLoadException,
    PredictionException,
    ValidationException,
    InferenceException,
    ErrorLogger,
)

MODELS_DIRECTORY = os.environ.get(
    "MODELS_DIRECTORY",
    os.path.join(os.path.dirname(__file__), "..", "training", "models"),
)

BASE_FEATURE_NAMES = [
    "amount",
    "user_average_amount",
    "user_transaction_count_5min",
    "user_transaction_count_1hour",
    "seconds_since_last_transaction",
    "amount_velocity_1h",
    "merchant_risk_score",
    "is_device_trusted",
    "has_country_mismatch",
    "amount_to_average_ratio",
    "hour_of_day",
    "ip_risk_score",
    "card_age_days",
]

# Legacy: used when model bundle does not include feature_names
FEATURE_NAMES = BASE_FEATURE_NAMES

_state: dict = {}
_error_logger = ErrorLogger()


def _load_latest_model(model_directory: str) -> tuple[Any, IsotonicRegression, dict, str, str, list[str]]:
    try:
        candidates = (
            glob.glob(os.path.join(model_directory, "lgbm_*.joblib"))
            + glob.glob(os.path.join(model_directory, "xgb_*.joblib"))
        )
        if not candidates:
            raise ModelLoadException(f"No model bundles found in {model_directory}")

        latest_bundle_path = max(candidates, key=os.path.getmtime)
        bundle: dict = joblib.load(latest_bundle_path)
        model_type = bundle.get("model_type", "xgboost")
        feature_names = bundle.get("feature_names", FEATURE_NAMES)
        return (
            bundle["model"], bundle["calibrator"], bundle["metrics"],
            bundle["version"], model_type, feature_names,
        )
    except ModelLoadException:
        raise
    except Exception as exception:
        _error_logger.error(f"Failed to load model from {model_directory}", exception)
        raise ModelLoadException(f"Model loading failed: {str(exception)}")


def _find_risk_level(probability: float, threshold: float) -> RiskLevel:
    if probability >= threshold:
        return RiskLevel.HIGH
    if probability >= threshold * 0.5:
        return RiskLevel.MEDIUM
    return RiskLevel.LOW


def _build_feature_values(request: FraudPredictionRequest) -> dict[str, float | int | bool]:
    return {
        "amount": request.amount,
        "user_average_amount": request.user_average_amount,
        "user_transaction_count_5min": request.user_transaction_count_5min,
        "user_transaction_count_1hour": request.user_transaction_count_1hour,
        "seconds_since_last_transaction": request.seconds_since_last_transaction,
        "amount_velocity_1h": getattr(request, "amount_velocity_1h", 0.0),
        "merchant_risk_score": request.merchant_risk_score,
        "is_device_trusted": request.is_device_trusted,
        "has_country_mismatch": request.has_country_mismatch,
        "amount_to_average_ratio": request.amount_to_average_ratio,
        "hour_of_day": request.hour_of_day,
        "ip_risk_score": request.ip_risk_score,
        "card_age_days": request.card_age_days,
    }


def _engineer_features_for_inference(base_values: dict[str, float | int | bool]) -> dict[str, float]:
    """Replicate training-time feature engineering for a single transaction."""
    amount = float(base_values["amount"])
    user_avg = float(base_values["user_average_amount"])
    tx_5m = float(base_values["user_transaction_count_5min"])
    tx_1h = float(base_values["user_transaction_count_1hour"])
    secs_since = float(base_values["seconds_since_last_transaction"])
    velocity_1h = float(base_values["amount_velocity_1h"])
    merchant_risk = float(base_values["merchant_risk_score"])
    is_trusted = float(base_values["is_device_trusted"])
    country_mm = float(base_values["has_country_mismatch"])
    ratio = float(base_values["amount_to_average_ratio"])
    hour = float(base_values["hour_of_day"])
    ip_risk = float(base_values["ip_risk_score"])
    card_age = float(base_values["card_age_days"])

    is_night = 1.0 if (hour < 6 or hour >= 22) else 0.0

    return {
        "log_amount": float(np.log1p(amount)),
        "log_seconds_since": float(np.log1p(secs_since)),
        "log_velocity_1h": float(np.log1p(velocity_1h)),
        "amount_x_merchant_risk": amount * merchant_risk,
        "amount_x_ip_risk": amount * ip_risk,
        "risk_score_product": merchant_risk * ip_risk,
        "ip_device_risk": ip_risk * (1.0 - is_trusted),
        "country_ip_risk": country_mm * ip_risk,
        "velocity_amount_interaction": tx_1h * ratio,
        "recency_velocity": tx_5m / max(secs_since, 1.0),
        "card_age_x_amount_ratio": card_age * ratio,
        "amount_deviation": abs(amount - user_avg) / max(user_avg, 1.0),
        "is_night": is_night,
        "night_amount_ratio": is_night * ratio,
        "velocity_intensity": velocity_1h / max(tx_1h, 1.0),
    }


def _build_explainability(
    request: FraudPredictionRequest,
    feature_vector: np.ndarray,
    model: Any,
    model_type: str,
    model_feature_names: list[str],
    top_k: int = 5,
) -> dict:
    base_values = _build_feature_values(request)

    if model_type == "lightgbm":
        contributions = model.predict(feature_vector, pred_contrib=True)[0]
    else:
        import xgboost as xgb
        dmatrix = xgb.DMatrix(feature_vector, feature_names=model_feature_names)
        contributions = model.get_booster().predict(dmatrix, pred_contribs=True)[0]

    feature_contributions = []
    for index, feature_name in enumerate(model_feature_names):
        if index >= len(contributions):
            break
        contribution = float(contributions[index])
        feature_contributions.append({
            "feature_name": feature_name,
            "feature_value": base_values.get(feature_name, None),
            "contribution": contribution,
            "direction": "INCREASED_FRAUD_RISK" if contribution >= 0 else "DECREASED_FRAUD_RISK",
        })

    top_contributing_features = sorted(
        feature_contributions,
        key=lambda item: abs(item["contribution"]),
        reverse=True,
    )[:top_k]

    return {
        "top_contributing_features": top_contributing_features,
    }


@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        model, calibrator, metrics, version, model_type, feature_names = _load_latest_model(MODELS_DIRECTORY)
        _state["model"] = model
        _state["calibrator"] = calibrator
        _state["threshold"] = metrics["threshold"]
        _state["version"] = version
        _state["model_type"] = model_type
        _state["feature_names"] = feature_names
        print(f"Loaded {model_type} model version {version} with threshold {_state['threshold']:.4f}")
    except ModelLoadException as exception:
        _error_logger.error("Model loading failed during startup", exception)
        raise
    yield
    _state.clear()

app = FastAPI(title="Sentinel Fraud Detection", lifespan=lifespan)


@app.exception_handler(ModelLoadException)
async def handle_model_load_exception(request: Request, exception: ModelLoadException):
    _error_logger.warn("Model loading error", exception)
    error_response = ErrorResponse(
        status_code=exception.status_code,
        message=exception.message,
    )
    return JSONResponse(
        status_code=exception.status_code,
        content=error_response.model_dump(),
    )


@app.exception_handler(PredictionException)
async def handle_prediction_exception(request: Request, exception: PredictionException):
    _error_logger.warn("Prediction error", exception)
    error_response = ErrorResponse(
        status_code=exception.status_code,
        message=exception.message,
    )
    return JSONResponse(
        status_code=exception.status_code,
        content=error_response.model_dump(),
    )


@app.exception_handler(ValidationException)
async def handle_validation_exception(request: Request, exception: ValidationException):
    _error_logger.warn("Validation error", exception)
    error_response = ErrorResponse(
        status_code=exception.status_code,
        message=exception.message,
    )
    return JSONResponse(
        status_code=exception.status_code,
        content=error_response.model_dump(),
    )


@app.exception_handler(InferenceException)
async def handle_inference_exception(request: Request, exception: InferenceException):
    _error_logger.warn("Inference error", exception)
    error_response = ErrorResponse(
        status_code=exception.status_code,
        message=exception.message,
    )
    return JSONResponse(
        status_code=exception.status_code,
        content=error_response.model_dump(),
    )


@app.exception_handler(Exception)
async def handle_generic_exception(request: Request, exception: Exception):
    _error_logger.error("Unexpected error", exception)
    error_response = ErrorResponse(
        status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
        message="An unexpected error occurred",
    )
    return JSONResponse(
        status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
        content=error_response.model_dump(),
    )


@app.post("/transaction/score", response_model=FraudPredictionResponse)
def score(request: FraudPredictionRequest) -> FraudPredictionResponse:
    try:
        if "model" not in _state:
            raise PredictionException("Model is not loaded, service is unavailable")

        model_type: str = _state["model_type"]
        model_feature_names: list[str] = _state["feature_names"]

        base_values = _build_feature_values(request)
        all_values = {**base_values}

        # Apply feature engineering if the model expects engineered features
        if len(model_feature_names) > len(BASE_FEATURE_NAMES):
            engineered = _engineer_features_for_inference(base_values)
            all_values.update(engineered)

        feature_vector = np.array([[float(all_values.get(f, 0.0)) for f in model_feature_names]])

        model = _state["model"]
        raw_probability: float = float(model.predict_proba(feature_vector)[0, 1])

        calibrator: IsotonicRegression | None = _state["calibrator"]
        fraud_probability = (
            float(calibrator.predict([raw_probability])[0])
            if calibrator is not None
            else raw_probability
        )

        response = FraudPredictionResponse(
            transaction_id=request.transaction_id,
            fraud_probability=fraud_probability,
            risk_level=_find_risk_level(fraud_probability, _state["threshold"]),
            model_version=_state["version"],
            explainability=_build_explainability(
                request, feature_vector, model, model_type, model_feature_names,
            ),
        )
        return response
    except PredictionException:
        raise
    except Exception as exception:
        _error_logger.error(f"Prediction scoring failed for transaction {request.transaction_id}", exception)
        raise PredictionException(f"Scoring failed: {str(exception)}")

if __name__ == "__main__":
    host = os.environ.get("HOST", "0.0.0.0")
    port = int(os.environ.get("PORT", "8083"))
    uvicorn.run("app:app", host=host, port=port, reload=False)
