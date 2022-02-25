package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTile;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTileWithProbability;

public interface BacktrackingSolver extends MinesweeperSolver {

	void solvePosition(VisibleTileWithProbability[][] board, int numberOfMines) throws Exception;

}
