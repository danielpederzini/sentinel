from __future__ import annotations

import argparse
import joblib
import os

import lightgbm as lgb
import numpy as np
import optuna
import pandas as pd
from sklearn.isotonic import IsotonicRegression
from sklearn.metrics import (
    average_precision_score,
    classification_report,
    confusion_matrix,
    precision_recall_curve,
)
from sklearn.model_selection import train_test_split
from tqdm import tqdm

from data_loader import load_data
from feature_schema import (
    BASE_FEATURES,
    CARD_TYPE_MAP,
    CATEGORICAL_FEATURES,
    MERCHANT_CATEGORY_MAP,
)

# ──────────────────────────────────────────────────────────────────────────────
# Feature engineering
# ──────────────────────────────────────────────────────────────────────────────

_MERCHANT_CATEGORY_MAP = MERCHANT_CATEGORY_MAP
_CARD_TYPE_MAP = CARD_TYPE_MAP


def encode_categoricals(df: pd.DataFrame) -> pd.DataFrame:
    """Label-encode merchant_category and card_type to integers."""
    out = df.copy()
    if "merchant_category" in out.columns:
        out["merchant_category"] = out["merchant_category"].map(_MERCHANT_CATEGORY_MAP).fillna(7).astype(int)
    if "card_type" in out.columns:
        out["card_type"] = out["card_type"].map(_CARD_TYPE_MAP).fillna(3).astype(int)
    return out


def engineer_features(
    df: pd.DataFrame,
    feature_caps: dict[str, float] | None = None,
) -> tuple[pd.DataFrame, dict[str, float]]:
    """Derive interaction and transformed features from base columns.

    All derived features use only the base feature columns so they can be
    replicated at inference time without access to historical user data.

    When ``feature_caps`` is *None* (training), the 99th-percentile caps are
    computed from the data and returned.  When provided (inference /
    re-application), the supplied caps are used instead so that production
    values are clipped to the same range the model was trained on.
    """
    out = df.copy()

    # Log transforms for skewed features
    out["log_amount"] = np.log1p(out["amount"])
    out["log_seconds_since"] = np.log1p(out["seconds_since_last_transaction"])
    out["log_velocity_1hour"] = np.log1p(out["amount_velocity_1hour"])

    # Interaction: amount × risk scores (capped at 99th percentile to limit outlier influence)
    raw_amount_x_merchant_risk = out["amount"] * out["merchant_risk_score"]
    caps: dict[str, float] = feature_caps or {}
    if "amount_x_merchant_risk" not in caps:
        caps["amount_x_merchant_risk"] = float(raw_amount_x_merchant_risk.quantile(0.99))
    out["amount_x_merchant_risk"] = raw_amount_x_merchant_risk.clip(upper=caps["amount_x_merchant_risk"])
    out["risk_score_product"] = out["merchant_risk_score"] * out["ip_risk_score"]

    # Interaction: device/country with risk
    device_untrusted = 1.0 - out["is_device_trusted"].astype(float)
    out["ip_device_risk"] = out["ip_risk_score"] * device_untrusted
    out["country_ip_risk"] = out["has_country_mismatch"].astype(float) * out["ip_risk_score"]

    # Velocity × amount interactions (capped to limit outlier influence)
    raw_velocity_amount = out["user_transaction_count_1hour"] * out["amount_to_average_ratio"]
    if "velocity_amount_interaction" not in caps:
        caps["velocity_amount_interaction"] = float(raw_velocity_amount.quantile(0.99))
    out["velocity_amount_interaction"] = raw_velocity_amount.clip(upper=caps["velocity_amount_interaction"])
    out["recency_velocity"] = (
        out["user_transaction_count_5min"] / np.clip(out["seconds_since_last_transaction"], 1, None)
    )

    # Amount deviation from user average
    out["amount_deviation"] = np.abs(out["amount"] - out["user_average_amount"]) / np.clip(
        out["user_average_amount"], 1.0, None
    )

    # Time-of-day features
    out["is_night"] = ((out["hour_of_day"] < 6) | (out["hour_of_day"] >= 22)).astype(float)

    # Velocity intensity (amount per recent transaction)
    out["velocity_intensity"] = out["amount_velocity_1hour"] / np.clip(
        out["user_transaction_count_1hour"], 1, None
    )

    return out, caps


def _print_pretraining_reports(data: pd.DataFrame) -> None:
    print("\nPre-training data checks")
    print(f"  Rows: {len(data)}, fraud_rate={data['is_fraud'].mean():.4f}")

    amount_bucket = pd.cut(
        data["amount"],
        bins=[0, 100, 1_000, 10_000, 100_000, np.inf],
        labels=["<=100", "100-1k", "1k-10k", "10k-100k", ">100k"],
        include_lowest=True,
    )
    ratio_bucket = pd.cut(
        data["amount_to_average_ratio"],
        bins=[0, 1.1, 2, 5, 20, np.inf],
        labels=["<=1.1", "1.1-2", "2-5", "5-20", ">20"],
        include_lowest=True,
    )
    no_history = data["user_historical_transaction_count"].fillna(0) == 0

    def print_bucket(title: str, bucket: pd.Series) -> None:
        rates = data.groupby(bucket, observed=False)["is_fraud"].agg(["count", "mean"])
        print(f"  {title}:")
        for idx, row in rates.iterrows():
            print(f"    {str(idx):10s} count={int(row['count']):7d} fraud_rate={row['mean']:.4f}")

    print_bucket("Amount buckets", amount_bucket)
    print_bucket("Amount/average ratio buckets", ratio_bucket)
    print(f"  No-history rows: count={int(no_history.sum())}, fraud_rate={data.loc[no_history, 'is_fraud'].mean():.4f}")
    large_cold = no_history & (data["amount"] >= 10_000)
    if large_cold.any():
        print(
            "  Large no-history rows: "
            f"count={int(large_cold.sum())}, fraud_rate={data.loc[large_cold, 'is_fraud'].mean():.4f}"
        )


def _probe_rows() -> pd.DataFrame:
    rows = [
        {
            "probe_name": "ordinary_first_transaction",
            "amount": 85.0,
            "user_average_amount": 90.0,
            "user_historical_transaction_count": 0,
            "user_transaction_count_5min": 0,
            "user_transaction_count_1hour": 0,
            "seconds_since_last_transaction": 30 * 24 * 3600,
            "amount_velocity_1hour": 85.0,
            "merchant_risk_score": 0.10,
            "is_device_trusted": True,
            "has_country_mismatch": False,
            "amount_to_average_ratio": 0.9444,
            "hour_of_day": 14,
            "ip_risk_score": 0.05,
            "card_age_days": 180,
            "user_account_age_days": 220,
            "day_of_week": 2,
            "merchant_category": "GROCERY",
            "card_type": "CREDIT",
            "distinct_merchant_count_1hour": 1,
        },
        {
            "probe_name": "established_moderate_mild_risk",
            "amount": 450.0,
            "user_average_amount": 220.0,
            "user_historical_transaction_count": 24,
            "user_transaction_count_5min": 0,
            "user_transaction_count_1hour": 1,
            "seconds_since_last_transaction": 7200,
            "amount_velocity_1hour": 450.0,
            "merchant_risk_score": 0.35,
            "is_device_trusted": True,
            "has_country_mismatch": False,
            "amount_to_average_ratio": 2.0455,
            "hour_of_day": 16,
            "ip_risk_score": 0.20,
            "card_age_days": 500,
            "user_account_age_days": 700,
            "day_of_week": 3,
            "merchant_category": "RESTAURANT",
            "card_type": "CREDIT",
            "distinct_merchant_count_1hour": 1,
        },
        {
            "probe_name": "large_first_transaction_low_context_risk",
            "amount": 1_000_000.0,
            "user_average_amount": 100.0,
            "user_historical_transaction_count": 0,
            "user_transaction_count_5min": 0,
            "user_transaction_count_1hour": 0,
            "seconds_since_last_transaction": 30 * 24 * 3600,
            "amount_velocity_1hour": 1_000_000.0,
            "merchant_risk_score": 0.12,
            "is_device_trusted": True,
            "has_country_mismatch": False,
            "amount_to_average_ratio": 10_000.0,
            "hour_of_day": 13,
            "ip_risk_score": 0.05,
            "card_age_days": 120,
            "user_account_age_days": 140,
            "day_of_week": 2,
            "merchant_category": "GROCERY",
            "card_type": "CREDIT",
            "distinct_merchant_count_1hour": 1,
        },
        {
            "probe_name": "large_first_transaction_untrusted_device",
            "amount": 1_000_000.0,
            "user_average_amount": 100.0,
            "user_historical_transaction_count": 0,
            "user_transaction_count_5min": 0,
            "user_transaction_count_1hour": 0,
            "seconds_since_last_transaction": 30 * 24 * 3600,
            "amount_velocity_1hour": 1_000_000.0,
            "merchant_risk_score": 0.12,
            "is_device_trusted": False,
            "has_country_mismatch": False,
            "amount_to_average_ratio": 10_000.0,
            "hour_of_day": 13,
            "ip_risk_score": 0.05,
            "card_age_days": 120,
            "user_account_age_days": 140,
            "day_of_week": 2,
            "merchant_category": "GROCERY",
            "card_type": "CREDIT",
            "distinct_merchant_count_1hour": 1,
        },
        {
            "probe_name": "large_first_transaction_risky_context",
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
        },
        {
            "probe_name": "small_velocity_burst",
            "amount": 25.0,
            "user_average_amount": 65.0,
            "user_historical_transaction_count": 18,
            "user_transaction_count_5min": 7,
            "user_transaction_count_1hour": 9,
            "seconds_since_last_transaction": 35,
            "amount_velocity_1hour": 380.0,
            "merchant_risk_score": 0.25,
            "is_device_trusted": False,
            "has_country_mismatch": False,
            "amount_to_average_ratio": 0.3846,
            "hour_of_day": 1,
            "ip_risk_score": 0.35,
            "card_age_days": 300,
            "user_account_age_days": 500,
            "day_of_week": 6,
            "merchant_category": "OTHER",
            "card_type": "DEBIT",
            "distinct_merchant_count_1hour": 5,
        },
    ]
    return pd.DataFrame(rows)


def _score_behavioral_probes(
    model: lgb.LGBMClassifier,
    calibrator: IsotonicRegression | None,
    feature_names: list[str],
) -> dict[str, float]:
    probes = _probe_rows()
    names = probes.pop("probe_name")
    X, _ = engineer_features(encode_categoricals(probes))
    X = X[feature_names]
    raw = model.predict_proba(X)[:, 1]
    probabilities = calibrator.predict(raw) if calibrator is not None else raw
    return {name: float(prob) for name, prob in zip(names, probabilities)}


# ──────────────────────────────────────────────────────────────────────────────
# Threshold optimization
# ──────────────────────────────────────────────────────────────────────────────

def _find_threshold_for_fbeta(y_true: np.ndarray, y_proba: np.ndarray, beta: float) -> float:
    precisions, recalls, thresholds = precision_recall_curve(y_true, y_proba)
    best_threshold = 0.5
    best_fbeta_score = 0.0
    for precision, recall, threshold in zip(precisions, recalls, thresholds):
        if precision + recall > 0:
            fbeta_score = (1 + beta**2) * precision * recall / (beta**2 * precision + recall)
            if fbeta_score > best_fbeta_score:
                best_fbeta_score = fbeta_score
                best_threshold = threshold
    return float(best_threshold)


# ──────────────────────────────────────────────────────────────────────────────
# LightGBM + Optuna training
# ──────────────────────────────────────────────────────────────────────────────

class _LGBMProgressCallback:
    def __init__(self, n_estimators: int):
        self._bar = tqdm(total=n_estimators, desc="Training", unit="round")

    def __call__(self, env: lgb.callback.CallbackEnv) -> None:
        self._bar.update(1)
        if env.iteration == env.end_iteration - 1:
            self._bar.close()


def _build_lgbm(params: dict, class_weight_scale: float) -> lgb.LGBMClassifier:
    return lgb.LGBMClassifier(
        objective="binary",
        metric="average_precision",
        is_unbalance=False,
        scale_pos_weight=class_weight_scale,
        random_state=42,
        verbosity=-1,
        **params,
    )


def _optuna_objective(
    trial: optuna.Trial,
    X_train: pd.DataFrame,
    y_train: pd.Series,
    X_val: pd.DataFrame,
    y_val: pd.Series,
    class_weight_scale: float,
    categorical_indices: list[int] | None = None,
) -> float:
    params = {
        "n_estimators": 100,
        "max_depth": trial.suggest_int("max_depth", 4, 10),
        "num_leaves": trial.suggest_int("num_leaves", 15, 127),
        "learning_rate": trial.suggest_float("learning_rate", 0.01, 0.2, log=True),
        "min_child_samples": trial.suggest_int("min_child_samples", 10, 200),
        "subsample": trial.suggest_float("subsample", 0.5, 1.0),
        "colsample_bytree": trial.suggest_float("colsample_bytree", 0.4, 1.0),
        "reg_alpha": trial.suggest_float("reg_alpha", 1e-3, 10.0, log=True),
        "reg_lambda": trial.suggest_float("reg_lambda", 1e-3, 10.0, log=True),
    }

    model = _build_lgbm(params, class_weight_scale)

    fit_kwargs = {}
    if categorical_indices:
        fit_kwargs["categorical_feature"] = categorical_indices

    model.fit(
        X_train, y_train,
        eval_set=[(X_val, y_val)],
        callbacks=[lgb.early_stopping(50, verbose=True), lgb.log_evaluation(period=0)],
        **fit_kwargs,
    )

    y_proba = model.predict_proba(X_val)[:, 1]
    return float(average_precision_score(y_val, y_proba))


def train_model(
    data_file: str,
    model_output_directory: str,
    model_version: str,
    n_estimators: int,
    max_depth: int,
    learning_rate: float,
    early_stopping_rounds: int,
    beta: float,
    device: str,
    n_trials: int,
    skip_tuning: bool,
) -> None:
    data = load_data(data_file)
    missing_base_features = [feature for feature in BASE_FEATURES if feature not in data.columns]
    if missing_base_features:
        raise ValueError(
            "Training data is missing required base features: "
            + ", ".join(sorted(missing_base_features))
        )
    _print_pretraining_reports(data)
    data = data.drop(columns=["transaction_id", "user_id"], errors="ignore")

    # Encode categoricals and engineer features
    data = encode_categoricals(data)
    data, feature_caps = engineer_features(data)

    X = data.drop(columns=["is_fraud"])
    y = data["is_fraud"].astype(int)

    feature_names = list(X.columns)
    categorical_indices = [feature_names.index(c) for c in CATEGORICAL_FEATURES if c in feature_names]
    print(f"Features ({len(feature_names)}): {feature_names}")

    class_weight_scale = float(y.value_counts()[0] / y.value_counts()[1])
    print(f"Class distribution: {y.value_counts().to_dict()}, scale_pos_weight={class_weight_scale:.2f}")

    X_temp, X_test, y_temp, y_test = train_test_split(X, y, test_size=0.15, random_state=42, stratify=y)
    X_train, X_cal, y_train, y_cal = train_test_split(
        X_temp, y_temp, test_size=0.15 / 0.85, random_state=42, stratify=y_temp,
    )

    # ── Hyperparameter tuning with Optuna ──
    if not skip_tuning:
        print(f"\nRunning Optuna hyperparameter search ({n_trials} trials)...")
        optuna.logging.set_verbosity(optuna.logging.INFO)
        study = optuna.create_study(direction="maximize", study_name="lgbm_fraud")

        study.optimize(
            lambda trial: _optuna_objective(
                trial, X_train, y_train, X_cal, y_cal, class_weight_scale, categorical_indices,
            ),
            n_trials=n_trials,
            show_progress_bar=True,
        )

        best_params = study.best_params
        best_params["n_estimators"] = n_estimators
        print(f"\nBest trial PR-AUC: {study.best_value:.4f}")
        print(f"Best params: {best_params}")
    else:
        best_params = {
            "n_estimators": n_estimators,
            "max_depth": max_depth,
            "learning_rate": learning_rate,
            "num_leaves": 108,
            "min_child_samples": 135,
            "subsample": 0.556,
            "colsample_bytree": 0.535,
            "reg_alpha": 1.677,
            "reg_lambda": 9.293,
        }
        print(f"\nSkipping tuning, using default params: {best_params}")

    # ── Final training with best params ──
    print("\nTraining final model...")
    final_model = _build_lgbm(best_params, class_weight_scale)

    progress_cb = _LGBMProgressCallback(best_params["n_estimators"])
    final_fit_kwargs = {}
    if categorical_indices:
        final_fit_kwargs["categorical_feature"] = categorical_indices
    final_model.fit(
        X_train, y_train,
        eval_set=[(X_cal, y_cal)],
        callbacks=[
            lgb.early_stopping(early_stopping_rounds, verbose=False),
            lgb.log_evaluation(period=0),
            progress_cb,
        ],
        **final_fit_kwargs,
    )

    # ── Isotonic calibration ──
    isotonic_calibrator = IsotonicRegression(out_of_bounds="clip")
    isotonic_calibrator.fit(final_model.predict_proba(X_cal)[:, 1], y_cal)

    y_proba = isotonic_calibrator.predict(final_model.predict_proba(X_test)[:, 1])

    # ── Threshold optimization ──
    threshold = _find_threshold_for_fbeta(y_test, y_proba, beta)
    print(f"\nThreshold for F-{beta:.2f}: {threshold:.4f}")
    threshold_strategy = f"fbeta_{beta}"

    y_pred = (y_proba >= threshold).astype(int)
    tn, fp, fn, tp = confusion_matrix(y_test, y_pred).ravel()
    report = classification_report(y_test, y_pred, output_dict=True)

    pr_auc = average_precision_score(y_test, y_proba)

    metrics = {
        "threshold": threshold,
        "threshold_strategy": threshold_strategy,
        "beta": beta,
        "pr_auc": pr_auc,
        "confusion_matrix": {"tn": int(tn), "fp": int(fp), "fn": int(fn), "tp": int(tp)},
        "accuracy": report["accuracy"],
        "precision": report["1"]["precision"],
        "recall": report["1"]["recall"],
        "f1_score": report["1"]["f1-score"],
        "support": report["1"]["support"],
    }

    behavioral_probes = _score_behavioral_probes(final_model, isotonic_calibrator, feature_names)
    metrics["behavioral_probes"] = behavioral_probes

    cm = metrics["confusion_matrix"]
    print(f"\nMetrics (threshold={metrics['threshold']:.4f}, strategy={metrics['threshold_strategy']})")
    print(f"  PR-AUC:    {metrics['pr_auc']:.4f}")
    print(f"  Precision: {metrics['precision']:.4f}")
    print(f"  Recall:    {metrics['recall']:.4f}")
    print(f"  F1:        {metrics['f1_score']:.4f}")
    print(f"  Accuracy:  {metrics['accuracy']:.4f}")
    print(f"  Confusion: TN={cm['tn']}  FP={cm['fp']}  FN={cm['fn']}  TP={cm['tp']}")

    # ── Feature importance (gain) ──
    importances = final_model.feature_importances_
    sorted_idx = np.argsort(importances)[::-1]
    print("\nTop-15 feature importances (gain):")
    for i in sorted_idx[:15]:
        print(f"  {feature_names[i]:35s} {importances[i]:>6d}")

    print("\nBehavioral probes:")
    for name, probability in behavioral_probes.items():
        print(f"  {name:45s} {probability:.4f}")

    # ── Save ──
    os.makedirs(model_output_directory, exist_ok=True)

    bundle = {
        "model": final_model,
        "calibrator": isotonic_calibrator,
        "metrics": metrics,
        "version": model_version,
        "model_type": "lightgbm",
        "feature_names": feature_names,
        "feature_caps": feature_caps,
        "best_params": best_params,
    }
    bundle_path = os.path.join(model_output_directory, f"lgbm_{model_version}.joblib")
    joblib.dump(bundle, bundle_path)
    print(f"\nBundle saved to {bundle_path}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Train a fraud detection model.")
    parser.add_argument("data_file", type=str, help="Path to the CSV file containing the training data.")
    parser.add_argument("model_output_directory", type=str, help="Path to save the trained model.")
    parser.add_argument("model_version", type=str, help="Version of the model.")
    parser.add_argument("--n-estimators", type=int, default=5_000,help="Maximum number of boosting rounds (default: 5000).")
    parser.add_argument("--max-depth", type=int, default=10, help="Maximum tree depth (default: 10).")
    parser.add_argument("--learning-rate", type=float, default=0.1607, help="Boosting learning rate (default: 0.1607).")
    parser.add_argument("--early-stopping-rounds", type=int, default=100, help="Stop if no improvement after this many rounds (default: 100).")
    parser.add_argument("--beta", type=float, default=2.0, help="F-beta threshold optimization (default: 2.0).")
    parser.add_argument("--device", type=str, default="gpu", help="Device for training: 'cpu' or 'gpu' (default: gpu).")
    parser.add_argument("--n-trials", type=int, default=50, help="Number of Optuna hyperparameter search trials (default: 50).")
    parser.add_argument("--skip-tuning", action="store_true", help="Skip Optuna tuning, use default/specified hyperparameters.")
    args = parser.parse_args()

    train_model(
        data_file=args.data_file,
        model_output_directory=args.model_output_directory,
        model_version=args.model_version,
        n_estimators=args.n_estimators,
        max_depth=args.max_depth,
        learning_rate=args.learning_rate,
        early_stopping_rounds=args.early_stopping_rounds,
        beta=args.beta,
        device=args.device,
        n_trials=args.n_trials,
        skip_tuning=args.skip_tuning,
    )


if __name__ == "__main__":
    main()
