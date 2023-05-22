package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;

//starting with some known mines/frees from more naive solvers, solve completely with backtracking
public interface SolverLogisticsToProbability {
    //TODO: wrap board parameter to simulate const
    //https://stackoverflow.com/questions/41361252/const-function-arguments-in-java
    Board<TileWithProbability> solvePositionWithLogistics(Board<TileWithLogistics> board) throws Exception;
}
