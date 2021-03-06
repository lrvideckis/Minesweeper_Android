package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import android.util.Pair;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;

import static com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class AwayCell {
	public static int getNumberOfAwayCells(VisibleTile[][] board) throws Exception {
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		final int rows = dimensions.first;
		final int cols = dimensions.second;
		int cntAwayCells = 0;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (isAwayCell(board, i, j, rows, cols)) {
					++cntAwayCells;
				}
			}
		}
		return cntAwayCells;
	}

	//returns true if cell has no visible neighbors
	public static boolean isAwayCell(VisibleTile[][] board, int row, int col, int rows, int cols) {
		if (board[row][col].getIsVisible()) {
			return false;
		}
		for (int[] adj : GetAdjacentCells.getAdjacentCells(row, col, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			if (board[adjI][adjJ].getIsVisible()) {
				return false;
			}
		}
		return true;
	}

	public static boolean isAwayCell(MinesweeperGame game, int row, int col) {
		if (game.getCell(row, col).getIsVisible()) {
			return false;
		}
		for (int[] adj : GetAdjacentCells.getAdjacentCells(row, col, game.getRows(), game.getCols())) {
			final int adjI = adj[0], adjJ = adj[1];
			if (game.getCell(adjI, adjJ).getIsVisible()) {
				return false;
			}
		}
		return true;
	}

	public static boolean isNextToAnAwayCell(MinesweeperGame game, int row, int col) {
		for (int[] adj : GetAdjacentCells.getAdjacentCells(row, col, game.getRows(), game.getCols())) {
			final int adjI = adj[0], adjJ = adj[1];
			if (isAwayCell(game, adjI, adjJ)) {
				return true;
			}
		}
		return false;
	}
}
