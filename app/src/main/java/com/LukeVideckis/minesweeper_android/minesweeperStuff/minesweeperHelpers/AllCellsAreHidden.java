package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTile;

public class AllCellsAreHidden {
	public static boolean allCellsAreHidden(VisibleTile[][] board) throws Exception {
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		final int rows = dimensions.first;
		final int cols = dimensions.second;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsVisible()) {
					return false;
				}
			}
		}
		return true;
	}
}
