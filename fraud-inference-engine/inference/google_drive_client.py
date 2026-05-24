from __future__ import annotations

import json
import os

from google.oauth2.service_account import Credentials
from googleapiclient.discovery import build
from googleapiclient.http import MediaIoBaseDownload

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


def download_latest_model(destination_directory: str) -> str | None:
    """Download the most recent lgbm_*.joblib file from Google Drive.

    Returns the local path of the downloaded file, or ``None`` when Google
    Drive is not configured.
    """
    folder_id = os.environ.get("GOOGLE_DRIVE_FOLDER_ID", "").strip()
    credentials_json = os.environ.get("GOOGLE_DRIVE_CREDENTIALS_JSON", "").strip()
    if not folder_id or not credentials_json:
        return None

    service = _build_drive_service()

    query = (
        f"'{folder_id}' in parents"
        " and name contains 'lgbm_'"
        " and name contains '.joblib'"
        " and trashed = false"
    )
    results = (
        service.files()
        .list(q=query, orderBy="createdTime desc", pageSize=1, fields="files(id,name)", supportsAllDrives=True, includeItemsFromAllDrives=True)
        .execute()
    )
    files = results.get("files", [])
    if not files:
        print("No model bundles found in Google Drive folder.")
        return None

    latest = files[0]
    os.makedirs(destination_directory, exist_ok=True)
    local_path = os.path.join(destination_directory, latest["name"])

    request = service.files().get_media(fileId=latest["id"], supportsAllDrives=True)
    with open(local_path, "wb") as fh:
        downloader = MediaIoBaseDownload(fh, request)
        done = False
        while not done:
            _, done = downloader.next_chunk()

    print(f"Downloaded {latest['name']} from Google Drive to {local_path}")
    return local_path
