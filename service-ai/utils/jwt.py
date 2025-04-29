from jose import jwt, JWTError
from config import settings
import base64
from utils.logging_util import LoggingUtil
from utils.exceptions import TokenValidationError
from fastapi import Request


logger = LoggingUtil.get_logger(__name__)

SECRET_KEY = base64.b64decode(settings.jwt_secret_key)
ALGORITHM = settings.jwt_algorithm

def verify_email_from_token(request: Request, email: str):
    try:
        token = request.cookies.get("accessToken")
        if not token:
            raise TokenValidationError("Missing access token cookie")

        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        token_email = payload.get("email")

        if token_email is None:
            raise TokenValidationError("Token does not contain email claim")

        if token_email != email:
            raise TokenValidationError("Email in token does not match request parameter", status_code=403)

    except JWTError:
        logger.error("Invalid JWT token")
        raise TokenValidationError("Invalid JWT token")

    except Exception:
        logger.error("Invalid or missing Authorization header")
        raise TokenValidationError("Invalid or missing Authorization header")