package com.LukeVideckis.minesweeper_android.activity;

import static java.lang.Thread.sleep;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ConvertGameBoardFormat;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.CreateSolvableBoard;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.HolyGrailSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.MyBacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTileWithProbability;
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
            toggleFlagModeOn = false,
            toggleBacktrackingHintsOn = false,
            toggleMineProbabilityOn = false,
            hasBeenAChangeSinceLastSolverRun = true,
            gameEndedFromHelpButton = false,
            lastActionWasGetHelpButton = false;
    private int numberOfRows, numberOfCols, numberOfMines, gameMode;
    private PopupWindow solverHitLimitPopup, getHelpModeIsDisabledPopup;
    private volatile PopupWindow couldNotFindNoGuessBoardPopup;
    private volatile MinesweeperGame minesweeperGame;
    private BacktrackingSolver holyGrailSolver;
    private VisibleTileWithProbability[][] board;
    private int lastTapRow, lastTapCol;
    private volatile Thread updateTimeThread;
    private volatile AlertDialog loadingScreenForSolvableBoardGeneration;
    private CreateSolvableBoard createSolvableBoard;
    private Thread createSolvableBoardThread, timerToBreakBoardGen = new Thread();
    private volatile SolvableBoardRunnable solvableBoardRunnable;

    public void stopTimerThread() {
        updateTimeThread.interrupt();
    }

    public void handleTap(float tapX, float tapY, boolean isLongTap) {
        if (tapX < 0f ||
                tapY < 0f ||
                tapX > numberOfCols * cellPixelLength ||
                tapY > numberOfRows * cellPixelLength) {
            return;
        }
        final int row = (int) (tapY / cellPixelLength);
        final int col = (int) (tapX / cellPixelLength);

        final boolean toggleFlag = (toggleFlagModeOn ^ isLongTap);

        if (minesweeperGame.isBeforeFirstClick() && !toggleFlag) {
            if (gameMode == R.id.no_guessing_mode || gameMode == R.id.no_guessing_mode_with_an_8) {
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

        if (!minesweeperGame.getIsGameLost()) {
            lastTapRow = row;
            lastTapCol = col;
        }

        try {
            //TODO: bug here: when you click a visible cell which results in revealing extra cells in easy/hard mode - make sure you win/lose
            //TODO: don't change mine configuration when the current config matches what you want
            if (minesweeperGame.clickCell(row, col, toggleFlag)) {
                hasBeenAChangeSinceLastSolverRun = true;
            }
            if (minesweeperGame.getIsGameLost()) {
                //run solver if in get help mode to correctly display deducible stuff (after losing)
                if (isGetHelpMode()) {
                    toggleBacktrackingHintsOn = hasBeenAChangeSinceLastSolverRun = true;
                    updateSolvedBoardWithBacktrackingSolver(false);
                }
            } else if (!(toggleFlag && !minesweeperGame.getCell(row, col).getIsVisible()) && (toggleBacktrackingHintsOn || toggleMineProbabilityOn)) {
                updateSolvedBoardWithBacktrackingSolver(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateNumberOfMines(minesweeperGame.getNumberOfMines() - minesweeperGame.getNumberOfFlags());
        lastActionWasGetHelpButton = false;
        findViewById(R.id.gridCanvas).invalidate();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.newGameButton:
                ImageButton newGameButton = findViewById(R.id.newGameButton);
                newGameButton.setImageResource(R.drawable.smiley_face);
                startNewGame();
                GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
                lastActionWasGetHelpButton = false;
                gameCanvas.invalidate();
                break;
            case R.id.toggleFlagMode:
                toggleFlagModeOn = !toggleFlagModeOn;
                Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
                if (toggleFlagModeOn) {
                    toggleFlagMode.setText(flagEmoji);
                } else {
                    toggleFlagMode.setText(mineEmoji);
                }
                break;
            case R.id.getHelpButton:
                try {
                    executeHelpButton();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
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
        switch (buttonView.getId()) {
            case R.id.toggleBacktrackingHints:
                handleHintToggle(isChecked);
                break;
            case R.id.toggleMineProbability:
                handleToggleMineProbability(isChecked);
                break;
        }
    }

    public void updateSolvedBoardWithBacktrackingSolver(boolean updatingFromGetHintButtonPress) throws Exception {
        if (!hasBeenAChangeSinceLastSolverRun) {
            return;
        }
        ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, false);
        try {
            holyGrailSolver.solvePosition(board, minesweeperGame.getNumberOfMines());
        } catch (HitIterationLimitException e) {
            if (updatingFromGetHintButtonPress) {
                displayGetHelpDisabledPopup();
            } else {
                solverHitIterationLimit();
            }
            return;
        }

        for (int i = 0; i < minesweeperGame.getRows(); ++i) {
            for (int j = 0; j < minesweeperGame.getCols(); ++j) {
                if (board[i][j].getIsLogicalMine() && !board[i][j].getMineProbability().equals(1)) {
                    throw new Exception("logical mine with non-1 mine probability " + i + " " + j);
                }
                if (board[i][j].getIsLogicalFree() && !board[i][j].getMineProbability().equals(0)) {
                    throw new Exception("logical free with non-zero mine probability " + i + " " + j);
                }
            }
        }
        minesweeperGame.updateLogicalStuff(board);
        hasBeenAChangeSinceLastSolverRun = false;
    }

    public void solverHitIterationLimit() {
        //TODO: think about changing this behavior to just (temporarily) switching modes to back to normal mode
        if (toggleBacktrackingHintsOn) {
            Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
            toggleHints.setChecked(false);
            toggleBacktrackingHintsOn = false;
        }
        if (toggleMineProbabilityOn) {
            Switch toggleProb = findViewById(R.id.toggleMineProbability);
            toggleProb.setChecked(false);
            toggleMineProbabilityOn = false;
        }
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

    public void disableSwitchesAndButtons() {
        Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
        toggleHints.setClickable(false);

        Switch toggleProbability = findViewById(R.id.toggleMineProbability);
        toggleProbability.setClickable(false);

        Button flagModeButton = findViewById(R.id.toggleFlagMode);
        flagModeButton.setClickable(false);

        ImageButton getHelp = findViewById(R.id.getHelpButton);
        getHelp.setClickable(false);
    }

    public void enableButtonsAndSwitchesAndSetToFalse() {
        Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
        toggleHints.setClickable(true);
        toggleHints.setChecked(false);

        Switch toggleProbability = findViewById(R.id.toggleMineProbability);
        toggleProbability.setClickable(true);
        toggleProbability.setChecked(false);

        Button flagModeButton = findViewById(R.id.toggleFlagMode);
        flagModeButton.setClickable(true);
        flagModeButton.setText(mineEmoji);
        toggleFlagModeOn = false;

        ImageButton getHelp = findViewById(R.id.getHelpButton);
        getHelp.setClickable(true);
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

    public MinesweeperGame getMinesweeperGame() {
        return minesweeperGame;
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public int getNumberOfCols() {
        return numberOfCols;
    }

    public boolean getToggleBacktrackingHintsOn() {
        return toggleBacktrackingHintsOn;
    }

    public boolean getToggleMineProbabilityOn() {
        return toggleMineProbabilityOn;
    }

    public VisibleTileWithProbability[][] getBoard() throws Exception {
        ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, (toggleBacktrackingHintsOn || toggleMineProbabilityOn));
        return board;
    }

    public int getLastTapRow() {
        return lastTapRow;
    }

    public int getLastTapCol() {
        return lastTapCol;
    }

    private void executeHelpButton() throws Exception {
        boolean solverHitIterationLimit = false;
        try {
            updateSolvedBoardWithBacktrackingSolver(true);
        } catch (HitIterationLimitException ignored) {
            solverHitIterationLimit = true;
        }
        try {
            minesweeperGame.revealRandomCellIfAllLogicalStuffIsCorrect(solverHitIterationLimit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        hasBeenAChangeSinceLastSolverRun = true;
        if (minesweeperGame.getIsGameLost()) {
            gameEndedFromHelpButton = true;
            updateSolvedBoardWithBacktrackingSolver(false);
            toggleBacktrackingHintsOn = true;
        } else if (toggleBacktrackingHintsOn || toggleMineProbabilityOn) {
            updateSolvedBoardWithBacktrackingSolver(false);
        }
        lastActionWasGetHelpButton = true;
        findViewById(R.id.gridCanvas).invalidate();
    }

    private void startNewGame() {
        try {
            minesweeperGame = new MinesweeperGame(numberOfRows, numberOfCols, numberOfMines);
        } catch (Exception e) {
            e.printStackTrace();
        }
        enableButtonsAndSwitchesAndSetToFalse();
        handleHintToggle(false);
        gameEndedFromHelpButton = false;
        hasBeenAChangeSinceLastSolverRun = true;

        if (timerToBreakBoardGen.isAlive()) {
            timerToBreakBoardGen.interrupt();
        }

        if (toggleFlagModeOn) {
            toggleFlagModeOn = false;
            Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
            toggleFlagMode.setText(mineEmoji);
        }

        stopTimerThread();
        updateTimeThread = new TimeUpdateThread();
        updateTime(0);
    }

    private void handleToggleMineProbability(boolean isChecked) {
        toggleMineProbabilityOn = isChecked;
        if (isChecked) {
            //TODO: don't update if hints is already enabled, it will do nothing
            try {
                updateSolvedBoardWithBacktrackingSolver(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        lastActionWasGetHelpButton = false;
        findViewById(R.id.gridCanvas).invalidate();
    }

    private void handleHintToggle(boolean isChecked) {
        toggleBacktrackingHintsOn = isChecked;
        GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
        if (isChecked) {
            try {
                updateSolvedBoardWithBacktrackingSolver(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        lastActionWasGetHelpButton = false;
        gameCanvas.invalidate();
    }

    private void setUpIterationLimitPopup() {
        solverHitLimitPopup = PopupHelper.initializePopup(this, R.layout.solver_hit_limit_popup);
        Button okButton = solverHitLimitPopup.getContentView().findViewById(R.id.solverHitLimitOkButton);
        okButton.setOnClickListener(view -> solverHitLimitPopup.dismiss());
        TextView textView = solverHitLimitPopup.getContentView().findViewById(R.id.iterationLimitText);
        String text = "Solver took more than ";
        text += NumberFormat.getNumberInstance(Locale.US).format(MyBacktrackingSolver.iterationLimit);
        text += " iterations. Hints and mine probability are currently not available.";
        textView.setText(text);
    }

    private void setUpGetHelpDisabledPopup() {
        getHelpModeIsDisabledPopup = PopupHelper.initializePopup(this, R.layout.get_help_disabled_popup);
        Button okButton = getHelpModeIsDisabledPopup.getContentView().findViewById(R.id.getHelpDisabledOkButton);
        okButton.setOnClickListener(view -> getHelpModeIsDisabledPopup.dismiss());
        TextView textView = getHelpModeIsDisabledPopup.getContentView().findViewById(R.id.getHelpDisabledText);
        String text = "Solver took more than ";
        text += NumberFormat.getNumberInstance(Locale.US).format(MyBacktrackingSolver.iterationLimit);
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
                MinesweeperGame solvableBoard = createSolvableBoard.getSolvableBoard(row, col, gameMode == R.id.no_guessing_mode_with_an_8, isInterrupted);
                if (isInterrupted.get()) {
                    if (backButtonWasPressed.get()) {
                        return;
                    }
                    //act as though solvable board generation failed to create normal board
                    throw new Exception();
                }
                synchronized (this) {
                    solvableBoard.setFlagsForHiddenCells(minesweeperGame);
                    minesweeperGame = solvableBoard;
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
                    if (minesweeperGame.clickCell(row, col, false)) {
                        hasBeenAChangeSinceLastSolverRun = true;
                    }
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
            createSolvableBoard = new CreateSolvableBoard(numberOfRows, numberOfCols, numberOfMines);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            minesweeperGame = new MinesweeperGame(numberOfRows, numberOfCols, numberOfMines);
        } catch (Exception e) {
            e.printStackTrace();
        }
        holyGrailSolver = new HolyGrailSolver(numberOfRows, numberOfCols);
        board = new VisibleTileWithProbability[numberOfRows][numberOfCols];
        for (int i = 0; i < numberOfRows; ++i) {
            for (int j = 0; j < numberOfCols; ++j) {
                board[i][j] = new VisibleTileWithProbability();
            }
        }
        ImageButton newGameButton = findViewById(R.id.newGameButton);
        newGameButton.setOnClickListener(this);
        Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
        toggleFlagMode.setOnClickListener(this);
        toggleFlagMode.setText(mineEmoji);

        Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
        toggleHints.setOnCheckedChangeListener(this);
        Switch toggleProbability = findViewById(R.id.toggleMineProbability);
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
