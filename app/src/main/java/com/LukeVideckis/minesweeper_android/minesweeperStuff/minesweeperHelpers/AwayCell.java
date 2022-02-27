package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.Tile;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileState;


public class AwayCell {
    public static int getNumberOfAwayCells(Board<TileNoFlagsForSolver> board) throws Exception {
        int cntAwayCells = 0;
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                if (isAwayCellSolver(board, i, j)) {
                    ++cntAwayCells;
                }
            }
        }
        return cntAwayCells;
    }

    //returns true if cell has no visible neighbors
    public static boolean isAwayCellSolver(Board<TileNoFlagsForSolver> board, int row, int col) throws Exception {
        if (board.getCell(row, col).isVisible) {
            return false;
        }
        for (TileNoFlagsForSolver adjTile : board.getAdjacentCells(row, col)) {
            if (adjTile.isVisible) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAwayCellEngine(Board<Tile> board, int row, int col) throws Exception {
        if (board.getCell(row, col).state == TileState.VISIBLE) {
            return false;
        }
        for (Tile adjTile : board.getAdjacentCells(row, col)) {
            if (adjTile.state == TileState.VISIBLE) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNextToAnAwayCellSolver(Board<TileNoFlagsForSolver> board, int row, int col) throws Exception {
        for (int[] adj : board.getAdjacentIndexes(row, col)) {
            if (isAwayCellSolver(board, adj[0], adj[1])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNextToAnAwayCellEngine(Board<Tile> board, int row, int col) throws Exception {
        for (int[] adj : board.getAdjacentIndexes(row, col)) {
            if (isAwayCellEngine(board, adj[0], adj[1])) {
                return true;
            }
        }
        return false;
    }
}
