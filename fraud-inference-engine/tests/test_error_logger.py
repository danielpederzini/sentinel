import logging

from shared.schemas.error_logger import ErrorLogger
from tests.test_constants import LOG_CONTEXT, LOG_EXCEPTION_MESSAGE


def test_warn_should_log_warning_with_context_and_exception(caplog) -> None:
    logger = ErrorLogger()
    exception = RuntimeError(LOG_EXCEPTION_MESSAGE)

    with caplog.at_level(logging.WARNING, logger=logger.logger.name):
        logger.warn(LOG_CONTEXT, exception)

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelname == "WARNING"
    assert LOG_CONTEXT in record.message
    assert LOG_EXCEPTION_MESSAGE in record.message


def test_error_should_log_error_with_context_and_exception(caplog) -> None:
    logger = ErrorLogger()
    exception = ValueError(LOG_EXCEPTION_MESSAGE)

    with caplog.at_level(logging.ERROR, logger=logger.logger.name):
        logger.error(LOG_CONTEXT, exception)

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelname == "ERROR"
    assert LOG_CONTEXT in record.message
    assert LOG_EXCEPTION_MESSAGE in record.message
