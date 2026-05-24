from __future__ import annotations

import json
import os

from google.oauth2.service_account import Credentials
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

SCOPES = ["https://www.googleapis.com/auth/drive"]


def _build_drive_service():
    credentials_json = os.environ.get("GOOGLE_DRIVE_CREDENTIALS_JSON", "")
    if not credentials_json:
        raise RuntimeError(
            "GOOGLE_DRIVE_CREDENTIALS_JSON is not set. "
            "Provide a path to a service-account JSON file or inline JSON."
        )

    if os.path.isfile(credentials_json):
        credentials = Credentials.from_service_account_file(credentials_json, scopes=SCOPES)
    else:
        info = json.loads(credentials_json)
        credentials = Credentials.from_service_account_info(info, scopes=SCOPES)

    return build("drive", "v3", credentials=credentials)


def upload_model(local_path: str) -> str:
    """Upload a .joblib model bundle to Google Drive and return the file ID."""
    folder_id = os.environ.get("GOOGLE_DRIVE_FOLDER_ID", "").strip()
    if not folder_id:
        raise RuntimeError("GOOGLE_DRIVE_FOLDER_ID is not set.")

    service = _build_drive_service()

    file_name = os.path.basename(local_path)
    file_metadata: dict = {"name": file_name, "parents": [folder_id]}
    media = MediaFileUpload(local_path, mimetype="application/octet-stream", resumable=True)

    uploaded_file = (
        service.files()
        .create(body=file_metadata, media_body=media, fields="id,name", supportsAllDrives=True)
        .execute()
    )

    print(f"Uploaded {file_name} to Google Drive (file ID: {uploaded_file['id']})")
    return uploaded_file["id"]
