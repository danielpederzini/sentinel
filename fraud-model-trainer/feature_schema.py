"""Single source of truth for the fraud model's feature/column schema.

Centralizes the category vocabularies, their integer encodings, the model's
base feature list, and the generated dataset's column order so the dataset
generator and the training pipeline cannot drift out of sync.
"""
from __future__ import annotations

MERCHANT_CATEGORIES = [
    "GROCERY", "RESTAURANT", "ENTERTAINMENT", "TRAVEL",
    "HEALTHCARE", "EDUCATION", "UTILITIES", "OTHER",
]
CARD_TYPES = ["CREDIT", "DEBIT", "CREDIT_AND_DEBIT", "OTHER"]

MERCHANT_CATEGORY_MAP = {name: index for index, name in enumerate(MERCHANT_CATEGORIES)}
CARD_TYPE_MAP = {name: index for index, name in enumerate(CARD_TYPES)}

BASE_FEATURES = [
    "amount",
    "user_average_amount",
    "user_historical_transaction_count",
    "user_transaction_count_5min",
    "user_transaction_count_1hour",
    "seconds_since_last_transaction",
    "amount_velocity_1hour",
    "merchant_risk_score",
    "is_device_trusted",
    "has_country_mismatch",
    "amount_to_average_ratio",
    "hour_of_day",
    "ip_risk_score",
    "card_age_days",
    "user_account_age_days",
    "day_of_week",
    "merchant_category",
    "card_type",
    "distinct_merchant_count_1hour",
]

CATEGORICAL_FEATURES = ["merchant_category", "card_type"]

OUTPUT_COLUMNS = ["transaction_id", "user_id", *BASE_FEATURES, "is_fraud"]
