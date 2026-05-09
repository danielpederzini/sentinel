from pydantic import BaseModel, Field

class FraudPredictionRequest(BaseModel):
    transaction_id: str
    amount: float = Field(gt=0)
    user_average_amount: float = Field(ge=0)
    user_transaction_count_5m: int = Field(ge=0)
    user_transaction_count_1h: int = Field(ge=0)
    time_since_last_transaction_sec: int = Field(ge=0)
    merchant_risk_score: float = Field(ge=0, le=1)
    device_is_trusted: bool
    country_mismatch: bool
    amount_vs_user_average_ratio: float = Field(ge=0)
    hour_of_day: int = Field(ge=0, le=23)
    ip_risk_score: float = Field(ge=0, le=1)
    card_age_days: int = Field(ge=0)