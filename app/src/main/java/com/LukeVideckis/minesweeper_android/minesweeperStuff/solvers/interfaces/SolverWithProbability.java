package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;

public interface SolverWithProbability {
    Board<TileWithProbability> solvePositionWithProbability(Board<TileNoFlagsForSolver> board) throws Exception;
}
