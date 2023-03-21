package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverAddLogisticsInPlace;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverStartingWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverWithProbability;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.LogisticState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;

public class HolyGrailSolver implements SolverWithProbability {

    private final SolverStartingWithLogistics recursiveSolver;
    private final SolverAddLogisticsInPlace gaussSolver, localSolver;
    private final int rows, cols;
    private final TileWithLogistics[][] logisticsGrid;

    public HolyGrailSolver(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        recursiveSolver = new IntenseRecursiveSolver(rows, cols);
        gaussSolver = new GaussianEliminationSolver(rows, cols);
        localSolver = new CheckForSimpleLocalDeductions();
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

        //Local & Gauss solver will leave logical frees/mines stored in logisticsBoard.
        //noinspection StatementWithEmptyBody
        while (localSolver.solvePosition(logisticsBoard) || gaussSolver.solvePosition(logisticsBoard))
            ;

        //Now use gauss solver findings in IntenseRecursiveSolver to help split by components
        return recursiveSolver.solvePositionWithLogistics(logisticsBoard);
    }
}
