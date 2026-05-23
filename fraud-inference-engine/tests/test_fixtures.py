from shared.schemas.enums import RiskLevel
from shared.schemas.response import ExplainabilityDetails, FeatureContribution, FraudPredictionResponse

from tests.test_constants import (
    FEATURE_CONTRIBUTION,
    FEATURE_DIRECTION,
    FEATURE_NAME,
    FEATURE_VALUE,
    FRAUD_PROBABILITY,
    MODEL_VERSION,
    TRANSACTION_ID,
    VALID_AMOUNT,
    VALID_DAY_OF_WEEK,
    VALID_HOUR_OF_DAY,
    VALID_MERCHANT_RISK_SCORE,
)


def valid_prediction_request_payload() -> dict:
    return {
        "transaction_id": TRANSACTION_ID,
        "amount": VALID_AMOUNT,
        "user_average_amount": 100.0,
        "user_historical_transaction_count": 5,
        "user_transaction_count_5min": 1,
        "user_transaction_count_1hour": 2,
        "seconds_since_last_transaction": 600,
        "amount_velocity_1hour": 10.0,
        "merchant_risk_score": VALID_MERCHANT_RISK_SCORE,
        "is_device_trusted": True,
        "has_country_mismatch": False,
        "amount_to_average_ratio": 1.0,
        "hour_of_day": VALID_HOUR_OF_DAY,
        "ip_risk_score": 0.2,
        "card_age_days": 30,
        "user_account_age_days": 365,
        "day_of_week": VALID_DAY_OF_WEEK,
        "merchant_category": 1,
        "card_type": 0,
        "distinct_merchant_count_1hour": 1,
    }


def feature_contribution() -> FeatureContribution:
    return FeatureContribution(
        feature_name=FEATURE_NAME,
        feature_value=FEATURE_VALUE,
        contribution=FEATURE_CONTRIBUTION,
        direction=FEATURE_DIRECTION,
    )


def explainability_details() -> ExplainabilityDetails:
    return ExplainabilityDetails(top_contributing_features=[feature_contribution()])


def fraud_prediction_response() -> FraudPredictionResponse:
    return FraudPredictionResponse(
        transaction_id=TRANSACTION_ID,
        fraud_probability=FRAUD_PROBABILITY,
        risk_level=RiskLevel.MEDIUM,
        model_version=MODEL_VERSION,
        explainability=explainability_details(),
    )
