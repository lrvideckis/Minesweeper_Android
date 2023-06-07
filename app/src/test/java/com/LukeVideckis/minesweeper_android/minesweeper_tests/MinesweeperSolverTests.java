package com.LukeVideckis.minesweeper_android.minesweeper_tests;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines.GameEngine;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines.GameState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.CreateSolvableBoard;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.HolyGrailSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.IntenseRecursiveSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BfsSolver.LocalDeductionBFSSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverNothingToLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverLogisticsToProbability;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverNothingToProbability;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.LogisticState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithMine;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;
import com.LukeVideckis.minesweeper_android.miscHelpers.BigFraction;
import com.LukeVideckis.minesweeper_android.test_helpers.NoSolutionFoundException;
import com.LukeVideckis.minesweeper_android.test_helpers.SlowBacktrackingSolver;
import com.LukeVideckis.minesweeper_android.test_helpers.TestEngine;
import com.LukeVideckis.minesweeper_android.test_helpers.TestTileNoFlagsForSolver;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;


public class MinesweeperSolverTests {
    @SuppressWarnings("SpellCheckingInspection")

    private final static String[][] previousFailedBoards = {

            {
                    "UUU3B2.",
                    "U2U3B31",
                    "U21324B",
                    "U3U4B4B",
                    "UUUUUUU",

                    "12"
            },

            //grid which causes cut nodes dfs to add multiple cut nodes to the list of cut nodes
            {
                    "UU1..1U",
                    "2U1112U",
                    "1111U21",
                    "1U123UU",
                    "U11UUUU",

                    "8",
            },

            //cut node not next to any sub component
            {
                    "..1UU",
                    "..1U1",
                    "..111",
                    "..1U2",
                    "..1UU",
                    "..13U",
                    "111UU",
                    "UU1UU",

                    "6"
            },

            //cut node next to >1 sub components
            {
                    "UU1..",
                    "UU1..",
                    "UU321",
                    "UUBUU",
                    "UUUUU",

                    "6"
            },

            //initial failing case for new recursive alg with cut nodes
            {
                    "U1......",
                    "U211111.",
                    "UUUUUU21",
                    "UUUUUUUU",

                    "7"
            },

            //testing new backtracking algorithm
            {
                    "UUUUUU",
                    "UU3221",
                    "UU21..",
                    "UUU1..",

                    "7",
            },

            //board where gauss solver determines away cells as mines
            {
                    "UUUUU",
                    "235UU",
                    "..3UU",
                    "..2UU",

                    "11"
            },

            //cell (0,3) is a logical - free, but fast solver doesn't set it
            //basically, testing if away cells are set as logical free/mine
            {
                    "U3UU",
                    "UUUU",
                    "U421",
                    "U2..",

                    "5"
            },

            //failed test after basically redoing HolyGrailSolver to be more efficient with BigIntegr
            {
                    "U1......",
                    "U1111...",
                    "UUUU1...",
                    "U1111...",
                    "U1......",

                    "3"
            },

            {
                    "UUU",
                    "UUU",
                    "UUU",
                    "12U",
                    ".11",
                    "...",
                    "122",
                    "UUU",

                    "9"
            },

            {
                    ".1U",
                    "23U",
                    "UUU",

                    "3",
            },

            //bug with dfs connect components - upper component and lower component should be the same, but DFS splits them into separate components
            {
                    "UUU",
                    "UU2",
                    "UUU",
                    "U21",
                    "22.",
                    "U21",
                    "UUU",

                    "6"
            },

            //bug with away cells - (incorrectly) returns true when cell is visible, and all adjacent cells are not visible
            {
                    "...",
                    "232",
                    "UUU",
                    "4UU",
                    "UUU",
                    "U3U",
                    "UUU",

                    "6"
            },
            //slow solver has bug in trivial case: no cells are visible
            {
                    "UUU",
                    "UUU",
                    "UUU",

                    "0"
            },
    };

    private static Board<TileNoFlagsForSolver> convertFormat(String[] stringBoard) throws Exception {
        final int rows = stringBoard.length - 1;
        final int cols = stringBoard[0].length();
        final int mines = Integer.parseInt(stringBoard[stringBoard.length - 1]);
        TileNoFlagsForSolver[][] tmpBoard = new TileNoFlagsForSolver[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                tmpBoard[i][j] = new TileNoFlagsForSolver();
            }
        }
        Board<TileNoFlagsForSolver> board = new Board<>(tmpBoard, mines);
        for (int i = 0; i + 1 < board.getRows(); ++i) {
            for (int j = 0; j < stringBoard[i].length(); ++j) {
                if (stringBoard[i].length() != stringBoard[0].length()) {
                    throw new Exception("jagged array - not all rows are the same length");
                }
                TestTileNoFlagsForSolver curr = new TestTileNoFlagsForSolver(stringBoard[i].charAt(j));
                board.getCell(i, j).set(curr);
            }
        }
        return board;
    }

    private static void printBoardWithProb(Board<TileWithProbability> board) throws Exception {
        System.out.println("mines: " + board.getMines() + " visible board is:");
        for (int i = 0; i < board.getRows(); i++) {
            for (int j = 0; j < board.getCols(); j++) {
                TileWithProbability visibleTile = board.getCell(i, j);
                if (visibleTile.isVisible) {
                    if (visibleTile.numberSurroundingMines == 0) {
                        System.out.print('.');
                    } else {
                        System.out.print(visibleTile.numberSurroundingMines);
                    }
                } else if (visibleTile.mineProbability.equals(0)) {
                    System.out.print('F');
                } else if (visibleTile.mineProbability.equals(1)) {
                    System.out.print('B');
                } else {
                    System.out.print('U');
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    private static void printBoard(Board<TileNoFlagsForSolver> board) throws Exception {
        System.out.println("mines: " + board.getMines() + " visible board is:");
        for (int i = 0; i < board.getRows(); i++) {
            for (int j = 0; j < board.getCols(); j++) {
                TileNoFlagsForSolver visibleTile = board.getCell(i, j);
                if (visibleTile.isVisible) {
                    if (visibleTile.numberSurroundingMines == 0) {
                        System.out.print('.');
                    } else {
                        System.out.print(visibleTile.numberSurroundingMines);
                    }
                } else {
                    System.out.print('U');
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    private static void throwIfBoardsAreDifferent(
            Board<TileWithProbability> boardFast,
            Board<TileWithProbability> boardSlow
    ) throws Exception {
        if (boardFast.getRows() != boardSlow.getRows() || boardFast.getCols() != boardSlow.getCols() || boardFast.getMines() != boardSlow.getMines()) {
            throw new Exception("board dimensions/mines don't match");
        }
        for (int i = 0; i < boardSlow.getRows(); ++i) {
            for (int j = 0; j < boardSlow.getCols(); ++j) {
                if (boardFast.getCell(i, j).isVisible != boardSlow.getCell(i, j).isVisible) {
                    printBoardWithProb(boardFast);
                    throw new Exception("tile visibility differs");
                }
                if (boardFast.getCell(i, j).isVisible) {
                    continue;
                }

                TileWithProbability fastTile = boardFast.getCell(i, j);
                TileWithProbability slowTile = boardSlow.getCell(i, j);

                if (!fastTile.mineProbability.equals(slowTile.mineProbability)) {
                    System.out.println("here, solver outputs don't match");
                    System.out.println("i,j: " + i + " " + j);
                    System.out.println("fast solver " + fastTile.mineProbability.getNumerator() + '/' + fastTile.mineProbability.getDenominator());
                    System.out.println("slow solver " + slowTile.mineProbability.getNumerator() + '/' + slowTile.mineProbability.getDenominator());
                    System.out.println("fast solver");
                    printBoardWithProb(boardFast);
                    System.out.println("slow solver");
                    printBoardWithProb(boardSlow);
                    throw new Exception("boards don't match");
                }
            }
        }
    }

    private static void checkProbabilitiesAroundClueAddToClue(
            Board<TileWithProbability> boardFast
    ) throws Exception {
        for (int i = 0; i < boardFast.getRows(); ++i) {
            for (int j = 0; j < boardFast.getCols(); ++j) {
                TileWithProbability centerTile = boardFast.getCell(i, j);
                if (!centerTile.isVisible) {
                    continue;
                }
                if (!centerTile.mineProbability.equals(0)) {
                    throw new Exception("visible tile doesn't have 0 probability");
                }
                BigFraction probabilitySum = new BigFraction(0);
                for (TileWithProbability surroundingTile : boardFast.getAdjacentCells(i, j)) {
                    if (surroundingTile.isVisible) {
                        continue;
                    }
                    probabilitySum.addWith(surroundingTile.mineProbability);
                }
                if (!probabilitySum.equals(centerTile.numberSurroundingMines)) {
                    throw new Exception("Sum of adjacent squares' probabilities should match clue.");
                }
            }
        }
    }

    //throws exception if test failed
    private static void throwIfFailed_compareBFSBoardToBacktrackingBoard(Board<TileWithProbability> boardBacktracking, Board<TileWithLogistics> boardBfs) throws Exception {
        if (boardBacktracking.getRows() != boardBfs.getRows() || boardBacktracking.getCols() != boardBfs.getCols() || boardBacktracking.getMines() != boardBfs.getMines()) {
            throw new Exception("board dimensions/mines don't match");
        }
        for (int i = 0; i < boardBacktracking.getRows(); ++i) {
            for (int j = 0; j < boardBacktracking.getCols(); ++j) {
                if (!boardBacktracking.getCell(i, j).mineProbability.equals(1) && boardBfs.getCell(i, j).logic == LogisticState.MINE) {
                    printBoardWithProb(boardBacktracking);
                    throw new Exception("it isn't a logical mine, but BFS solver says it's a logical mine " + i + " " + j);
                }
                if (!boardBacktracking.getCell(i, j).mineProbability.equals(0) && boardBfs.getCell(i, j).logic == LogisticState.FREE) {
                    printBoardWithProb(boardBacktracking);
                    throw new Exception("it isn't a logical free, but BFS solver says it's a logical free " + i + " " + j);
                }
            }
        }
    }

    private static Board<TileNoFlagsForSolver> convertToNewBoard(GameEngine minesweeperGame) throws Exception {
        final int rows = minesweeperGame.getRows();
        final int cols = minesweeperGame.getCols();
        Board<TileNoFlagsForSolver> board = new Board<>(new TileNoFlagsForSolver[rows][cols], minesweeperGame.getNumberOfMines());
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                board.getGrid()[i][j] = new TileNoFlagsForSolver();
                board.getCell(i, j).set(minesweeperGame.getCell(i, j));
            }
        }
        return board;
    }

    private static Board<TileWithLogistics> convertToAddLogistics(Board<TileNoFlagsForSolver> board) throws Exception {
        TileWithLogistics[][] tmpBoard = new TileWithLogistics[board.getRows()][board.getCols()];
        for (int i = 0; i < board.getRows(); i++) {
            for (int j = 0; j < board.getCols(); j++) {
                tmpBoard[i][j] = new TileWithLogistics();
                tmpBoard[i][j].set(board.getCell(i, j));
            }
        }
        return new Board<>(tmpBoard, board.getMines());
    }

    private static boolean noLogicalFrees(Board<TileWithProbability> solverBoard) throws Exception {
        for (int i = 0; i < solverBoard.getRows(); i++) {
            for (int j = 0; j < solverBoard.getCols(); j++) {
                if (!solverBoard.getCell(i, j).isVisible && solverBoard.getCell(i, j).mineProbability.equals(0)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void testPreviouslyFailedBoards() throws Exception {
        int testID = 1;
        for (String[] stringBoard : previousFailedBoards) {
            System.out.println("test number: " + (testID++));
            final int rows = stringBoard.length - 1;
            final int cols = stringBoard[0].length();

            SolverNothingToProbability holyGrailSolver = new HolyGrailSolver(rows, cols);
            SolverNothingToProbability slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

            Board<TileWithProbability> fastOut, slowOut;

            try {
                fastOut = holyGrailSolver.solvePositionWithProbability(convertFormat(stringBoard));
            } catch (NoSolutionFoundException ignored) {
                System.out.println("no solution found, void test");
                continue;
            }

            try {
                slowOut = slowBacktrackingSolver.solvePositionWithProbability(convertFormat(stringBoard));
            } catch (NoSolutionFoundException ignored) {
                System.out.println("SLOW solver didn't find a solution, void test");
                continue;
            } catch (HitIterationLimitException ignored) {
                System.out.println("SLOW solver hit iteration limit, void test");
                continue;
            }
            throwIfBoardsAreDifferent(fastOut, slowOut);
        }
        System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
    }

    @Test
    public void performTestsForMineProbability() throws Exception {
        int numberOfTests = 128;
        for (int testID = 1; testID <= numberOfTests; ++testID) {
            //TODO: revisit these bounds
            System.out.println();
            System.out.print("test number: " + testID);
            int[] bounds = genSmallBoundsForSlowSolver();
            final int rows = bounds[0];
            final int cols = bounds[1];
            final int mines = bounds[2];
            final boolean hasAn8 = (bounds[3] == 1);
            System.out.print(" dimensions (rows, cols, mines, has8) = (" + rows + " " + cols + " " + mines + " " + hasAn8 + ") ");

            SolverNothingToProbability holyGrailSolver = new HolyGrailSolver(rows, cols);
            SolverNothingToProbability slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

            TestEngine gameEngine = new TestEngine(rows, cols, mines, hasAn8);
            gameEngine.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);

            int numberOfTimesSolverIsRun = 0;
            while (gameEngine.getGameState() != GameState.WON) {
                if (gameEngine.getGameState() == GameState.LOST) {
                    throw new Exception("here 1: game is lost, but this shouldn't happen, failed test");
                }
                Board<TileWithProbability> fastOut, slowOut;
                try {
                    fastOut = holyGrailSolver.solvePositionWithProbability(convertToNewBoard(gameEngine));
                } catch (HitIterationLimitException ignored) {
                    printBoard(convertToNewBoard(gameEngine));
                    System.out.print("fast solver hit iteration limit, void test");
                    break;
                }
                try {
                    slowOut = slowBacktrackingSolver.solvePositionWithProbability(convertToNewBoard(gameEngine));
                } catch (HitIterationLimitException ignored) {
                    System.out.print("slow solver hit iteration limit, void test, # of solver previous successful runs this game = " + numberOfTimesSolverIsRun);
                    break;
                }
                throwIfBoardsAreDifferent(fastOut, slowOut);
                checkProbabilitiesAroundClueAddToClue(fastOut);
                numberOfTimesSolverIsRun++;
                //click all logical frees
                boolean clickedFree = false;
                for (int i = 0; i < rows; ++i) {
                    for (int j = 0; j < cols; ++j) {
                        TileWithProbability cell = fastOut.getCell(i, j);
                        if (!cell.isVisible && cell.mineProbability.equals(0)) {
                            clickedFree = true;
                            gameEngine.clickCell(i, j, false);
                        }
                    }
                }
                if (!clickedFree) {
                    gameEngine.revealRandomFreeCell();
                }
            }
            if (gameEngine.getGameState() == GameState.LOST) {
                throw new Exception("here2 game is lost, but this shouldn't happen, failed test");
            }
        }
        System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
    }

    @Test
    public void performTestsForGaussSolver() throws Exception {
        int numberOfTests = 20;
        for (int testID = 1; testID <= numberOfTests; ++testID) {
            System.out.print("test number: " + testID);
            //a 3-by-8 grid is guaranteed to have some place for the 8
            final int rows = MyMath.getRand(3, 15);
            final int cols = MyMath.getRand(8, 15);
            final int mines = MyMath.getRand(8, rows * cols - 10);//if you click in the middle of a 3-by-8 grid, you can fit an 8 above/below and then 6 extra mines below/above
            System.out.println(" rows, cols, mines " + rows + " " + cols + " " + mines);

            SolverLogisticsToProbability fastSolver = new IntenseRecursiveSolver(rows, cols);
            SolverNothingToLogistics bfsSolver = new LocalDeductionBFSSolver(rows, cols);

            GameEngine gameEngine = new GameEngine(rows, cols, mines, testID <= numberOfTests / 2 /*exactly half the tests*/);
            {
                int numberOfClicks = MyMath.getRand(0, 4);
                while (numberOfClicks-- > 0 && gameEngine.getGameState() != GameState.LOST) {
                    int x = MyMath.getRand(0, rows - 1);
                    int y = MyMath.getRand(0, cols - 1);
                    gameEngine.clickCell(x, y, false);
                }
            }
            if (gameEngine.getGameState() == GameState.LOST) {
                System.out.println("game over, void test");
                continue;
            }
            Board<TileWithProbability> fastOut;
            try {
                fastOut = fastSolver.solvePositionWithLogistics(convertToAddLogistics(convertToNewBoard(gameEngine)));
            } catch (HitIterationLimitException ignored) {
                System.out.println("backtracking solver hit iteration limit, void test");
                continue;
            }
            Board<TileWithLogistics> boardBfs = bfsSolver.solvePosition(convertToNewBoard(gameEngine));
            throwIfFailed_compareBFSBoardToBacktrackingBoard(fastOut, boardBfs);
        }
        System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
    }

    @Test
    public void performTestsMultipleRunsOfSameBoard() throws Exception {
        int numberOfTests = 3;
        for (int testID = 1; testID <= numberOfTests; ++testID) {
            System.out.println("test number: " + testID);
            int[] bounds = genSmallBoundsForSlowSolver();
            final int rows = bounds[0];
            final int cols = bounds[1];
            final int mines = bounds[2];
            final boolean hasAn8 = (bounds[3] == 1);

            SolverNothingToProbability holyGrailSolver = new HolyGrailSolver(rows, cols);
            SolverNothingToLogistics bfsSolver = new LocalDeductionBFSSolver(rows, cols);
            SolverNothingToProbability slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

            GameEngine gameEngine = new GameEngine(rows, cols, mines, hasAn8);
            int numberOfClicks = MyMath.getRand(0, 4);
            while (numberOfClicks-- > 0 && gameEngine.getGameState() != GameState.LOST) {
                gameEngine.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
            }
            if (gameEngine.getGameState() == GameState.LOST) {
                System.out.println("game over, void test");
                continue;
            }

            Board<TileWithProbability> slowOut;
            try {
                slowOut = slowBacktrackingSolver.solvePositionWithProbability(convertToNewBoard(gameEngine));
            } catch (HitIterationLimitException ignored) {
                System.out.println("slow solver hit iteration limit, void test");
                continue;
            }
            for (int i = 0; i < 3; ++i) {
                Board<TileWithProbability> fastOut = holyGrailSolver.solvePositionWithProbability(convertToNewBoard(gameEngine));
                throwIfBoardsAreDifferent(fastOut, slowOut);

                Board<TileWithLogistics> boardBfs = bfsSolver.solvePosition(convertToNewBoard(gameEngine));
                throwIfFailed_compareBFSBoardToBacktrackingBoard(fastOut, boardBfs);
            }
        }
        System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
    }

    @Test
    public void TestThatSolvableBoardsAreSolvable() throws Exception {
        int numberOfTests = 20;
        long sumTimes = 0;
        for (int testID = 1; testID <= numberOfTests; ++testID) {
            System.out.println("test number: " + testID);

            //TODO: revisit these bounds - too big for slow solver
            final int rows = MyMath.getRand(8, 30);
            final int cols = MyMath.getRand(8, 30);
            final int mines = MyMath.getRand(8, Math.min(100, Math.min(rows * cols - 10, (int) (rows * cols * 0.23f))));
            final boolean hasAn8 = (testID <= 10);

            System.out.print(" rows, cols, mines, hasAn8: " + rows + " " + cols + " " + mines + " " + hasAn8);
            System.out.print(" percentage: " + mines / (float) (rows * cols));

            SolverNothingToProbability solver = new HolyGrailSolver(rows, cols);

            final int firstClickI = MyMath.getRand(0, rows - 1);
            final int firstClickJ = MyMath.getRand(0, cols - 1);

            long startTime = System.currentTimeMillis();
            Board<TileWithMine> solvableBoard = CreateSolvableBoard.getSolvableBoard(rows, cols, mines, firstClickI, firstClickJ, hasAn8, new AtomicBoolean(false));
            System.out.println(" time to create solvable board: " + (System.currentTimeMillis() - startTime) + " ms");

            {
                int minesFromSolvableBoard = 0;
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        if (solvableBoard.getCell(i, j).isMine) {
                            minesFromSolvableBoard++;
                        }
                    }
                }
                if (minesFromSolvableBoard != mines) {
                    throw new Exception("supposedly solvable board has incorrect number of mines");
                }
            }

            if (hasAn8) {
                boolean foundAn8 = false;
                for (int i = 0; i < rows && !foundAn8; i++) {
                    for (int j = 0; j < cols && !foundAn8; j++) {
                        int cntMine = 0;
                        for (TileWithMine adj : solvableBoard.getAdjacentCells(i, j)) {
                            if (adj.isMine) {
                                cntMine++;
                            }
                        }
                        if (cntMine == 8) {
                            foundAn8 = true;
                        }
                    }
                }
                if (!foundAn8) {
                    throw new Exception("gen solvable board didn't create a board containing an 8");
                }
            }

            GameEngine gameEngine = new GameEngine(solvableBoard, firstClickI, firstClickJ, hasAn8);

            sumTimes += System.currentTimeMillis() - startTime;
            TileNoFlagsForSolver[][] tmpBoard = new TileNoFlagsForSolver[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    tmpBoard[i][j] = new TileNoFlagsForSolver();
                }
            }
            Board<TileNoFlagsForSolver> visibleBoard = new Board<>(tmpBoard, mines);

            boolean hitIterationLimit = false;
            while (gameEngine.getGameState() == GameState.STILL_GOING) {
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        visibleBoard.getCell(i, j).set(gameEngine.getCell(i, j));
                    }
                }
                Board<TileWithProbability> solverRes;
                try {
                    solverRes = solver.solvePositionWithProbability(visibleBoard);
                } catch (HitIterationLimitException ignored) {
                    System.out.println("hit iteration limit, void test");
                    hitIterationLimit = true;
                    break;
                }

                if (noLogicalFrees(solverRes)) {
                    printBoardWithProb(solverRes);
                    throw new Exception("no logical frees, failed test");
                }

                for (int i = 0; i < rows; ++i) {
                    for (int j = 0; j < cols; ++j) {
                        if (!solverRes.getCell(i, j).isVisible && solverRes.getCell(i, j).mineProbability.equals(0)) {
                            gameEngine.clickCell(i, j, false);
                        }
                    }
                }
            }
            if (hitIterationLimit) {
                continue;
            }
            if (gameEngine.getGameState() != GameState.WON) {
                throw new Exception("game is not won, failed test");
            }
        }
        System.out.println("average total time (ms): " + sumTimes / numberOfTests);
        System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
    }

    //returns [# rows, # cols, # mines, 0/1 for hasAn8]
    private int[] genSmallBoundsForSlowSolver() throws Exception {
        final boolean hasAn8 = (MyMath.getRand(0, 2) == 0);
        if (hasAn8) {
            //a 3-by-8 grid is guaranteed to have some place for the 8
            final int rows = MyMath.getRand(3, 5);
            final int cols = MyMath.getRand(8, 10);
            final int mines = MyMath.getRand(8, 14);//if you click in the middle of a 3-by-8 grid, you can fit an 8 above/below and then 6 extra mines below/above
            return new int[]{rows, cols, mines, 1};
        }
        final int rows = MyMath.getRand(3, 10);
        final int cols = MyMath.getRand(3, 45 / rows);
        int mines = MyMath.getRand(0, Math.min(16, rows * cols - 9));

        return new int[]{rows, cols, mines, 0};
    }
}
