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

# ──────────────────────────────────────────────────────────────────────────────
# Output schema (backward-compatible with training_pipeline.py)
# ──────────────────────────────────────────────────────────────────────────────

_OUTPUT_COLUMNS = [
    "transaction_id",
    "user_id",
    "amount",
    "user_average_amount",
    "user_transaction_count_5min",
    "user_transaction_count_1hour",
    "seconds_since_last_transaction",
    "amount_velocity_1h",
    "merchant_risk_score",
    "is_device_trusted",
    "has_country_mismatch",
    "amount_to_average_ratio",
    "hour_of_day",
    "ip_risk_score",
    "card_age_days",
    "is_fraud",
]

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

_N_IPS = 1500
_N_MERCHANTS = 350


# ──────────────────────────────────────────────────────────────────────────────
# User segments
# ──────────────────────────────────────────────────────────────────────────────

class UserSegment(Enum):
    CASUAL = "casual"
    FREQUENT = "frequent"
    HIGH_VALUE = "high_value"
    TRAVELER = "traveler"
    BUSINESS = "business"


@dataclass(frozen=True)
class SegmentConfig:
    weight: float
    baseline_amount_log_mean: float
    baseline_amount_log_sigma: float
    activity_shape: float
    activity_scale: float
    trusted_device_mean: float
    travel_rate: float
    card_age_log_mean: float
    card_age_log_sigma: float
    fraud_susceptibility: float
    preferred_merchant_count: tuple[int, int]
    hour_distribution: str


_SEGMENT_CONFIGS: dict[UserSegment, SegmentConfig] = {
    UserSegment.CASUAL: SegmentConfig(
        weight=0.35,
        baseline_amount_log_mean=math.log(55.0),
        baseline_amount_log_sigma=0.7,
        activity_shape=1.5, activity_scale=0.6,
        trusted_device_mean=0.92,
        travel_rate=0.04,
        card_age_log_mean=math.log(500.0), card_age_log_sigma=0.8,
        fraud_susceptibility=0.8,
        preferred_merchant_count=(2, 5),
        hour_distribution="mixed",
    ),
    UserSegment.FREQUENT: SegmentConfig(
        weight=0.25,
        baseline_amount_log_mean=math.log(120.0),
        baseline_amount_log_sigma=0.6,
        activity_shape=3.0, activity_scale=1.2,
        trusted_device_mean=0.94,
        travel_rate=0.06,
        card_age_log_mean=math.log(700.0), card_age_log_sigma=0.7,
        fraud_susceptibility=1.0,
        preferred_merchant_count=(4, 10),
        hour_distribution="mixed",
    ),
    UserSegment.HIGH_VALUE: SegmentConfig(
        weight=0.12,
        baseline_amount_log_mean=math.log(450.0),
        baseline_amount_log_sigma=0.9,
        activity_shape=2.0, activity_scale=0.8,
        trusted_device_mean=0.95,
        travel_rate=0.10,
        card_age_log_mean=math.log(1200.0), card_age_log_sigma=0.6,
        fraud_susceptibility=1.4,
        preferred_merchant_count=(3, 7),
        hour_distribution="business",
    ),
    UserSegment.TRAVELER: SegmentConfig(
        weight=0.13,
        baseline_amount_log_mean=math.log(180.0),
        baseline_amount_log_sigma=0.8,
        activity_shape=2.2, activity_scale=1.0,
        trusted_device_mean=0.88,
        travel_rate=0.18,
        card_age_log_mean=math.log(600.0), card_age_log_sigma=0.8,
        fraud_susceptibility=1.2,
        preferred_merchant_count=(5, 12),
        hour_distribution="uniform",
    ),
    UserSegment.BUSINESS: SegmentConfig(
        weight=0.15,
        baseline_amount_log_mean=math.log(320.0),
        baseline_amount_log_sigma=0.75,
        activity_shape=3.5, activity_scale=1.5,
        trusted_device_mean=0.90,
        travel_rate=0.12,
        card_age_log_mean=math.log(400.0), card_age_log_sigma=0.9,
        fraud_susceptibility=1.1,
        preferred_merchant_count=(3, 8),
        hour_distribution="business",
    ),
}


# ──────────────────────────────────────────────────────────────────────────────
# Registries — enhanced with temporal drift and merchant category tiers
# ──────────────────────────────────────────────────────────────────────────────

@dataclass
class IpRecord:
    true_risk: float
    is_fraud_ring: bool
    drift_rate: float


@dataclass
class MerchantRecord:
    risk_score: float
    category_risk_tier: int   # 0=low, 1=medium, 2=high
    drift_rate: float


def _build_ip_registry(rng: np.random.Generator, n: int) -> list[IpRecord]:
    records: list[IpRecord] = []
    for _ in range(n):
        tier = float(rng.random())
        if tier < 0.58:
            base = float(rng.beta(1.5, 9.0))
        elif tier < 0.82:
            base = float(rng.beta(2.8, 4.5))
        else:
            base = float(rng.beta(6.5, 2.0))
        true_risk = float(np.clip(base + rng.normal(0.0, 0.025), 0.0, 1.0))
        is_fraud_ring = base > 0.55 and float(rng.random()) < 0.28
        drift_rate = float(rng.normal(0.0, 0.002))
        records.append(IpRecord(true_risk=true_risk, is_fraud_ring=is_fraud_ring, drift_rate=drift_rate))
    return records


def _build_merchant_registry(rng: np.random.Generator, n: int) -> list[MerchantRecord]:
    records: list[MerchantRecord] = []
    for _ in range(n):
        tier = float(rng.random())
        if tier < 0.67:
            base = float(rng.beta(1.5, 8.0))
            category_risk_tier = 0
        elif tier < 0.88:
            base = float(rng.beta(3.0, 4.0))
            category_risk_tier = 1
        else:
            base = float(rng.beta(5.0, 2.5))
            category_risk_tier = 2
        risk_score = float(np.clip(base + rng.normal(0.0, 0.025), 0.0, 1.0))
        drift_rate = float(rng.normal(0.0, 0.001))
        records.append(MerchantRecord(risk_score=risk_score, category_risk_tier=category_risk_tier, drift_rate=drift_rate))
    return records


# ──────────────────────────────────────────────────────────────────────────────
# Fraud signal framework
# ──────────────────────────────────────────────────────────────────────────────

class FraudSignal(Enum):
    HIGH_AMOUNT = "high_amount"
    RISKY_IP = "risky_ip"
    RISKY_MERCHANT = "risky_merchant"
    DEVICE_MISMATCH = "device_mismatch"
    COUNTRY_MISMATCH = "country_mismatch"
    NEW_CARD = "new_card"
    NIGHT_TIME = "night_time"
    BURST = "burst"


class FraudScenario(Enum):
    MULTI_SIGNAL = "multi_signal"
    ACCOUNT_TAKEOVER = "account_takeover"
    FRIENDLY_FRAUD = "friendly_fraud"
    MERCHANT_COLLUSION = "merchant_collusion"
    SYNTHETIC_IDENTITY = "synthetic_identity"
    STEALTH = "stealth"


_ALL_SIGNALS = list(FraudSignal)

_SIGNAL_BASE_PROBS: dict[FraudSignal, float] = {
    FraudSignal.HIGH_AMOUNT: 0.55,
    FraudSignal.RISKY_IP: 0.45,
    FraudSignal.RISKY_MERCHANT: 0.50,
    FraudSignal.DEVICE_MISMATCH: 0.40,
    FraudSignal.COUNTRY_MISMATCH: 0.35,
    FraudSignal.NEW_CARD: 0.25,
    FraudSignal.NIGHT_TIME: 0.30,
    FraudSignal.BURST: 0.35,
}

_SIGNAL_CORRELATIONS: dict[tuple[FraudSignal, FraudSignal], float] = {
    (FraudSignal.HIGH_AMOUNT, FraudSignal.RISKY_MERCHANT): 0.20,
    (FraudSignal.RISKY_IP, FraudSignal.DEVICE_MISMATCH): 0.15,
    (FraudSignal.COUNTRY_MISMATCH, FraudSignal.RISKY_IP): 0.18,
    (FraudSignal.BURST, FraudSignal.HIGH_AMOUNT): 0.12,
    (FraudSignal.NEW_CARD, FraudSignal.HIGH_AMOUNT): 0.15,
    (FraudSignal.NIGHT_TIME, FraudSignal.RISKY_IP): 0.10,
    (FraudSignal.DEVICE_MISMATCH, FraudSignal.COUNTRY_MISMATCH): 0.12,
}


def _sample_fraud_signals(
    rng: np.random.Generator,
    scenario: FraudScenario,
    segment: UserSegment,
    escalation_score: float,
) -> dict[FraudSignal, float]:
    signals: dict[FraudSignal, float] = {}

    if scenario == FraudScenario.STEALTH:
        n_signals = 1 if float(rng.random()) < 0.45 else 2
        order = rng.permutation(len(_ALL_SIGNALS))
        for idx in order[:n_signals]:
            signals[_ALL_SIGNALS[idx]] = float(rng.uniform(0.20, 0.40))
        return signals

    if scenario == FraudScenario.FRIENDLY_FRAUD:
        if float(rng.random()) < 0.30:
            options = [FraudSignal.HIGH_AMOUNT, FraudSignal.NIGHT_TIME]
            sig = options[int(rng.integers(0, len(options)))]
            signals[sig] = float(rng.uniform(0.05, 0.20))
        return signals

    if scenario == FraudScenario.ACCOUNT_TAKEOVER:
        n_signals = [1, 2, 3][int(rng.choice(3, p=[0.30, 0.45, 0.25]))]
        weights = np.array([0.10, 0.12, 0.10, 0.25, 0.20, 0.08, 0.10, 0.05], dtype=float)
        weights /= weights.sum()
        chosen = rng.choice(len(_ALL_SIGNALS), size=n_signals, replace=False, p=weights)
        for idx in chosen:
            signals[_ALL_SIGNALS[int(idx)]] = float(rng.uniform(0.30, 0.70))
        return signals

    if scenario == FraudScenario.MERCHANT_COLLUSION:
        signals[FraudSignal.RISKY_MERCHANT] = float(rng.uniform(0.50, 0.85))
        if float(rng.random()) < 0.35:
            signals[FraudSignal.HIGH_AMOUNT] = float(rng.uniform(0.20, 0.50))
        if float(rng.random()) < 0.20:
            signals[FraudSignal.BURST] = float(rng.uniform(0.15, 0.40))
        return signals

    if scenario == FraudScenario.SYNTHETIC_IDENTITY:
        signals[FraudSignal.NEW_CARD] = float(rng.uniform(0.60, 0.95))
        signals[FraudSignal.BURST] = float(rng.uniform(0.30, 0.70))
        if float(rng.random()) < 0.40:
            signals[FraudSignal.HIGH_AMOUNT] = float(rng.uniform(0.25, 0.60))
        return signals

    # MULTI_SIGNAL: 2-4 concurrent signals with correlation structure
    target_count = [2, 3, 4][int(rng.choice(3, p=[0.30, 0.45, 0.25]))]
    target_count = min(target_count + (1 if escalation_score > 0.5 else 0), len(_ALL_SIGNALS))

    adjusted_probs = dict(_SIGNAL_BASE_PROBS)
    if segment == UserSegment.HIGH_VALUE:
        adjusted_probs[FraudSignal.HIGH_AMOUNT] *= 1.3
    if segment == UserSegment.TRAVELER:
        adjusted_probs[FraudSignal.COUNTRY_MISMATCH] *= 0.7
    if segment == UserSegment.BUSINESS:
        adjusted_probs[FraudSignal.RISKY_MERCHANT] *= 1.2

    order = rng.permutation(len(_ALL_SIGNALS))
    for idx in order:
        if len(signals) >= target_count:
            break
        sig = _ALL_SIGNALS[idx]
        prob = adjusted_probs.get(sig, 0.3)
        for existing_sig in signals:
            boost = _SIGNAL_CORRELATIONS.get((existing_sig, sig), 0.0)
            boost += _SIGNAL_CORRELATIONS.get((sig, existing_sig), 0.0)
            prob = min(0.95, prob + boost)
        if float(rng.random()) < prob:
            strength = float(np.clip(rng.beta(3.0, 2.0), 0.2, 1.0))
            strength = min(1.0, strength + escalation_score * 0.15)
            signals[sig] = strength

    while len(signals) < 2:
        remaining = [s for s in _ALL_SIGNALS if s not in signals]
        pick = remaining[int(rng.integers(0, len(remaining)))]
        signals[pick] = float(rng.uniform(0.3, 0.7))

    return signals


def _choose_fraud_scenario(
    rng: np.random.Generator,
    segment: UserSegment,
    stealth_rate: float,
    account_age_days: int,
    escalation_score: float,
) -> FraudScenario:
    effective_stealth = stealth_rate * max(0.3, 1.0 - escalation_score)
    if float(rng.random()) < effective_stealth:
        return FraudScenario.STEALTH

    if account_age_days < 60 and float(rng.random()) < 0.25:
        return FraudScenario.SYNTHETIC_IDENTITY

    scenario_weights: dict[FraudScenario, float] = {
        FraudScenario.MULTI_SIGNAL: 0.45,
        FraudScenario.ACCOUNT_TAKEOVER: 0.20,
        FraudScenario.FRIENDLY_FRAUD: 0.12,
        FraudScenario.MERCHANT_COLLUSION: 0.13,
        FraudScenario.SYNTHETIC_IDENTITY: 0.10,
    }

    if segment == UserSegment.HIGH_VALUE:
        scenario_weights[FraudScenario.ACCOUNT_TAKEOVER] *= 1.5
    if segment == UserSegment.CASUAL:
        scenario_weights[FraudScenario.FRIENDLY_FRAUD] *= 1.3
    if segment == UserSegment.BUSINESS:
        scenario_weights[FraudScenario.MERCHANT_COLLUSION] *= 1.4

    scenarios = list(scenario_weights.keys())
    weights = np.array([scenario_weights[s] for s in scenarios], dtype=float)
    weights /= weights.sum()
    return scenarios[int(rng.choice(len(scenarios), p=weights))]


# ──────────────────────────────────────────────────────────────────────────────
# User profile & state
# ──────────────────────────────────────────────────────────────────────────────

@dataclass
class UserProfile:
    user_id: str
    segment: UserSegment
    home_country: str
    baseline_amount: float
    activity_weight: float
    trusted_device_rate: float
    card_creation_date: datetime
    home_ip_idx: int
    roaming_ip_indices: list[int]
    preferred_merchant_indices: list[int]
    travel_rate: float
    hour_preference: str


@dataclass
class UserState:
    profile: UserProfile
    running_amount_sum: float = 0.0
    running_amount_count: int = 0
    recent_timestamps_5m: deque = field(default_factory=deque)
    recent_timestamps_1h: deque = field(default_factory=deque)
    recent_amounts_1h: deque = field(default_factory=deque)
    last_timestamp: datetime | None = None
    transaction_count: int = 0
    escalation_score: float = 0.0
    last_amount: float = 0.0


# ──────────────────────────────────────────────────────────────────────────────
# Helpers
# ──────────────────────────────────────────────────────────────────────────────

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


def _sample_ip_risk(rng: np.random.Generator, ip_record: IpRecord, days_elapsed: float) -> float:
    drift = ip_record.drift_rate * days_elapsed
    noise = float(rng.normal(0.0, 0.04))
    return float(round(np.clip(ip_record.true_risk + drift + noise, 0.0, 1.0), 4))


def _sample_merchant_risk(rng: np.random.Generator, merchant_record: MerchantRecord, days_elapsed: float) -> float:
    drift = merchant_record.drift_rate * days_elapsed
    noise = float(rng.normal(0.0, 0.03))
    return float(round(np.clip(merchant_record.risk_score + drift + noise, 0.0, 1.0), 4))


# ──────────────────────────────────────────────────────────────────────────────
# Temporal patterns
# ──────────────────────────────────────────────────────────────────────────────

def _sample_hour_of_day(
    rng: np.random.Generator,
    preference: str,
    is_fraud: bool,
    signals: dict[FraudSignal, float],
) -> int:
    if FraudSignal.NIGHT_TIME in signals:
        strength = signals[FraudSignal.NIGHT_TIME]
        if float(rng.random()) < 0.5 + 0.4 * strength:
            night_hours = [0, 1, 2, 3, 4, 5, 22, 23]
            return night_hours[int(rng.integers(0, len(night_hours)))]

    if preference == "business":
        weights = np.array([1, 1, 1, 1, 1, 2, 3, 5, 8, 10, 10, 9, 8, 8, 9, 10, 9, 7, 5, 4, 3, 2, 1, 1], dtype=float)
    elif preference == "evening":
        weights = np.array([1, 1, 1, 1, 1, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 6, 8, 9, 10, 9, 7, 4, 2], dtype=float)
    elif preference == "uniform":
        weights = np.ones(24, dtype=float)
    else:
        weights = np.array([2, 1, 1, 1, 1, 2, 3, 5, 7, 8, 8, 7, 7, 6, 6, 6, 7, 8, 8, 7, 6, 5, 4, 3], dtype=float)

    if is_fraud and FraudSignal.NIGHT_TIME not in signals:
        night_boost = np.array(
            [1.4, 1.5, 1.5, 1.5, 1.4, 1.2, 1.0, 0.9, 0.9, 0.9, 0.9, 0.9,
             0.9, 0.9, 0.9, 0.9, 0.9, 1.0, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5],
            dtype=float,
        )
        weights *= night_boost

    weights /= weights.sum()
    return int(rng.choice(24, p=weights))


def _apply_day_of_week_effect(rng: np.random.Generator, base_gap: float, preference: str) -> float:
    day_factors = np.array([0.80, 0.85, 0.90, 0.95, 1.00, 1.30, 1.25], dtype=float)
    day_weights = np.full(7, 1.0 / 7)
    factor = float(day_factors[int(rng.choice(7, p=day_weights))])
    if preference == "business":
        return base_gap * factor
    return base_gap * float(rng.uniform(0.85, 1.15))


def _apply_seasonal_variation(rng: np.random.Generator, amount: float, day_of_year: int) -> float:
    if 305 <= day_of_year <= 365:
        amount *= float(rng.uniform(1.05, 1.35))
    elif day_of_year <= 31:
        amount *= float(rng.uniform(0.85, 0.98))
    elif 152 <= day_of_year <= 243:
        amount *= float(rng.uniform(1.00, 1.15))
    return amount


# ──────────────────────────────────────────────────────────────────────────────
# Feature generators (overlap-aware)
# ──────────────────────────────────────────────────────────────────────────────

def _sample_amount(
    rng: np.random.Generator,
    baseline: float,
    is_fraud: bool,
    signals: dict[FraudSignal, float],
    segment: UserSegment,
) -> float:
    if is_fraud and FraudSignal.HIGH_AMOUNT in signals:
        strength = signals[FraudSignal.HIGH_AMOUNT]
        low = 1.2 + 0.3 * strength
        high = 2.0 + 2.0 * strength
        multiplier = float(rng.uniform(low, high))
    elif is_fraud:
        multiplier = float(rng.uniform(0.7, 2.0))
    else:
        multiplier = float(rng.uniform(0.6, 1.6))
        if float(rng.random()) < 0.15:
            multiplier = float(rng.uniform(1.5, 2.5))
        if segment in (UserSegment.HIGH_VALUE, UserSegment.BUSINESS):
            multiplier *= float(rng.uniform(0.9, 1.4))

    noise = float(rng.normal(loc=1.0, scale=0.10))
    return _clip_amount(baseline * multiplier * max(0.25, noise))


def _sample_device_trusted(
    rng: np.random.Generator,
    base_rate: float,
    is_fraud: bool,
    signals: dict[FraudSignal, float],
) -> bool:
    if is_fraud and FraudSignal.DEVICE_MISMATCH in signals:
        strength = signals[FraudSignal.DEVICE_MISMATCH]
        prob = float(rng.uniform(0.3, 0.8 - 0.3 * strength))
    elif is_fraud:
        prob = base_rate * float(rng.uniform(0.6, 0.95))
    else:
        prob = float(np.clip(base_rate + rng.normal(0.0, 0.05), 0.6, 0.95))
    return bool(rng.random() < prob)


def _sample_country_mismatch(
    rng: np.random.Generator,
    travel_rate: float,
    is_fraud: bool,
    signals: dict[FraudSignal, float],
    segment: UserSegment,
) -> bool:
    if is_fraud and FraudSignal.COUNTRY_MISMATCH in signals:
        strength = signals[FraudSignal.COUNTRY_MISMATCH]
        prob = 0.40 + 0.45 * strength
    elif is_fraud:
        prob = 0.12 + float(rng.uniform(0.0, 0.10))
    else:
        prob = travel_rate
        if segment == UserSegment.TRAVELER:
            prob = float(rng.uniform(0.12, 0.22))
        elif segment == UserSegment.BUSINESS:
            prob = float(rng.uniform(0.08, 0.15))
    return bool(rng.random() < prob)


def _sample_card_age(
    rng: np.random.Generator,
    actual_card_age: int,
    is_fraud: bool,
    signals: dict[FraudSignal, float],
) -> int:
    if is_fraud and FraudSignal.NEW_CARD in signals:
        strength = signals[FraudSignal.NEW_CARD]
        max_age = int(120 - 80 * strength)
        return int(rng.integers(1, max(2, max_age)))
    if is_fraud:
        noise = float(rng.normal(0.0, 0.15))
        return int(np.clip(actual_card_age * (0.85 + noise), 1, 3650))
    noise = float(rng.normal(0.0, 0.05))
    return int(np.clip(actual_card_age * (1.0 + noise), 1, 3650))


# ──────────────────────────────────────────────────────────────────────────────
# IP and merchant selection
# ──────────────────────────────────────────────────────────────────────────────

def _pick_ip_index(
    rng: np.random.Generator,
    profile: UserProfile,
    is_fraud: bool,
    signals: dict[FraudSignal, float],
    fraud_ring_indices: list[int],
    n_ips: int,
) -> int:
    if is_fraud and FraudSignal.RISKY_IP in signals:
        strength = signals[FraudSignal.RISKY_IP]
        if fraud_ring_indices and float(rng.random()) < 0.3 + 0.4 * strength:
            return int(rng.choice(fraud_ring_indices))
        return int(rng.integers(0, n_ips))

    if is_fraud:
        if fraud_ring_indices and float(rng.random()) < 0.25:
            return int(rng.choice(fraud_ring_indices))
        return int(rng.integers(0, n_ips))

    home_prob = 0.55 if profile.segment == UserSegment.TRAVELER else 0.78
    if float(rng.random()) < home_prob:
        return profile.home_ip_idx

    if fraud_ring_indices and float(rng.random()) < 0.06:
        return int(rng.choice(fraud_ring_indices))

    if profile.roaming_ip_indices:
        return int(rng.choice(profile.roaming_ip_indices))
    return int(rng.integers(0, n_ips))


def _pick_merchant_index(
    rng: np.random.Generator,
    profile: UserProfile,
    is_fraud: bool,
    signals: dict[FraudSignal, float],
    high_risk_merchant_indices: list[int],
    n_merchants: int,
) -> int:
    if is_fraud and FraudSignal.RISKY_MERCHANT in signals:
        strength = signals[FraudSignal.RISKY_MERCHANT]
        if high_risk_merchant_indices and float(rng.random()) < 0.4 + 0.4 * strength:
            return int(rng.choice(high_risk_merchant_indices))
        return int(rng.integers(0, n_merchants))

    if is_fraud:
        if high_risk_merchant_indices and float(rng.random()) < 0.30:
            return int(rng.choice(high_risk_merchant_indices))
        return int(rng.integers(0, n_merchants))

    if float(rng.random()) < 0.62 and profile.preferred_merchant_indices:
        return int(rng.choice(profile.preferred_merchant_indices))

    if high_risk_merchant_indices and float(rng.random()) < 0.08:
        return int(rng.choice(high_risk_merchant_indices))

    return int(rng.integers(0, n_merchants))


# ──────────────────────────────────────────────────────────────────────────────
# Profile construction
# ──────────────────────────────────────────────────────────────────────────────

def _assign_segment(rng: np.random.Generator) -> UserSegment:
    segments = list(_SEGMENT_CONFIGS.keys())
    weights = np.array([_SEGMENT_CONFIGS[s].weight for s in segments], dtype=float)
    weights /= weights.sum()
    return segments[int(rng.choice(len(segments), p=weights))]


def _build_profiles(
    rng: np.random.Generator,
    user_count: int,
    simulation_start: datetime,
    ip_registry: list[IpRecord],
    merchant_registry: list[MerchantRecord],
    fraud_ring_indices: list[int],
) -> list[UserProfile]:
    n_ips = len(ip_registry)
    n_merchants = len(merchant_registry)
    profiles: list[UserProfile] = []

    for index in range(user_count):
        segment = _assign_segment(rng)
        cfg = _SEGMENT_CONFIGS[segment]

        home_country = str(rng.choice(_COUNTRIES, p=_COUNTRY_WEIGHTS))
        baseline_amount = float(np.clip(
            rng.lognormal(mean=cfg.baseline_amount_log_mean, sigma=cfg.baseline_amount_log_sigma),
            5.0, 5000.0,
        ))
        activity_weight = float(rng.gamma(shape=cfg.activity_shape, scale=cfg.activity_scale))
        trusted_device_rate = float(np.clip(
            rng.normal(loc=cfg.trusted_device_mean, scale=0.05), 0.60, 0.995,
        ))
        card_age_days = int(np.clip(
            rng.lognormal(mean=cfg.card_age_log_mean, sigma=cfg.card_age_log_sigma),
            7, 3650,
        ))

        if fraud_ring_indices and float(rng.random()) < 0.06:
            home_ip_idx = int(rng.choice(fraud_ring_indices))
        else:
            home_ip_idx = int(rng.integers(0, n_ips))

        n_roaming = int(rng.integers(2, 6))
        roaming_ip_indices = [int(rng.integers(0, n_ips)) for _ in range(n_roaming)]

        n_preferred = int(rng.integers(cfg.preferred_merchant_count[0], cfg.preferred_merchant_count[1] + 1))
        preferred_merchant_indices = [int(rng.integers(0, n_merchants)) for _ in range(n_preferred)]

        profiles.append(UserProfile(
            user_id=f"user-{index + 1:04d}",
            segment=segment,
            home_country=home_country,
            baseline_amount=baseline_amount,
            activity_weight=activity_weight,
            trusted_device_rate=trusted_device_rate,
            card_creation_date=simulation_start - timedelta(days=card_age_days),
            home_ip_idx=home_ip_idx,
            roaming_ip_indices=roaming_ip_indices,
            preferred_merchant_indices=preferred_merchant_indices,
            travel_rate=cfg.travel_rate,
            hour_preference=cfg.hour_distribution,
        ))
    return profiles


# ──────────────────────────────────────────────────────────────────────────────
# History seeding & window management
# ──────────────────────────────────────────────────────────────────────────────

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
        noise = max(0.25, float(rng.normal(1.0, 0.12)))
        amount = _clip_amount(state.profile.baseline_amount * float(rng.uniform(0.75, 1.15)) * noise)
        state.running_amount_sum += amount
        state.running_amount_count += 1
        state.last_timestamp = timestamp
        state.last_amount = amount
        state.recent_timestamps_1h.append(timestamp)
        state.recent_amounts_1h.append(amount)
        if timestamp >= simulation_start - timedelta(seconds=_FIVE_MINUTES):
            state.recent_timestamps_5m.append(timestamp)


def _prune_windows(state: UserState, current_timestamp: datetime) -> None:
    cutoff_5m = current_timestamp - timedelta(seconds=_FIVE_MINUTES)
    cutoff_1h = current_timestamp - timedelta(seconds=_ONE_HOUR)
    while state.recent_timestamps_5m and state.recent_timestamps_5m[0] < cutoff_5m:
        state.recent_timestamps_5m.popleft()
    while state.recent_timestamps_1h and state.recent_timestamps_1h[0] < cutoff_1h:
        state.recent_timestamps_1h.popleft()
        if state.recent_amounts_1h:
            state.recent_amounts_1h.popleft()


def _choose_event_kind(rng: np.random.Generator, fraud_rate: float, stealth_rate: float) -> tuple[bool, str]:
    if float(rng.random()) >= fraud_rate:
        return False, "legit"

    if float(rng.random()) < stealth_rate:
        return True, "stealth"

    fraud_kinds = np.array(["burst", "high_amount", "risky_merchant", "device_mismatch", "country_mismatch", "new_card", "night_time", "risky_ip"], dtype=object)
    fraud_weights = _normalize_weights(np.array([0.20, 0.18, 0.14, 0.13, 0.11, 0.10, 0.07, 0.07], dtype=float))
    return True, str(rng.choice(fraud_kinds, p=fraud_weights))


def _choose_gap_seconds(
    rng: np.random.Generator,
    is_fraud: bool,
    signals: dict[FraudSignal, float],
    activity_weight: float,
    hour_preference: str,
) -> int:
    if FraudSignal.BURST in signals:
        strength = signals[FraudSignal.BURST]
        max_gap = int(420 - 300 * strength)
        return int(rng.integers(15, max(20, max_gap)))

    if is_fraud:
        return int(np.clip(rng.lognormal(mean=math.log(45 * 60), sigma=1.2), 120, 36 * 3600))

    if float(rng.random()) < 0.12:
        return int(rng.integers(30, 600))

    cadence = 60.0 * 60.0 * float(np.clip(rng.lognormal(mean=math.log(14.0), sigma=0.7), 1.5, 48.0))
    cadence /= max(0.35, activity_weight)
    cadence = _apply_day_of_week_effect(rng, cadence, hour_preference)
    return int(np.clip(cadence, 2 * 60, 9 * 24 * 3600))


def _build_event_timestamp(
    state: UserState,
    rng: np.random.Generator,
    is_fraud: bool,
    signals: dict[FraudSignal, float],
) -> datetime:
    base_timestamp = state.last_timestamp or datetime(2026, 1, 1, 8, 0, 0)
    gap_seconds = _choose_gap_seconds(
        rng, is_fraud, signals, state.profile.activity_weight, state.profile.hour_preference,
    )
    return base_timestamp + timedelta(seconds=gap_seconds)


# ──────────────────────────────────────────────────────────────────────────────
# Row generation
# ──────────────────────────────────────────────────────────────────────────────

def _generate_row(
    state: UserState,
    rng: np.random.Generator,
    fraud_rate: float,
    stealth_rate: float,
    ip_registry: list[IpRecord],
    merchant_registry: list[MerchantRecord],
    fraud_ring_indices: list[int],
    high_risk_merchant_indices: list[int],
    simulation_start: datetime,
) -> dict[str, Any]:
    profile = state.profile
    is_fraud = float(rng.random()) < fraud_rate

    # Warmup: new accounts don't fraud immediately (except synthetic identity)
    if is_fraud and state.transaction_count < 3 and float(rng.random()) < 0.70:
        is_fraud = False

    if is_fraud:
        state.escalation_score = min(1.0, state.escalation_score + float(rng.uniform(0.02, 0.08)))

    if is_fraud:
        ref_ts = state.last_timestamp or (simulation_start - timedelta(days=1))
        account_age = max(1, int((ref_ts - profile.card_creation_date).total_seconds()) // 86400)
        scenario = _choose_fraud_scenario(rng, profile.segment, stealth_rate, account_age, state.escalation_score)
        signals = _sample_fraud_signals(rng, scenario, profile.segment, state.escalation_score)
    else:
        signals = {}

    event_timestamp = _build_event_timestamp(state, rng, is_fraud, signals)
    _prune_windows(state, event_timestamp)
    days_elapsed = (event_timestamp - simulation_start).total_seconds() / 86400.0

    user_average_amount = (
        state.running_amount_sum / state.running_amount_count
        if state.running_amount_count > 0
        else profile.baseline_amount
    )

    # IP & merchant
    ip_idx = _pick_ip_index(rng, profile, is_fraud, signals, fraud_ring_indices, len(ip_registry))
    merchant_idx = _pick_merchant_index(rng, profile, is_fraud, signals, high_risk_merchant_indices, len(merchant_registry))
    ip_risk_score = _sample_ip_risk(rng, ip_registry[ip_idx], days_elapsed)
    merchant_risk_score = _sample_merchant_risk(rng, merchant_registry[merchant_idx], days_elapsed)

    # Amount with overlap
    amount = _sample_amount(rng, user_average_amount, is_fraud, signals, profile.segment)

    # Correlation: high-risk merchant category → slightly higher legit amounts
    if not is_fraud and merchant_registry[merchant_idx].category_risk_tier == 2:
        amount = _clip_amount(amount * float(rng.uniform(1.05, 1.25)))

    # Seasonal variation
    day_of_year = event_timestamp.timetuple().tm_yday
    amount = _clip_amount(_apply_seasonal_variation(rng, amount, day_of_year))

    # Device trust
    is_device_trusted = _sample_device_trusted(rng, profile.trusted_device_rate, is_fraud, signals)

    # Country mismatch
    has_country_mismatch = _sample_country_mismatch(rng, profile.travel_rate, is_fraud, signals, profile.segment)

    # Card age
    actual_card_age = max(1, int((event_timestamp - profile.card_creation_date).days))
    card_age_days = _sample_card_age(rng, actual_card_age, is_fraud, signals)

    # Hour of day
    hour_of_day = _sample_hour_of_day(rng, profile.hour_preference, is_fraud, signals)
    event_timestamp = event_timestamp.replace(
        hour=hour_of_day,
        minute=int(rng.integers(0, 60)),
        second=int(rng.integers(0, 60)),
        microsecond=0,
    )

    amount_to_average_ratio = float(amount / max(1.0, user_average_amount))
    seconds_since_last = (
        int((event_timestamp - state.last_timestamp).total_seconds())
        if state.last_timestamp is not None
        else int(30 * 24 * 3600)
    )
    user_transaction_count_5m = int(len(state.recent_timestamps_5m))
    user_transaction_count_1h = int(len(state.recent_timestamps_1h))

    amount_velocity_1h = round(float(sum(state.recent_amounts_1h)) + amount, 2)

    # Update state
    state.running_amount_sum += amount
    state.running_amount_count += 1
    state.last_timestamp = event_timestamp
    state.last_amount = amount
    state.transaction_count += 1
    state.recent_timestamps_5m.append(event_timestamp)
    state.recent_timestamps_1h.append(event_timestamp)
    state.recent_amounts_1h.append(amount)

    return {
        "transaction_id": _generate_transaction_id(),
        "user_id": state.profile.user_id,
        "amount": round(amount, 2),
        "user_average_amount": round(user_average_amount, 2),
        "user_transaction_count_5min": user_transaction_count_5m,
        "user_transaction_count_1hour": user_transaction_count_1h,
        "seconds_since_last_transaction": seconds_since_last,
        "amount_velocity_1h": amount_velocity_1h,
        "merchant_risk_score": merchant_risk_score,
        "is_device_trusted": bool(is_device_trusted),
        "has_country_mismatch": bool(has_country_mismatch),
        "amount_to_average_ratio": round(amount_to_average_ratio, 4),
        "hour_of_day": hour_of_day,
        "ip_risk_score": ip_risk_score,
        "card_age_days": int(card_age_days),
        "is_fraud": bool(is_fraud),
    }


# ──────────────────────────────────────────────────────────────────────────────
# Data quality issues
# ──────────────────────────────────────────────────────────────────────────────

def _inject_data_quality_issues(
    df: pd.DataFrame,
    rng: np.random.Generator,
    missing_rate: float = 0.02,
    outlier_rate: float = 0.005,
    noise_scale: float = 0.03,
) -> pd.DataFrame:
    df = df.copy()
    n = len(df)

    continuous_cols = [
        "amount", "user_average_amount", "merchant_risk_score",
        "ip_risk_score", "amount_to_average_ratio",
    ]
    integer_cols = [
        "user_transaction_count_5min", "user_transaction_count_1hour",
        "seconds_since_last_transaction", "card_age_days", "hour_of_day",
    ]

    # Sparse NaN injection
    for col in continuous_cols + integer_cols:
        mask = rng.random(n) < missing_rate
        if mask.any():
            df.loc[mask, col] = np.nan

    # Outliers in continuous features
    for col in continuous_cols:
        mask = rng.random(n) < outlier_rate
        if mask.any():
            valid = df[col].dropna()
            if len(valid) > 0:
                col_std = float(valid.std())
                col_mean = float(valid.mean())
                count = int(mask.sum())
                signs = rng.choice([-1, 1], size=count).astype(float)
                magnitudes = rng.uniform(3, 6, size=count)
                df.loc[mask, col] = col_mean + signs * magnitudes * col_std

    # Measurement noise
    for col in continuous_cols:
        valid_mask = df[col].notna()
        if valid_mask.any():
            noise = rng.normal(0, noise_scale, size=int(valid_mask.sum()))
            df.loc[valid_mask, col] = df.loc[valid_mask, col].astype(float) * (1.0 + noise)

    # Data entry errors
    for col in ["card_age_days", "seconds_since_last_transaction"]:
        mask = rng.random(n) < 0.003
        if mask.any():
            valid = df[col].dropna()
            if len(valid) > 0:
                df.loc[mask, col] = rng.choice(valid.values, size=int(mask.sum()))

    # Clamp non-negative
    for col in ["amount", "user_average_amount", "merchant_risk_score",
                 "ip_risk_score", "card_age_days", "seconds_since_last_transaction"]:
        valid = df[col].notna()
        if valid.any():
            df.loc[valid, col] = df.loc[valid, col].clip(lower=0)

    for col in ["merchant_risk_score", "ip_risk_score"]:
        valid = df[col].notna()
        if valid.any():
            df.loc[valid, col] = df.loc[valid, col].clip(lower=0, upper=1)

    return df


# ──────────────────────────────────────────────────────────────────────────────
# Validation metrics
# ──────────────────────────────────────────────────────────────────────────────

def compute_feature_overlap(df: pd.DataFrame) -> dict[str, float]:
    fraud = df[df["is_fraud"]]
    legit = df[~df["is_fraud"]]
    overlaps: dict[str, float] = {}

    continuous_cols = [
        "amount", "user_average_amount", "merchant_risk_score", "ip_risk_score",
        "amount_to_average_ratio", "card_age_days", "seconds_since_last_transaction",
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
        bin_w = float(bins[1] - bins[0])
        overlaps[col] = round(float(min(1.0, np.sum(np.minimum(hist_f, hist_l)) * bin_w)), 4)

    for col in ("is_device_trusted", "has_country_mismatch", "hour_of_day"):
        if col not in df.columns:
            continue
        f_dist = fraud[col].value_counts(normalize=True)
        l_dist = legit[col].value_counts(normalize=True)
        all_vals = set(f_dist.index) | set(l_dist.index)
        overlaps[col] = round(float(sum(min(f_dist.get(v, 0), l_dist.get(v, 0)) for v in all_vals)), 4)

    return overlaps


def compute_single_feature_auc(df: pd.DataFrame) -> dict[str, float]:
    from sklearn.metrics import roc_auc_score

    y = df["is_fraud"].astype(int)
    feature_cols = [c for c in _OUTPUT_COLUMNS if c not in ("transaction_id", "is_fraud")]
    aucs: dict[str, float] = {}

    for col in feature_cols:
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

    avg_overlap = float(np.mean(list(overlaps.values()))) if overlaps else 0.0
    max_auc = max(aucs.values()) if aucs else 0.5

    return {
        "total_rows": len(df),
        "fraud_count": n_fraud,
        "legit_count": len(df) - n_fraud,
        "fraud_rate": round(n_fraud / max(1, len(df)), 4),
        "feature_overlaps": overlaps,
        "single_feature_aucs": aucs,
        "avg_overlap": round(avg_overlap, 4),
        "max_single_feature_auc": round(max_auc, 4),
        "overlap_target_met": avg_overlap >= 0.60,
        "auc_target_met": max_auc <= 0.70,
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
        "card_age_days", "hour_of_day", "seconds_since_last_transaction", "is_device_trusted",
    ]

    fig, axes = plt.subplots(2, 4, figsize=(20, 10))
    axes_flat = axes.flatten()

    for idx, col in enumerate(plot_cols):
        ax = axes_flat[idx]
        f_vals = fraud[col].dropna()
        l_vals = legit[col].dropna()

        if col in ("is_device_trusted", "has_country_mismatch"):
            categories = sorted(set(f_vals) | set(l_vals))
            x = np.arange(len(categories))
            width = 0.35
            f_counts = [float((f_vals == c).sum()) / max(1, len(f_vals)) for c in categories]
            l_counts = [float((l_vals == c).sum()) / max(1, len(l_vals)) for c in categories]
            ax.bar(x - width / 2, l_counts, width, label="Legit", alpha=0.7, color="steelblue")
            ax.bar(x + width / 2, f_counts, width, label="Fraud", alpha=0.7, color="firebrick")
            ax.set_xticks(x)
            ax.set_xticklabels([str(c) for c in categories])
        else:
            combined = pd.concat([f_vals, l_vals])
            bins = np.histogram_bin_edges(combined, bins=50)
            ax.hist(l_vals, bins=bins, alpha=0.6, density=True, label="Legit", color="steelblue")
            ax.hist(f_vals, bins=bins, alpha=0.6, density=True, label="Fraud", color="firebrick")

        ax.set_title(col, fontsize=10)
        ax.legend(fontsize=8)

    plt.suptitle("Feature Distributions: Fraud vs Legitimate", fontsize=14, y=1.02)
    plt.tight_layout()

    out_dir = os.path.dirname(output_path)
    if out_dir:
        os.makedirs(out_dir, exist_ok=True)
    fig.savefig(output_path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    return output_path


# ──────────────────────────────────────────────────────────────────────────────
# Main simulation API
# ──────────────────────────────────────────────────────────────────────────────

def simulate(
    row_count: int,
    fraud_rate: float = 0.05,
    stealth_rate: float = 0.38,
    seed: int | None = None,
    inject_quality_issues: bool = True,
) -> pd.DataFrame:
    """Simulate synthetic transactions with realistic overlapping feature distributions."""
    if row_count <= 0:
        return pd.DataFrame(columns=_OUTPUT_COLUMNS)

    rng = np.random.default_rng(seed)
    simulation_start = datetime(2026, 1, 1, 8, 0, 0)

    ip_registry = _build_ip_registry(rng, _N_IPS)
    merchant_registry = _build_merchant_registry(rng, _N_MERCHANTS)
    fraud_ring_indices = [i for i, ip in enumerate(ip_registry) if ip.is_fraud_ring]
    high_risk_merchant_indices = [i for i, m in enumerate(merchant_registry) if m.risk_score > 0.50]

    user_count = max(120, min(400, row_count // 40 + 120))
    profiles = _build_profiles(rng, user_count, simulation_start, ip_registry, merchant_registry, fraud_ring_indices)
    states = {p.user_id: UserState(profile=p) for p in profiles}

    for s in states.values():
        _seed_hidden_history(s, rng, simulation_start)

    activity_weights = _normalize_weights(np.array([p.activity_weight for p in profiles], dtype=float))
    rows: list[dict[str, Any]] = []

    for _ in tqdm(range(row_count), desc="Generating transactions", unit="tx"):
        selected_profile = profiles[int(rng.choice(len(profiles), p=activity_weights))]
        user_state = states[selected_profile.user_id]

        row = _generate_row(
            user_state, rng,
            fraud_rate=fraud_rate, stealth_rate=stealth_rate,
            ip_registry=ip_registry, merchant_registry=merchant_registry,
            fraud_ring_indices=fraud_ring_indices,
            high_risk_merchant_indices=high_risk_merchant_indices,
            simulation_start=simulation_start,
        )

        # Test-then-large: for certain fraud types, prepend a small probe transaction.
        # The probe mimics a real-world pattern where fraudsters validate a stolen card
        # with a tiny purchase before making a large one.
        if (
            row["is_fraud"]
            and user_state.transaction_count >= 2
            and float(rng.random()) < 0.22
        ):
            probe_amount = _clip_amount(
                user_state.profile.baseline_amount * float(rng.uniform(0.03, 0.12))
            )
            probe_ts = user_state.last_timestamp or datetime(2026, 1, 1, 8, 0, 0)
            probe_ts = probe_ts - timedelta(seconds=int(rng.integers(30, 180)))
            rows.append({
                "transaction_id": _generate_transaction_id(),
                "user_id": selected_profile.user_id,
                "amount": round(probe_amount, 2),
                "user_average_amount": row["user_average_amount"],
                "user_transaction_count_5min": max(0, row["user_transaction_count_5min"] - 1),
                "user_transaction_count_1hour": max(0, row["user_transaction_count_1hour"] - 1),
                "seconds_since_last_transaction": int(rng.integers(60, 600)),
                "amount_velocity_1h": round(probe_amount, 2),
                "merchant_risk_score": row["merchant_risk_score"],
                "is_device_trusted": row["is_device_trusted"],
                "has_country_mismatch": row["has_country_mismatch"],
                "amount_to_average_ratio": round(
                    probe_amount / max(1.0, row["user_average_amount"]), 4
                ),
                "hour_of_day": row["hour_of_day"],
                "ip_risk_score": row["ip_risk_score"],
                "card_age_days": row["card_age_days"],
                "is_fraud": True,
            })

        rows.append(row)

    data = pd.DataFrame.from_records(rows, columns=_OUTPUT_COLUMNS)

    if inject_quality_issues:
        data = _inject_data_quality_issues(data, rng)

    return data.sample(frac=1, random_state=seed).reset_index(drop=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate a synthetic fraud detection dataset.")
    parser.add_argument("num_rows", type=int, help="Number of rows to generate")
    parser.add_argument("--fraud-rate", type=float, default=0.05)
    parser.add_argument("--stealth-rate", type=float, default=0.38)
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
    print(f"Saved {len(data)} rows ({int(data['is_fraud'].sum())} fraud, {int((~data['is_fraud']).sum())} legitimate) to '{args.output}'")

    if args.validate:
        stats = validate_dataset(data)
        print(f"\n{'=' * 60}")
        print("Validation Results")
        print(f"{'=' * 60}")
        print(f"  Fraud rate:           {stats['fraud_rate']:.4f}")
        print(f"  Avg feature overlap:  {stats['avg_overlap']:.4f} (target: >=0.60)")
        print(f"  Max single-feat AUC:  {stats['max_single_feature_auc']:.4f} (target: <=0.70)")
        print(f"  Overlap target met:   {stats['overlap_target_met']}")
        print(f"  AUC target met:       {stats['auc_target_met']}")
        print(f"\n  Feature overlaps:")
        for feat, ov in sorted(stats["feature_overlaps"].items(), key=lambda x: x[1]):
            print(f"    {feat:35s} {ov:.4f}")
        print(f"\n  Single-feature AUCs:")
        for feat, auc_val in sorted(stats["single_feature_aucs"].items(), key=lambda x: -x[1]):
            print(f"    {feat:35s} {auc_val:.4f}")

    if args.plot:
        plot_path = plot_feature_distributions(data, args.plot)
        print(f"\nFeature distribution plots saved to '{plot_path}'")


if __name__ == "__main__":
    main()
