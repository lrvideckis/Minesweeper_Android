package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;

import static com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.MinesweeperSolver.VisibleTile;

public class CheckForLocalStuff {
	public static boolean checkAndUpdateBoardForTrivialStuff(VisibleTile[][] board) throws Exception {
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		final int rows = dimensions.first, cols = dimensions.second;
		boolean foundNewStuff = false;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile cell = board[i][j];
				if (!cell.getIsVisible()) {
					continue;
				}
				final int[][] adjCells = GetAdjacentCells.getAdjacentCells(i, j, rows, cols);
				int cntAdjacentMines = 0, cntAdjacentFrees = 0, cntTotalAdjacentCells = 0;
				for (int[] adj : adjCells) {
					final int adjI = adj[0], adjJ = adj[1];
					if (board[adjI][adjJ].getIsVisible()) {
						continue;
					}
					++cntTotalAdjacentCells;
					if (board[adjI][adjJ].isLogicalMine) {
						++cntAdjacentMines;
					}
					if (board[adjI][adjJ].isLogicalFree) {
						++cntAdjacentFrees;
					}
				}
				if (cntTotalAdjacentCells == 0) {
					continue;
				}
				if (cntAdjacentMines == cell.getNumberSurroundingMines()) {
					//anything that's not a mine is free
					for (int[] adj : adjCells) {
						final int adjI = adj[0], adjJ = adj[1];
						if (board[adjI][adjJ].getIsVisible()) {
							continue;
						}
						if (board[adjI][adjJ].isLogicalMine) {
							continue;
						}
						if (!board[adjI][adjJ].isLogicalFree) {
							foundNewStuff = true;
							board[adjI][adjJ].isLogicalFree = true;
						}
					}
				}
				if (cntTotalAdjacentCells - cntAdjacentFrees == cell.getNumberSurroundingMines()) {
					//anything that's not free is a mine
					for (int[] adj : adjCells) {
						final int adjI = adj[0], adjJ = adj[1];
						if (board[adjI][adjJ].getIsVisible()) {
							continue;
						}
						if (board[adjI][adjJ].isLogicalFree) {
							continue;
						}
						if (!board[adjI][adjJ].isLogicalMine) {
							foundNewStuff = true;
							board[adjI][adjJ].isLogicalMine = true;
						}
					}
				}
			}
		}
		return foundNewStuff;
	}
}
