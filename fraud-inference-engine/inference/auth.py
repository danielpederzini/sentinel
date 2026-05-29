import logging
import os

import jwt
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import PyJWKClient

logger = logging.getLogger(__name__)

JWKS_URI = os.environ.get("JWKS_URI", "http://localhost:8081/.well-known/jwks.json")
EXPECTED_ISSUER = os.environ.get("SERVICE_AUTH_ISSUER", "anti-fraud-orchestrator")
EXPECTED_AUDIENCE = os.environ.get("SERVICE_AUTH_AUDIENCE", "sentinel-internal")

# Fetches and caches signing keys from the orchestrator's JWKS endpoint, lazily on first use.
_jwk_client = PyJWKClient(JWKS_URI)

# auto_error=False so a missing token yields our own 401 (instead of the default 403).
_bearer_scheme = HTTPBearer(auto_error=False, description="Service-to-service JWT issued by the Anti-Fraud Orchestrator")


def verify_token(credentials: HTTPAuthorizationCredentials | None = Depends(_bearer_scheme)) -> None:
    if credentials is None or not credentials.credentials:
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")

    try:
        signing_key = _jwk_client.get_signing_key_from_jwt(credentials.credentials)
        jwt.decode(
            credentials.credentials,
            signing_key.key,
            algorithms=["RS256"],
            issuer=EXPECTED_ISSUER,
            audience=EXPECTED_AUDIENCE,
        )
    except Exception as exception:
        logger.warning("JWT verification failed: %s", exception)
        raise HTTPException(status_code=401, detail="Invalid authentication token")
