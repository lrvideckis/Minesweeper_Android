package com.LukeVideckis.minesweeper_android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.miscHelpers.PopupHelper;

//TODO: change game board scale to be pivoted around focus point instead of the middle of the screen
//TODO: add settings page were you can choose whether or not to have a zero-start, also choose iteration limit of backtracking solver, also choose defaults for Flag Mode, Game mode, etc
//TODO: Make minesweeper endless: always force >= 1 visible tile on the screen
//TODO: Recommend the guess which will reveal the greatest amount of further stuff
//TODO: save personal high scores (the time) for beginner, intermediate, expert

public class StartScreenActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    public static final String
            MY_PREFERENCES = "MyPrefs",
            NUMBER_OF_ROWS = "numRows",
            NUMBER_OF_COLS = "numCols",
            NUMBER_OF_MINES = "numMines",
            GAME_MODE = "gameMode";
    private static final float maxMinePercentage = 0.23f;
    private static final int rowsColsMax = 30;
    private SharedPreferences sharedPreferences;
    private PopupWindow normalModeInfoPopup, noGuessingModeInfoPopup, getHelpModeInfoPopup;
    private int gameMode, rowsColsMin, minesMin, minesMax;

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
            displayNormalModeInfoPopup();
        } else if (v.getId() == R.id.no_guessing_mode_info) {
            displayNoGuessingModeInfoPopup();
        } else if (v.getId() == R.id.get_help_mode_info) {
            displayGetHelpModeInfoPopup();
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

    public void displayNormalModeInfoPopup() {
        PopupHelper.displayPopup(normalModeInfoPopup, findViewById(R.id.startScreenLayout), getResources());
    }

    public void displayNoGuessingModeInfoPopup() {
        PopupHelper.displayPopup(noGuessingModeInfoPopup, findViewById(R.id.startScreenLayout), getResources());
    }

    public void displayGetHelpModeInfoPopup() {
        PopupHelper.displayPopup(getHelpModeInfoPopup, findViewById(R.id.startScreenLayout), getResources());
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
        return true;
    }

    private void setMinMaxText(int rows, int cols, int mines, SeekBar rowsInput, SeekBar colsInput, SeekBar mineInput) throws Exception {
        if (gameMode == R.id.no_guessing_mode) {
            rowsColsMin = 10;
        } else {
            rowsColsMin = 3;
        }
        rowsInput.setMax(rowsColsMax - rowsColsMin);
        colsInput.setMax(rowsColsMax - rowsColsMin);

        rows = Math.min(rowsColsMax, Math.max(rowsColsMin, rows));
        cols = Math.min(rowsColsMax, Math.max(rowsColsMin, cols));

        if (sharedPreferences.getBoolean(SettingsActivity.GENERATE_GAMES_WITH_8_SETTING, false)) {
            minesMin = 8;
        }

        if (gameMode == R.id.no_guessing_mode) {
            minesMin = 0;
            minesMax = (int) (rows * cols * maxMinePercentage);
            minesMax = Math.min(minesMax, 100);
        } else if (gameMode == R.id.get_help_mode) {
            minesMin = 0;
            minesMax = rows * cols - 9;
            minesMax = Math.min(minesMax, 100);
        } else {//only normal mode
            minesMin = 0;
            minesMax = rows * cols - 9;
            minesMax = Math.min(minesMax, 999);
        }
        mineInput.setMax(minesMax - minesMin);
        if (minesMin > minesMax) {
            throw new Exception("minesMin > minesMax");
        }
        mines = Math.max(minesMin, Math.min(minesMax, mines));

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

    private void setUpNormalModeInfoPopup() {
        normalModeInfoPopup = PopupHelper.initializePopup(this, R.layout.normal_mode_info);
        Button okButton = normalModeInfoPopup.getContentView().findViewById(R.id.normalModeInfoOkButton);
        okButton.setOnClickListener(view -> normalModeInfoPopup.dismiss());
    }

    private void setUpNoGuessingModeInfoPopup() {
        noGuessingModeInfoPopup = PopupHelper.initializePopup(this, R.layout.no_guessing_mode_info);
        Button okButton = noGuessingModeInfoPopup.getContentView().findViewById(R.id.noGuessingModeInfoOkButton);
        okButton.setOnClickListener(view -> noGuessingModeInfoPopup.dismiss());
    }

    private void setUpGetHelpModeInfoPopup() {
        getHelpModeInfoPopup = PopupHelper.initializePopup(this, R.layout.get_help_mode_info);
        Button okButton = getHelpModeInfoPopup.getContentView().findViewById(R.id.getHelpModeOkButton);
        okButton.setOnClickListener(view -> getHelpModeInfoPopup.dismiss());
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
            rowsInput.setProgress(10 - rowsColsMin);
            colsInput.setProgress(10 - rowsColsMin);
            minesInput.setProgress(10 - minesMin);
            try {
                setMinMaxText(10, 10, 10, rowsInput, colsInput, minesInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button intermediate = findViewById(R.id.intermediate);
        intermediate.setOnClickListener(view -> {
            rowsInput.setProgress(14 - rowsColsMin);
            colsInput.setProgress(16 - rowsColsMin);
            minesInput.setProgress(40 - minesMin);
            try {
                setMinMaxText(14, 16, 40, rowsInput, colsInput, minesInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button expert = findViewById(R.id.expert);
        expert.setOnClickListener(view -> {
            rowsInput.setProgress(16 - rowsColsMin);
            colsInput.setProgress(30 - rowsColsMin);
            minesInput.setProgress(99 - minesMin);
            try {
                setMinMaxText(16, 30, 99, rowsInput, colsInput, minesInput);
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

        setUpNormalModeInfoPopup();
        setUpNoGuessingModeInfoPopup();
        setUpGetHelpModeInfoPopup();
    }
}
