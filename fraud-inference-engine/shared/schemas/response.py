from pydantic import BaseModel, Field
from .enums import RiskLevel


class FeatureContribution(BaseModel):
    feature_name: str
    feature_value: float | int | bool | None
    contribution: float
    direction: str


class ExplainabilityDetails(BaseModel):
    top_contributing_features: list[FeatureContribution] = Field(default_factory=list)


class FraudPredictionResponse(BaseModel):
    transaction_id: str
    fraud_probability: float = Field(ge=0, le=1)
    risk_level: RiskLevel
    model_version: str
    explainability: ExplainabilityDetails