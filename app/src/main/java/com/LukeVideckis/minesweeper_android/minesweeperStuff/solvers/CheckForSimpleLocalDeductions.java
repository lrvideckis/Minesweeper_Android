package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverAddLogisticsInPlace;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.LogisticState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;

import java.util.ArrayList;

//Plan: completely re-write this class
public class CheckForSimpleLocalDeductions implements SolverAddLogisticsInPlace {

    @Override
    public boolean solvePosition(Board<TileWithLogistics> board/*input-output param, assumes logical stuff is correct*/) throws Exception {
        boolean foundNewStuff = false;
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                TileNoFlagsForSolver cell = board.getCell(i, j);
                if (!cell.isVisible) {
                    continue;
                }
                ArrayList<TileWithLogistics> adjCells = board.getAdjacentCells(i, j);
                int cntAdjacentMines = 0, cntAdjacentFrees = 0, cntTotalAdjacentCells = 0;
                for (TileWithLogistics adjTile : adjCells) {
                    if (adjTile.isVisible) {
                        continue;
                    }
                    ++cntTotalAdjacentCells;
                    if (adjTile.logic == LogisticState.MINE) {
                        ++cntAdjacentMines;
                    }
                    if (adjTile.logic == LogisticState.FREE) {
                        ++cntAdjacentFrees;
                    }
                }
                if (cntTotalAdjacentCells == 0) {
                    continue;
                }
                if (cntAdjacentMines == cell.numberSurroundingMines) {
                    //anything that's not a mine is free
                    for (TileWithLogistics adjTile : adjCells) {
                        if (adjTile.isVisible) {
                            continue;
                        }
                        if (adjTile.logic == LogisticState.MINE) {
                            continue;
                        }
                        if (adjTile.logic != LogisticState.FREE) {
                            foundNewStuff = true;
                            adjTile.logic = LogisticState.FREE;
                        }
                    }
                }
                if (cntTotalAdjacentCells - cntAdjacentFrees == cell.numberSurroundingMines) {
                    //anything that's not free is a mine
                    for (TileWithLogistics adjTile : adjCells) {
                        if (adjTile.isVisible) {
                            continue;
                        }
                        if (adjTile.logic == LogisticState.FREE) {
                            continue;
                        }
                        if (adjTile.logic != LogisticState.MINE) {
                            foundNewStuff = true;
                            adjTile.logic = LogisticState.MINE;
                        }
                    }
                }
            }
        }
        return foundNewStuff;
    }
}
