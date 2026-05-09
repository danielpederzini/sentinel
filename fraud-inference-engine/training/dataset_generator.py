import uuid
import os
import argparse
import pandas as pd
import numpy as np

_LEGIT_HOUR_WEIGHTS = np.array([1 if hour < 7 or hour >= 21 else 3 for hour in range(24)], dtype=float)
_LEGIT_HOUR_WEIGHTS /= _LEGIT_HOUR_WEIGHTS.sum()

_FRAUD_HOUR_WEIGHTS = np.array([4 if hour < 6 else 1 for hour in range(24)], dtype=float)
_FRAUD_HOUR_WEIGHTS /= _FRAUD_HOUR_WEIGHTS.sum()

def _generate_transaction_id() -> str:
    return str(uuid.uuid4())

def _generate_legitimate_row(rng: np.random.Generator) -> dict:
    user_average_amount = rng.uniform(20, 500)
    amount = max(0.01, rng.normal(loc=user_average_amount, scale=user_average_amount * 0.5))
    ratio = amount / user_average_amount if user_average_amount > 0 else 1.0

    count_last_5_minutes = int(rng.integers(0, 8))
    count_last_hour = int(rng.integers(0, 20))
    return {
        "transaction_id": _generate_transaction_id(),
        "amount": round(amount, 2),
        "user_average_amount": round(user_average_amount, 2),
        "user_transaction_count_5m": count_last_5_minutes,
        "user_transaction_count_1h": count_last_hour,
        "time_since_last_transaction_sec": int(rng.integers(30, 86400)),
        "merchant_risk_score": round(float(rng.uniform(0.0, 0.7)), 4),
        "device_is_trusted": bool(rng.random() > 0.08),
        "country_mismatch": bool(rng.random() < 0.05),
        "amount_vs_user_average_ratio": round(ratio, 4),
        "hour_of_day": int(rng.choice(24, p=_LEGIT_HOUR_WEIGHTS)),
        "ip_risk_score": round(float(rng.beta(2, 8)), 4),
        "card_age_days": int(rng.integers(180, 3650)),
        "is_fraud": False,
    }


def _generate_fraud_row(rng: np.random.Generator, stealth_rate: float = 0.20) -> dict:
    user_average_amount = rng.uniform(20, 500)

    # stealth fraud: no strong signals, blends in with legitimate traffic
    if rng.random() < stealth_rate:
        amount = max(0.01, rng.normal(loc=user_average_amount, scale=user_average_amount * 0.5))
        ratio = amount / user_average_amount if user_average_amount > 0 else 1.0
        count_last_5_minutes = int(rng.integers(0, 8))
        count_last_hour = int(rng.integers(0, 20))
        return {
            "transaction_id": _generate_transaction_id(),
            "amount": round(amount, 2),
            "user_average_amount": round(user_average_amount, 2),
            "user_transaction_count_5m": count_last_5_minutes,
            "user_transaction_count_1h": count_last_hour,
            "time_since_last_transaction_sec": int(rng.integers(30, 86400)),
            "merchant_risk_score": round(float(rng.uniform(0.0, 0.7)), 4),
            "device_is_trusted": bool(rng.random() > 0.08),
            "country_mismatch": bool(rng.random() < 0.05),
            "amount_vs_user_average_ratio": round(ratio, 4),
            "hour_of_day": int(rng.choice(24, p=_LEGIT_HOUR_WEIGHTS)),
            "ip_risk_score": round(float(rng.beta(2, 8)), 4),
            "card_age_days": int(rng.integers(180, 3650)),
            "is_fraud": True,
        }

    fraud_signals = rng.choice(
        ["high_amount", "burst", "risky_merchant", "device_mismatch", "country_mismatch", "new_card", "night_time", "risky_ip"],
        size=rng.integers(1, 4),
        replace=False,
    )

    amount_multiplier = rng.uniform(1.5, 4.0) if "high_amount" in fraud_signals else rng.uniform(0.3, 2.5)
    amount = max(0.01, user_average_amount * amount_multiplier)
    ratio = amount / user_average_amount if user_average_amount > 0 else amount_multiplier

    count_last_5_minutes = int(rng.integers(5, 15)) if "burst" in fraud_signals else int(rng.integers(0, 8))
    count_last_hour = int(rng.integers(12, 35)) if "burst" in fraud_signals else int(rng.integers(0, 20))
    time_since_last_transaction = int(rng.integers(5, 300)) if "burst" in fraud_signals else int(rng.integers(30, 86400))

    merchant_risk_score = round(float(rng.uniform(0.4, 1.0)), 4) if "risky_merchant" in fraud_signals else round(float(rng.uniform(0.0, 0.7)), 4)
    device_is_trusted = bool(rng.random() > 0.6) if "device_mismatch" in fraud_signals else bool(rng.random() > 0.1)
    country_mismatch = bool(rng.random() < 0.6) if "country_mismatch" in fraud_signals else bool(rng.random() < 0.05)

    hour_of_day = int(rng.choice(24, p=_FRAUD_HOUR_WEIGHTS)) if "night_time" in fraud_signals else int(rng.choice(24))
    ip_risk_score = round(float(rng.uniform(0.5, 1.0)), 4) if "risky_ip" in fraud_signals else round(float(rng.beta(3, 4)), 4)
    card_age_days = int(rng.integers(1, 90)) if "new_card" in fraud_signals else int(rng.integers(1, 3650))

    return {
        "transaction_id": _generate_transaction_id(),
        "amount": round(amount, 2),
        "user_average_amount": round(user_average_amount, 2),
        "user_transaction_count_5m": count_last_5_minutes,
        "user_transaction_count_1h": count_last_hour,
        "time_since_last_transaction_sec": time_since_last_transaction,
        "merchant_risk_score": merchant_risk_score,
        "device_is_trusted": device_is_trusted,
        "country_mismatch": country_mismatch,
        "amount_vs_user_average_ratio": round(ratio, 4),
        "hour_of_day": hour_of_day,
        "ip_risk_score": ip_risk_score,
        "card_age_days": card_age_days,
        "is_fraud": True,
    }


def simulate(
    row_count: int,
    fraud_rate: float = 0.05,
    stealth_rate: float = 0.20,
    seed: int | None = None,
) -> pd.DataFrame:
    """
    Simulate a synthetic fraud detection dataset.

    Args:
        row_count (int): Number of rows to generate.
        fraud_rate (float): Fraction of fraudulent rows (default: 0.05).
        stealth_rate (float): Fraction of fraud rows that are stealth (default: 0.20).
        seed (int | None): Random seed for reproducibility (default: None).

    Returns:
        pd.DataFrame: A DataFrame containing the simulated dataset.
    """
    rng = np.random.default_rng(seed)
    fraud_count = max(1, int(row_count * fraud_rate))
    legitimate_count = row_count - fraud_count

    rows = (
        [_generate_legitimate_row(rng) for _ in range(legitimate_count)]
        + [_generate_fraud_row(rng, stealth_rate=stealth_rate) for _ in range(fraud_count)]
    )

    data = pd.DataFrame(rows)
    data = data.sample(frac=1, random_state=seed).reset_index(drop=True)
    return data

def main():
    parser = argparse.ArgumentParser(description="Generate a synthetic fraud detection dataset.")
    parser.add_argument("num_rows", type=int, help="Number of rows to generate")
    parser.add_argument("--fraud-rate", type=float, default=0.05, help="Fraction of fraudulent rows (default: 0.05)")
    parser.add_argument("--stealth-rate", type=float, default=0.1, help="Fraction of fraud rows with no strong signals (default: 0.1)")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for reproducibility (default: 42)")
    parser.add_argument("--output", type=str, default="data/transactions.csv", help="Output CSV path (default: data/transactions.csv)")
    args = parser.parse_args()

    data = simulate(row_count=args.num_rows, fraud_rate=args.fraud_rate, stealth_rate=args.stealth_rate, seed=args.seed)

    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    data.to_csv(args.output, index=False)
    print(f"Saved {len(data)} rows ({data['is_fraud'].sum()} fraud, {(~data['is_fraud']).sum()} legitimate) to '{args.output}'")

if __name__ == "__main__":
    main()
