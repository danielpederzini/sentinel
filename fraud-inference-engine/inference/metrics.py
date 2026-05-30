"""Custom Prometheus business metrics for the fraud inference engine.

These register on the default prometheus_client registry, so they are exposed on
the same /metrics endpoint as the FastAPI Instrumentator HTTP metrics.
"""
from prometheus_client import Counter, Gauge, Histogram

FRAUD_SCORING_TOTAL = Counter(
    "fraud_scoring",
    "Total transactions scored, labeled by risk level and model version",
    ["risk_level", "model_version"],
)

FRAUD_PROBABILITY = Histogram(
    "fraud_probability",
    "Distribution of predicted fraud probabilities",
    buckets=(0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 1.0),
)

FRAUD_SCORING_DURATION_SECONDS = Histogram(
    "fraud_scoring_duration_seconds",
    "Model inference + explainability duration in seconds",
    buckets=(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5),
)

FRAUD_MODEL_INFO = Gauge(
    "fraud_model_info",
    "Currently loaded model version (value is always 1; the version is a label)",
    ["version"],
)

FRAUD_MODEL_LOADED = Gauge(
    "fraud_model_loaded",
    "Whether a model is currently loaded (1) or not (0)",
)


def set_active_model(version: str) -> None:
    """Record the currently loaded model version, replacing any previous one."""
    FRAUD_MODEL_INFO.clear()
    FRAUD_MODEL_INFO.labels(version=version).set(1)
    FRAUD_MODEL_LOADED.set(1)
