"""
Sentinel Load Test Script
=========================
Sends realistic transactions across 100k users while keeping per-user
frequency below 1 transaction/hour to avoid triggering HIGH risk scores.

Math:
  100,000 users / 3,600 seconds = ~27.78 req/s
  Rounding to 28 req/s gives each user at most 1 transaction per hour
  when cycling through users sequentially.

Usage:
  pip install aiohttp tqdm
  python load_test.py --base-url http://localhost:8080 --users 100000

The script cycles through all users once (one full pass = 100k requests),
then stops. To run continuously, use --loops 0 for infinite looping.
"""

import argparse
import asyncio
import random
import time
import uuid
from datetime import datetime, timezone

import aiohttp
from tqdm import tqdm

MERCHANT_CATEGORIES = [
    "GROCERY", "RESTAURANT", "ENTERTAINMENT", "TRAVEL",
    "HEALTHCARE", "EDUCATION", "UTILITIES", "OTHER",
]

CARD_TYPES = ["CREDIT", "DEBIT", "CREDIT_AND_DEBIT", "OTHER"]

COUNTRIES = [
    "US", "BR", "AR", "DE", "ES", "SE", "NL", "GB", "CA", "JP",
    "AU", "MX", "CL", "ZA", "PT", "FR", "IT", "CH", "CN", "IN",
]

COUNTRY_WEIGHTS = [
    0.16, 0.12, 0.08, 0.10, 0.08, 0.09, 0.07, 0.07, 0.05, 0.05,
    0.04, 0.04, 0.03, 0.03, 0.02, 0.02, 0.02, 0.02, 0.02, 0.01,
]


def generate_user_pool(num_users: int) -> list[dict]:
    """Pre-generate stable user profiles for consistent, low-risk transactions."""
    users = []
    for i in range(num_users):
        country = random.choices(COUNTRIES, weights=COUNTRY_WEIGHTS, k=1)[0]
        users.append({
            "user_id": f"user-{i:06d}",
            "card_id": f"card-{i:06d}",
            "merchant_id": f"merchant-{random.randint(0, 999):04d}",
            "device_id": f"device-{i:06d}",
            "country_code": country,
            "ip_address": f"10.{(i >> 16) & 0xFF}.{(i >> 8) & 0xFF}.{i & 0xFF}",
            "base_amount": round(random.lognormvariate(3.5, 0.7), 2),
        })
    return users


def build_transaction(user: dict) -> dict:
    """Build a single transaction payload from a user profile."""
    amount_jitter = random.uniform(0.8, 1.2)
    amount = round(max(1.0, user["base_amount"] * amount_jitter), 2)

    return {
        "transactionId": str(uuid.uuid4()),
        "userId": user["user_id"],
        "cardId": user["card_id"],
        "merchantId": user["merchant_id"],
        "deviceId": user["device_id"],
        "amount": amount,
        "countryCode": user["country_code"],
        "ipAddress": user["ip_address"],
        "creationDateTime": datetime.now(timezone.utc)
            .replace(tzinfo=None)
            .isoformat(),
    }


async def send_transaction(
    session: aiohttp.ClientSession,
    url: str,
    payload: dict,
    stats: dict,
) -> None:
    """Send a single transaction and track result."""
    start = time.monotonic()
    try:
        async with session.post(url, json=payload) as response:
            elapsed = time.monotonic() - start
            stats["latencies"].append(elapsed)
            if response.status == 202:
                stats["success"] += 1
            else:
                stats["errors"] += 1
                if stats["errors"] <= 5:
                    body = await response.text()
                    print(f"\n  [ERR {response.status}] {body[:200]}")
    except Exception as exception:
        stats["errors"] += 1
        if stats["errors"] <= 5:
            print(f"\n  [EXC] {exception}")


async def run_load_test(
    base_url: str,
    users: list[dict],
    target_rate: float,
    concurrency: int,
    loops: int,
) -> None:
    """Main load test loop: sends transactions at the target rate."""
    url = f"{base_url}/api/v1/transactions"
    interval = 1.0 / target_rate
    total_users = len(users)

    connector = aiohttp.TCPConnector(limit=concurrency, limit_per_host=concurrency)
    timeout = aiohttp.ClientTimeout(total=30)

    print(f"Target:      {base_url}")
    print(f"Users:       {total_users:,}")
    print(f"Target rate: {target_rate:.1f} req/s")
    print(f"Concurrency: {concurrency}")
    print(f"Interval:    {interval*1000:.1f} ms between requests")
    print(f"Loops:       {'infinite' if loops == 0 else loops}")
    print()

    loop_count = 0
    while loops == 0 or loop_count < loops:
        loop_count += 1
        if loops != 1:
            print(f"--- Loop {loop_count} ---")

        stats = {"success": 0, "errors": 0, "latencies": []}
        semaphore = asyncio.Semaphore(concurrency)
        loop_start = time.monotonic()
        tasks: list[asyncio.Task] = []

        async with aiohttp.ClientSession(
            connector=connector,
            timeout=timeout,
        ) as session:
            progress = tqdm(
                total=total_users,
                desc="Sending",
                unit="req",
                dynamic_ncols=True,
            )

            async def throttled_send(user_profile: dict) -> None:
                async with semaphore:
                    payload = build_transaction(user_profile)
                    await send_transaction(session, url, payload, stats)
                    progress.update(1)

            for i, user in enumerate(users):
                task = asyncio.create_task(throttled_send(user))
                tasks.append(task)

                if (i + 1) % concurrency == 0:
                    elapsed = time.monotonic() - loop_start
                    expected = (i + 1) * interval
                    sleep_time = expected - elapsed
                    if sleep_time > 0:
                        await asyncio.sleep(sleep_time)

            await asyncio.gather(*tasks)
            progress.close()

        elapsed = time.monotonic() - loop_start
        total_requests = stats["success"] + stats["errors"]
        actual_rate = total_requests / elapsed if elapsed > 0 else 0

        latencies = stats["latencies"]
        if latencies:
            latencies.sort()
            avg_latency = sum(latencies) / len(latencies)
            p50 = latencies[len(latencies) // 2]
            p95 = latencies[int(len(latencies) * 0.95)]
            p99 = latencies[int(len(latencies) * 0.99)]
        else:
            avg_latency = p50 = p95 = p99 = 0.0

        print(f"\nResults:")
        print(f"  Duration:     {elapsed:.1f}s")
        print(f"  Total:        {total_requests:,}")
        print(f"  Success:      {stats['success']:,}")
        print(f"  Errors:       {stats['errors']:,}")
        print(f"  Actual rate:  {actual_rate:.1f} req/s")
        print(f"  Latency avg:  {avg_latency*1000:.1f} ms")
        print(f"  Latency p50:  {p50*1000:.1f} ms")
        print(f"  Latency p95:  {p95*1000:.1f} ms")
        print(f"  Latency p99:  {p99*1000:.1f} ms")
        print()

    await connector.close()


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Sentinel load test — sends realistic transactions for N users"
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost:8080",
        help="Transaction ingestor base URL (default: http://localhost:8080)",
    )
    parser.add_argument(
        "--users",
        type=int,
        default=100_000,
        help="Number of unique users to cycle through (default: 100000)",
    )
    parser.add_argument(
        "--rate",
        type=float,
        default=0.0,
        help="Target requests per second (default: auto-calculated as users/3600)",
    )
    parser.add_argument(
        "--concurrency",
        type=int,
        default=50,
        help="Max concurrent requests in flight (default: 50)",
    )
    parser.add_argument(
        "--loops",
        type=int,
        default=1,
        help="Number of full passes through user pool (0 = infinite, default: 1)",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=42,
        help="Random seed for reproducible user profiles (default: 42)",
    )
    args = parser.parse_args()

    random.seed(args.seed)
    target_rate = args.rate if args.rate > 0 else args.users / 3600.0

    print("Generating user pool...")
    users = generate_user_pool(args.users)
    random.shuffle(users)
    print(f"Generated {len(users):,} user profiles\n")

    asyncio.run(run_load_test(
        base_url=args.base_url,
        users=users,
        target_rate=target_rate,
        concurrency=args.concurrency,
        loops=args.loops,
    ))


if __name__ == "__main__":
    main()
