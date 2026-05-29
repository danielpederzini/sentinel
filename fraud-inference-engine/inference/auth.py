import logging
import os

import jwt
from fastapi import HTTPException, Request
from jwt import PyJWKClient

logger = logging.getLogger(__name__)

JWKS_URI = os.environ.get("JWKS_URI", "http://localhost:8081/.well-known/jwks.json")
EXPECTED_ISSUER = os.environ.get("SERVICE_AUTH_ISSUER", "anti-fraud-orchestrator")
EXPECTED_AUDIENCE = os.environ.get("SERVICE_AUTH_AUDIENCE", "sentinel-internal")

_BEARER_PREFIX = "Bearer "

# Fetches and caches signing keys from the orchestrator's JWKS endpoint, lazily on first use.
_jwk_client = PyJWKClient(JWKS_URI)


def verify_token(request: Request) -> None:
    auth_header = request.headers.get("Authorization", "")
    if not auth_header.startswith(_BEARER_PREFIX):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")

    token = auth_header[len(_BEARER_PREFIX):]
    try:
        signing_key = _jwk_client.get_signing_key_from_jwt(token)
        jwt.decode(
            token,
            signing_key.key,
            algorithms=["RS256"],
            issuer=EXPECTED_ISSUER,
            audience=EXPECTED_AUDIENCE,
        )
    except Exception as exception:
        logger.warning("JWT verification failed: %s", exception)
        raise HTTPException(status_code=401, detail="Invalid authentication token")
