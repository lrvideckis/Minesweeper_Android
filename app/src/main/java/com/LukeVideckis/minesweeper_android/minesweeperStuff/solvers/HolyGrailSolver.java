package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTile;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTileWithProbability;

public class HolyGrailSolver implements BacktrackingSolver {

	private final BacktrackingSolver myBacktrackingSolver;
	private final MinesweeperSolver gaussSolver;
	private final VisibleTileWithProbability[][] tempBoardWithProbability;
	private final int rows, cols;

	public HolyGrailSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		myBacktrackingSolver = new MyBacktrackingSolver(rows, cols);
		gaussSolver = new GaussianEliminationSolver(rows, cols);
		tempBoardWithProbability = new VisibleTileWithProbability[rows][cols];
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				tempBoardWithProbability[i][j] = new VisibleTileWithProbability(board[i][j]);
			}
		}
		solvePosition(tempBoardWithProbability, numberOfMines);
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				board[i][j].set(tempBoardWithProbability[i][j]);
			}
		}
	}

	@Override
	public void solvePosition(VisibleTileWithProbability[][] board, int numberOfMines) throws Exception {
		gaussSolver.solvePosition(board, numberOfMines);
		myBacktrackingSolver.solvePosition(board, numberOfMines);
	}
}
