package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverAddLogisticsInPlace;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverStartingWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverWithProbability;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;

public class HolyGrailSolver implements SolverWithProbability {

    private final SolverStartingWithLogistics recursiveSolver;
    private final SolverAddLogisticsInPlace gaussSolver;
    private final int rows, cols;
    private final TileWithLogistics[][] logisticsGrid;

    public HolyGrailSolver(int rows, int cols) throws Exception {
        this.rows = rows;
        this.cols = cols;
        recursiveSolver = new IntenseRecursiveSolver(rows, cols);
        gaussSolver = new GaussianEliminationSolver(rows, cols);
        logisticsGrid = new TileWithLogistics[rows][cols];
    }

    @Override
    public Board<TileWithProbability> solvePositionWithProbability(Board<TileNoFlagsForSolver> board) throws Exception {
        if (rows != board.getRows() || cols != board.getCols()) {
            throw new Exception("board dimensions don't match");
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                logisticsGrid[i][j].set(board.getCell(i, j));
                logisticsGrid[i][j].isLogicalFree = false;
                logisticsGrid[i][j].isLogicalMine = false;
            }
        }
        Board<TileWithLogistics> logisticsBoard = new Board<>(logisticsGrid, board.getMines());

        //gauss solver runs until it doesn't find anything (including CheckForLocalStuff).
        //It leaves logical frees/mines stored in logisticsBoard.
        gaussSolver.solvePosition(logisticsBoard);

        //Now use gauss solver findings in MyBackrackingSolver to help split by components
        return recursiveSolver.solvePositionWithLogistics(logisticsBoard);
    }
}
