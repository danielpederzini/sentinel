import pandas as pd

def load_data(file_path: str) -> pd.DataFrame:
    """
    Load data from a CSV file.

    Args:
        file_path (str): Path to the CSV file containing the data.

    Returns:
        pd.DataFrame: A DataFrame containing the data.
    """
    try:
        data = pd.read_csv(file_path)
        return data
    except Exception as exception:
        print(f"Error loading data: {exception}")
        raise