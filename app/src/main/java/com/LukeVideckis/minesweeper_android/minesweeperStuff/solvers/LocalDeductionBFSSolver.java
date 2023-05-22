package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverNothingToLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.LogisticState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;

import java.util.ArrayList;

public class LocalDeductionBFSSolver implements SolverNothingToLogistics {
    private final int rows, cols;

    public LocalDeductionBFSSolver(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    @Override
    public Board<TileWithLogistics> solvePosition(Board<TileNoFlagsForSolver> board) throws Exception {
        if(board.getRows() != rows || board.getCols() != cols) {
            throw new Exception("array bounds don't match");
        }
        //always allocate new board to avoid any potential issues with shallow copies between solver runs
        TileWithLogistics[][] tmpBoard = new TileWithLogistics[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                tmpBoard[i][j] = new TileWithLogistics();
                tmpBoard[i][j].set(board.getCell(i, j));
            }
        }
        Board<TileWithLogistics> boardWithLogistics = new Board<>(tmpBoard, board.getMines());

        boolean foundNewStuff = true;
        while(foundNewStuff) {
            foundNewStuff = false;
            for (int i = 0; i < boardWithLogistics.getRows(); ++i) {
                for (int j = 0; j < boardWithLogistics.getCols(); ++j) {
                    TileNoFlagsForSolver cell = boardWithLogistics.getCell(i, j);
                    if (!cell.isVisible) {
                        continue;
                    }
                    ArrayList<TileWithLogistics> adjCells = boardWithLogistics.getAdjacentCells(i, j);
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
        }
        return boardWithLogistics;
    }
}