import argparse
import joblib
import os
import numpy as np
import xgboost as xgb
from sklearn.isotonic import IsotonicRegression
from sklearn.metrics import average_precision_score, confusion_matrix, classification_report, precision_recall_curve
from sklearn.model_selection import train_test_split
from data_loader import load_data

def _find_threshold_for_fbeta(y_true, y_proba: np.ndarray, beta: float) -> float:
    """Return the threshold that maximizes the F-beta score."""
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


def train_model(
    data_file: str,
    model_output_directory: str,
    model_version: str,
    n_estimators: int,
    max_depth: int,
    learning_rate: float,
    early_stopping_rounds: int,
    beta: float | None,
    device: str,
) -> None:
    data = load_data(data_file)
    data = data.drop(columns=["transaction_id"])

    X = data.drop(columns=["is_fraud"])
    y = data["is_fraud"].astype(int)

    class_weight_scale = y.value_counts()[0] / y.value_counts()[1]
    print(f"Class distribution: {y.value_counts().to_dict()}, scale_pos_weight={class_weight_scale:.2f}")

    X_temp, X_test, y_temp, y_test = train_test_split(X, y, test_size=0.15, random_state=42, stratify=y)
    X_train, X_cal, y_train, y_cal = train_test_split(
        X_temp, y_temp, test_size=0.15 / 0.85, random_state=42, stratify=y_temp
    )

    model = xgb.XGBClassifier(
        n_estimators=n_estimators,
        max_depth=max_depth,
        learning_rate=learning_rate,
        subsample=0.8,
        colsample_bytree=0.8,
        random_state=42,
        use_label_encoder=False,
        eval_metric="aucpr",
        scale_pos_weight=class_weight_scale,
        early_stopping_rounds=early_stopping_rounds,
        device=device,
    )

    model.fit(X_train, y_train, eval_set=[(X_cal, y_cal)], verbose=True)

    isotonic_regularization = IsotonicRegression(out_of_bounds="clip")
    isotonic_regularization.fit(model.predict_proba(X_cal)[:, 1], y_cal)

    y_proba = isotonic_regularization.predict(model.predict_proba(X_test)[:, 1])

    threshold = _find_threshold_for_fbeta(y_test, y_proba, beta)
    print(f"Threshold for F-{beta:.2f}: {threshold:.4f}")
    threshold_strategy = f"fbeta_{beta}"

    y_pred = (y_proba >= threshold).astype(int)
    true_negatives, false_positives, false_negatives, true_positives = confusion_matrix(y_test, y_pred).ravel()
    classification_report_dict = classification_report(y_test, y_pred, output_dict=True)

    precision_recall_auc = average_precision_score(y_test, y_proba)

    metrics = {
        "threshold": threshold,
        "threshold_strategy": threshold_strategy,
        "beta": beta,
        "pr_auc": precision_recall_auc,
        "confusion_matrix": {
            "tn": int(true_negatives),
            "fp": int(false_positives),
            "fn": int(false_negatives),
            "tp": int(true_positives),
        },
        "accuracy": classification_report_dict["accuracy"],
        "precision": classification_report_dict["1"]["precision"],
        "recall": classification_report_dict["1"]["recall"],
        "f1_score": classification_report_dict["1"]["f1-score"],
        "support": classification_report_dict["1"]["support"],
    }

    os.makedirs(model_output_directory, exist_ok=True)

    bundle = {
        "model": model,
        "calibrator": isotonic_regularization,
        "metrics": metrics,
        "version": model_version,
    }
    bundle_path = os.path.join(model_output_directory, f"xgb_{model_version}.joblib")
    joblib.dump(bundle, bundle_path)
    print(f"Bundle saved to {bundle_path}")


def main():
    parser = argparse.ArgumentParser(description="Train a fraud detection model.")
    parser.add_argument("data_file", type=str, help="Path to the CSV file containing the training data.")
    parser.add_argument("model_output_directory", type=str, help="Path to save the trained model.")
    parser.add_argument("model_version", type=str, help="Version of the model.")
    parser.add_argument("--n-estimators", type=int, default=5_000,
                        help="Maximum number of boosting rounds (default: 5000).")
    parser.add_argument("--max-depth", type=int, default=5,
                        help="Maximum tree depth (default: 5).")
    parser.add_argument("--learning-rate", type=float, default=0.05,
                        help="Boosting learning rate (default: 0.05).")
    parser.add_argument("--early-stopping-rounds", type=int, default=100,
                        help="Stop if no improvement after this many rounds (default: 100).")
    parser.add_argument("--beta", type=float, default=0.5,
                        help="If set, optimize F-beta threshold (beta<1 weights precision more) (default: 0.5).")
    parser.add_argument("--device", type=str, default="cuda",
                        help="Device for XGBoost training: 'cuda' or 'cpu' (default: cuda).")
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
    )

if __name__ == "__main__":
    main()