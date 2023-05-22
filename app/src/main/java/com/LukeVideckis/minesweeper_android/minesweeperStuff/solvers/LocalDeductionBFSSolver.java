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

        for (int i = 0; i < boardWithLogistics.getRows(); ++i) {
            for (int j = 0; j < boardWithLogistics.getCols(); ++j) {
                if(!boardWithLogistics.getCell(i, j).isVisible) {
                    continue;
                }
            }
        }

        return boardWithLogistics;
    }
    private class bfs_state {
        //represents index of cell
        private int centerI, centerJ;
        //number in range [0, 2^8)
        //012
        //3 4
        //567
        //each on-bit means that that relative tile is in the subset
        private int subsetSurroundingSquares;
        private int minPossibleMines, maxPossibleMines;
    }
    private class bfs_transition {
    }
}