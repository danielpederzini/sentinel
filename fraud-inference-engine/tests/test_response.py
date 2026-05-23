import pytest
from pydantic import ValidationError

from shared.schemas.enums import RiskLevel
from shared.schemas.response import ExplainabilityDetails, FraudPredictionResponse
from tests.test_constants import (
    FEATURE_NAME,
    FRAUD_PROBABILITY,
    INVALID_FRAUD_PROBABILITY_ABOVE_ONE,
    TRANSACTION_ID,
)
from tests.test_fixtures import explainability_details, fraud_prediction_response


def test_fraud_prediction_response_should_accept_valid_payload() -> None:
    response = fraud_prediction_response()

    assert response.transaction_id == TRANSACTION_ID
    assert response.fraud_probability == FRAUD_PROBABILITY
    assert response.risk_level == RiskLevel.MEDIUM


def test_fraud_prediction_response_should_reject_probability_out_of_range() -> None:
    payload = fraud_prediction_response().model_dump()
    payload["fraud_probability"] = INVALID_FRAUD_PROBABILITY_ABOVE_ONE

    with pytest.raises(ValidationError):
        FraudPredictionResponse(**payload)


def test_explainability_details_should_default_to_empty_feature_list() -> None:
    details = ExplainabilityDetails()

    assert details.top_contributing_features == []


def test_explainability_details_should_store_contributions() -> None:
    details = explainability_details()

    assert len(details.top_contributing_features) == 1
    assert details.top_contributing_features[0].feature_name == FEATURE_NAME
