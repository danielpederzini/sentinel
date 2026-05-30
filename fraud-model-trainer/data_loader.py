import logging

import pandas as pd

logger = logging.getLogger(__name__)


def load_data(file_path: str) -> pd.DataFrame:
    """
    Load data from a CSV file.

    Args:
        file_path (str): Path to the CSV file containing the data.

    Returns:
        pd.DataFrame: A DataFrame containing the data.
    """
    try:
        return pd.read_csv(file_path)
    except (OSError, ValueError, pd.errors.ParserError) as exception:
        logger.error("Error loading data from %s: %s", file_path, exception)
        raise