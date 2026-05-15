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

# ──────────────────────────────────────────────────────────────────────────────
# Feature engineering
# ──────────────────────────────────────────────────────────────────────────────

BASE_FEATURES = [
    "amount",
    "user_average_amount",
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
]


def engineer_features(df: pd.DataFrame) -> pd.DataFrame:
    """Derive interaction and transformed features from base columns.

    All derived features use only the base feature columns so they can be
    replicated at inference time without access to historical user data.
    """
    out = df.copy()

    # Log transforms for skewed features
    out["log_amount"] = np.log1p(out["amount"])
    out["log_seconds_since"] = np.log1p(out["seconds_since_last_transaction"])
    out["log_velocity_1hour"] = np.log1p(out["amount_velocity_1hour"])

    # Interaction: amount × risk scores
    out["amount_x_merchant_risk"] = out["amount"] * out["merchant_risk_score"]
    out["amount_x_ip_risk"] = out["amount"] * out["ip_risk_score"]
    out["risk_score_product"] = out["merchant_risk_score"] * out["ip_risk_score"]

    # Interaction: device/country with risk
    device_untrusted = 1.0 - out["is_device_trusted"].astype(float)
    out["ip_device_risk"] = out["ip_risk_score"] * device_untrusted
    out["country_ip_risk"] = out["has_country_mismatch"].astype(float) * out["ip_risk_score"]

    # Velocity × amount interactions
    out["velocity_amount_interaction"] = (
        out["user_transaction_count_1hour"] * out["amount_to_average_ratio"]
    )
    out["recency_velocity"] = (
        out["user_transaction_count_5min"] / np.clip(out["seconds_since_last_transaction"], 1, None)
    )

    # Card age × amount ratio (new card with high spending)
    out["card_age_x_amount_ratio"] = out["card_age_days"] * out["amount_to_average_ratio"]

    # Amount deviation from user average
    out["amount_deviation"] = np.abs(out["amount"] - out["user_average_amount"]) / np.clip(
        out["user_average_amount"], 1.0, None
    )

    # Time-of-day features
    out["is_night"] = ((out["hour_of_day"] < 6) | (out["hour_of_day"] >= 22)).astype(float)
    out["night_amount_ratio"] = out["is_night"] * out["amount_to_average_ratio"]

    # Velocity intensity (amount per recent transaction)
    out["velocity_intensity"] = out["amount_velocity_1hour"] / np.clip(
        out["user_transaction_count_1hour"], 1, None
    )

    return out


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
) -> float:
    params = {
        "n_estimators": 3000,
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

    model.fit(
        X_train, y_train,
        eval_set=[(X_val, y_val)],
        callbacks=[lgb.early_stopping(100, verbose=False), lgb.log_evaluation(period=0)],
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
    data = data.drop(columns=["transaction_id", "user_id"], errors="ignore")

    # Feature engineering
    data = engineer_features(data)

    X = data.drop(columns=["is_fraud"])
    y = data["is_fraud"].astype(int)

    feature_names = list(X.columns)
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
        optuna.logging.set_verbosity(optuna.logging.WARNING)
        study = optuna.create_study(direction="maximize", study_name="lgbm_fraud")

        study.optimize(
            lambda trial: _optuna_objective(
                trial, X_train, y_train, X_cal, y_cal, class_weight_scale,
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
            "num_leaves": 63,
            "min_child_samples": 50,
            "subsample": 0.8,
            "colsample_bytree": 0.7,
            "reg_alpha": 0.1,
            "reg_lambda": 1.0,
        }
        print(f"\nSkipping tuning, using default params: {best_params}")

    # ── Final training with best params ──
    print("\nTraining final model...")
    final_model = _build_lgbm(best_params, class_weight_scale)

    progress_cb = _LGBMProgressCallback(best_params["n_estimators"])
    final_model.fit(
        X_train, y_train,
        eval_set=[(X_cal, y_cal)],
        callbacks=[
            lgb.early_stopping(early_stopping_rounds, verbose=False),
            lgb.log_evaluation(period=0),
            progress_cb,
        ],
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

    cm = metrics["confusion_matrix"]
    print(f"\nMetrics (threshold={metrics['threshold']:.4f}, strategy={metrics['threshold_strategy']})")
    print(f"  PR-AUC:    {metrics['pr_auc']:.4f}")
    print(f"  Precision: {metrics['precision']:.4f}")
    print(f"  Recall:    {metrics['recall']:.4f}")
    print(f"  F1:        {metrics['f1_score']:.4f}")
    print(f"  Accuracy:  {metrics['accuracy']:.4f}")
    print(f"  Confusion: TN={cm['tn']}  FP={cm['fp']}  FN={cm['fn']}  TP={cm['tp']}")

    # ── Feature importance ──
    importances = final_model.feature_importances_
    sorted_idx = np.argsort(importances)[::-1]
    print("\nTop-15 feature importances (gain):")
    for i in sorted_idx[:15]:
        print(f"  {feature_names[i]:35s} {importances[i]:>6d}")

    # ── Save ──
    os.makedirs(model_output_directory, exist_ok=True)

    bundle = {
        "model": final_model,
        "calibrator": isotonic_calibrator,
        "metrics": metrics,
        "version": model_version,
        "model_type": "lightgbm",
        "feature_names": feature_names,
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
    parser.add_argument("--max-depth", type=int, default=7, help="Maximum tree depth (default: 7).")
    parser.add_argument("--learning-rate", type=float, default=0.05, help="Boosting learning rate (default: 0.05).")
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
