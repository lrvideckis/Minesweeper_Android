package com.LukeVideckis.minesweeper_android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.activity.activityHelpers.DifficultyDeterminer;
import com.LukeVideckis.minesweeper_android.miscHelpers.DifficultyConstants;
import com.LukeVideckis.minesweeper_android.miscHelpers.GameModeConstants;

public class StartScreenActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    public static final String
            MY_PREFERENCES = "MyPrefs",
            NUMBER_OF_ROWS = "numRows",
            NUMBER_OF_COLS = "numCols",
            NUMBER_OF_MINES = "numMines",
            DIFFICULTY_STR = "difficultyStr",
            GAME_MODE = "gameMode";
    private static final float maxMinePercentage = 0.23f;
    private static final int rowsColsMin = 10, rowsColsMax = 30, minesMin = 8;
    private SharedPreferences sharedPreferences;
    private int gameMode, minesMax;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }
        final SeekBar rowsInput = findViewById(R.id.rowsInput);
        final SeekBar colsInput = findViewById(R.id.colsInput);
        final SeekBar mineInput = findViewById(R.id.mineInput);

        final int rows = rowsInput.getProgress() + rowsColsMin;
        final int cols = colsInput.getProgress() + rowsColsMin;
        final int mines = mineInput.getProgress() + minesMin;

        try {
            setMinMaxText(rows, cols, mines, rowsInput, colsInput, mineInput);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onClick(View v) {
        final SeekBar rowsInput = findViewById(R.id.rowsInput);
        final SeekBar colsInput = findViewById(R.id.colsInput);
        final SeekBar mineInput = findViewById(R.id.mineInput);

        final int rows = rowsInput.getProgress() + rowsColsMin;
        final int cols = colsInput.getProgress() + rowsColsMin;
        final int mines = mineInput.getProgress() + minesMin;
        RadioButton noGuessingMode, normalMode, getHelpMode;
        if (v.getId() == R.id.normal_mode) {
            noGuessingMode = findViewById(R.id.no_guessing_mode);
            noGuessingMode.setChecked(false);
            getHelpMode = findViewById(R.id.get_help_mode);
            getHelpMode.setChecked(false);
            gameMode = R.id.normal_mode;
            try {
                setMinMaxText(rows, cols, mines, rowsInput, colsInput, mineInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.no_guessing_mode) {
            normalMode = findViewById(R.id.normal_mode);
            normalMode.setChecked(false);
            getHelpMode = findViewById(R.id.get_help_mode);
            getHelpMode.setChecked(false);
            gameMode = R.id.no_guessing_mode;
            try {
                setMinMaxText(rows, cols, mines, rowsInput, colsInput, mineInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.get_help_mode) {
            normalMode = findViewById(R.id.normal_mode);
            normalMode.setChecked(false);
            noGuessingMode = findViewById(R.id.no_guessing_mode);
            noGuessingMode.setChecked(false);
            gameMode = R.id.get_help_mode;
            try {
                setMinMaxText(rows, cols, mines, rowsInput, colsInput, mineInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.normal_mode_info) {
            new AlertDialog.Builder(this)
                    .setMessage("This is normal minesweeper. The first square won't be a mine.")
                    .show();
        } else if (v.getId() == R.id.no_guessing_mode_info) {
            new AlertDialog.Builder(this)
                    .setMessage("It is possible to win the entire game with reasoning. No guessing is required. You may need to consider the total number of mines in your deductions.")
                    .show();
        } else if (v.getId() == R.id.get_help_mode_info) {
            new AlertDialog.Builder(this)
                    .setMessage("This is minesweeper with a twist: there is a help button which randomly reveals 1 square (which isn't a mine). If you tap the help button when there are un-flagged deducible mines or un-tapped deducible non-mines, then you lose.")
                    .show();
        } else if (v.getId() == R.id.rowsDecrement) {
            try {
                setMinMaxText(rows - 1, cols, mines, rowsInput, colsInput, mineInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.rowsIncrement) {
            try {
                setMinMaxText(rows + 1, cols, mines, rowsInput, colsInput, mineInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.colsDecrement) {
            try {
                setMinMaxText(rows, cols - 1, mines, rowsInput, colsInput, mineInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.colsIncrement) {
            try {
                setMinMaxText(rows, cols + 1, mines, rowsInput, colsInput, mineInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.minesDecrement) {
            try {
                setMinMaxText(rows, cols, mines - 1, rowsInput, colsInput, mineInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.minesIncrement) {
            try {
                setMinMaxText(rows, cols, mines + 1, rowsInput, colsInput, mineInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {//needed to show settings gear
        getMenuInflater().inflate(R.menu.top_toolbar_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(StartScreenActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        if (item.getItemId() == R.id.action_leaderboard) {
            final int rows = ((SeekBar) findViewById(R.id.rowsInput)).getProgress() + rowsColsMin;
            final int cols = ((SeekBar) findViewById(R.id.colsInput)).getProgress() + rowsColsMin;
            final int mines = ((SeekBar) findViewById(R.id.mineInput)).getProgress() + minesMin;

            DifficultyDeterminer difficultyDeterminer = new DifficultyDeterminer(rows, cols, mines);

            if (difficultyDeterminer.isStandardDifficulty()) {
                Intent intent = new Intent(StartScreenActivity.this, LeaderboardActivity.class);
                try {
                    intent.putExtra(DIFFICULTY_STR, difficultyDeterminer.getDifficultyAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final RadioButton noGuessMode = findViewById(R.id.no_guessing_mode);
                final RadioButton getHelpMode = findViewById(R.id.get_help_mode);
                if (noGuessMode.isChecked()) {
                    intent.putExtra(GAME_MODE, GameModeConstants.NO_GUESS_MODE);
                } else if (getHelpMode.isChecked()) {
                    intent.putExtra(GAME_MODE, GameModeConstants.GET_HELP_MODE);
                } else {//default is normal mode
                    intent.putExtra(GAME_MODE, GameModeConstants.NORMAL_MODE);
                }
                startActivity(intent);
            } else {
                new AlertDialog.Builder(this)
                        .setMessage("Set the Height, Width, Mines to Beginner, Intermediate, or Expert to view that specific leaderboard.")
                        .show();
            }
        }
        return true;
    }

    private void setMinMaxText(int rows, int cols, int mines, SeekBar rowsInput, SeekBar colsInput, SeekBar mineInput) throws Exception {
        //logic for settings rows, cols, mines
        rows = Math.min(rowsColsMax, Math.max(rowsColsMin, rows));
        cols = Math.min(rowsColsMax, Math.max(rowsColsMin, cols));

        if (gameMode == R.id.no_guessing_mode) {
            minesMax = Math.min((int) (rows * cols * maxMinePercentage), 100);
        } else if (gameMode == R.id.get_help_mode) {
            //reasoning for minus 10: 9 to ensure a zero start, and an extra 1 non-mine to ensure space for an 8
            minesMax = Math.min(rows * cols - 10, 100);
        } else {//only normal mode
            minesMax = Math.min(rows * cols - 10, 999);
        }

        if (minesMin > minesMax) {
            throw new Exception("minesMin > minesMax");
        }
        mines = Math.max(minesMin, Math.min(minesMax, mines));

        //update seekbars and UI text
        mineInput.setMax(minesMax - minesMin);

        rowsInput.setProgress(rows - rowsColsMin);
        colsInput.setProgress(cols - rowsColsMin);
        mineInput.setProgress(mines - minesMin);

        //update rows text
        TextView rowsText = findViewById(R.id.rowsText);
        String text = "Height: " + rows;
        rowsText.setText(text);

        //update cols text
        TextView colsText = findViewById(R.id.colsText);
        text = "Width: " + cols;
        colsText.setText(text);

        //update cols text
        TextView minesText = findViewById(R.id.mineText);
        text = "Mines: " + mines + '\n';
        double minePercentage = 100 * mines / (double) (rows * cols);
        text += String.format(getResources().getString(R.string.two_decimal_places), minePercentage);
        text += '%';
        minesText.setText(text);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_screen);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);

        sharedPreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);

        final Button rowsDecrement = findViewById(R.id.rowsDecrement);
        final Button rowsIncrement = findViewById(R.id.rowsIncrement);
        final Button colsDecrement = findViewById(R.id.colsDecrement);
        final Button colsIncrement = findViewById(R.id.colsIncrement);
        final Button minesDecrement = findViewById(R.id.minesDecrement);
        final Button minesIncrement = findViewById(R.id.minesIncrement);
        rowsDecrement.setOnClickListener(this);
        rowsIncrement.setOnClickListener(this);
        colsDecrement.setOnClickListener(this);
        colsIncrement.setOnClickListener(this);
        minesDecrement.setOnClickListener(this);
        minesIncrement.setOnClickListener(this);

        final SeekBar rowsInput = findViewById(R.id.rowsInput);
        final SeekBar colsInput = findViewById(R.id.colsInput);
        final SeekBar minesInput = findViewById(R.id.mineInput);

        rowsInput.setOnSeekBarChangeListener(this);
        colsInput.setOnSeekBarChangeListener(this);
        minesInput.setOnSeekBarChangeListener(this);

        RadioButton normalMode = findViewById(R.id.normal_mode);
        normalMode.setOnClickListener(this);

        RadioButton noGuessingMode = findViewById(R.id.no_guessing_mode);
        noGuessingMode.setOnClickListener(this);

        RadioButton getHelpMode = findViewById(R.id.get_help_mode);
        getHelpMode.setOnClickListener(this);

        final int previousRows = sharedPreferences.getInt(NUMBER_OF_ROWS, 10);
        final int previousCols = sharedPreferences.getInt(NUMBER_OF_COLS, 10);
        final int previousMines = sharedPreferences.getInt(NUMBER_OF_MINES, 10);
        gameMode = sharedPreferences.getInt(GAME_MODE, R.id.normal_mode);

        if (gameMode == R.id.no_guessing_mode) {
            noGuessingMode.setChecked(true);
        } else if (gameMode == R.id.get_help_mode) {
            getHelpMode.setChecked(true);
        } else {//default is normal mode
            normalMode.setChecked(true);
        }
        try {
            setMinMaxText(previousRows, previousCols, previousMines, rowsInput, colsInput, minesInput);
        } catch (Exception e) {
            e.printStackTrace();
        }
        rowsInput.setMax(rowsColsMax - rowsColsMin);
        colsInput.setMax(rowsColsMax - rowsColsMin);
        minesInput.setMax(minesMax - minesMin);

        rowsInput.setProgress(previousRows - rowsColsMin);
        colsInput.setProgress(previousCols - rowsColsMin);
        minesInput.setProgress(previousMines - minesMin);

        Button startNewGameButton = findViewById(R.id.startNewGameButton);
        startNewGameButton.setOnClickListener(new startNewGameButtonListener(rowsInput, colsInput, minesInput));

        Button beginner = findViewById(R.id.beginner);
        beginner.setOnClickListener(view -> {
            /*
             * beginner is 10 x 10 because solvable boards with an 8 aren't possible on 9x9 boards
             * with start click in the center. It was easier to just make 10x10 the default for
             * beginner boards
             */
            rowsInput.setProgress(DifficultyConstants.BEGINNER_ROWS - rowsColsMin);
            colsInput.setProgress(DifficultyConstants.BEGINNER_COLS - rowsColsMin);
            minesInput.setProgress(DifficultyConstants.BEGINNER_MINES - minesMin);
            try {
                setMinMaxText(DifficultyConstants.BEGINNER_ROWS, DifficultyConstants.BEGINNER_COLS, DifficultyConstants.BEGINNER_MINES, rowsInput, colsInput, minesInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button intermediate = findViewById(R.id.intermediate);
        intermediate.setOnClickListener(view -> {
            rowsInput.setProgress(DifficultyConstants.INTERMEDIATE_ROWS - rowsColsMin);
            colsInput.setProgress(DifficultyConstants.INTERMEDIATE_COLS - rowsColsMin);
            minesInput.setProgress(DifficultyConstants.INTERMEDIATE_MINES - minesMin);
            try {
                setMinMaxText(DifficultyConstants.INTERMEDIATE_ROWS, DifficultyConstants.INTERMEDIATE_COLS, DifficultyConstants.INTERMEDIATE_MINES, rowsInput, colsInput, minesInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button expert = findViewById(R.id.expert);
        expert.setOnClickListener(view -> {
            rowsInput.setProgress(DifficultyConstants.EXPERT_ROWS - rowsColsMin);
            colsInput.setProgress(DifficultyConstants.EXPERT_COLS - rowsColsMin);
            minesInput.setProgress(DifficultyConstants.EXPERT_MINES - minesMin);
            try {
                setMinMaxText(DifficultyConstants.EXPERT_ROWS, DifficultyConstants.EXPERT_COLS, DifficultyConstants.EXPERT_MINES, rowsInput, colsInput, minesInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        TextView normalModeInfo = findViewById(R.id.normal_mode_info);
        normalModeInfo.setOnClickListener(this);

        TextView noGuessingModeInfo = findViewById(R.id.no_guessing_mode_info);
        noGuessingModeInfo.setOnClickListener(this);

        TextView getHelpModeInfo = findViewById(R.id.get_help_mode_info);
        getHelpModeInfo.setOnClickListener(this);
    }

    private class startNewGameButtonListener implements View.OnClickListener {
        private final SeekBar rowInput;
        private final SeekBar colInput;
        private final SeekBar mineInput;

        public startNewGameButtonListener(SeekBar rowInput, SeekBar colInput, SeekBar mineInput) {
            this.rowInput = rowInput;
            this.colInput = colInput;
            this.mineInput = mineInput;
        }

        @Override
        public void onClick(View v) {
            final int numberOfRows = rowInput.getProgress() + rowsColsMin;
            final int numberOfCols = colInput.getProgress() + rowsColsMin;
            final int numberOfMines = mineInput.getProgress() + minesMin;

            final RadioButton normalMode = findViewById(R.id.normal_mode);
            final RadioButton noGuessMode = findViewById(R.id.no_guessing_mode);
            final RadioButton getHelpMode = findViewById(R.id.get_help_mode);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(NUMBER_OF_ROWS, numberOfRows);
            editor.putInt(NUMBER_OF_COLS, numberOfCols);
            editor.putInt(NUMBER_OF_MINES, numberOfMines);
            if (normalMode.isChecked()) {
                editor.putInt(GAME_MODE, R.id.normal_mode);
            } else if (noGuessMode.isChecked()) {
                editor.putInt(GAME_MODE, R.id.no_guessing_mode);
            } else if (getHelpMode.isChecked()) {
                editor.putInt(GAME_MODE, R.id.get_help_mode);
            }
            editor.apply();

            Intent intent = new Intent(StartScreenActivity.this, GameActivity.class);
            intent.putExtra(NUMBER_OF_ROWS, numberOfRows);
            intent.putExtra(NUMBER_OF_COLS, numberOfCols);
            intent.putExtra(NUMBER_OF_MINES, numberOfMines);
            if (noGuessMode.isChecked()) {
                intent.putExtra(GAME_MODE, R.id.no_guessing_mode);
            } else if (getHelpMode.isChecked()) {
                intent.putExtra(GAME_MODE, R.id.get_help_mode);
            } else {//default is normal mode
                intent.putExtra(GAME_MODE, R.id.normal_mode);
            }
            startActivity(intent);
        }
    }
}
