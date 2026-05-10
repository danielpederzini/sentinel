from pydantic import BaseModel, Field
from http import HTTPStatus


class ErrorResponse(BaseModel):
    status_code: int
    message: str


class InferenceException(Exception):
    """Base exception for inference engine failures."""
    def __init__(self, message: str, status_code: int = HTTPStatus.INTERNAL_SERVER_ERROR):
        self.message = message
        self.status_code = status_code
        super().__init__(message)


class ModelLoadException(InferenceException):
    """Exception raised when model loading fails."""
    def __init__(self, message: str):
        super().__init__(message, HTTPStatus.SERVICE_UNAVAILABLE)


class PredictionException(InferenceException):
    """Exception raised during prediction scoring."""
    def __init__(self, message: str):
        super().__init__(message, HTTPStatus.INTERNAL_SERVER_ERROR)


class ValidationException(InferenceException):
    """Exception raised for validation failures."""
    def __init__(self, message: str):
        super().__init__(message, HTTPStatus.BAD_REQUEST)
