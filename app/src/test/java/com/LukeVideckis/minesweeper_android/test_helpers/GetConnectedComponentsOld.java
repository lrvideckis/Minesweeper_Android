package com.LukeVideckis.minesweeper_android.test_helpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.Dsu;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.RowColToIndex;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTile;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;;

import java.util.ArrayList;


public class GetConnectedComponentsOld {


	private static int rows, cols;

	public static ArrayList<ArrayList<Pair<Integer, Integer>>> getComponentsWithKnownCellsOld(VisibleTile[][] board) throws Exception {
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		Dsu disjointSet = new Dsu(rows * cols);
		boolean[][] unknownStatusSpot = new boolean[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (!board[i][j].getIsVisible()) {
					continue;
				}
				for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
					final int adjI = adj[0], adjJ = adj[1];
					VisibleTile adjTile = board[adjI][adjJ];
					if (adjTile.getIsVisible() || adjTile.getIsLogicalMine() || adjTile.getIsLogicalFree()) {
						continue;
					}
					disjointSet.merge(RowColToIndex.rowColToIndex(i, j, rows, cols), RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols));
					unknownStatusSpot[adjI][adjJ] = true;
				}
			}
		}
		boolean[][] visited = new boolean[rows][cols];
		ArrayList<ArrayList<Pair<Integer, Integer>>> components = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (visited[i][j] || !unknownStatusSpot[i][j]) {
					continue;
				}
				ArrayList<Pair<Integer, Integer>> component = new ArrayList<>();
				dfs(i, j, component, visited, unknownStatusSpot, disjointSet.find(RowColToIndex.rowColToIndex(i, j, rows, cols)), disjointSet);
				components.add(component);
			}
		}
		return components;
	}

	private static void dfs(
			int i,
			int j,
			ArrayList<Pair<Integer, Integer>> component,
			boolean[][] visited,
			boolean[][] unknownStatusSpot,
			int ccId,
			Dsu disjointSet
	) {
		component.add(new Pair<>(i, j));
		visited[i][j] = true;
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			if (visited[adjI][adjJ] || !unknownStatusSpot[adjI][adjJ] || ccId != disjointSet.find(RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols))) {
				continue;
			}
			dfs(adjI, adjJ, component, visited, unknownStatusSpot, ccId, disjointSet);
		}
		for (int di = -2; di <= 2; ++di) {
			for (int dj = -2; dj <= 2; ++dj) {
				if (di == 0 && dj == 0) {
					continue;
				}
				final int adjI = i + di;
				final int adjJ = j + dj;
				if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
					continue;
				}
				if (visited[adjI][adjJ] || !unknownStatusSpot[adjI][adjJ] || ccId != disjointSet.find(RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols))) {
					continue;
				}
				dfs(adjI, adjJ, component, visited, unknownStatusSpot, ccId, disjointSet);
			}
		}
	}
}
