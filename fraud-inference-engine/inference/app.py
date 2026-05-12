import glob
import os
import sys
import logging

import joblib
import numpy as np
import uvicorn
import xgboost as xgb
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

FEATURE_NAMES = [
    "amount",
    "user_average_amount",
    "user_transaction_count_5min",
    "user_transaction_count_1hour",
    "seconds_since_last_transaction",
    "merchant_risk_score",
    "is_device_trusted",
    "has_country_mismatch",
    "amount_to_average_ratio",
    "hour_of_day",
    "ip_risk_score",
    "card_age_days",
]

_state: dict = {}
_error_logger = ErrorLogger()


def _load_latest_model(model_directory: str) -> tuple[xgb.XGBClassifier, IsotonicRegression, dict, str]:
    try:
        candidates = glob.glob(os.path.join(model_directory, "xgb_*.joblib"))
        if not candidates:
            raise ModelLoadException(f"No model bundles found in {model_directory}")

        latest_bundle_path = max(candidates, key=os.path.getmtime)
        bundle: dict = joblib.load(latest_bundle_path)
        return bundle["model"], bundle["calibrator"], bundle["metrics"], bundle["version"]
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
        "merchant_risk_score": request.merchant_risk_score,
        "is_device_trusted": request.is_device_trusted,
        "has_country_mismatch": request.has_country_mismatch,
        "amount_to_average_ratio": request.amount_to_average_ratio,
        "hour_of_day": request.hour_of_day,
        "ip_risk_score": request.ip_risk_score,
        "card_age_days": request.card_age_days,
    }


def _build_explainability(
    request: FraudPredictionRequest,
    feature_vector: np.ndarray,
    model: xgb.XGBClassifier,
    top_k: int = 5,
) -> dict:
    feature_values = _build_feature_values(request)
    dmatrix = xgb.DMatrix(feature_vector, feature_names=FEATURE_NAMES)
    contributions = model.get_booster().predict(dmatrix, pred_contribs=True)[0]

    feature_contributions = []
    for index, feature_name in enumerate(FEATURE_NAMES):
        contribution = float(contributions[index])
        feature_contributions.append({
            "feature_name": feature_name,
            "feature_value": feature_values[feature_name],
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
        model, calibrator, metrics, version = _load_latest_model(MODELS_DIRECTORY)
        _state["model"] = model
        _state["calibrator"] = calibrator
        _state["threshold"] = metrics["threshold"]
        _state["version"] = version
        print(f"Loaded model version {version} with threshold {_state['threshold']:.4f}")
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

        feature_vector = np.array([[
            request.amount,
            request.user_average_amount,
            request.user_transaction_count_5min,
            request.user_transaction_count_1hour,
            request.seconds_since_last_transaction,
            request.merchant_risk_score,
            int(request.is_device_trusted),
            int(request.has_country_mismatch),
            request.amount_to_average_ratio,
            request.hour_of_day,
            request.ip_risk_score,
            request.card_age_days,
        ]])

        model: xgb.XGBClassifier = _state["model"]
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
            explainability=_build_explainability(request, feature_vector, model),
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
