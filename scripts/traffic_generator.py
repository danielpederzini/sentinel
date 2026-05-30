#!/usr/bin/env python3
"""Sentinel traffic generator.

Posts synthetic transactions to the Transaction Ingestor so the Grafana
dashboards show live data. IDs are aligned with the Flyway seed data so the
Feature Manager can resolve users/cards/merchants/devices.

Seed ranges (see feature-manager V1__create_schema_and_seed_mock_data.sql):
  users     : user-000001 .. user-100000
  cards     : card-000001 .. card-100000   (one per user, same index)
  merchants : merchant-0001 .. merchant-0150
  devices   : device-000001 .. device-100000

Usage:
  python traffic_generator.py --rate 10 --duration 300 --fraud-ratio 0.01
  python traffic_generator.py --rate 5            # run until Ctrl+C

Requires no third-party packages (uses urllib from the standard library).
"""
import argparse
import json
import random
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime

SEED_USERS = 100_000
SEED_MERCHANTS = 150
SEED_DEVICES = 100_000

COUNTRIES = [
    "US", "BR", "AR", "DE", "ES", "SE", "NL", "GB", "CA", "JP",
    "AU", "MX", "CL", "ZA", "PT", "FR", "IT", "CH", "CN", "IN",
]


def _random_ip() -> str:
    return f"{random.randint(1, 223)}.{random.randint(0, 255)}.{random.randint(0, 255)}.{random.randint(1, 254)}"


def build_transaction(fraud_ratio: float) -> dict:
    user_index = random.randint(1, SEED_USERS)
    is_fraud_like = random.random() < fraud_ratio

    transaction = {
        "transactionId": f"tx-gen-{uuid.uuid4().hex[:12]}",
        "userId": f"user-{user_index:06d}",
        "cardId": f"card-{user_index:06d}",
        "merchantId": f"merchant-{random.randint(1, SEED_MERCHANTS):04d}",
        "countryCode": random.choice(COUNTRIES),
        "ipAddress": _random_ip(),
        "creationDateTime": datetime.now().replace(microsecond=0).isoformat(),
    }

    if is_fraud_like:
        # Nudge toward higher-risk inputs: large amount, no trusted device on file.
        transaction["amount"] = round(random.uniform(2000, 9000), 2)
    else:
        transaction["amount"] = round(random.uniform(10, 600), 2)
        if random.random() < 0.8:
            transaction["deviceId"] = f"device-{random.randint(1, SEED_DEVICES):06d}"

    return transaction


def post_transaction(url: str, transaction: dict) -> int:
    data = json.dumps(transaction).encode("utf-8")
    request = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(request, timeout=10) as response:
        return response.status


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate synthetic transaction traffic for Sentinel.")
    parser.add_argument("--url", default="http://localhost:8080/api/v1/transactions", help="Transaction Ingestor endpoint")
    parser.add_argument("--rate", type=float, default=10.0, help="Transactions per second")
    parser.add_argument("--duration", type=int, default=0, help="Seconds to run (0 = until interrupted)")
    parser.add_argument("--fraud-ratio", type=float, default=0.01, help="Fraction of fraud-like transactions (0-1)")
    parser.add_argument("--seed", type=int, default=None, help="Optional RNG seed for reproducibility")
    args = parser.parse_args()

    if args.seed is not None:
        random.seed(args.seed)

    interval = 1.0 / args.rate if args.rate > 0 else 0.0
    deadline = time.time() + args.duration if args.duration > 0 else None

    sent = 0
    failed = 0
    started = time.time()
    print(f"Posting ~{args.rate}/s to {args.url} (fraud-ratio={args.fraud_ratio}). Ctrl+C to stop.")
    try:
        while deadline is None or time.time() < deadline:
            transaction = build_transaction(args.fraud_ratio)
            try:
                post_transaction(args.url, transaction)
                sent += 1
            except urllib.error.URLError as exception:
                failed += 1
                if failed <= 5:
                    print(f"  request failed: {exception}", file=sys.stderr)
            if sent and sent % 50 == 0:
                elapsed = time.time() - started
                print(f"  sent={sent} failed={failed} ({sent / elapsed:.1f}/s)")
            if interval:
                time.sleep(interval)
    except KeyboardInterrupt:
        print("\nStopping...")

    print(f"Done. sent={sent} failed={failed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
