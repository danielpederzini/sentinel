import glob
import os
import sys

import joblib
import numpy as np
import xgboost as xgb
from contextlib import asynccontextmanager
from fastapi import FastAPI
from sklearn.isotonic import IsotonicRegression

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from shared.schemas import FraudPredictionRequest, FraudPredictionResponse, RiskLevel

MODELS_DIRECTORY = os.environ.get(
    "MODELS_DIRECTORY",
    os.path.join(os.path.dirname(__file__), "..", "training", "models"),
)

_state: dict = {}

def _load_latest_model(model_directory: str) -> tuple[xgb.XGBClassifier, IsotonicRegression, dict, str]:
    candidates = glob.glob(os.path.join(model_directory, "xgb_*.joblib"))
    if not candidates:
        raise RuntimeError(f"No model bundles found in {model_directory}")

    latest_bundle_path = max(candidates, key=os.path.getmtime)
    bundle: dict = joblib.load(latest_bundle_path)
    return bundle["model"], bundle["calibrator"], bundle["metrics"], bundle["version"]


def _find_risk_level(probability: float, threshold: float) -> RiskLevel:
    if probability >= threshold:
        return RiskLevel.HIGH
    if probability >= threshold * 0.5:
        return RiskLevel.MEDIUM
    return RiskLevel.LOW


@asynccontextmanager
async def lifespan(app: FastAPI):
    model, calibrator, metrics, version = _load_latest_model(MODELS_DIRECTORY)
    _state["model"] = model
    _state["calibrator"] = calibrator
    _state["threshold"] = metrics["threshold"]
    _state["version"] = version
    yield
    _state.clear()

app = FastAPI(title="Sentinel Fraud Detection", lifespan=lifespan)

@app.post("/transaction/score", response_model=FraudPredictionResponse)
def score(request: FraudPredictionRequest) -> FraudPredictionResponse:
    feature_vector = np.array([[
        request.amount,
        request.user_average_amount,
        request.user_transaction_count_5m,
        request.user_transaction_count_1h,
        request.time_since_last_transaction_sec,
        request.merchant_risk_score,
        int(request.device_is_trusted),
        int(request.country_mismatch),
        request.amount_vs_user_average_ratio,
        request.hour_of_day,
        request.ip_risk_score,
        request.card_age_days,
        request.transaction_count_ratio,
    ]])

    raw_probability: float = float(_state["model"].predict_proba(feature_vector)[0, 1])

    calibrator: IsotonicRegression | None = _state["calibrator"]
    fraud_probability = float(calibrator.predict([raw_probability])[0]) if calibrator is not None else raw_probability

    return FraudPredictionResponse(
        transaction_id=request.transaction_id,
        fraud_probability=fraud_probability,
        risk_level=_find_risk_level(fraud_probability, _state["threshold"]),
        model_version=_state["version"],
    )
