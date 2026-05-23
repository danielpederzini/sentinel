from __future__ import annotations

import argparse
import json
import os

import joblib

from training_pipeline import _score_behavioral_probes


def main() -> None:
    parser = argparse.ArgumentParser(description="Score canonical fraud behavior probes against a model bundle.")
    parser.add_argument("bundle", help="Path to an lgbm_*.joblib model bundle")
    args = parser.parse_args()

    bundle_path = os.path.abspath(args.bundle)
    bundle = joblib.load(bundle_path)
    probabilities = _score_behavioral_probes(
        bundle["model"],
        bundle.get("calibrator"),
        bundle["feature_names"],
    )
    print(json.dumps(probabilities, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
