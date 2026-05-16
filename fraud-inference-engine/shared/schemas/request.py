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
    log_amount: float = Field(default=0.0)
    log_seconds_since: float = Field(default=0.0)
    log_velocity_1hour: float = Field(default=0.0)
    amount_x_merchant_risk: float = Field(default=0.0)
    amount_x_ip_risk: float = Field(default=0.0)
    risk_score_product: float = Field(default=0.0)
    ip_device_risk: float = Field(default=0.0)
    country_ip_risk: float = Field(default=0.0)
    velocity_amount_interaction: float = Field(default=0.0)
    recency_velocity: float = Field(default=0.0)
    card_age_x_amount_ratio: float = Field(default=0.0)
    amount_deviation: float = Field(default=0.0)
    is_night: float = Field(default=0.0)
    night_amount_ratio: float = Field(default=0.0)
    velocity_intensity: float = Field(default=0.0)