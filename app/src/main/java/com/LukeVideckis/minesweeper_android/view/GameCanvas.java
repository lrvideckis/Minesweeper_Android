package com.LukeVideckis.minesweeper_android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.LukeVideckis.minesweeper_android.activity.GameActivity;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines.GameState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.Tile;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;


public class GameCanvas extends View {

    private final Paint black = new Paint();
    private final DrawCellHelpers drawCellHelpers;
    private final RectF tempCellRect = new RectF();
    private final ScaleListener scaleListener;
    private Board<TileWithProbability> boardSolverOutput;

    public GameCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        black.setColor(Color.BLACK);
        black.setStrokeWidth(3);
        final GameActivity gameActivity = (GameActivity) getContext();
        scaleListener = new ScaleListener(context, this, gameActivity.getNumberOfRows(), gameActivity.getNumberOfCols());
        setOnTouchListener(scaleListener);
        drawCellHelpers = new DrawCellHelpers(context, gameActivity.getNumberOfRows(), gameActivity.getNumberOfCols());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        scaleListener.setScreenWidthAndHeight(getWidth(), getHeight());
    }

    private void drawCell(
            Canvas canvas,
            BigFraction mineProb,
            Tile gameCell,
            Boolean isMine,
            int i,
            int j,
            int startX,
            int startY,
            boolean drawRedBackground,
            boolean isGetHelp,
            GameState currGameState
    ) throws Exception {
        GameActivity gameActivity = (GameActivity) getContext();

        //start of actually drawing cell
        if (gameCell.state == TileState.VISIBLE) {
            drawCellHelpers.drawNumberedCell(canvas, gameCell.numberSurroundingMines, i, j, startX, startY);
            if (isGetHelp && currGameState == GameState.STILL_GOING) {
                drawCellHelpers.drawRedBoundary(canvas, startX, startY);
            }
            return;
        }

        boolean displayedLogicalStuff = false;
        if (drawRedBackground) {
            displayedLogicalStuff = true;
            drawCellHelpers.drawEndGameTap(canvas, i, j);

        } else if (mineProb.equals(1) && gameActivity.getToggleBacktrackingHintsOn() && gameCell.state != TileState.FLAGGED) {
            displayedLogicalStuff = true;
            drawCellHelpers.drawLogicalMine(canvas, i, j, getResources());
        } else if (mineProb.equals(0) && gameActivity.getToggleBacktrackingHintsOn() && gameCell.state != TileState.FLAGGED) {
            displayedLogicalStuff = true;
            drawCellHelpers.drawLogicalFree(canvas, i, j, getResources());
        } else {
            drawCellHelpers.drawBlankCell(canvas, i, j, getResources());
        }

        if (gameActivity.getToggleMineProbabilityOn() && gameCell.state != TileState.VISIBLE && !displayedLogicalStuff && gameCell.state != TileState.FLAGGED) {
            drawCellHelpers.drawMineProbability(canvas, startX, startY, mineProb, getResources());
        }

        if (gameCell.state == TileState.FLAGGED) {
            drawCellHelpers.drawFlag(canvas, startX, startY);
            if (currGameState == GameState.LOST) {
                if (isMine == null) {
                    throw new Exception("we should have mine info since game is over");
                }
                if (!isMine) {
                    drawCellHelpers.drawBlackX(canvas, startX, startY);
                } else if (gameActivity.isGetHelpMode() && !mineProb.equals(1)) {
                    drawCellHelpers.drawBlackX(canvas, startX, startY);
                }
            } else if (mineProb.equals(0) && (gameActivity.getToggleBacktrackingHintsOn() || gameActivity.getToggleMineProbabilityOn())) {
                drawCellHelpers.drawBlackX(canvas, startX, startY);
            }
        } else if (currGameState == GameState.LOST && isMine != null && isMine) {
            drawCellHelpers.drawMine(canvas, startX, startY);
        }
    }

    private boolean cellIsOffScreen(int startX, int startY) {
        tempCellRect.set(startX, startY, startX + GameActivity.cellPixelLength, startY + GameActivity.cellPixelLength);
        scaleListener.getMatrix().mapRect(tempCellRect);
        return (tempCellRect.right < 0 || tempCellRect.bottom < 0 || tempCellRect.top > getHeight() || tempCellRect.left > getWidth());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.setMatrix(scaleListener.getMatrix());

        GameActivity gameActivity = (GameActivity) getContext();

        final int numberOfRows = gameActivity.getEngineGetHelpMode().getRows();
        final int numberOfCols = gameActivity.getEngineGetHelpMode().getCols();

        boolean haveDrawnARow = false;

        GameState currGameState;
        try {
            boardSolverOutput = gameActivity.getSolverOutput();
            currGameState = gameActivity.getEngineGetHelpMode().getGameState();
        } catch (Exception e) {
            currGameState = GameState.STILL_GOING;
            e.printStackTrace();
        }


        for (int i = 0; i < numberOfRows; ++i) {
            boolean haveDrawnACell = false;
            for (int j = 0; j < numberOfCols; ++j) {
                final int startX = j * GameActivity.cellPixelLength;
                final int startY = i * GameActivity.cellPixelLength;
                if (cellIsOffScreen(startX, startY)) {
                    if (haveDrawnACell) {
                        break;
                    }
                    continue;
                }
                haveDrawnACell = true;
                haveDrawnARow = true;
                try {
                    drawCell(
                            canvas,
                            boardSolverOutput.getCell(i, j).mineProbability,
                            gameActivity.getEngineGetHelpMode().getCell(i, j),
                            currGameState != GameState.STILL_GOING ? gameActivity.getEngineGetHelpMode().getCellWithMine(i, j).isMine : null,
                            i,
                            j,
                            startX,
                            startY,
                            (currGameState == GameState.LOST && i == gameActivity.getLastTapRow() && j == gameActivity.getLastTapCol() && !gameActivity.getGameEndedFromHelpButton()),
                            (gameActivity.isGetHelp() && i == gameActivity.getEngineGetHelpMode().getHelpRow() && j == gameActivity.getEngineGetHelpMode().getHelpCol()),
                            currGameState
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!haveDrawnACell && haveDrawnARow) {
                break;
            }
        }
        for (int j = 0; j <= numberOfCols; ++j) {
            canvas.drawLine(j * GameActivity.cellPixelLength, 0, j * GameActivity.cellPixelLength, numberOfRows * GameActivity.cellPixelLength, black);
        }
        for (int i = 0; i <= numberOfRows; ++i) {
            canvas.drawLine(0, i * GameActivity.cellPixelLength, numberOfCols * GameActivity.cellPixelLength, i * GameActivity.cellPixelLength, black);
        }
        try {
            if (currGameState == GameState.WON) {
                gameActivity.disableSwitchesAndButtons();
                gameActivity.setNewGameButtonWinFace();
                gameActivity.stopTimerThread();
            } else if (currGameState == GameState.LOST) {
                gameActivity.disableSwitchesAndButtons();
                gameActivity.setNewGameButtonDeadFace();
                gameActivity.stopTimerThread();
            }
        } catch (Exception e) {
            //shouldn't reach here: it's from getIsGameWon -> getCell -> array out of bounds
            e.printStackTrace();
        }
    }
}
