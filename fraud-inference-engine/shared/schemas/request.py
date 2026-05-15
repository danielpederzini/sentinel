from pydantic import BaseModel, Field

class FraudPredictionRequest(BaseModel):
    transaction_id: str
    amount: float = Field(gt=0)
    user_average_amount: float = Field(ge=0)
    user_transaction_count_5min: int = Field(ge=0)
    user_transaction_count_1hour: int = Field(ge=0)
    seconds_since_last_transaction: int = Field(ge=0)
    amount_velocity_1hour: float = Field(ge=0, default=0.0)
    merchant_risk_score: float = Field(ge=0, le=1)
    is_device_trusted: bool
    has_country_mismatch: bool
    amount_to_average_ratio: float = Field(ge=0)
    hour_of_day: int = Field(ge=0, le=23)
    ip_risk_score: float = Field(ge=0, le=1)
    card_age_days: int = Field(ge=0)
    amount_velocity_1hour: float = Field(ge=0, default=0.0)