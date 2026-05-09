from pydantic import BaseModel, Field
from .enums import RiskLevel

class FraudPredictionResponse(BaseModel):
    transaction_id: str
    fraud_probability: float = Field(ge=0, le=1)
    risk_level: RiskLevel
    model_version: str