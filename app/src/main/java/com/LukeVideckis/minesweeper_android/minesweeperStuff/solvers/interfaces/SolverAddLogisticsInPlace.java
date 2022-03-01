package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;

public interface SolverAddLogisticsInPlace {
    //it should be this:
    //Board<TileWithLogistics> solvePosition(Board<TileNoFlagsForSolver> board) throws Exception;

    //returns true if new logical stuff is found
    boolean solvePosition(Board<TileWithLogistics> board/*input-output param, assumes logical stuff is correct*/) throws Exception;
}
