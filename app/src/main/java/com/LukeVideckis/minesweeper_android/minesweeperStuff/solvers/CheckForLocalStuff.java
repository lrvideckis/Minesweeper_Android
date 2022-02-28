package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;

import java.util.ArrayList;


public class CheckForLocalStuff {
    public static boolean checkAndUpdateBoardForTrivialStuff(Board<TileWithLogistics> board/*input-output param, assumes logical stuff is correct*/) throws Exception {
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
                    if (adjTile.isLogicalMine) {
                        ++cntAdjacentMines;
                    }
                    if (adjTile.isLogicalFree) {
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
                        if (adjTile.isLogicalMine) {
                            continue;
                        }
                        if (!adjTile.isLogicalFree) {
                            foundNewStuff = true;
                            adjTile.isLogicalFree = true;
                        }
                    }
                }
                if (cntTotalAdjacentCells - cntAdjacentFrees == cell.numberSurroundingMines) {
                    //anything that's not free is a mine
                    for (TileWithLogistics adjTile : adjCells) {
                        if (adjTile.isVisible) {
                            continue;
                        }
                        if (adjTile.isLogicalFree) {
                            continue;
                        }
                        if (!adjTile.isLogicalMine) {
                            foundNewStuff = true;
                            adjTile.isLogicalMine = true;
                        }
                    }
                }
            }
        }
        return foundNewStuff;
    }
}
