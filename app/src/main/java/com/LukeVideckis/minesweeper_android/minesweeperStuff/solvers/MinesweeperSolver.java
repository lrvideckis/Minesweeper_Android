package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTile;

public interface MinesweeperSolver {
    void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception;
}
