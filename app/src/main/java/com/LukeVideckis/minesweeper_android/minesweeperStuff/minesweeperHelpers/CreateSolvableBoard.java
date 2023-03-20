package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines.EngineForCreatingSolvableBoard;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines.GameState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.CheckForSimpleLocalDeductions;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.IntenseRecursiveSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverAddLogisticsInPlace;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverStartingWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.LogisticState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithMine;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CreateSolvableBoard {
    private CreateSolvableBoard() throws Exception {
        throw new Exception("No instances allowed!");
    }

    //returns positions of mines as we need to initialize the game engine with this board. and it's
    //exponentially hard to figure out where the mines are from the visible board. We know where the
    //mines are from this function. Let's not re-solve for mine locations.
    //
    //really, this should only return the positions of mines
    public static Board<TileWithMine> getSolvableBoard(final int rows, final int cols, final int mines, final int firstClickI, final int firstClickJ, final boolean hasAn8, AtomicBoolean isInterrupted) throws Exception {
        TileWithLogistics[][] tmpBoard = new TileWithLogistics[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                tmpBoard[i][j] = new TileWithLogistics();
            }
        }
        Board<TileWithLogistics> solverBoard = new Board<>(tmpBoard, mines);
        //intentionally not holy grail solver to be more precise when we do backtracking
        SolverStartingWithLogistics myBacktrackingSolver = new IntenseRecursiveSolver(rows, cols);
        SolverAddLogisticsInPlace localSolver = new CheckForSimpleLocalDeductions();
        SolverAddLogisticsInPlace gaussSolver = new GaussianEliminationSolver(rows, cols);

        if (solverBoard.outOfBounds(firstClickI, firstClickJ)) {
            throw new Exception("first click is out of bounds");
        }

        Stack<EngineForCreatingSolvableBoard> gameStack = new Stack<>();

        while (!isInterrupted.get()) {
            EngineForCreatingSolvableBoard gameEngine = new EngineForCreatingSolvableBoard(rows, cols, mines, hasAn8);
            gameEngine.clickCell(firstClickI, firstClickJ, false);

            /* Main board generation loop.
             * I'm calling an "interesting" mine a mine which is next to at least 1 clue
             *
             * In this loop, we try to create a solvable board by this
             * algorithm:
             *
             * 		while(board isn't won yet) {
             *	 		while (there exists a deducible non-mine square) {
             * 				click all those deducible non-mine squares;
             * 				continue;
             *  		}
             *
             * 			//now there aren't any deducible mine-free squares and the game isn't won, so
             * 			//we resort to moving positions of mines. Also, we'll change 1 mine to a
             * 			//non-"interesting" square (a square not next to any clue).
             *
             * 			//Doing this has pros:
             * 			//		- the entire board generation algorithm runs faster (fast enough to
             * 			//		  execute in real time for the user).
             * 			//		- this step will always eventually produce a deducible free square as
             * 			//		  eventually one border clue will become 0, leading to more clues.
             *
             * 			//And cons:
             * 			//		- many mines will be eventually moved to the outside of the board
             * 			//		  effectively making the board smaller
             * 			//		- the mine density of the inside of the board will be smaller, which
             * 			//		  generally creates easier boards
             *
             * 			randomly move the positions of non-deducible "interesting" mines, and move 1
             * 			"interesting" mine to a square not next to any mines;
             *  	}
             *
             *
             * The above algorithm can fail to generate a solvable board when there's a 50/50 at the
             * very end (there may be other cases when the alg. fails). This is why there is an outer
             * loop. So we can restart completely on a fresh random board.
             */
            int cnt = 0;
            GameState currState;
            while ((currState = gameEngine.getGameState()) != GameState.WON && !isInterrupted.get()) {
                if (currState == GameState.LOST) {
                    throw new Exception("game is lost, but board generator should never lose");
                }

                for (int i = 0; i < solverBoard.getRows(); i++) {
                    for (int j = 0; j < solverBoard.getCols(); j++) {
                        TileWithLogistics curr = solverBoard.getCell(i, j);
                        curr.set(gameEngine.getCell(i, j));
                        curr.logic = LogisticState.UNKNOWN;
                    }
                }

                /* Try to deduce free squares with local rules, and then Gaussian Elimination. There
                 * is the possibility of not finding deducible free squares, even if they exist.
                 */
                if (localSolver.solvePosition(solverBoard) || gaussSolver.solvePosition(solverBoard)) {
                    if (everyComponentHasLogicalFrees(gameEngine, solverBoard)) {
                        gameStack.push(new EngineForCreatingSolvableBoard(gameEngine));
                    }
                    if (clickedLogicalFrees(gameEngine, solverBoard)) {
                        continue;
                    }
                }

                try {
                    {
                        Board<TileWithProbability> tmpResult = myBacktrackingSolver.solvePositionWithLogistics(solverBoard);
                        for (int i = 0; i < rows; i++) {
                            for (int j = 0; j < cols; j++) {
                                TileWithLogistics solverCell = solverBoard.getCell(i, j);
                                TileWithProbability tmpCell = tmpResult.getCell(i, j);
                                solverCell.set(tmpCell);
                                if (solverCell.isVisible) {
                                    solverCell.logic = LogisticState.UNKNOWN;
                                } else {
                                    solverCell.setLogic(tmpCell.mineProbability);
                                }
                            }
                        }
                    }
                    if (everyComponentHasLogicalFrees(gameEngine, solverBoard)) {
                        gameStack.push(new EngineForCreatingSolvableBoard(gameEngine));
                    }
                    if (clickedLogicalFrees(gameEngine, solverBoard)) {
                        continue;
                    }
                } catch (HitIterationLimitException ignored) {
                }

                if ((cnt++) % 12 == 0) {
                    try {
                        gameEngine.shuffleInterestingMinesAndMakeOneAway(firstClickI, firstClickJ);
                        gameStack.clear();
                    } catch (Exception ignored) {
                        break;
                    }
                } else {
                    if (!gameStack.empty()) {
                        gameStack.pop();
                    }
                    while (!gameStack.empty() && !everyComponentHasLogicalFrees(gameEngine, solverBoard)) {
                        gameEngine = new EngineForCreatingSolvableBoard(gameStack.pop());
                    }
                    if (!everyComponentHasLogicalFrees(gameEngine, solverBoard)) {
                        break;
                    }
                    try {
                        gameEngine.shuffleAwayMines();
                    } catch (Exception ignored) {
                        break;
                    }
                }
            }

            //found solvable board, let's return it, gameEngine is source of truth here
            if (gameEngine.getGameState() == GameState.WON) {
                TileWithMine[][] grid = new TileWithMine[rows][cols];
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        grid[i][j] = new TileWithMine();
                        grid[i][j].set(gameEngine.getCellWithMine(i, j));
                    }
                }
                return new Board<>(grid, mines);
            }
            //inner-while-loop can break out without finding a solvable board if various things fail.
            //In this case, we try again completely from scratch
        }
        //Here, we've timed out
        throw new Exception("timed out");
    }

    //returns true if we clicked a logical-free, thus expanding/progressing
    private static boolean clickedLogicalFrees(EngineForCreatingSolvableBoard gameEngine, Board<TileWithLogistics> solverBoard) throws Exception {
        gameEngine.checkCorrectnessOfSolverOutput(solverBoard);
        boolean clickedFree = false;
        for (int i = 0; i < solverBoard.getRows(); ++i) {
            for (int j = 0; j < solverBoard.getCols(); ++j) {
                if (solverBoard.getCell(i, j).logic == LogisticState.FREE) {
                    gameEngine.clickCell(i, j, false);
                    clickedFree = true;
                }
            }
        }
        return clickedFree;
    }

    private static boolean everyComponentHasLogicalFrees(EngineForCreatingSolvableBoard gameEngine, Board<TileWithLogistics> solverBoard) throws Exception {
        Dsu disjointSet = GetConnectedComponents.getDsuOfComponentsWithKnownMines(new Board<>(solverBoard.getGrid(), solverBoard.getMines()));
        boolean[] hasLogicalFree = new boolean[solverBoard.getRows() * solverBoard.getCols()];
        boolean hasAtLeastOneLogicalFree = false;
        for (int i = 0; i < solverBoard.getRows(); ++i) {
            for (int j = 0; j < solverBoard.getCols(); ++j) {
                if (solverBoard.getCell(i, j).logic == LogisticState.FREE) {
                    hasAtLeastOneLogicalFree = true;
                    hasLogicalFree[disjointSet.find(RowColToIndex.rowColToIndex(i, j, solverBoard.getRows(), solverBoard.getCols()))] = true;
                }
            }
        }
        if (!hasAtLeastOneLogicalFree) {
            return false;
        }
        for (int i = 0; i < solverBoard.getRows(); ++i) {
            for (int j = 0; j < solverBoard.getCols(); ++j) {
                if (gameEngine.isInterestingCell(i, j) &&
                        solverBoard.getCell(i, j).logic != LogisticState.MINE &&
                        !hasLogicalFree[disjointSet.find(RowColToIndex.rowColToIndex(i, j, solverBoard.getRows(), solverBoard.getCols()))]) {
                    return false;
                }
            }
        }
        return true;
    }
}
