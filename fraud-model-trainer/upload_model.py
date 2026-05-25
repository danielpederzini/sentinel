from __future__ import annotations

import argparse
import os

from minio import Minio


def upload_model(
    file_path: str,
    endpoint: str,
    access_key: str,
    secret_key: str,
    bucket: str,
    use_ssl: bool,
) -> None:
    client = Minio(endpoint, access_key=access_key, secret_key=secret_key, secure=use_ssl)

    if not client.bucket_exists(bucket):
        client.make_bucket(bucket)
        print(f"Created MinIO bucket '{bucket}'")

    object_name = os.path.basename(file_path)
    client.fput_object(bucket, object_name, file_path)
    print(f"Uploaded model to MinIO: {bucket}/{object_name}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Upload a model bundle to MinIO.")
    parser.add_argument("file_path", type=str, help="Path to the .joblib model bundle file.")
    parser.add_argument("--endpoint", type=str, default=os.environ.get("MINIO_ENDPOINT", "localhost:9000"), help="MinIO endpoint (default: localhost:9000).")
    parser.add_argument("--access-key", type=str, default=os.environ.get("MINIO_ACCESS_KEY", "minioadmin"), help="MinIO access key (default: minioadmin).")
    parser.add_argument("--secret-key", type=str, default=os.environ.get("MINIO_SECRET_KEY", "minioadmin"), help="MinIO secret key (default: minioadmin).")
    parser.add_argument("--bucket", type=str, default=os.environ.get("MINIO_BUCKET", "models"), help="MinIO bucket name (default: models).")
    parser.add_argument("--use-ssl", action="store_true", default=os.environ.get("MINIO_USE_SSL", "false").lower() == "true", help="Use SSL for MinIO connection.")
    args = parser.parse_args()

    upload_model(
        file_path=args.file_path,
        endpoint=args.endpoint,
        access_key=args.access_key,
        secret_key=args.secret_key,
        bucket=args.bucket,
        use_ssl=args.use_ssl,
    )


if __name__ == "__main__":
    main()
