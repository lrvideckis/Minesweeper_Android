package com.LukeVideckis.minesweeper_android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.LukeVideckis.minesweeper_android.activity.GameActivity;
import com.LukeVideckis.minesweeper_android.activity.ScaleListener;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;

import static com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BacktrackingSolver.VisibleTileWithProbability;

public class GameCanvas extends View {

	private final Paint black = new Paint();
	private final DrawCellHelpers drawCellHelpers;
	private final RectF tempCellRect = new RectF();
	private final ScaleListener scaleListener;
	private VisibleTileWithProbability[][] visibleBoard;

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
			VisibleTileWithProbability solverCell,
			MinesweeperGame.Tile gameCell,
			int i,
			int j,
			int startX,
			int startY,
			boolean drawRedBackground,
			boolean isGetHelp
	) throws Exception {

		//error checking
		GameActivity gameActivity = (GameActivity) getContext();
		if (gameActivity.getToggleBacktrackingHintsOn() || gameActivity.getToggleMineProbabilityOn()) {
			if (!solverCell.isEverythingEqual(gameCell)) {
				throw new Exception("solver cell doesn't match game cell (including logical stuff)");
			}
		} else {
			if (!solverCell.isNonLogicalStuffEqual(gameCell)) {
				throw new Exception("solver cell doesn't match game cell (non logical stuff)");
			}
		}
		if (solverCell.getIsLogicalMine() && !gameCell.isMine()) {
			throw new Exception("solver says: logical mine, but it's not a mine");
		}
		if (solverCell.getIsLogicalFree() && gameCell.isMine()) {
			throw new Exception("gauss solver says: logical free, but it's not free");
		}
		if (gameActivity.getMinesweeperGame().getIsGameWon() && gameActivity.getMinesweeperGame().getIsGameLost()) {
			throw new Exception("game is both won and lost");
		}


		if (gameCell.getIsVisible()) {
			drawCellHelpers.drawNumberedCell(canvas, gameCell.getNumberSurroundingMines(), i, j, startX, startY);
			if (isGetHelp) {
				drawCellHelpers.drawRedBoundary(canvas, startX, startY);
			}
			return;
		}

		boolean displayedLogicalStuff = false;
		if (drawRedBackground) {
			displayedLogicalStuff = true;
			drawCellHelpers.drawEndGameTap(canvas, i, j);
		} else if (solverCell.getIsLogicalMine() && gameActivity.getToggleBacktrackingHintsOn() && !gameCell.isFlagged()) {
			displayedLogicalStuff = true;
			drawCellHelpers.drawLogicalMine(canvas, i, j, getResources());
		} else if (solverCell.getIsLogicalFree() && gameActivity.getToggleBacktrackingHintsOn() && !gameCell.isFlagged()) {
			displayedLogicalStuff = true;
			drawCellHelpers.drawLogicalFree(canvas, i, j, getResources());
		} else {
			drawCellHelpers.drawBlankCell(canvas, i, j, getResources());
		}

		if (gameActivity.getToggleMineProbabilityOn() && !solverCell.getIsVisible() && !displayedLogicalStuff && !gameCell.isFlagged()) {
			drawCellHelpers.drawMineProbability(canvas, startX, startY, solverCell.getMineProbability(), getResources());
		}

		if (gameCell.isFlagged()) {
			drawCellHelpers.drawFlag(canvas, startX, startY);
			if (gameActivity.getMinesweeperGame().getIsGameLost()) {
				if (!gameCell.isMine()) {
					drawCellHelpers.drawBlackX(canvas, startX, startY);
				} else if (gameActivity.isGetHelpMode() && !gameCell.getIsLogicalMine()) {
					drawCellHelpers.drawBlackX(canvas, startX, startY);
				}
			} else if (solverCell.getIsLogicalFree() && (gameActivity.getToggleBacktrackingHintsOn() || gameActivity.getToggleMineProbabilityOn())) {
				drawCellHelpers.drawBlackX(canvas, startX, startY);
			}
		} else if (gameCell.isMine() && gameActivity.getMinesweeperGame().getIsGameLost()) {
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

		final int numberOfRows = gameActivity.getMinesweeperGame().getRows();
		final int numberOfCols = gameActivity.getMinesweeperGame().getCols();

		boolean haveDrawnARow = false;

		try {
			visibleBoard = gameActivity.getBoard();
		} catch (Exception e) {
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
							visibleBoard[i][j],
							gameActivity.getMinesweeperGame().getCell(i, j),
							i,
							j,
							startX,
							startY,
							(gameActivity.getMinesweeperGame().getIsGameLost() && i == gameActivity.getLastTapRow() && j == gameActivity.getLastTapCol() && !gameActivity.getGameEndedFromHelpButton()),
							(gameActivity.isGetHelp() && i == gameActivity.getMinesweeperGame().getHelpRow() && j == gameActivity.getMinesweeperGame().getHelpCol())
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
		if (gameActivity.getMinesweeperGame().getIsGameWon()) {
			gameActivity.disableSwitchesAndButtons();
			gameActivity.setNewGameButtonWinFace();
			gameActivity.stopTimerThread();
		} else if (gameActivity.getMinesweeperGame().getIsGameLost()) {
			gameActivity.disableSwitchesAndButtons();
			gameActivity.setNewGameButtonDeadFace();
			gameActivity.stopTimerThread();
		}
	}
}
