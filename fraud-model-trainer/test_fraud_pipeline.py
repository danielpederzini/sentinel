from __future__ import annotations

import unittest

import pandas as pd

from dataset_generator import _CARD_TYPES, _MERCHANT_CATEGORIES, simulate, validate_dataset
from training_pipeline import BASE_FEATURES, encode_categoricals, engineer_features


class DatasetGeneratorTests(unittest.TestCase):
    def test_generator_emits_required_features_and_cold_start_lift(self) -> None:
        data = simulate(6_000, fraud_rate=0.06, seed=7, inject_quality_issues=False)

        for column in BASE_FEATURES + ["transaction_id", "user_id", "is_fraud"]:
            self.assertIn(column, data.columns)

        self.assertTrue(set(data["merchant_category"]).issubset(set(_MERCHANT_CATEGORIES)))
        self.assertTrue(set(data["card_type"]).issubset(set(_CARD_TYPES)))

        stats = validate_dataset(data)
        self.assertGreater(stats["large_cold_start_count"], 0)
        self.assertGreater(stats["large_cold_start_lift"], 1.4)

    def test_engineered_features_match_expected_formulas(self) -> None:
        row = pd.DataFrame([{
            "amount": 1_000_000.0,
            "user_average_amount": 100.0,
            "user_historical_transaction_count": 0,
            "user_transaction_count_5min": 0,
            "user_transaction_count_1hour": 0,
            "seconds_since_last_transaction": 30 * 24 * 3600,
            "amount_velocity_1hour": 1_000_000.0,
            "merchant_risk_score": 0.88,
            "is_device_trusted": False,
            "has_country_mismatch": True,
            "amount_to_average_ratio": 10_000.0,
            "hour_of_day": 2,
            "ip_risk_score": 0.91,
            "card_age_days": 3,
            "user_account_age_days": 5,
            "day_of_week": 7,
            "merchant_category": "TRAVEL",
            "card_type": "CREDIT",
            "distinct_merchant_count_1hour": 1,
        }])

        engineered, _ = engineer_features(encode_categoricals(row))
        engineered = engineered.iloc[0]

        self.assertEqual(engineered["user_historical_transaction_count"], 0)
        self.assertAlmostEqual(engineered["amount_deviation"], 9_999.0)
        self.assertAlmostEqual(engineered["amount_x_merchant_risk"], 880_000.0)
        self.assertAlmostEqual(engineered["risk_score_product"], 0.8008)
        self.assertAlmostEqual(engineered["ip_device_risk"], 0.91)
        self.assertAlmostEqual(engineered["country_ip_risk"], 0.91)
        self.assertEqual(engineered["is_night"], 1.0)


if __name__ == "__main__":
    unittest.main()
