package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
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
        recursiveSolver = new IntenseRecursiveSolver(rows, cols);
        localSolver = new LocalDeductionBFSSolver();
        logisticsGrid = new TileWithLogistics[rows][cols];
    }

    @Override
    // Leaves mine probability for visible cells equal to 0
    // So don't check for logical frees by just checking mine prob = 0. You also have to check that cell is not visible.
    public Board<TileWithProbability> solvePositionWithProbability(Board<TileNoFlagsForSolver> board) throws Exception {
        if (rows != board.getRows() || cols != board.getCols()) {
            throw new Exception("board dimensions don't match");
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                logisticsGrid[i][j] = new TileWithLogistics();
                logisticsGrid[i][j].set(board.getCell(i, j));
                logisticsGrid[i][j].logic = LogisticState.UNKNOWN;
            }
        }
        Board<TileWithLogistics> logisticsBoard = new Board<>(logisticsGrid, board.getMines());

        localSolver.solvePosition(logisticsBoard);

        //Now use gauss solver findings in IntenseRecursiveSolver to help split by components
        return recursiveSolver.solvePositionWithLogistics(logisticsBoard);
    }
}
