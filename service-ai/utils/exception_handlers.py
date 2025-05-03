from fastapi import Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException
from utils.exceptions import TokenValidationError
import logging

logger = logging.getLogger(__name__)

def add_exception_handlers(app):
    @app.exception_handler(TokenValidationError)
    async def token_validation_error_handler(request, exc: TokenValidationError):
        return JSONResponse(
            status_code=exc.status_code,
            content={"status": exc.status_code, "message": exc.message}
        )
    
    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(request: Request, exc: StarletteHTTPException):
        logger.warning(f"HTTP error {exc.status_code}: {exc.detail}")
        return JSONResponse(
            status_code=exc.status_code,
            content={"status": exc.status_code, "message": str(exc.detail)}
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        return JSONResponse(
            status_code=422,
            content={"status": 422, "message": str(exc)}
        )

    @app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception):
        logger.error(f"Unhandled exception: {exc}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"status": 500, "message": "Internal Server Error"}
        )
