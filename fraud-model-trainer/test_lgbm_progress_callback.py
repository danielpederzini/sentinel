from __future__ import annotations

import unittest
from unittest.mock import MagicMock, patch

from training_pipeline import _LGBMProgressCallback

N_ESTIMATORS = 5
LAST_ITERATION = 4
NON_FINAL_ITERATION = 2


class LGBMProgressCallbackTests(unittest.TestCase):
    @patch("training_pipeline.tqdm")
    def test_callback_should_update_progress_each_iteration(self, mock_tqdm: MagicMock) -> None:
        mock_bar = MagicMock()
        mock_tqdm.return_value = mock_bar
        callback = _LGBMProgressCallback(N_ESTIMATORS)
        env = MagicMock(iteration=NON_FINAL_ITERATION, end_iteration=N_ESTIMATORS)

        callback(env)

        mock_tqdm.assert_called_once_with(total=N_ESTIMATORS, desc="Training", unit="round")
        mock_bar.update.assert_called_once_with(1)
        mock_bar.close.assert_not_called()

    @patch("training_pipeline.tqdm")
    def test_callback_should_close_bar_on_final_iteration(self, mock_tqdm: MagicMock) -> None:
        mock_bar = MagicMock()
        mock_tqdm.return_value = mock_bar
        callback = _LGBMProgressCallback(N_ESTIMATORS)
        env = MagicMock(iteration=LAST_ITERATION, end_iteration=N_ESTIMATORS)

        callback(env)

        mock_bar.update.assert_called_once_with(1)
        mock_bar.close.assert_called_once()


if __name__ == "__main__":
    unittest.main()
