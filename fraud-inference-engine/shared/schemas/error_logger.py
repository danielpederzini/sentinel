import logging


class ErrorLogger:
    """Centralized error logging for the inference engine."""

    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def warn(self, context: str, exception: Exception):
        """Log a warning with context and exception details."""
        self.logger.warning(f"{context}: {str(exception)}", exc_info=exception)

    def error(self, context: str, exception: Exception):
        """Log an error with context and exception details."""
        self.logger.error(f"{context}: {str(exception)}", exc_info=exception)
