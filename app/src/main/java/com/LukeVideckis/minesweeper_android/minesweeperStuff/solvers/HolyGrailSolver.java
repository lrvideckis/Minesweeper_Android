package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BfsSolver.LocalDeductionBFSSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverNothingToLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverLogisticsToProbability;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverNothingToProbability;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.LogisticState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;

public class HolyGrailSolver implements SolverNothingToProbability {

    private final SolverLogisticsToProbability recursiveSolver;
    private final SolverNothingToLogistics localSolver;
    private final int rows, cols;
    private final TileWithLogistics[][] logisticsGrid;

    public HolyGrailSolver(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        localSolver = new LocalDeductionBFSSolver(rows, cols);
        recursiveSolver = new IntenseRecursiveSolver(rows, cols);
        logisticsGrid = new TileWithLogistics[rows][cols];
    }

    @Override
    // Leaves mine probability for visible cells equal to 0
    // So don't check for logical frees by just checking mine prob = 0. You also have to check that cell is not visible.
    public Board<TileWithProbability> solvePositionWithProbability(Board<TileNoFlagsForSolver> board) throws Exception {
        if (rows != board.getRows() || cols != board.getCols()) {
            throw new Exception("board dimensions don't match");
        }
        Board<TileWithLogistics> logisticsBoard = localSolver.solvePosition(board);
        boolean debugBFSSolver = true;
        if(debugBFSSolver) {
            TileWithProbability[][] tmpBoard = new TileWithProbability[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    tmpBoard[i][j] = new TileWithProbability();
                    tmpBoard[i][j].set(logisticsBoard.getCell(i, j));
                    if (logisticsBoard.getCell(i, j).logic == LogisticState.FREE) {
                        tmpBoard[i][j].mineProbability.setValues(0, 1);
                    } else if (logisticsBoard.getCell(i, j).logic == LogisticState.MINE) {
                        tmpBoard[i][j].mineProbability.setValues(1, 1);
                    } else {
                        tmpBoard[i][j].mineProbability.setValues(1, 2);
                    }
                }
            }
            return new Board<TileWithProbability>(tmpBoard, board.getMines());
        }
        //Now use bfs solver findings in IntenseRecursiveSolver to help split by components
        return recursiveSolver.solvePositionWithLogistics(logisticsBoard);
    }
}
