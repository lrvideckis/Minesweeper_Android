package com.LukeVideckis.minesweeper_android.activity;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines.EngineGetHelpMode;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines.GameState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.CreateSolvableBoard;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.HolyGrailSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.IntenseRecursiveSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverWithProbability;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithMine;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;
import com.LukeVideckis.minesweeper_android.miscHelpers.PopupHelper;
import com.LukeVideckis.minesweeper_android.view.GameCanvas;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    public final static String
            flagEmoji = new String(Character.toChars(0x1F6A9)),
            mineEmoji = new String(Character.toChars(0x1F4A3));
    public static final int cellPixelLength = 150;
    private static final long
            millisecondsBeforeDisplayingLoadingScreen = 100,
            maxTimeToGenerateSolvableBoardMilliseconds = 2000;
    private final MaxTimeToCreateSolvableBoard maxTimeToCreateSolvableBoard = new MaxTimeToCreateSolvableBoard();
    private final AtomicBoolean finishedBoardGen = new AtomicBoolean(false);
    private boolean
            toggleFlagModeOn,
            gameEndedFromHelpButton = false,
            lastActionWasGetHelpButton = false;
    private int numberOfRows, numberOfCols, numberOfMines, gameMode;
    private PopupWindow solverHitLimitPopup, getHelpModeIsDisabledPopup;
    private volatile PopupWindow couldNotFindNoGuessBoardPopup;
    private volatile EngineGetHelpMode engineGetHelpMode;
    private SolverWithProbability holyGrailSolver;
    private Board<TileWithProbability> boardSolverOutput;
    private Board<TileNoFlagsForSolver> boardSolverInput;
    private int lastTapRow, lastTapCol;
    private volatile Thread updateTimeThread;
    private volatile AlertDialog loadingScreenForSolvableBoardGeneration;
    private Thread createSolvableBoardThread, timerToBreakBoardGen = new Thread(maxTimeToCreateSolvableBoard);

    private volatile SolvableBoardRunnable solvableBoardRunnable;

    public void stopTimerThread() {
        updateTimeThread.interrupt();
    }

    public void handleTap(float tapX, float tapY, boolean isLongTap) throws Exception {
        if (tapX < 0f ||
                tapY < 0f ||
                tapX > numberOfCols * cellPixelLength ||
                tapY > numberOfRows * cellPixelLength) {
            return;
        }
        final int row = (int) (tapY / cellPixelLength);
        final int col = (int) (tapX / cellPixelLength);

        final boolean toggleFlag = (toggleFlagModeOn ^ isLongTap);

        if (engineGetHelpMode.isBeforeFirstClick() && !toggleFlag) {
            if (gameMode == R.id.no_guessing_mode) {
                finishedBoardGen.set(false);

                new Thread(new DelayLoadingScreenRunnable(finishedBoardGen)).start();

                solvableBoardRunnable = new SolvableBoardRunnable(row, col);
                createSolvableBoardThread = new Thread(solvableBoardRunnable) {
                    @Override
                    public void interrupt() {
                        super.interrupt();
                        solvableBoardRunnable.interrupt();
                    }
                };
                createSolvableBoardThread.start();

                timerToBreakBoardGen = new Thread(maxTimeToCreateSolvableBoard);
                timerToBreakBoardGen.start();
                return;
            }
            updateTimeThread.start();
        }

        if (engineGetHelpMode.getGameState() != GameState.LOST) {
            lastTapRow = row;
            lastTapCol = col;
        }

        try {
            //TODO: bug here: when you click a visible cell which results in revealing extra cells in easy/hard mode - make sure you win/lose
            //TODO: don't change mine configuration when the current config matches what you want
            engineGetHelpMode.clickCell(row, col, toggleFlag);
            if (engineGetHelpMode.getGameState() == GameState.LOST) {
                //run solver if in get help mode to correctly display deducible stuff (after losing)
                if (isGetHelpMode()) {
                    try {
                        runSolver();
                    } catch (HitIterationLimitException ignored) {
                        solverHitIterationLimit();
                    }
                }
            } else if (!(toggleFlag && engineGetHelpMode.getCell(row, col).state != TileState.VISIBLE) && (getToggleBacktrackingHintsOn() || getToggleMineProbabilityOn())) {
                try {
                    runSolver();
                } catch (HitIterationLimitException ignored) {
                    solverHitIterationLimit();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateNumberOfMines(engineGetHelpMode.getNumberOfMines() - engineGetHelpMode.getNumberOfFlags());
        if (engineGetHelpMode.getGameState() == GameState.STILL_GOING) {
            lastActionWasGetHelpButton = false;
        }
        findViewById(R.id.gridCanvas).invalidate();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.newGameButton) {
            ImageButton newGameButton = findViewById(R.id.newGameButton);
            newGameButton.setImageResource(R.drawable.smiley_face);
            try {
                startNewGame();
                updateNumberOfMines(engineGetHelpMode.getNumberOfMines() - engineGetHelpMode.getNumberOfFlags());
            } catch (Exception e) {
                e.printStackTrace();
            }
            GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
            lastActionWasGetHelpButton = false;
            gameCanvas.invalidate();
        } else if (v.getId() == R.id.toggleFlagMode) {
            toggleFlagModeOn = !toggleFlagModeOn;
            Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
            if (toggleFlagModeOn) {
                toggleFlagMode.setText(flagEmoji);
            } else {
                toggleFlagMode.setText(mineEmoji);
            }
        } else if (v.getId() == R.id.getHelpButton) {
            try {
                executeHelpButton();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isGetHelp() {
        return lastActionWasGetHelpButton;
    }

    public boolean getGameEndedFromHelpButton() {
        return gameEndedFromHelpButton;
    }

    public boolean isGetHelpMode() {
        return gameMode == R.id.get_help_mode;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.toggleBacktrackingHints) {
            try {
                handleHintToggle(isChecked);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (buttonView.getId() == R.id.toggleMineProbability) {
            try {
                handleToggleMineProbability(isChecked);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runSolver() throws Exception {
        for (int i = 0; i < boardSolverInput.getRows(); i++) {
            for (int j = 0; j < boardSolverInput.getCols(); j++) {
                boardSolverInput.getCell(i, j).set(engineGetHelpMode.getCell(i, j));
            }
        }
        if (boardSolverInput.getMines() != engineGetHelpMode.getNumberOfMines()) {
            throw new Exception("number of mines doesn't match");
        }
        boardSolverOutput = holyGrailSolver.solvePositionWithProbability(boardSolverInput);
    }

    public void solverHitIterationLimit() {
        SwitchCompat toggleHints = findViewById(R.id.toggleBacktrackingHints);
        toggleHints.setChecked(false);
        SwitchCompat toggleProb = findViewById(R.id.toggleMineProbability);
        toggleProb.setChecked(false);
        PopupHelper.displayPopup(solverHitLimitPopup, findViewById(R.id.gameLayout), getResources());
    }

    public void updateNumberOfMines(int numberOfMinesLeft) {
        numberOfMinesLeft = Math.max(numberOfMinesLeft, 0);
        String minesLeft;
        if (numberOfMinesLeft < 10) {
            minesLeft = "00" + numberOfMinesLeft;
        } else if (numberOfMinesLeft < 100) {
            minesLeft = "0" + numberOfMinesLeft;
        } else {
            minesLeft = String.valueOf(numberOfMinesLeft);
        }
        TextView numberOfMines = findViewById(R.id.showNumberOfMines);
        numberOfMines.setText(minesLeft);
    }

    public void setClickabilityOfSwitchesAndButtons(boolean isClickable) {
        SwitchCompat toggleHints = findViewById(R.id.toggleBacktrackingHints);
        toggleHints.setClickable(isClickable);

        SwitchCompat toggleProbability = findViewById(R.id.toggleMineProbability);
        toggleProbability.setClickable(isClickable);

        Button flagModeButton = findViewById(R.id.toggleFlagMode);
        flagModeButton.setClickable(isClickable);

        ImageButton getHelp = findViewById(R.id.getHelpButton);
        getHelp.setClickable(isClickable);
    }

    public void setNewGameButtonDeadFace() {
        ImageButton newGameButton = findViewById(R.id.newGameButton);
        newGameButton.setImageResource(R.drawable.dead_face);
    }

    public void setNewGameButtonWinFace() {
        ImageButton newGameButton = findViewById(R.id.newGameButton);
        newGameButton.setImageResource(R.drawable.win_face);
    }

    @Override
    public void onBackPressed() {
        stopTimerThread();
        Intent intent = new Intent(GameActivity.this, StartScreenActivity.class);
        startActivity(intent);
    }

    public EngineGetHelpMode getEngineGetHelpMode() {
        return engineGetHelpMode;
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public int getNumberOfCols() {
        return numberOfCols;
    }

    public boolean getToggleBacktrackingHintsOn() {
        SwitchCompat toggleHints = findViewById(R.id.toggleBacktrackingHints);
        return toggleHints.isChecked();
    }

    public boolean getToggleMineProbabilityOn() {
        SwitchCompat toggleProbability = findViewById(R.id.toggleMineProbability);
        return toggleProbability.isChecked();
    }

    public int getLastTapRow() {
        return lastTapRow;
    }

    public int getLastTapCol() {
        return lastTapCol;
    }

    public Board<TileWithProbability> getSolverOutput() {
        return boardSolverOutput;
    }

    private void executeHelpButton() throws Exception {
        try {
            runSolver();
            //solver succeeds - check logical stuff is correct, and either reveal cell or end game
            if (engineGetHelpMode.userIdentifiedAllLogicalStuffCorrectly(boardSolverOutput)) {
                engineGetHelpMode.revealRandomCell();
                lastActionWasGetHelpButton = true;//only show red indicating square when we've newly revealed a cell
                if (getToggleBacktrackingHintsOn() || getToggleMineProbabilityOn()) {//resolve now that board is updated
                    try {
                        runSolver();
                    } catch (HitIterationLimitException ignored) {
                        solverHitIterationLimit();
                    }
                }
            } else {
                engineGetHelpMode.endGameFromFailedHint();
                gameEndedFromHelpButton = true;
                //run solver when game ends to show user what deducible stuff they missed
                try {
                    runSolver();
                } catch (HitIterationLimitException ignored) {
                    //end of game, fail quietly (no error popup, don't change hint/probability toggles)
                }
            }
        } catch (HitIterationLimitException ignored) {
            //solver doesn't succeed - display error message and reveal random cell
            displayGetHelpDisabledPopup();
            engineGetHelpMode.revealRandomCell();
            SwitchCompat toggleHints = findViewById(R.id.toggleBacktrackingHints);
            toggleHints.setChecked(false);
            SwitchCompat toggleProb = findViewById(R.id.toggleMineProbability);
            toggleProb.setChecked(false);
        }
        findViewById(R.id.gridCanvas).invalidate();
    }

    private void startNewGame() throws Exception {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences(StartScreenActivity.MY_PREFERENCES, Context.MODE_PRIVATE);
            final boolean hasAn8 = sharedPreferences.getBoolean(SettingsActivity.GENERATE_GAMES_WITH_8_SETTING, false);
            engineGetHelpMode = new EngineGetHelpMode(numberOfRows, numberOfCols, numberOfMines, hasAn8);
            TileWithProbability[][] tmpBoard = new TileWithProbability[numberOfRows][numberOfCols];
            for (int i = 0; i < numberOfRows; i++) {
                for (int j = 0; j < numberOfCols; j++) {
                    tmpBoard[i][j] = new TileWithProbability();
                }
            }
            if (getToggleBacktrackingHintsOn() || getToggleMineProbabilityOn()) {
                try {
                    runSolver();
                } catch (HitIterationLimitException e) {
                    boardSolverOutput = new Board<>(tmpBoard, numberOfMines);
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setClickabilityOfSwitchesAndButtons(true);
        gameEndedFromHelpButton = false;

        if (timerToBreakBoardGen.isAlive()) {
            timerToBreakBoardGen.interrupt();
        }

        stopTimerThread();
        updateTimeThread = new TimeUpdateThread();
        updateTime(0);
    }

    private void handleToggleMineProbability(boolean isChecked) throws Exception {
        handleEitherHintOrProbToggle(isChecked);
    }

    private void handleHintToggle(boolean isChecked) throws Exception {
        handleEitherHintOrProbToggle(isChecked);
    }

    private void handleEitherHintOrProbToggle(boolean isChecked) throws Exception {
        if (isChecked) {
            try {
                runSolver();
            } catch (HitIterationLimitException ignored) {
                solverHitIterationLimit();
            }
        }
        lastActionWasGetHelpButton = false;
        findViewById(R.id.gridCanvas).invalidate();
    }

    private void setUpIterationLimitPopup() {
        solverHitLimitPopup = PopupHelper.initializePopup(this, R.layout.solver_hit_limit_popup);
        Button okButton = solverHitLimitPopup.getContentView().findViewById(R.id.solverHitLimitOkButton);
        okButton.setOnClickListener(view -> solverHitLimitPopup.dismiss());
        TextView textView = solverHitLimitPopup.getContentView().findViewById(R.id.iterationLimitText);
        String text = "Solver took more than ";
        text += NumberFormat.getNumberInstance(Locale.US).format(IntenseRecursiveSolver.iterationLimit);
        text += " iterations. Deducible squares and mine probability are currently not available.";
        textView.setText(text);
    }

    private void setUpGetHelpDisabledPopup() {
        getHelpModeIsDisabledPopup = PopupHelper.initializePopup(this, R.layout.get_help_disabled_popup);
        Button okButton = getHelpModeIsDisabledPopup.getContentView().findViewById(R.id.getHelpDisabledOkButton);
        okButton.setOnClickListener(view -> getHelpModeIsDisabledPopup.dismiss());
        TextView textView = getHelpModeIsDisabledPopup.getContentView().findViewById(R.id.getHelpDisabledText);
        String text = "Solver took more than ";
        text += NumberFormat.getNumberInstance(Locale.US).format(IntenseRecursiveSolver.iterationLimit);
        text += " iterations. A random square will be revealed without checking if there are missed deducible squares.";
        textView.setText(text);
    }

    private void setUpNoGuessBoardPopup() {
        couldNotFindNoGuessBoardPopup = PopupHelper.initializePopup(this, R.layout.couldnt_find_no_guess_board_popup);
        Button okButton = couldNotFindNoGuessBoardPopup.getContentView().findViewById(R.id.noGuessBoardOkButton);
        okButton.setOnClickListener(view -> couldNotFindNoGuessBoardPopup.dismiss());
    }

    private void displayNoGuessBoardPopup() {
        PopupHelper.displayPopup(couldNotFindNoGuessBoardPopup, findViewById(R.id.gameLayout), getResources());
    }

    private void displayGetHelpDisabledPopup() {
        PopupHelper.displayPopup(getHelpModeIsDisabledPopup, findViewById(R.id.gameLayout), getResources());
    }

    private void updateTime(int newTime) {
        String currTime;
        if (newTime < 10) {
            currTime = "00" + newTime;
        } else if (newTime < 100) {
            currTime = "0" + newTime;
        } else {
            currTime = String.valueOf(newTime);
        }
        TextView timeText = findViewById(R.id.timeTextView);
        timeText.setText(currTime);
    }

    //TODO: move to it's own file
    private class DelayLoadingScreenRunnable implements Runnable {
        private final AtomicBoolean finishedBoardGen;

        public DelayLoadingScreenRunnable(AtomicBoolean finishedBoardGen) {
            this.finishedBoardGen = finishedBoardGen;
        }

        public void run() {
            try {
                sleep(millisecondsBeforeDisplayingLoadingScreen);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!finishedBoardGen.get()) {
                runOnUiThread(() -> loadingScreenForSolvableBoardGeneration.show());
            } else {
                lastActionWasGetHelpButton = false;
                findViewById(R.id.gridCanvas).invalidate();
            }
        }
    }

    //TODO: move to it's own file
    private class MaxTimeToCreateSolvableBoard implements Runnable {
        public void run() {
            try {
                sleep(maxTimeToGenerateSolvableBoardMilliseconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (createSolvableBoardThread.isAlive()) {
                createSolvableBoardThread.interrupt();
            }
        }
    }

    //TODO: move to it's own file
    private class SolvableBoardRunnable implements Runnable {
        private final AtomicBoolean
                isInterrupted = new AtomicBoolean(false);
        private final AtomicBoolean backButtonWasPressed = new AtomicBoolean(false);
        private final int row;
        private final int col;

        public SolvableBoardRunnable(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public void run() {
            try {
                SharedPreferences sharedPreferences = getSharedPreferences(StartScreenActivity.MY_PREFERENCES, Context.MODE_PRIVATE);
                final boolean hasAn8 = sharedPreferences.getBoolean(SettingsActivity.GENERATE_GAMES_WITH_8_SETTING, false);
                Board<TileWithMine> solvableBoard = CreateSolvableBoard.getSolvableBoard(numberOfRows, numberOfCols, numberOfMines, row, col, hasAn8, isInterrupted);
                if (isInterrupted.get()) {
                    if (backButtonWasPressed.get()) {
                        return;
                    }
                    //act as though solvable board generation failed to create normal board
                    throw new Exception();
                }
                synchronized (this) {
                    for (int i = 0; i < numberOfRows; i++) {
                        for (int j = 0; j < numberOfCols; j++) {
                            if (engineGetHelpMode.getCell(i, j).state == TileState.FLAGGED) {
                                solvableBoard.getCell(i, j).state = TileState.FLAGGED;
                            }
                        }
                    }
                    engineGetHelpMode = new EngineGetHelpMode(solvableBoard, row, col, hasAn8);
                }
                finishedBoardGen.set(true);
                updateTimeThread.start();
                runOnUiThread(() -> {
                    loadingScreenForSolvableBoardGeneration.dismiss();
                    lastActionWasGetHelpButton = false;
                    findViewById(R.id.gridCanvas).invalidate();
                });
            } catch (Exception ignored) {
                finishedBoardGen.set(true);
                updateTimeThread.start();
                try {
                    engineGetHelpMode.clickCell(row, col, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(() -> {
                    displayNoGuessBoardPopup();
                    loadingScreenForSolvableBoardGeneration.dismiss();
                    lastActionWasGetHelpButton = false;
                    findViewById(R.id.gridCanvas).invalidate();
                });
            }
        }

        public void setBackButtonPressed() {
            backButtonWasPressed.set(true);
        }

        public void interrupt() {
            isInterrupted.set(true);
        }
    }

    private class TimeUpdateThread extends Thread {
        @Override
        public void run() {
            try {
                synchronized (this) {
                    AtomicInteger time = new AtomicInteger(-1);
                    while (time.incrementAndGet() <= 999) {
                        runOnUiThread(() -> updateTime(time.get()));
                        wait(1000);
                    }
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        numberOfRows = getIntent().getIntExtra(StartScreenActivity.NUMBER_OF_ROWS, 1);
        numberOfCols = getIntent().getIntExtra(StartScreenActivity.NUMBER_OF_COLS, 1);
        numberOfMines = getIntent().getIntExtra(StartScreenActivity.NUMBER_OF_MINES, 1);
        //default game mode is normal mode
        gameMode = getIntent().getIntExtra(StartScreenActivity.GAME_MODE, R.id.normal_mode);
        setContentView(R.layout.game);

        try {
            SharedPreferences sharedPreferences = getSharedPreferences(StartScreenActivity.MY_PREFERENCES, Context.MODE_PRIVATE);
            final boolean hasAn8 = sharedPreferences.getBoolean(SettingsActivity.GENERATE_GAMES_WITH_8_SETTING, false);
            engineGetHelpMode = new EngineGetHelpMode(numberOfRows, numberOfCols, numberOfMines, hasAn8);
            holyGrailSolver = new HolyGrailSolver(numberOfRows, numberOfCols);
            TileNoFlagsForSolver[][] tmpIn = new TileNoFlagsForSolver[numberOfRows][numberOfCols];
            for (int i = 0; i < numberOfRows; i++) {
                for (int j = 0; j < numberOfCols; j++) {
                    tmpIn[i][j] = new TileNoFlagsForSolver();
                }
            }
            boardSolverInput = new Board<>(tmpIn, numberOfMines);
            TileWithProbability[][] tmpOut = new TileWithProbability[numberOfRows][numberOfCols];
            for (int i = 0; i < numberOfRows; i++) {
                for (int j = 0; j < numberOfCols; j++) {
                    tmpOut[i][j] = new TileWithProbability();
                }
            }
            boardSolverOutput = new Board<>(tmpOut, numberOfMines);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ImageButton newGameButton = findViewById(R.id.newGameButton);
        newGameButton.setOnClickListener(this);
        Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
        toggleFlagMode.setOnClickListener(this);

        SharedPreferences sharedPreferences = getSharedPreferences(StartScreenActivity.MY_PREFERENCES, Context.MODE_PRIVATE);
        toggleFlagModeOn = sharedPreferences.getBoolean(SettingsActivity.TOGGLE_FLAGS_SETTING, false);
        if (toggleFlagModeOn) {
            toggleFlagMode.setText(flagEmoji);
        } else {
            toggleFlagMode.setText(mineEmoji);
        }

        SwitchCompat toggleHints = findViewById(R.id.toggleBacktrackingHints);
        toggleHints.setOnCheckedChangeListener(this);
        SwitchCompat toggleProbability = findViewById(R.id.toggleMineProbability);
        toggleProbability.setOnCheckedChangeListener(this);

        ImageButton getHelpButton = findViewById(R.id.getHelpButton);
        getHelpButton.setOnClickListener(this);
        if (gameMode == R.id.get_help_mode) {
            getHelpButton.setVisibility(View.VISIBLE);
        }

        updateNumberOfMines(numberOfMines);
        setUpIterationLimitPopup();
        setUpGetHelpDisabledPopup();
        setUpNoGuessBoardPopup();

        updateTimeThread = new TimeUpdateThread();

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setOnKeyListener((dialog, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK &&
                            event.getAction() == KeyEvent.ACTION_UP &&
                            !event.isCanceled()) {
                        dialog.cancel();
                        solvableBoardRunnable.setBackButtonPressed();
                        createSolvableBoardThread.interrupt();
                        onBackPressed();
                        return true;
                    }
                    return false;
                });

        builder.setCancelable(false);
        builder.setView(R.layout.layout_loading_dialog);
        loadingScreenForSolvableBoardGeneration = builder.create();
    }
}
