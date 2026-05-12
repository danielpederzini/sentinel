from __future__ import annotations

import argparse
import os
import uuid
from collections import deque
from dataclasses import dataclass, field
from datetime import datetime, timedelta

import numpy as np
import pandas as pd
from tqdm import tqdm

_OUTPUT_COLUMNS = [
    "transaction_id",
    "amount",
    "user_average_amount",
    "user_transaction_count_5min",
    "user_transaction_count_1hour",
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

_FIVE_MINUTES = 5 * 60
_ONE_HOUR = 60 * 60

_N_IPS = 1500
_N_MERCHANTS = 350


# ─── Registries ────────────────────────────────────────────────────────────────

@dataclass
class IpRecord:
    """Shared IP address with a continuous, pre-computed reputation score."""
    true_risk: float     # continuous [0, 1]
    is_fraud_ring: bool  # part of a known fraud network; may be shared with legit users


@dataclass
class MerchantRecord:
    """Merchant entity with a continuous risk score."""
    risk_score: float    # continuous [0, 1]


def _build_ip_registry(rng: np.random.Generator, n: int) -> list[IpRecord]:
    records = []
    for _ in range(n):
        tier = float(rng.random())
        if tier < 0.58:
            base = float(rng.beta(1.5, 9.0))   # clean: residential / corporate
        elif tier < 0.82:
            base = float(rng.beta(2.8, 4.5))   # mixed: VPNs, shared proxies, cloud NAT
        else:
            base = float(rng.beta(6.5, 2.0))   # tainted: known abuse, TOR exits, fraud datacenters
        true_risk = float(np.clip(base + rng.normal(0.0, 0.025), 0.0, 1.0))
        is_fraud_ring = base > 0.55 and float(rng.random()) < 0.28
        records.append(IpRecord(true_risk=true_risk, is_fraud_ring=is_fraud_ring))
    return records


def _build_merchant_registry(rng: np.random.Generator, n: int) -> list[MerchantRecord]:
    records = []
    for _ in range(n):
        tier = float(rng.random())
        if tier < 0.67:
            base = float(rng.beta(1.5, 8.0))   # grocery, utilities, subscriptions
        else:
            base = float(rng.beta(5.0, 2.5))   # travel, gaming, luxury, crypto exchanges
        risk_score = float(np.clip(base + rng.normal(0.0, 0.025), 0.0, 1.0))
        records.append(MerchantRecord(risk_score=risk_score))
    return records


# ─── User profile & state ──────────────────────────────────────────────────────

@dataclass
class UserProfile:
    user_id: str
    home_country: str
    baseline_amount: float
    activity_weight: float
    trusted_device_rate: float
    card_creation_date: datetime
    home_ip_idx: int
    roaming_ip_indices: list[int]
    preferred_merchant_indices: list[int]


@dataclass
class UserState:
    profile: UserProfile
    running_amount_sum: float = 0.0
    running_amount_count: int = 0
    recent_timestamps_5m: deque = field(default_factory=deque)
    recent_timestamps_1h: deque = field(default_factory=deque)
    last_timestamp: datetime | None = None


# ─── Helpers ───────────────────────────────────────────────────────────────────

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


def _sample_ip_risk(rng: np.random.Generator, ip_record: IpRecord) -> float:
    """IP risk score with per-transaction observation noise (simulates dynamic DB score)."""
    noise = float(rng.normal(0.0, 0.04))
    return float(round(np.clip(ip_record.true_risk + noise, 0.0, 1.0), 4))


def _sample_merchant_risk(rng: np.random.Generator, merchant_record: MerchantRecord) -> float:
    """Merchant risk with small noise (score can drift slightly over time)."""
    noise = float(rng.normal(0.0, 0.03))
    return float(round(np.clip(merchant_record.risk_score + noise, 0.0, 1.0), 4))


def _pick_ip_index(
    rng: np.random.Generator,
    profile: UserProfile,
    event_kind: str,
    is_fraud: bool,
    fraud_ring_indices: list[int],
    n_ips: int,
) -> int:
    if event_kind == "risky_ip" and fraud_ring_indices:
        return int(rng.choice(fraud_ring_indices))
    if is_fraud and event_kind != "stealth":
        # most non-stealth fraud originates from a tainted or previously unseen IP
        if fraud_ring_indices and float(rng.random()) < 0.55:
            return int(rng.choice(fraud_ring_indices))
        return int(rng.integers(0, n_ips))
    # legit and stealth: mostly home IP, occasionally a roaming one
    if float(rng.random()) < 0.82:
        return profile.home_ip_idx
    if profile.roaming_ip_indices:
        return int(rng.choice(profile.roaming_ip_indices))
    return int(rng.integers(0, n_ips))


def _pick_merchant_index(
    rng: np.random.Generator,
    profile: UserProfile,
    event_kind: str,
    is_fraud: bool,
    high_risk_merchant_indices: list[int],
    n_merchants: int,
) -> int:
    if event_kind == "risky_merchant" and high_risk_merchant_indices:
        return int(rng.choice(high_risk_merchant_indices))
    if is_fraud and event_kind != "stealth":
        # non-stealth fraud often targets high-risk merchant categories
        if high_risk_merchant_indices and float(rng.random()) < 0.65:
            return int(rng.choice(high_risk_merchant_indices))
        return int(rng.integers(0, n_merchants))
    # legit and stealth: preferred merchants most of the time
    if float(rng.random()) < 0.62 and profile.preferred_merchant_indices:
        return int(rng.choice(profile.preferred_merchant_indices))
    return int(rng.integers(0, n_merchants))


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
        home_country = str(rng.choice(_COUNTRIES, p=_COUNTRY_WEIGHTS))
        baseline_amount = float(np.clip(rng.lognormal(mean=np.log(120.0), sigma=0.85), 8.0, 2500.0))
        activity_weight = float(rng.gamma(shape=2.0, scale=1.0))
        trusted_device_rate = float(np.clip(rng.normal(loc=0.93, scale=0.05), 0.70, 0.995))
        card_age_days = int(np.clip(rng.lognormal(mean=np.log(365.0), sigma=0.9), 15, 3650))

        # ~4% of legitimate users accidentally share a fraud-ring IP (same office, carrier NAT, VPN exit)
        if fraud_ring_indices and float(rng.random()) < 0.04:
            home_ip_idx = int(rng.choice(fraud_ring_indices))
        else:
            home_ip_idx = int(rng.integers(0, n_ips))

        n_roaming = int(rng.integers(2, 5))
        roaming_ip_indices = [int(rng.integers(0, n_ips)) for _ in range(n_roaming)]

        n_preferred = int(rng.integers(3, 8))
        preferred_merchant_indices = [int(rng.integers(0, n_merchants)) for _ in range(n_preferred)]

        profiles.append(UserProfile(
            user_id=f"user-{index + 1:04d}",
            home_country=home_country,
            baseline_amount=baseline_amount,
            activity_weight=activity_weight,
            trusted_device_rate=trusted_device_rate,
            card_creation_date=simulation_start - timedelta(days=card_age_days),
            home_ip_idx=home_ip_idx,
            roaming_ip_indices=roaming_ip_indices,
            preferred_merchant_indices=preferred_merchant_indices,
        ))
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
    # 2-minute minimum allows realistic quick back-to-back transactions (e.g. split bills, shopping)
    return int(np.clip(cadence_seconds, 2 * 60, 9 * 24 * 3600))


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


def _generate_row(
    state: UserState,
    rng: np.random.Generator,
    fraud_rate: float,
    stealth_rate: float,
    ip_registry: list[IpRecord],
    merchant_registry: list[MerchantRecord],
    fraud_ring_indices: list[int],
    high_risk_merchant_indices: list[int],
) -> dict:
    is_fraud, event_kind = _choose_event_kind(rng, fraud_rate=fraud_rate, stealth_rate=stealth_rate)
    event_timestamp = _build_event_timestamp(state, rng, event_kind)
    _prune_windows(state, event_timestamp)

    user_average_amount = (
        state.running_amount_sum / state.running_amount_count
        if state.running_amount_count > 0
        else state.profile.baseline_amount
    )

    # ── IP & merchant: registry-based continuous scores, shared across users ──
    ip_idx = _pick_ip_index(rng, state.profile, event_kind, is_fraud, fraud_ring_indices, len(ip_registry))
    merchant_idx = _pick_merchant_index(rng, state.profile, event_kind, is_fraud, high_risk_merchant_indices, len(merchant_registry))
    ip_risk_score = _sample_ip_risk(rng, ip_registry[ip_idx])
    merchant_risk_score = _sample_merchant_risk(rng, merchant_registry[merchant_idx])

    # ── Amount ──
    if event_kind == "high_amount":
        amount = _sample_profile_amount(rng, user_average_amount, 2.5, 5.0)
    elif event_kind == "stealth":
        amount = _sample_profile_amount(rng, user_average_amount, 0.72, 1.22)
    else:
        amount = _sample_profile_amount(rng, user_average_amount, 0.55, 1.55)
        # ~4% of legit transactions are occasional large purchases (vacation, appliances, gifts)
        if not is_fraud and float(rng.random()) < 0.04:
            amount = _sample_profile_amount(rng, user_average_amount, 1.8, 3.5)

    # ── Device trust ──
    trusted_probability = state.profile.trusted_device_rate
    if event_kind == "device_mismatch":
        trusted_probability *= 0.20
    is_device_trusted = bool(rng.random() < trusted_probability)

    # ── Country mismatch ──
    if event_kind == "country_mismatch":
        mismatch_probability = 0.82
    elif is_fraud and event_kind != "stealth":
        mismatch_probability = 0.22
    else:
        mismatch_probability = 0.04
    has_country_mismatch = bool(rng.random() < mismatch_probability)

    # ── Card age ──
    if event_kind == "new_card":
        card_age_days = int(rng.integers(1, 90))
    else:
        card_age_days = int(np.clip((event_timestamp - state.profile.card_creation_date).days, 1, 3650))

    amount_to_average_ratio = float(amount / max(1.0, user_average_amount))
    seconds_since_last_transaction = (
        int((event_timestamp - state.last_timestamp).total_seconds())
        if state.last_timestamp is not None
        else int(30 * 24 * 3600)
    )
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
        "merchant_risk_score": merchant_risk_score,
        "is_device_trusted": bool(is_device_trusted),
        "has_country_mismatch": bool(has_country_mismatch),
        "amount_to_average_ratio": round(amount_to_average_ratio, 4),
        "hour_of_day": int(event_timestamp.hour),
        "ip_risk_score": ip_risk_score,
        "card_age_days": int(card_age_days),
        "is_fraud": bool(is_fraud),
    }


def simulate(
    row_count: int,
    fraud_rate: float = 0.05,
    stealth_rate: float = 0.20,
    seed: int | None = None,
) -> pd.DataFrame:
    """Simulate synthetic transactions with continuous feature distributions and cross-user IP contamination."""
    if row_count <= 0:
        return pd.DataFrame(columns=_OUTPUT_COLUMNS)

    rng = np.random.default_rng(seed)
    simulation_start = datetime(2026, 1, 1, 8, 0, 0)

    ip_registry = _build_ip_registry(rng, _N_IPS)
    merchant_registry = _build_merchant_registry(rng, _N_MERCHANTS)
    fraud_ring_indices = [i for i, ip in enumerate(ip_registry) if ip.is_fraud_ring]
    high_risk_merchant_indices = [i for i, m in enumerate(merchant_registry) if m.risk_score > 0.55]

    user_count = max(120, min(400, row_count // 40 + 120))
    profiles = _build_profiles(rng, user_count, simulation_start, ip_registry, merchant_registry, fraud_ring_indices)
    states = {profile.user_id: UserState(profile=profile) for profile in profiles}

    for state in states.values():
        _seed_hidden_history(state, rng, simulation_start)

    activity_weights = _normalize_weights(np.array([profile.activity_weight for profile in profiles], dtype=float))
    rows: list[dict] = []

    for _ in tqdm(range(row_count), desc="Generating transactions", unit="tx"):
        selected_profile = profiles[int(rng.choice(len(profiles), p=activity_weights))]
        rows.append(_generate_row(
            states[selected_profile.user_id], rng,
            fraud_rate=fraud_rate, stealth_rate=stealth_rate,
            ip_registry=ip_registry, merchant_registry=merchant_registry,
            fraud_ring_indices=fraud_ring_indices,
            high_risk_merchant_indices=high_risk_merchant_indices,
        ))

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
