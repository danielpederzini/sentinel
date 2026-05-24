from __future__ import annotations

import argparse
import math
import os
import uuid
from collections import deque
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
from typing import Any

import numpy as np
import pandas as pd
from tqdm import tqdm

_MERCHANT_CATEGORIES = [
    "GROCERY", "RESTAURANT", "ENTERTAINMENT", "TRAVEL",
    "HEALTHCARE", "EDUCATION", "UTILITIES", "OTHER",
]
_CARD_TYPES = ["CREDIT", "DEBIT", "CREDIT_AND_DEBIT", "OTHER"]
_COUNTRIES = np.array(
    ["US", "BR", "AR", "DE", "ES", "SE", "NL", "GB", "CA", "JP",
     "AU", "MX", "CL", "ZA", "PT", "FR", "IT", "CH", "CN", "IN"],
    dtype=object,
)
_COUNTRY_WEIGHTS = np.array(
    [0.16, 0.12, 0.08, 0.10, 0.08, 0.09, 0.07, 0.07, 0.05, 0.05,
     0.04, 0.04, 0.03, 0.03, 0.02, 0.02, 0.02, 0.02, 0.02, 0.01],
    dtype=float,
)
_COUNTRY_WEIGHTS /= _COUNTRY_WEIGHTS.sum()

_FIVE_MINUTES = 5 * 60
_ONE_HOUR = 60 * 60
_COLD_START_SECONDS = 30 * 24 * 3600
_COLD_START_AVERAGE_AMOUNT = 100.0

_OUTPUT_COLUMNS = [
    "transaction_id",
    "user_id",
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
    "is_fraud",
]


class UserSegment(Enum):
    CASUAL = "casual"
    FREQUENT = "frequent"
    HIGH_VALUE = "high_value"
    TRAVELER = "traveler"
    BUSINESS = "business"


class Scenario(Enum):
    LEGITIMATE_ROUTINE = "LegitimateRoutine"
    LEGITIMATE_HIGH_VALUE = "LegitimateHighValue"
    FIRST_TRANSACTION_LARGE_PURCHASE = "FirstTransactionLargePurchase"
    ACCOUNT_TAKEOVER = "AccountTakeover"
    CARD_TESTING_THEN_LARGE_PURCHASE = "CardTestingThenLargePurchase"
    MERCHANT_COLLUSION = "MerchantCollusion"
    VELOCITY_BURST = "VelocityBurst"
    SYNTHETIC_IDENTITY = "SyntheticIdentity"
    STEALTH_FRAUD = "StealthFraud"


@dataclass(frozen=True)
class SegmentConfig:
    weight: float
    amount_mean: float
    amount_sigma: float
    activity_weight: float
    trusted_device_rate: float
    travel_rate: float
    account_age_days: tuple[int, int]
    card_age_days: tuple[int, int]
    preferred_merchants: tuple[int, int]
    hour_profile: str


@dataclass(frozen=True)
class QualityConfig:
    missing_rate: float = 0.01
    outlier_rate: float = 0.002
    noise_scale: float = 0.01


@dataclass(frozen=True)
class ValidationConfig:
    min_avg_feature_overlap: float = 0.55
    max_single_feature_auc: float = 0.78
    min_large_cold_start_fraud_lift: float = 2.0


@dataclass(frozen=True)
class GeneratorConfig:
    fraud_rate: float = 0.05
    stealth_rate: float = 0.08
    cold_start_rate: float = 0.16
    burst_budget_rate: float = 0.04
    quality: QualityConfig = QualityConfig()
    validation: ValidationConfig = ValidationConfig()


@dataclass(frozen=True)
class MerchantRecord:
    risk_score: float
    category: str


@dataclass(frozen=True)
class IpRecord:
    risk_score: float
    fraud_ring: bool


@dataclass
class UserProfile:
    user_id: str
    segment: UserSegment
    home_country: str
    baseline_amount: float
    activity_weight: float
    trusted_device_rate: float
    travel_rate: float
    account_created_at: datetime
    card_created_at: datetime
    card_type: str
    home_ip: int
    roaming_ips: list[int]
    preferred_merchants: list[int]
    hour_profile: str


@dataclass
class UserState:
    profile: UserProfile
    running_amount_sum: float = 0.0
    running_amount_count: int = 0
    last_timestamp: datetime | None = None
    recent_timestamps_5m: deque[datetime] = field(default_factory=deque)
    recent_timestamps_1h: deque[datetime] = field(default_factory=deque)
    recent_amounts_1h: deque[float] = field(default_factory=deque)
    recent_merchants_1h: deque[int] = field(default_factory=deque)


_SEGMENTS: dict[UserSegment, SegmentConfig] = {
    UserSegment.CASUAL: SegmentConfig(0.35, 55, 0.70, 0.7, 0.93, 0.04, (30, 900), (30, 1600), (2, 5), "mixed"),
    UserSegment.FREQUENT: SegmentConfig(0.25, 120, 0.60, 1.4, 0.94, 0.06, (90, 1200), (90, 2200), (4, 10), "mixed"),
    UserSegment.HIGH_VALUE: SegmentConfig(0.12, 900, 0.85, 0.9, 0.95, 0.09, (180, 1800), (180, 2600), (3, 7), "business"),
    UserSegment.TRAVELER: SegmentConfig(0.13, 180, 0.80, 1.0, 0.88, 0.20, (90, 1400), (90, 2200), (5, 12), "uniform"),
    UserSegment.BUSINESS: SegmentConfig(0.15, 360, 0.75, 1.6, 0.90, 0.12, (60, 1200), (60, 1800), (3, 8), "business"),
}


def _generate_transaction_id() -> str:
    return str(uuid.uuid4())


def _clip_amount(amount: float, upper: float = 1_250_000.0) -> float:
    return float(round(np.clip(amount, 0.01, upper), 2))


def _sample_lognormal(rng: np.random.Generator, mean: float, sigma: float) -> float:
    return float(rng.lognormal(mean=math.log(mean), sigma=sigma))


def _choice(rng: np.random.Generator, values: list[Any], weights: list[float] | None = None) -> Any:
    if weights is None:
        return values[int(rng.integers(0, len(values)))]
    probs = np.array(weights, dtype=float)
    probs /= probs.sum()
    return values[int(rng.choice(len(values), p=probs))]


def _build_ip_registry(rng: np.random.Generator, count: int = 1500) -> list[IpRecord]:
    records: list[IpRecord] = []
    for _ in range(count):
        tier = float(rng.random())
        if tier < 0.70:
            risk = float(rng.beta(1.4, 10.0))
        elif tier < 0.90:
            risk = float(rng.beta(3.0, 4.5))
        else:
            risk = float(rng.beta(6.5, 2.0))
        risk = float(np.clip(risk + rng.normal(0, 0.025), 0, 1))
        records.append(IpRecord(risk_score=risk, fraud_ring=risk > 0.62 and float(rng.random()) < 0.35))
    return records


def _build_merchant_registry(rng: np.random.Generator, count: int = 350) -> list[MerchantRecord]:
    records: list[MerchantRecord] = []
    for _ in range(count):
        tier = float(rng.random())
        if tier < 0.66:
            risk = float(rng.beta(1.5, 8.0))
            category = str(rng.choice(["GROCERY", "HEALTHCARE", "EDUCATION", "UTILITIES"]))
        elif tier < 0.88:
            risk = float(rng.beta(3.0, 4.0))
            category = str(rng.choice(["RESTAURANT", "OTHER"]))
        else:
            risk = float(rng.beta(5.0, 2.3))
            category = str(rng.choice(["ENTERTAINMENT", "TRAVEL"]))
        records.append(MerchantRecord(risk_score=float(np.clip(risk, 0, 1)), category=category))
    return records


def _build_profiles(
    rng: np.random.Generator,
    count: int,
    start: datetime,
    ips: list[IpRecord],
    merchants: list[MerchantRecord],
) -> list[UserProfile]:
    segments = list(_SEGMENTS)
    weights = [_SEGMENTS[s].weight for s in segments]
    profiles: list[UserProfile] = []
    low_risk_ips = [i for i, ip in enumerate(ips) if ip.risk_score < 0.35] or list(range(len(ips)))
    low_mid_merchants = [i for i, m in enumerate(merchants) if m.risk_score < 0.55] or list(range(len(merchants)))

    for _ in range(count):
        segment = _choice(rng, segments, weights)
        cfg = _SEGMENTS[segment]
        baseline = _clip_amount(_sample_lognormal(rng, cfg.amount_mean, cfg.amount_sigma), upper=80_000)
        account_age = int(rng.integers(cfg.account_age_days[0], cfg.account_age_days[1] + 1))
        card_age = int(rng.integers(cfg.card_age_days[0], cfg.card_age_days[1] + 1))
        preferred_count = int(rng.integers(cfg.preferred_merchants[0], cfg.preferred_merchants[1] + 1))
        profiles.append(UserProfile(
            user_id=str(uuid.uuid4()),
            segment=segment,
            home_country=str(rng.choice(_COUNTRIES, p=_COUNTRY_WEIGHTS)),
            baseline_amount=baseline,
            activity_weight=cfg.activity_weight * float(rng.uniform(0.65, 1.35)),
            trusted_device_rate=float(np.clip(cfg.trusted_device_rate + rng.normal(0, 0.03), 0.65, 0.98)),
            travel_rate=cfg.travel_rate,
            account_created_at=start - timedelta(days=account_age),
            card_created_at=start - timedelta(days=max(1, min(card_age, account_age + int(rng.integers(15, 365))))),
            card_type=str(rng.choice(_CARD_TYPES, p=[0.54, 0.29, 0.12, 0.05])),
            home_ip=int(rng.choice(low_risk_ips)),
            roaming_ips=[int(x) for x in rng.choice(len(ips), size=3, replace=False)],
            preferred_merchants=[int(x) for x in rng.choice(low_mid_merchants, size=preferred_count, replace=False)],
            hour_profile=cfg.hour_profile,
        ))
    return profiles


def _seed_history(state: UserState, rng: np.random.Generator, start: datetime) -> None:
    profile = state.profile
    seed_count = int(np.clip(rng.poisson(6 * profile.activity_weight), 0, 30))
    for _ in range(seed_count):
        amount = _clip_amount(_sample_lognormal(rng, profile.baseline_amount, 0.35))
        state.running_amount_sum += amount
        state.running_amount_count += 1
    if seed_count:
        state.last_timestamp = start - timedelta(seconds=int(rng.integers(2 * 3600, 10 * 24 * 3600)))


def _prune_windows(state: UserState, ts: datetime) -> None:
    while state.recent_timestamps_5m and (ts - state.recent_timestamps_5m[0]).total_seconds() > _FIVE_MINUTES:
        state.recent_timestamps_5m.popleft()
    while state.recent_timestamps_1h and (ts - state.recent_timestamps_1h[0]).total_seconds() > _ONE_HOUR:
        state.recent_timestamps_1h.popleft()
        state.recent_amounts_1h.popleft()
        state.recent_merchants_1h.popleft()


def _hour(rng: np.random.Generator, profile: str, night_bias: bool = False) -> int:
    if night_bias and float(rng.random()) < 0.75:
        return int(rng.choice([0, 1, 2, 3, 4, 5, 22, 23]))
    if profile == "business":
        weights = np.array([1, 1, 1, 1, 1, 2, 3, 5, 8, 10, 10, 9, 8, 8, 9, 10, 9, 7, 5, 4, 3, 2, 1, 1], dtype=float)
    elif profile == "uniform":
        weights = np.ones(24, dtype=float)
    else:
        weights = np.array([2, 1, 1, 1, 1, 2, 3, 5, 7, 8, 8, 7, 7, 6, 6, 6, 7, 8, 8, 7, 6, 5, 4, 3], dtype=float)
    weights /= weights.sum()
    return int(rng.choice(24, p=weights))


def _choose_scenario(state: UserState, rng: np.random.Generator, cfg: GeneratorConfig) -> Scenario:
    no_history = state.running_amount_count == 0
    if no_history:
        return _choice(
            rng,
            [
                Scenario.FIRST_TRANSACTION_LARGE_PURCHASE,
                Scenario.SYNTHETIC_IDENTITY,
                Scenario.LEGITIMATE_ROUTINE,
                Scenario.LEGITIMATE_HIGH_VALUE,
            ],
            [0.06, 0.04, 0.65, 0.25],
        )

    if float(rng.random()) >= cfg.fraud_rate:
        if state.profile.segment in (UserSegment.HIGH_VALUE, UserSegment.BUSINESS) and float(rng.random()) < 0.18:
            return Scenario.LEGITIMATE_HIGH_VALUE
        return Scenario.LEGITIMATE_ROUTINE

    fraud_scenarios = [
        Scenario.ACCOUNT_TAKEOVER,
        Scenario.CARD_TESTING_THEN_LARGE_PURCHASE,
        Scenario.MERCHANT_COLLUSION,
        Scenario.VELOCITY_BURST,
        Scenario.SYNTHETIC_IDENTITY,
        Scenario.STEALTH_FRAUD,
        Scenario.FIRST_TRANSACTION_LARGE_PURCHASE,
    ]
    weights = [0.25, 0.14, 0.14, 0.18, 0.10, cfg.stealth_rate, 0.09]
    return _choice(rng, fraud_scenarios, weights)


def _scenario_is_fraud(scenario: Scenario) -> bool:
    return scenario not in {Scenario.LEGITIMATE_ROUTINE, Scenario.LEGITIMATE_HIGH_VALUE}


def _pick_ip(rng: np.random.Generator, profile: UserProfile, scenario: Scenario, ips: list[IpRecord]) -> int:
    risky = [i for i, ip in enumerate(ips) if ip.risk_score > 0.65 or ip.fraud_ring]
    if scenario == Scenario.FIRST_TRANSACTION_LARGE_PURCHASE:
        if risky and float(rng.random()) < 0.45:
            return int(rng.choice(risky))
    if scenario in {Scenario.ACCOUNT_TAKEOVER, Scenario.SYNTHETIC_IDENTITY}:
        if risky and float(rng.random()) < 0.70:
            return int(rng.choice(risky))
    if scenario == Scenario.STEALTH_FRAUD and float(rng.random()) < 0.55:
        return int(rng.choice(profile.roaming_ips))
    if float(rng.random()) < 0.88:
        return profile.home_ip
    return int(rng.choice(profile.roaming_ips))


def _pick_merchant(rng: np.random.Generator, profile: UserProfile, scenario: Scenario, merchants: list[MerchantRecord]) -> int:
    risky = [i for i, m in enumerate(merchants) if m.risk_score > 0.60]
    if scenario == Scenario.FIRST_TRANSACTION_LARGE_PURCHASE and risky:
        if float(rng.random()) < 0.45:
            return int(rng.choice(risky))
    if scenario == Scenario.MERCHANT_COLLUSION and risky:
        if float(rng.random()) < 0.78:
            return int(rng.choice(risky))
    if scenario == Scenario.ACCOUNT_TAKEOVER and float(rng.random()) < 0.55 and risky:
        return int(rng.choice(risky))
    if scenario == Scenario.VELOCITY_BURST and float(rng.random()) < 0.35 and risky:
        return int(rng.choice(risky))
    if float(rng.random()) < 0.82:
        return int(rng.choice(profile.preferred_merchants))
    return int(rng.integers(0, len(merchants)))


def _gap_seconds(rng: np.random.Generator, profile: UserProfile, scenario: Scenario, no_history: bool) -> int:
    if no_history:
        return _COLD_START_SECONDS
    if scenario == Scenario.VELOCITY_BURST:
        return int(rng.integers(8, 110))
    if scenario == Scenario.CARD_TESTING_THEN_LARGE_PURCHASE:
        return int(rng.integers(30, 180))
    if _scenario_is_fraud(scenario):
        return int(np.clip(rng.lognormal(mean=math.log(25 * 60), sigma=0.85), 60, 18 * 3600))
    if float(rng.random()) < 0.02:
        return int(rng.integers(120, 900))
    cadence = 60 * 60 * float(np.clip(rng.lognormal(mean=math.log(14), sigma=0.7), 1.5, 72.0))
    cadence /= max(0.35, profile.activity_weight)
    return int(np.clip(cadence, 2 * 60, 9 * 24 * 3600))


def _amount_for_scenario(
    rng: np.random.Generator,
    baseline: float,
    profile: UserProfile,
    scenario: Scenario,
    no_history: bool,
) -> float:
    if scenario == Scenario.FIRST_TRANSACTION_LARGE_PURCHASE:
        if float(rng.random()) < 0.03:
            return _clip_amount(_sample_lognormal(rng, max(50_000.0, baseline * 35), 0.75))
        floor = max(3_000.0, baseline * 6)
        return _clip_amount(_sample_lognormal(rng, floor, 0.65), upper=250_000)
    if scenario == Scenario.LEGITIMATE_HIGH_VALUE:
        if no_history:
            if float(rng.random()) < 0.08:
                return _clip_amount(_sample_lognormal(rng, 750_000.0, 0.45), upper=1_500_000)
            return _clip_amount(_sample_lognormal(rng, max(25_000.0, baseline * 15), 0.70), upper=500_000)
        multiplier = float(rng.uniform(2.0, 10.0 if profile.segment == UserSegment.HIGH_VALUE else 5.5))
        return _clip_amount(_sample_lognormal(rng, baseline * multiplier, 0.45), upper=500_000)
    if scenario == Scenario.CARD_TESTING_THEN_LARGE_PURCHASE:
        return _clip_amount(_sample_lognormal(rng, max(1_500, baseline * 6), 0.65))
    if scenario == Scenario.ACCOUNT_TAKEOVER:
        return _clip_amount(_sample_lognormal(rng, max(500, baseline * float(rng.uniform(4.0, 14.0))), 0.50))
    if scenario == Scenario.MERCHANT_COLLUSION:
        return _clip_amount(_sample_lognormal(rng, max(350, baseline * float(rng.uniform(2.0, 6.0))), 0.55))
    if scenario == Scenario.VELOCITY_BURST:
        return _clip_amount(_sample_lognormal(rng, max(40, baseline * float(rng.uniform(0.45, 2.2))), 0.50))
    if scenario == Scenario.SYNTHETIC_IDENTITY:
        return _clip_amount(_sample_lognormal(rng, max(800, baseline * float(rng.uniform(3.0, 10.0))), 0.70))
    if scenario == Scenario.STEALTH_FRAUD:
        return _clip_amount(_sample_lognormal(rng, baseline * float(rng.uniform(0.75, 2.6)), 0.40))
    amount = _sample_lognormal(rng, baseline, 0.42)
    if float(rng.random()) < 0.04:
        amount *= float(rng.uniform(1.5, 2.5))
    return _clip_amount(amount)


def _materialize_row(
    state: UserState,
    rng: np.random.Generator,
    scenario: Scenario,
    ips: list[IpRecord],
    merchants: list[MerchantRecord],
    simulation_start: datetime,
) -> dict[str, Any]:
    profile = state.profile
    is_fraud = _scenario_is_fraud(scenario)
    historical_count = state.running_amount_count
    no_history = historical_count == 0
    behavior_baseline = state.running_amount_sum / historical_count if historical_count else profile.baseline_amount
    feature_average = state.running_amount_sum / historical_count if historical_count else _COLD_START_AVERAGE_AMOUNT

    base_ts = state.last_timestamp or simulation_start
    event_ts = base_ts + timedelta(seconds=_gap_seconds(rng, profile, scenario, no_history))
    hour = _hour(rng, profile.hour_profile, night_bias=scenario in {Scenario.ACCOUNT_TAKEOVER, Scenario.STEALTH_FRAUD})
    event_ts = event_ts.replace(hour=hour, minute=int(rng.integers(0, 60)), second=int(rng.integers(0, 60)), microsecond=0)
    while state.last_timestamp is not None and event_ts <= state.last_timestamp:
        event_ts += timedelta(days=1)
    _prune_windows(state, event_ts)

    ip_idx = _pick_ip(rng, profile, scenario, ips)
    merchant_idx = _pick_merchant(rng, profile, scenario, merchants)
    ip_risk = float(round(np.clip(ips[ip_idx].risk_score + rng.normal(0, 0.035), 0, 1), 4))
    merchant_risk = float(round(np.clip(merchants[merchant_idx].risk_score + rng.normal(0, 0.025), 0, 1), 4))

    amount = _amount_for_scenario(rng, behavior_baseline, profile, scenario, no_history)
    if scenario == Scenario.LEGITIMATE_ROUTINE and merchants[merchant_idx].category in {"TRAVEL", "ENTERTAINMENT"}:
        amount = _clip_amount(amount * float(rng.uniform(1.05, 1.25)))

    if scenario == Scenario.ACCOUNT_TAKEOVER:
        is_device_trusted = bool(rng.random() < 0.20)
        has_country_mismatch = bool(rng.random() < 0.66)
    elif scenario == Scenario.FIRST_TRANSACTION_LARGE_PURCHASE:
        is_device_trusted = bool(rng.random() < 0.45)
        has_country_mismatch = bool(rng.random() < 0.30)
    elif scenario == Scenario.SYNTHETIC_IDENTITY:
        is_device_trusted = bool(rng.random() < 0.22)
        has_country_mismatch = bool(rng.random() < 0.42)
    elif scenario == Scenario.STEALTH_FRAUD:
        is_device_trusted = bool(rng.random() < 0.52)
        has_country_mismatch = bool(rng.random() < 0.28)
    elif scenario == Scenario.VELOCITY_BURST:
        is_device_trusted = bool(rng.random() < 0.55)
        has_country_mismatch = bool(rng.random() < 0.22)
    elif scenario == Scenario.MERCHANT_COLLUSION:
        is_device_trusted = bool(rng.random() < 0.60)
        has_country_mismatch = bool(rng.random() < profile.travel_rate)
    else:
        is_device_trusted = bool(rng.random() < profile.trusted_device_rate)
        has_country_mismatch = bool(rng.random() < profile.travel_rate)

    seconds_since_last = (
        int((event_ts - state.last_timestamp).total_seconds())
        if state.last_timestamp is not None
        else _COLD_START_SECONDS
    )
    transaction_count_5m = len(state.recent_timestamps_5m)
    transaction_count_1h = len(state.recent_timestamps_1h)
    amount_velocity_1hour = round(float(sum(state.recent_amounts_1h)) + amount, 2)
    distinct_merchants = len(set(state.recent_merchants_1h) | {merchant_idx})
    amount_to_average_ratio = amount / max(1.0, feature_average)
    card_age_days = max(1, int((event_ts - profile.card_created_at).days))
    account_age_days = max(1, int((event_ts - profile.account_created_at).days))

    row = {
        "transaction_id": _generate_transaction_id(),
        "user_id": profile.user_id,
        "amount": round(amount, 2),
        "user_average_amount": round(feature_average, 2),
        "user_historical_transaction_count": int(historical_count),
        "user_transaction_count_5min": int(transaction_count_5m),
        "user_transaction_count_1hour": int(transaction_count_1h),
        "seconds_since_last_transaction": int(seconds_since_last),
        "amount_velocity_1hour": amount_velocity_1hour,
        "merchant_risk_score": merchant_risk,
        "is_device_trusted": bool(is_device_trusted),
        "has_country_mismatch": bool(has_country_mismatch),
        "amount_to_average_ratio": round(float(amount_to_average_ratio), 4),
        "hour_of_day": int(hour),
        "ip_risk_score": ip_risk,
        "card_age_days": int(card_age_days),
        "user_account_age_days": int(account_age_days),
        "day_of_week": int(event_ts.isoweekday()),
        "merchant_category": merchants[merchant_idx].category,
        "card_type": profile.card_type,
        "distinct_merchant_count_1hour": int(distinct_merchants),
        "is_fraud": bool(is_fraud),
        "_scenario": scenario.value,
    }

    state.running_amount_sum += amount
    state.running_amount_count += 1
    state.last_timestamp = event_ts
    state.recent_timestamps_5m.append(event_ts)
    state.recent_timestamps_1h.append(event_ts)
    state.recent_amounts_1h.append(amount)
    state.recent_merchants_1h.append(merchant_idx)
    return row


def _inject_data_quality_issues(df: pd.DataFrame, rng: np.random.Generator, cfg: QualityConfig) -> pd.DataFrame:
    df = df.copy()
    n = len(df)
    continuous_cols = [
        "amount", "user_average_amount", "merchant_risk_score", "ip_risk_score",
        "amount_to_average_ratio", "amount_velocity_1hour",
    ]
    integer_cols = [
        "user_transaction_count_5min", "user_transaction_count_1hour",
        "seconds_since_last_transaction", "card_age_days", "user_account_age_days",
        "user_historical_transaction_count",
    ]

    for col in continuous_cols + integer_cols:
        mask = rng.random(n) < cfg.missing_rate
        if mask.any():
            df.loc[mask, col] = np.nan

    for col in continuous_cols:
        mask = rng.random(n) < cfg.outlier_rate
        if mask.any():
            valid = df[col].dropna()
            if len(valid):
                df.loc[mask, col] = valid.median() * rng.uniform(0.1, 8.0, size=int(mask.sum()))

    for col in continuous_cols:
        valid_mask = df[col].notna()
        if valid_mask.any():
            df.loc[valid_mask, col] = df.loc[valid_mask, col].astype(float) * (
                1.0 + rng.normal(0, cfg.noise_scale, size=int(valid_mask.sum()))
            )

    for col in ["amount", "user_average_amount", "amount_velocity_1hour", "card_age_days", "seconds_since_last_transaction"]:
        valid = df[col].notna()
        if valid.any():
            df.loc[valid, col] = df.loc[valid, col].clip(lower=0)
    for col in ["merchant_risk_score", "ip_risk_score"]:
        valid = df[col].notna()
        if valid.any():
            df.loc[valid, col] = df.loc[valid, col].clip(lower=0, upper=1)
    return df


def simulate(
    row_count: int,
    fraud_rate: float = 0.05,
    stealth_rate: float = 0.08,
    seed: int | None = None,
    inject_quality_issues: bool = True,
) -> pd.DataFrame:
    if row_count <= 0:
        return pd.DataFrame(columns=_OUTPUT_COLUMNS)

    cfg = GeneratorConfig(fraud_rate=fraud_rate, stealth_rate=stealth_rate)
    rng = np.random.default_rng(seed)
    simulation_start = datetime(2026, 1, 1, 8, 0, 0)
    ips = _build_ip_registry(rng)
    merchants = _build_merchant_registry(rng)
    user_count = max(160, min(900, row_count // 35 + 120))
    profiles = _build_profiles(rng, user_count, simulation_start, ips, merchants)
    states = {p.user_id: UserState(profile=p) for p in profiles}
    for state in states.values():
        if float(rng.random()) > cfg.cold_start_rate:
            _seed_history(state, rng, simulation_start)

    activity = np.array([p.activity_weight for p in profiles], dtype=float)
    activity /= activity.sum()
    rows: list[dict[str, Any]] = []
    burst_queue: list[str] = []
    max_burst_rows = int(row_count * cfg.burst_budget_rate)
    burst_rows = 0
    target_fraud_rows = max(1, int(row_count * fraud_rate))
    fraud_rows = 0

    for _ in tqdm(range(row_count), desc="Generating transactions", unit="tx"):
        from_burst_queue = False
        if burst_queue:
            user_id = burst_queue.pop(0)
            state = states[user_id]
            scenario = Scenario.VELOCITY_BURST
            burst_rows += 1
            from_burst_queue = True
        else:
            profile = profiles[int(rng.choice(len(profiles), p=activity))]
            state = states[profile.user_id]
            scenario = _choose_scenario(state, rng, cfg)
            if fraud_rows >= target_fraud_rows and _scenario_is_fraud(scenario):
                scenario = (
                    Scenario.LEGITIMATE_HIGH_VALUE
                    if state.profile.segment in (UserSegment.HIGH_VALUE, UserSegment.BUSINESS)
                    else Scenario.LEGITIMATE_ROUTINE
                )

        row = _materialize_row(state, rng, scenario, ips, merchants, simulation_start)
        rows.append(row)
        if row["is_fraud"]:
            fraud_rows += 1

        if scenario == Scenario.CARD_TESTING_THEN_LARGE_PURCHASE and fraud_rows < target_fraud_rows:
            probe = row.copy()
            probe["transaction_id"] = _generate_transaction_id()
            probe["amount"] = round(max(0.99, row["user_average_amount"] * float(rng.uniform(0.02, 0.08))), 2)
            probe["amount_to_average_ratio"] = round(probe["amount"] / max(1.0, row["user_average_amount"]), 4)
            probe["amount_velocity_1hour"] = round(float(probe["amount"]), 2)
            probe["is_fraud"] = True
            rows.append(probe)
            fraud_rows += 1

        if (
            scenario == Scenario.VELOCITY_BURST
            and not from_burst_queue
            and burst_rows < max_burst_rows
            and fraud_rows < target_fraud_rows
        ):
            for _ in range(min(int(rng.integers(2, 7)), max_burst_rows - burst_rows - len(burst_queue))):
                burst_queue.append(state.profile.user_id)

    data = pd.DataFrame.from_records(rows)
    scenario_counts = data.pop("_scenario").value_counts().to_dict() if "_scenario" in data else {}
    data = data[_OUTPUT_COLUMNS]
    if inject_quality_issues:
        data = _inject_data_quality_issues(data, rng, cfg.quality)
    data = data.sample(frac=1, random_state=seed).reset_index(drop=True)
    data.attrs["scenario_counts"] = scenario_counts
    return data


def compute_feature_overlap(df: pd.DataFrame) -> dict[str, float]:
    fraud = df[df["is_fraud"]]
    legit = df[~df["is_fraud"]]
    overlaps: dict[str, float] = {}
    continuous_cols = [
        "amount", "user_average_amount", "merchant_risk_score", "ip_risk_score",
        "amount_to_average_ratio", "card_age_days", "seconds_since_last_transaction",
        "user_historical_transaction_count",
    ]
    for col in continuous_cols:
        f_vals = fraud[col].dropna().values
        l_vals = legit[col].dropna().values
        if len(f_vals) == 0 or len(l_vals) == 0:
            overlaps[col] = 0.0
            continue
        combined = np.concatenate([f_vals, l_vals])
        bins = np.histogram_bin_edges(combined, bins=50)
        hist_f, _ = np.histogram(f_vals, bins=bins, density=True)
        hist_l, _ = np.histogram(l_vals, bins=bins, density=True)
        bin_w = float(bins[1] - bins[0]) if len(bins) > 1 else 1.0
        overlaps[col] = round(float(min(1.0, np.sum(np.minimum(hist_f, hist_l)) * bin_w)), 4)
    for col in ("is_device_trusted", "has_country_mismatch", "hour_of_day"):
        f_dist = fraud[col].value_counts(normalize=True)
        l_dist = legit[col].value_counts(normalize=True)
        vals = set(f_dist.index) | set(l_dist.index)
        overlaps[col] = round(float(sum(min(f_dist.get(v, 0), l_dist.get(v, 0)) for v in vals)), 4)
    return overlaps


def compute_single_feature_auc(df: pd.DataFrame) -> dict[str, float]:
    from sklearn.metrics import roc_auc_score

    y = df["is_fraud"].astype(int)
    skip = {"transaction_id", "user_id", "is_fraud", "merchant_category", "card_type"}
    aucs: dict[str, float] = {}
    for col in [c for c in _OUTPUT_COLUMNS if c not in skip]:
        vals = df[col].copy()
        if vals.dtype == bool or vals.dtype == object:
            vals = vals.astype(float)
        valid = vals.notna()
        if valid.sum() < 100:
            aucs[col] = 0.5
            continue
        try:
            auc = roc_auc_score(y[valid], vals[valid])
            aucs[col] = round(float(max(auc, 1 - auc)), 4)
        except ValueError:
            aucs[col] = 0.5
    return aucs


def validate_dataset(df: pd.DataFrame) -> dict[str, Any]:
    n_fraud = int(df["is_fraud"].sum())
    overlaps = compute_feature_overlap(df)
    aucs = compute_single_feature_auc(df)
    base_rate = float(df["is_fraud"].mean()) if len(df) else 0.0
    large_cold = (
        (df["amount"] >= 10_000)
        & (df["user_historical_transaction_count"].fillna(0) == 0)
    )
    large_cold_rate = float(df.loc[large_cold, "is_fraud"].mean()) if large_cold.any() else 0.0
    return {
        "total_rows": len(df),
        "fraud_count": n_fraud,
        "legit_count": len(df) - n_fraud,
        "fraud_rate": round(base_rate, 4),
        "feature_overlaps": overlaps,
        "single_feature_aucs": aucs,
        "avg_overlap": round(float(np.mean(list(overlaps.values()))) if overlaps else 0.0, 4),
        "max_single_feature_auc": max(aucs.values()) if aucs else 0.5,
        "large_cold_start_count": int(large_cold.sum()),
        "large_cold_start_fraud_rate": round(large_cold_rate, 4),
        "large_cold_start_lift": round(large_cold_rate / max(base_rate, 0.0001), 2),
    }


def plot_feature_distributions(df: pd.DataFrame, output_path: str = "feature_distributions.png") -> str:
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError:
        return "matplotlib not installed; skipping plot"

    fraud = df[df["is_fraud"]]
    legit = df[~df["is_fraud"]]
    plot_cols = [
        "amount", "merchant_risk_score", "ip_risk_score", "amount_to_average_ratio",
        "user_historical_transaction_count", "seconds_since_last_transaction",
        "is_device_trusted", "has_country_mismatch",
    ]
    fig, axes = plt.subplots(2, 4, figsize=(20, 10))
    for ax, col in zip(axes.flatten(), plot_cols):
        f_vals = fraud[col].dropna()
        l_vals = legit[col].dropna()
        if col in {"is_device_trusted", "has_country_mismatch"}:
            categories = sorted(set(f_vals) | set(l_vals))
            x = np.arange(len(categories))
            ax.bar(x - 0.18, [float((l_vals == c).mean()) for c in categories], 0.35, label="Legit")
            ax.bar(x + 0.18, [float((f_vals == c).mean()) for c in categories], 0.35, label="Fraud")
            ax.set_xticks(x)
            ax.set_xticklabels([str(c) for c in categories])
        else:
            combined = pd.concat([f_vals, l_vals])
            bins = np.histogram_bin_edges(combined, bins=50)
            ax.hist(l_vals, bins=bins, alpha=0.6, density=True, label="Legit")
            ax.hist(f_vals, bins=bins, alpha=0.6, density=True, label="Fraud")
        ax.set_title(col)
        ax.legend(fontsize=8)
    plt.tight_layout()
    out_dir = os.path.dirname(output_path)
    if out_dir:
        os.makedirs(out_dir, exist_ok=True)
    fig.savefig(output_path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    return output_path


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate a synthetic fraud detection dataset.")
    parser.add_argument("num_rows", type=int, help="Number of rows to generate")
    parser.add_argument("--fraud-rate", type=float, default=0.05)
    parser.add_argument("--stealth-rate", type=float, default=0.08)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--output", type=str, default="data/transactions.csv")
    parser.add_argument("--no-quality-issues", action="store_true", help="Disable data quality issue injection")
    parser.add_argument("--validate", action="store_true", help="Run validation metrics after generation")
    parser.add_argument("--plot", type=str, default=None, help="Save feature distribution plots to this path")
    args = parser.parse_args()

    data = simulate(
        row_count=args.num_rows,
        fraud_rate=args.fraud_rate,
        stealth_rate=args.stealth_rate,
        seed=args.seed,
        inject_quality_issues=not args.no_quality_issues,
    )

    output_directory = os.path.dirname(args.output)
    if output_directory:
        os.makedirs(output_directory, exist_ok=True)
    data.to_csv(args.output, index=False)
    print(f"Saved {len(data)} rows ({int(data['is_fraud'].sum())} fraud) to '{args.output}'")

    if args.validate:
        stats = validate_dataset(data)
        print("\nValidation Results")
        print(f"  Fraud rate:                 {stats['fraud_rate']:.4f}")
        print(f"  Avg feature overlap:        {stats['avg_overlap']:.4f}")
        print(f"  Max single-feature AUC:     {stats['max_single_feature_auc']:.4f}")
        print(f"  Large cold-start count:     {stats['large_cold_start_count']}")
        print(f"  Large cold-start fraud:     {stats['large_cold_start_fraud_rate']:.4f}")
        print(f"  Large cold-start lift:      {stats['large_cold_start_lift']:.2f}x")
        print("\n  Top single-feature AUCs:")
        for feat, auc_val in sorted(stats["single_feature_aucs"].items(), key=lambda x: -x[1])[:10]:
            print(f"    {feat:38s} {auc_val:.4f}")

    if args.plot:
        print(f"\nFeature distribution plots saved to '{plot_feature_distributions(data, args.plot)}'")


if __name__ == "__main__":
    main()
