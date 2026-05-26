import glob
import json
import os
import sys
import logging
import threading

import joblib
import lightgbm as lgb
import numpy as np
import shap
import uvicorn
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from prometheus_fastapi_instrumentator import Instrumentator
from kafka import KafkaConsumer
from minio import Minio
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
    os.path.join(os.path.dirname(__file__), "models"),
)

logger = logging.getLogger(__name__)

_state: dict = {}
_state_lock = threading.Lock()
_error_logger = ErrorLogger()


def _download_models_from_minio(model_directory: str) -> None:
    endpoint = os.environ.get("MINIO_ENDPOINT")
    if not endpoint:
        logger.info("MINIO_ENDPOINT not set, skipping MinIO download")
        return

    access_key = os.environ.get("MINIO_ACCESS_KEY", "minioadmin")
    secret_key = os.environ.get("MINIO_SECRET_KEY", "minioadmin")
    bucket = os.environ.get("MINIO_BUCKET", "models")
    use_ssl = os.environ.get("MINIO_USE_SSL", "false").lower() == "true"

    client = Minio(endpoint, access_key=access_key, secret_key=secret_key, secure=use_ssl)

    if not client.bucket_exists(bucket):
        logger.warning("MinIO bucket '%s' does not exist, skipping download", bucket)
        return

    os.makedirs(model_directory, exist_ok=True)
    objects = list(client.list_objects(bucket, prefix="lgbm_"))
    if not objects:
        logger.warning("No model objects found in MinIO bucket '%s'", bucket)
        return

    latest_object = max(objects, key=lambda obj: obj.last_modified)
    local_path = os.path.join(model_directory, latest_object.object_name)
    client.fget_object(bucket, latest_object.object_name, local_path)
    logger.info("Downloaded model from MinIO: %s/%s", bucket, latest_object.object_name)


def _load_latest_model(
    model_directory: str,
) -> tuple[lgb.LGBMClassifier, IsotonicRegression, dict, str, list[str], dict[str, float]]:
    try:
        candidates = glob.glob(os.path.join(model_directory, "lgbm_*.joblib"))
        if not candidates:
            raise ModelLoadException(f"No model bundles found in {model_directory}")

        latest_bundle_path = max(candidates, key=os.path.getmtime)
        bundle: dict = joblib.load(latest_bundle_path)
        feature_names: list[str] = bundle["feature_names"]
        feature_caps: dict[str, float] = bundle.get("feature_caps", {})
        return bundle["model"], bundle["calibrator"], bundle["metrics"], bundle["version"], feature_names, feature_caps
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


def _build_feature_values(
    request: FraudPredictionRequest,
    feature_caps: dict[str, float] | None = None,
) -> dict[str, float | int | bool]:
    caps = feature_caps or {}
    values = {
        "amount": request.amount,
        "user_average_amount": request.user_average_amount,
        "user_historical_transaction_count": request.user_historical_transaction_count,
        "user_transaction_count_5min": request.user_transaction_count_5min,
        "user_transaction_count_1hour": request.user_transaction_count_1hour,
        "seconds_since_last_transaction": request.seconds_since_last_transaction,
        "amount_velocity_1hour": request.amount_velocity_1hour,
        "merchant_risk_score": request.merchant_risk_score,
        "is_device_trusted": request.is_device_trusted,
        "has_country_mismatch": request.has_country_mismatch,
        "amount_to_average_ratio": request.amount_to_average_ratio,
        "hour_of_day": request.hour_of_day,
        "ip_risk_score": request.ip_risk_score,
        "card_age_days": request.card_age_days,
        "user_account_age_days": request.user_account_age_days,
        "day_of_week": request.day_of_week,
        "merchant_category": request.merchant_category,
        "card_type": request.card_type,
        "distinct_merchant_count_1hour": request.distinct_merchant_count_1hour,
        "log_amount": request.log_amount,
        "log_seconds_since": request.log_seconds_since,
        "log_velocity_1hour": request.log_velocity_1hour,
        "amount_x_merchant_risk": request.amount_x_merchant_risk,
        "risk_score_product": request.risk_score_product,
        "ip_device_risk": request.ip_device_risk,
        "country_ip_risk": request.country_ip_risk,
        "velocity_amount_interaction": request.velocity_amount_interaction,
        "recency_velocity": request.recency_velocity,
        "amount_deviation": request.amount_deviation,
        "is_night": request.is_night,
        "velocity_intensity": request.velocity_intensity,
    }
    for feature_name, cap_value in caps.items():
        if feature_name in values and isinstance(values[feature_name], (int, float)):
            values[feature_name] = min(values[feature_name], cap_value)
    return values


def _validate_model_feature_contract(feature_names: list[str]) -> None:
    available_fields = set(FraudPredictionRequest.model_fields.keys())
    mapped_fields = set(_build_feature_values(FraudPredictionRequest(
        transaction_id="contract-check",
        amount=1.0,
        user_average_amount=1.0,
        user_historical_transaction_count=0,
        user_transaction_count_5min=0,
        user_transaction_count_1hour=0,
        seconds_since_last_transaction=0,
        amount_velocity_1hour=0.0,
        merchant_risk_score=0.0,
        is_device_trusted=True,
        has_country_mismatch=False,
        amount_to_average_ratio=1.0,
        hour_of_day=0,
        ip_risk_score=0.0,
        card_age_days=0,
    )).keys())
    missing = [name for name in feature_names if name not in available_fields and name not in mapped_fields]
    if missing:
        raise ModelLoadException(
            "Model feature contract mismatch. Request schema cannot provide: "
            + ", ".join(sorted(missing))
        )


def _build_explainability(
    request: FraudPredictionRequest,
    feature_vector: np.ndarray,
    model: lgb.LGBMClassifier,
    feature_names: list[str],
    explainer: shap.TreeExplainer,
    top_k: int = 5,
) -> dict:
    base_values = _build_feature_values(request)
    shap_values = explainer.shap_values(feature_vector)

    # For binary classification, shap_values is a list of two arrays
    # (one per class). Use class 1 (fraud) contributions.
    if isinstance(shap_values, list):
        contributions = shap_values[1][0]
    else:
        contributions = shap_values[0]

    feature_contributions = []
    for index, feature_name in enumerate(feature_names):
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


def _apply_model_to_state(
    model: lgb.LGBMClassifier,
    calibrator: IsotonicRegression,
    metrics: dict,
    version: str,
    feature_names: list[str],
    feature_caps: dict[str, float],
) -> None:
    _validate_model_feature_contract(feature_names)
    explainer = shap.TreeExplainer(model)
    with _state_lock:
        _state["model"] = model
        _state["calibrator"] = calibrator
        _state["threshold"] = metrics["threshold"]
        _state["version"] = version
        _state["feature_names"] = feature_names
        _state["feature_caps"] = feature_caps
        _state["explainer"] = explainer
    caps_info = ", ".join(f"{k}={v:.2f}" for k, v in feature_caps.items()) if feature_caps else "none"
    logger.info("Loaded LightGBM model version %s with threshold %.4f, caps: %s", version, metrics['threshold'], caps_info)


def _handle_model_released(event: dict) -> None:
    object_name = event.get("object_name", "")
    bucket = event.get("bucket", os.environ.get("MINIO_BUCKET", "models"))
    if not object_name:
        logger.warning("Received models.released event with no object_name, skipping")
        return

    logger.info("Received models.released event for %s/%s", bucket, object_name)
    endpoint = os.environ.get("MINIO_ENDPOINT")
    if not endpoint:
        logger.warning("MINIO_ENDPOINT not set, cannot download new model")
        return

    access_key = os.environ.get("MINIO_ACCESS_KEY", "minioadmin")
    secret_key = os.environ.get("MINIO_SECRET_KEY", "minioadmin")
    use_ssl = os.environ.get("MINIO_USE_SSL", "false").lower() == "true"

    client = Minio(endpoint, access_key=access_key, secret_key=secret_key, secure=use_ssl)
    local_path = os.path.join(MODELS_DIRECTORY, object_name)
    os.makedirs(MODELS_DIRECTORY, exist_ok=True)
    client.fget_object(bucket, object_name, local_path)
    logger.info("Downloaded model from MinIO: %s/%s", bucket, object_name)

    bundle: dict = joblib.load(local_path)
    _apply_model_to_state(
        model=bundle["model"],
        calibrator=bundle["calibrator"],
        metrics=bundle["metrics"],
        version=bundle["version"],
        feature_names=bundle["feature_names"],
        feature_caps=bundle.get("feature_caps", {}),
    )
    logger.info("Model hotswapped to version %s", bundle['version'])


def _start_kafka_consumer() -> threading.Thread | None:
    bootstrap_servers = os.environ.get("KAFKA_BOOTSTRAP_SERVERS")
    topic = os.environ.get("KAFKA_MODELS_RELEASED_TOPIC", "models.released")
    if not bootstrap_servers:
        logger.info("KAFKA_BOOTSTRAP_SERVERS not set, model hotswap via Kafka disabled")
        return None

    def consume() -> None:
        consumer = KafkaConsumer(
            topic,
            bootstrap_servers=bootstrap_servers,
            value_deserializer=lambda v: json.loads(v.decode("utf-8")),
            group_id="fraud-inference-engine",
            auto_offset_reset="latest",
            enable_auto_commit=True,
        )
        logger.info("Kafka consumer started, listening on '%s'", topic)
        for message in consumer:
            try:
                _handle_model_released(message.value)
            except Exception as exception:
                _error_logger.error("Failed to hotswap model from Kafka event", exception)

    thread = threading.Thread(target=consume, daemon=True)
    thread.start()
    return thread


@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        _download_models_from_minio(MODELS_DIRECTORY)
        model, calibrator, metrics, version, feature_names, feature_caps = _load_latest_model(MODELS_DIRECTORY)
        _apply_model_to_state(model, calibrator, metrics, version, feature_names, feature_caps)
    except ModelLoadException as exception:
        _error_logger.error("Model loading failed during startup", exception)
        raise
    consumer_thread = _start_kafka_consumer()
    yield
    _state.clear()

app = FastAPI(title="Sentinel Fraud Detection", lifespan=lifespan)

Instrumentator(
    should_group_status_codes=False,
    excluded_handlers=["/metrics"],
).instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)


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
        with _state_lock:
            if "model" not in _state:
                raise PredictionException("Model is not loaded, service is unavailable")
            model: lgb.LGBMClassifier = _state["model"]
            calibrator: IsotonicRegression | None = _state["calibrator"]
            threshold: float = _state["threshold"]
            version: str = _state["version"]
            feature_names: list[str] = _state["feature_names"]
            feature_caps: dict[str, float] = _state.get("feature_caps", {})
            explainer: shap.TreeExplainer = _state["explainer"]

        feature_values = _build_feature_values(request, feature_caps)
        missing_features = [feature for feature in feature_names if feature not in feature_values]
        if missing_features:
            raise PredictionException(
                "Request is missing model features: " + ", ".join(sorted(missing_features))
            )
        feature_vector = np.array([[float(feature_values[f]) for f in feature_names]])

        raw_probability: float = float(model.predict_proba(feature_vector)[0, 1])

        fraud_probability = (
            float(calibrator.predict([raw_probability])[0])
            if calibrator is not None
            else raw_probability
        )

        risk_level = _find_risk_level(fraud_probability, threshold)

        response = FraudPredictionResponse(
            transaction_id=request.transaction_id,
            fraud_probability=fraud_probability,
            risk_level=risk_level,
            model_version=version,
            explainability=_build_explainability(
                request, feature_vector, model, feature_names, explainer,
            ),
        )
        logger.info(
            "Scored transaction %s | riskLevel: %s | fraudProbability: %.4f | modelVersion: %s",
            request.transaction_id, risk_level.value, fraud_probability, version,
        )
        return response
    except PredictionException:
        raise
    except Exception as exception:
        _error_logger.error(f"Prediction scoring failed for transaction {request.transaction_id}", exception)
        raise PredictionException(f"Scoring failed: {str(exception)}")

if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    )
    host = os.environ.get("HOST", "0.0.0.0")
    port = int(os.environ.get("PORT", "8083"))
    uvicorn.run("app:app", host=host, port=port, reload=False)
