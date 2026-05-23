import pytest
from pydantic import ValidationError

from shared.schemas.request import FraudPredictionRequest
from tests.test_constants import (
    INVALID_HOUR_OF_DAY,
    INVALID_MERCHANT_RISK_ABOVE_ONE,
    INVALID_NEGATIVE_AMOUNT,
    INVALID_ZERO_AMOUNT,
    TRANSACTION_ID,
    VALID_AMOUNT,
)
from tests.test_fixtures import valid_prediction_request_payload


def test_fraud_prediction_request_should_accept_valid_payload() -> None:
    request = FraudPredictionRequest(**valid_prediction_request_payload())

    assert request.transaction_id == TRANSACTION_ID
    assert request.amount == VALID_AMOUNT


@pytest.mark.parametrize(
    "override",
    [
        {"amount": INVALID_ZERO_AMOUNT},
        {"amount": INVALID_NEGATIVE_AMOUNT},
        {"merchant_risk_score": INVALID_MERCHANT_RISK_ABOVE_ONE},
        {"hour_of_day": INVALID_HOUR_OF_DAY},
    ],
)
def test_fraud_prediction_request_should_reject_invalid_values(override: dict) -> None:
    payload = valid_prediction_request_payload()
    payload.update(override)

    with pytest.raises(ValidationError):
        FraudPredictionRequest(**payload)


def test_fraud_prediction_request_should_apply_defaults_for_optional_fields() -> None:
    payload = valid_prediction_request_payload()
    payload.pop("amount_velocity_1hour", None)
    payload.pop("log_amount", None)
    payload.pop("day_of_week", None)
    payload.pop("merchant_category", None)

    request = FraudPredictionRequest(**payload)

    assert request.amount_velocity_1hour == 0.0
    assert request.log_amount == 0.0
    assert request.day_of_week == 1
    assert request.merchant_category == 7
