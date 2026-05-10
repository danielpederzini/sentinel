from __future__ import annotations

import argparse
import os
import uuid
from collections import deque
from dataclasses import dataclass, field
from datetime import datetime, timedelta

import numpy as np
import pandas as pd

_OUTPUT_COLUMNS = [
    "transaction_id",
    "amount",
    "user_average_amount",
    "user_transaction_count5_min",
    "user_transaction_count1_hour",
    "seconds_since_last_transaction",
    "merchant_risk_score",
    "is_device_trusted",
    "has_country_mismatch",
    "amount_to_average_ratio",
    "hour_of_day",
    "ip_risk_score",
    "card_age_days",
    "is_fraud",
]

_COUNTRIES = np.array(["US", "BR", "AR", "DE", "ES", "SE", "NL", "GB", "CA", "JP", "AU", "MX", "CL", "ZA", "PT", "FR", "IT", "CH", "CN", "IN"], dtype=object)
_COUNTRY_WEIGHTS = np.array([0.16, 0.12, 0.08, 0.10, 0.08, 0.09, 0.07, 0.07, 0.05, 0.05, 0.04, 0.04, 0.03, 0.03, 0.02, 0.02, 0.02, 0.02, 0.02, 0.01], dtype=float)
_COUNTRY_WEIGHTS /= _COUNTRY_WEIGHTS.sum()

_MERCHANT_LOW_RISK_POOL = np.array([0.05, 0.12, 0.18, 0.31, 0.41], dtype=float)
_MERCHANT_LOW_RISK_WEIGHTS = np.array([0.28, 0.24, 0.20, 0.16, 0.12], dtype=float)
_MERCHANT_HIGH_RISK_POOL = np.array([0.58, 0.73, 0.89], dtype=float)
_MERCHANT_HIGH_RISK_WEIGHTS = np.array([0.40, 0.35, 0.25], dtype=float)

_IP_LOW_RISK_POOL = np.array([0.02, 0.05, 0.08, 0.11, 0.15, 0.19, 0.23, 0.28, 0.34], dtype=float)
_IP_LOW_RISK_WEIGHTS = np.array([0.16, 0.15, 0.14, 0.13, 0.12, 0.11, 0.10, 0.08, 0.01], dtype=float)
_IP_MEDIUM_RISK_POOL = np.array([0.42, 0.55, 0.68, 0.81, 0.93], dtype=float)
_IP_MEDIUM_RISK_WEIGHTS = np.array([0.24, 0.23, 0.21, 0.18, 0.14], dtype=float)
_IP_HIGH_RISK_POOL = np.array([1.05, 1.18, 1.31, 1.47, 1.62, 1.79], dtype=float)
_IP_HIGH_RISK_WEIGHTS = np.array([0.10, 0.16, 0.20, 0.20, 0.18, 0.16], dtype=float)

_FIVE_MINUTES = 5 * 60
_ONE_HOUR = 60 * 60


@dataclass
class UserProfile:
    user_id: str
    home_country: str
    baseline_amount: float
    activity_weight: float
    trusted_device_rate: float
    card_creation_date: datetime


@dataclass
class UserState:
    profile: UserProfile
    running_amount_sum: float = 0.0
    running_amount_count: int = 0
    recent_timestamps_5m: deque = field(default_factory=deque)
    recent_timestamps_1h: deque = field(default_factory=deque)
    last_timestamp: datetime | None = None


def _generate_transaction_id() -> str:
    return str(uuid.uuid4())


def _normalize_weights(weights: np.ndarray) -> np.ndarray:
    weights = np.asarray(weights, dtype=float)
    total = float(weights.sum())
    if total <= 0:
        return np.full(len(weights), 1.0 / len(weights))
    return weights / total


def _clip_amount(amount: float) -> float:
    return float(max(0.01, round(amount, 2)))


def _sample_profile_amount(rng: np.random.Generator, baseline_amount: float, low_multiplier: float, high_multiplier: float) -> float:
    multiplier = float(rng.uniform(low_multiplier, high_multiplier))
    noise = float(rng.normal(loc=1.0, scale=0.12))
    return _clip_amount(baseline_amount * multiplier * max(0.25, noise))


def _sample_merchant_risk(rng: np.random.Generator, is_fraud: bool, risky_merchant: bool) -> float:
    if risky_merchant:
        return float(round(rng.choice(_MERCHANT_HIGH_RISK_POOL, p=_normalize_weights(_MERCHANT_HIGH_RISK_WEIGHTS)), 4))
    if is_fraud:
        pool = np.concatenate((_MERCHANT_LOW_RISK_POOL, _MERCHANT_HIGH_RISK_POOL))
        weights = np.concatenate((np.array([0.10, 0.10, 0.12, 0.16, 0.16], dtype=float), _MERCHANT_HIGH_RISK_WEIGHTS))
        return float(round(rng.choice(pool, p=_normalize_weights(weights)), 4))
    return float(round(rng.choice(_MERCHANT_LOW_RISK_POOL, p=_normalize_weights(_MERCHANT_LOW_RISK_WEIGHTS)), 4))


def _sample_ip_risk(rng: np.random.Generator, is_fraud: bool, risky_ip: bool) -> float:
    if risky_ip:
        return float(round(rng.choice(_IP_HIGH_RISK_POOL, p=_normalize_weights(_IP_HIGH_RISK_WEIGHTS)), 4))
    if is_fraud:
        pool = np.concatenate((_IP_MEDIUM_RISK_POOL, _IP_HIGH_RISK_POOL))
        weights = np.concatenate((_IP_MEDIUM_RISK_WEIGHTS, _IP_HIGH_RISK_WEIGHTS))
        return float(round(rng.choice(pool, p=_normalize_weights(weights)), 4))
    return float(round(rng.choice(_IP_LOW_RISK_POOL, p=_normalize_weights(_IP_LOW_RISK_WEIGHTS)), 4))


def _build_profiles(rng: np.random.Generator, user_count: int, simulation_start: datetime) -> list[UserProfile]:
    profiles: list[UserProfile] = []
    for index in range(user_count):
        home_country = str(rng.choice(_COUNTRIES, p=_COUNTRY_WEIGHTS))
        baseline_amount = float(np.clip(rng.lognormal(mean=np.log(120.0), sigma=0.85), 8.0, 2500.0))
        activity_weight = float(rng.gamma(shape=2.0, scale=1.0))
        trusted_device_rate = float(np.clip(rng.normal(loc=0.93, scale=0.05), 0.70, 0.995))
        card_age_days = int(np.clip(rng.lognormal(mean=np.log(365.0), sigma=0.9), 15, 3650))
        profiles.append(
            UserProfile(
                user_id=f"user-{index + 1:04d}",
                home_country=home_country,
                baseline_amount=baseline_amount,
                activity_weight=activity_weight,
                trusted_device_rate=trusted_device_rate,
                card_creation_date=simulation_start - timedelta(days=card_age_days),
            )
        )
    return profiles


def _seed_hidden_history(state: UserState, rng: np.random.Generator, simulation_start: datetime) -> None:
    recent_seed = float(rng.random()) < 0.35
    if recent_seed:
        hidden_event_count = int(rng.integers(3, 6))
        timestamp = simulation_start - timedelta(seconds=int(rng.integers(30, 240)))
        gap_low, gap_high = 15, 90
    else:
        hidden_event_count = int(rng.integers(2, 6))
        timestamp = simulation_start - timedelta(seconds=int(rng.integers(1800, 6 * 3600)))
        gap_low, gap_high = 180, 2 * 3600

    for _ in range(hidden_event_count):
        timestamp += timedelta(seconds=int(rng.integers(gap_low, gap_high)))
        amount = _sample_profile_amount(rng, state.profile.baseline_amount, 0.75, 1.15)
        state.running_amount_sum += amount
        state.running_amount_count += 1
        state.last_timestamp = timestamp
        state.recent_timestamps_1h.append(timestamp)
        if timestamp >= simulation_start - timedelta(seconds=_FIVE_MINUTES):
            state.recent_timestamps_5m.append(timestamp)


def _prune_windows(state: UserState, current_timestamp: datetime) -> None:
    cutoff_5m = current_timestamp - timedelta(seconds=_FIVE_MINUTES)
    cutoff_1h = current_timestamp - timedelta(seconds=_ONE_HOUR)
    while state.recent_timestamps_5m and state.recent_timestamps_5m[0] < cutoff_5m:
        state.recent_timestamps_5m.popleft()
    while state.recent_timestamps_1h and state.recent_timestamps_1h[0] < cutoff_1h:
        state.recent_timestamps_1h.popleft()


def _choose_event_kind(rng: np.random.Generator, fraud_rate: float, stealth_rate: float) -> tuple[bool, str]:
    if float(rng.random()) >= fraud_rate:
        return False, "legit"

    if float(rng.random()) < stealth_rate:
        return True, "stealth"

    fraud_kinds = np.array(["burst", "high_amount", "risky_merchant", "device_mismatch", "country_mismatch", "new_card", "night_time", "risky_ip"], dtype=object)
    fraud_weights = _normalize_weights(np.array([0.20, 0.18, 0.14, 0.13, 0.11, 0.10, 0.07, 0.07], dtype=float))
    return True, str(rng.choice(fraud_kinds, p=fraud_weights))


def _choose_gap_seconds(rng: np.random.Generator, event_kind: str, activity_weight: float) -> int:
    if event_kind == "burst":
        return int(rng.integers(20, 420))

    if event_kind in {"high_amount", "risky_merchant", "device_mismatch", "country_mismatch", "new_card", "night_time", "risky_ip"}:
        return int(np.clip(rng.lognormal(mean=np.log(10 * 60), sigma=1.0), 60, 18 * 3600))

    cadence_seconds = 60 * 60 * float(np.clip(rng.lognormal(mean=np.log(16.0), sigma=0.6), 2.0, 48.0))
    cadence_seconds /= max(0.35, activity_weight)
    return int(np.clip(cadence_seconds, 10 * 60, 9 * 24 * 3600))


def _build_event_timestamp(state: UserState, rng: np.random.Generator, event_kind: str) -> datetime:
    base_timestamp = state.last_timestamp or datetime(2026, 1, 1, 8, 0, 0)
    gap_seconds = _choose_gap_seconds(rng, event_kind, state.profile.activity_weight)
    event_timestamp = base_timestamp + timedelta(seconds=gap_seconds)

    if event_kind == "night_time":
        night_hour = int(rng.choice([0, 1, 2, 3, 4, 5, 22, 23]))
        event_timestamp = event_timestamp.replace(
            hour=night_hour,
            minute=int(rng.integers(0, 60)),
            second=int(rng.integers(0, 60)),
            microsecond=0,
        )
        if event_timestamp <= base_timestamp:
            event_timestamp += timedelta(days=1)

    return event_timestamp


def _generate_row(state: UserState, rng: np.random.Generator, fraud_rate: float, stealth_rate: float) -> dict:
    is_fraud, event_kind = _choose_event_kind(rng, fraud_rate=fraud_rate, stealth_rate=stealth_rate)
    event_timestamp = _build_event_timestamp(state, rng, event_kind)

    _prune_windows(state, event_timestamp)
    user_average_amount = state.running_amount_sum / state.running_amount_count if state.running_amount_count > 0 else state.profile.baseline_amount

    if is_fraud and event_kind == "stealth":
        amount = _sample_profile_amount(rng, user_average_amount, 0.72, 1.22)
        merchant_risk_score = _sample_merchant_risk(rng, is_fraud=True, risky_merchant=False)
        is_device_trusted = bool(rng.random() < state.profile.trusted_device_rate)
        has_country_mismatch = bool(rng.random() < 0.08)
        ip_risk_score = _sample_ip_risk(rng, is_fraud=True, risky_ip=False)
        card_age_days = int(np.clip((event_timestamp - state.profile.card_creation_date).days, 1, 3650))
    else:
        amount = _sample_profile_amount(rng, user_average_amount, 0.55, 1.55)
        merchant_risk_score = _sample_merchant_risk(rng, is_fraud=is_fraud, risky_merchant=event_kind == "risky_merchant")
        trusted_probability = state.profile.trusted_device_rate
        if event_kind == "device_mismatch":
            trusted_probability *= 0.25
        is_device_trusted = bool(rng.random() < trusted_probability)
        mismatch_probability = 0.04 if not is_fraud else 0.22
        if event_kind == "country_mismatch":
            mismatch_probability = 0.82
        has_country_mismatch = bool(rng.random() < mismatch_probability)
        ip_risk_score = _sample_ip_risk(rng, is_fraud=is_fraud, risky_ip=event_kind == "risky_ip")
        if event_kind == "new_card":
            card_age_days = int(rng.integers(1, 120))
        else:
            card_age_days = int(np.clip((event_timestamp - state.profile.card_creation_date).days, 1, 3650))

    amount_to_average_ratio = float(amount / max(1.0, user_average_amount))
    seconds_since_last_transaction = int((event_timestamp - state.last_timestamp).total_seconds()) if state.last_timestamp is not None else int(30 * 24 * 3600)
    user_transaction_count_5m = int(len(state.recent_timestamps_5m))
    user_transaction_count_1h = int(len(state.recent_timestamps_1h))

    state.running_amount_sum += amount
    state.running_amount_count += 1
    state.last_timestamp = event_timestamp
    state.recent_timestamps_5m.append(event_timestamp)
    state.recent_timestamps_1h.append(event_timestamp)

    return {
        "transaction_id": _generate_transaction_id(),
        "amount": round(amount, 2),
        "user_average_amount": round(user_average_amount, 2),
        "user_transaction_count5_min": user_transaction_count_5m,
        "user_transaction_count1_hour": user_transaction_count_1h,
        "seconds_since_last_transaction": seconds_since_last_transaction,
        "merchant_risk_score": round(merchant_risk_score, 4),
        "is_device_trusted": bool(is_device_trusted),
        "has_country_mismatch": bool(has_country_mismatch),
        "amount_to_average_ratio": round(amount_to_average_ratio, 4),
        "hour_of_day": int(event_timestamp.hour),
        "ip_risk_score": round(ip_risk_score, 4),
        "card_age_days": int(card_age_days),
        "is_fraud": bool(is_fraud),
    }


def simulate(
    row_count: int,
    fraud_rate: float = 0.05,
    stealth_rate: float = 0.20,
    seed: int | None = None,
) -> pd.DataFrame:
    """Simulate synthetic transactions by deriving features from user event histories."""
    if row_count <= 0:
        return pd.DataFrame(columns=_OUTPUT_COLUMNS)

    rng = np.random.default_rng(seed)
    simulation_start = datetime(2026, 1, 1, 8, 0, 0)
    user_count = max(120, min(400, row_count // 40 + 120))
    profiles = _build_profiles(rng, user_count=user_count, simulation_start=simulation_start)
    states = {profile.user_id: UserState(profile=profile) for profile in profiles}

    for state in states.values():
        _seed_hidden_history(state, rng, simulation_start)

    activity_weights = _normalize_weights(np.array([profile.activity_weight for profile in profiles], dtype=float))
    rows: list[dict] = []

    for _ in range(row_count):
        selected_profile = profiles[int(rng.choice(len(profiles), p=activity_weights))]
        rows.append(_generate_row(states[selected_profile.user_id], rng, fraud_rate=fraud_rate, stealth_rate=stealth_rate))

    data = pd.DataFrame.from_records(rows, columns=_OUTPUT_COLUMNS)
    return data.sample(frac=1, random_state=seed).reset_index(drop=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate a synthetic fraud detection dataset.")
    parser.add_argument("num_rows", type=int, help="Number of rows to generate")
    parser.add_argument("--fraud-rate", type=float, default=0.05, help="Fraction of fraudulent rows (default: 0.05)")
    parser.add_argument("--stealth-rate", type=float, default=0.20, help="Fraction of fraud rows with no strong signals (default: 0.20)")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for reproducibility (default: 42)")
    parser.add_argument("--output", type=str, default="data/transactions.csv", help="Output CSV path (default: data/transactions.csv)")
    args = parser.parse_args()

    data = simulate(row_count=args.num_rows, fraud_rate=args.fraud_rate, stealth_rate=args.stealth_rate, seed=args.seed)

    output_directory = os.path.dirname(args.output)
    if output_directory:
        os.makedirs(output_directory, exist_ok=True)
    data.to_csv(args.output, index=False)
    print(f"Saved {len(data)} rows ({int(data['is_fraud'].sum())} fraud, {int((~data['is_fraud']).sum())} legitimate) to '{args.output}'")


if __name__ == "__main__":
    main()
