from http import HTTPStatus

from shared.schemas.exceptions import (
    ErrorResponse,
    InferenceException,
    ModelLoadException,
    PredictionException,
    ValidationException,
)
from tests.test_constants import (
    ERROR_MESSAGE,
    HTTP_STATUS_BAD_REQUEST,
    HTTP_STATUS_INTERNAL_SERVER_ERROR,
    HTTP_STATUS_SERVICE_UNAVAILABLE,
    VALIDATION_MESSAGE,
)


def test_error_response_should_store_status_code_and_message() -> None:
    response = ErrorResponse(status_code=HTTP_STATUS_BAD_REQUEST, message=VALIDATION_MESSAGE)

    assert response.status_code == HTTP_STATUS_BAD_REQUEST
    assert response.message == VALIDATION_MESSAGE


def test_inference_exception_should_store_message_and_status_code() -> None:
    exception = InferenceException(ERROR_MESSAGE, HTTPStatus.CONFLICT)

    assert str(exception) == ERROR_MESSAGE
    assert exception.message == ERROR_MESSAGE
    assert exception.status_code == HTTPStatus.CONFLICT


def test_model_load_exception_should_use_service_unavailable_status() -> None:
    exception = ModelLoadException(ERROR_MESSAGE)

    assert exception.message == ERROR_MESSAGE
    assert exception.status_code == HTTP_STATUS_SERVICE_UNAVAILABLE


def test_prediction_exception_should_use_internal_server_error_status() -> None:
    exception = PredictionException(ERROR_MESSAGE)

    assert exception.message == ERROR_MESSAGE
    assert exception.status_code == HTTP_STATUS_INTERNAL_SERVER_ERROR


def test_validation_exception_should_use_bad_request_status() -> None:
    exception = ValidationException(VALIDATION_MESSAGE)

    assert exception.message == VALIDATION_MESSAGE
    assert exception.status_code == HTTP_STATUS_BAD_REQUEST
