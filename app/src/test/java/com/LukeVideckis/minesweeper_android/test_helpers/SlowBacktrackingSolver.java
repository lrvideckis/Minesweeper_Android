package com.LukeVideckis.minesweeper_android.test_helpers;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AllCellsAreHidden;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MutableInt;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverWithProbability;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;

public class SlowBacktrackingSolver implements SolverWithProbability {

    private final static int iterationLimit = 10000;
    private final int[][][] lastUnvisitedSpot;
    private final boolean[][] isMine;
    private final int[][] cntSurroundingMines;
    private final BigFraction[][] numberOfTotalConfigs;
    private Board<TileWithProbability> solutionBoard;

    public SlowBacktrackingSolver(int rows, int cols) {
        isMine = new boolean[rows][cols];
        cntSurroundingMines = new int[rows][cols];
        lastUnvisitedSpot = new int[rows][cols][2];
        numberOfTotalConfigs = new BigFraction[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                numberOfTotalConfigs[i][j] = new BigFraction(0);
            }
        }
    }

    @Override
    public Board<TileWithProbability> solvePositionWithProbability(Board<TileNoFlagsForSolver> board) throws Exception {
        initialize(board);//creates deep copy of solution board to return

        if (AllCellsAreHidden.allCellsAreHidden(new Board<>(board.getGrid(), board.getMines()))) {
            for (int i = 0; i < board.getRows(); ++i) {
                for (int j = 0; j < board.getCols(); ++j) {
                    solutionBoard.getCell(i, j).mineProbability.setValues(board.getMines(), board.getRows() * board.getCols());
                }
            }
            return solutionBoard;
        }

        ArrayList<Pair<Integer, Integer>> component = new ArrayList<>();
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                if (!board.getCell(i, j).isVisible) {
                    component.add(new Pair<>(i, j));
                }
            }
        }
        initializeLastUnvisitedSpot(component, board);

        MutableInt currIterations = new MutableInt(0);
        MutableInt currNumberOfMines = new MutableInt(0);
        solveComponent(0, component, currIterations, currNumberOfMines, board);

        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                TileNoFlagsForSolver curr = board.getCell(i, j);
                if (curr.isVisible) {
                    continue;
                }
                if (numberOfTotalConfigs[i][j].equals(0)) {
                    throw new NoSolutionFoundException("There should be at least one mine configuration for non-visible cells");
                }
                solutionBoard.getCell(i, j).mineProbability.divideWith(numberOfTotalConfigs[i][j]);
            }
        }
        return solutionBoard;
    }

    private void initializeLastUnvisitedSpot(ArrayList<Pair<Integer, Integer>> component, Board<TileNoFlagsForSolver> board) throws Exception {
        for (Pair<Integer, Integer> spot : component) {
            for (int[] adj : board.getAdjacentIndexes(spot.first, spot.second)) {
                final int adjI = adj[0], adjJ = adj[1];
                if (board.getCell(adjI, adjJ).isVisible) {
                    lastUnvisitedSpot[adjI][adjJ][0] = spot.first;
                    lastUnvisitedSpot[adjI][adjJ][1] = spot.second;
                }
            }
        }
    }

    private void initialize(Board<TileNoFlagsForSolver> board) throws Exception {

        TileWithProbability[][] tmpBoard = new TileWithProbability[board.getRows()][board.getCols()];
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                isMine[i][j] = false;
                cntSurroundingMines[i][j] = 0;
                lastUnvisitedSpot[i][j][0] = 0;
                lastUnvisitedSpot[i][j][1] = 0;
                numberOfTotalConfigs[i][j].setValues(0, 1);
                tmpBoard[i][j] = new TileWithProbability();
                tmpBoard[i][j].set(board.getCell(i, j));
            }
        }
        solutionBoard = new Board<>(tmpBoard, board.getMines());//hack to update # of mines
    }

    private void solveComponent(int pos, ArrayList<Pair<Integer, Integer>> component, MutableInt currIterations, MutableInt currNumberOfMines, Board<TileNoFlagsForSolver> board) throws Exception {
        if (pos == component.size()) {
            checkSolution(currNumberOfMines.get(), board);
            return;
        }
        currIterations.addWith(1);
        if (currIterations.get() >= iterationLimit) {
            throw new HitIterationLimitException("too many iterations");
        }
        final int i = component.get(pos).first;
        final int j = component.get(pos).second;

        //try mine
        isMine[i][j] = true;
        if (checkSurroundingConditions(i, j, component.get(pos), 1, board)) {
            currNumberOfMines.addWith(1);
            updateSurroundingMineCnt(i, j, 1, board);
            solveComponent(pos + 1, component, currIterations, currNumberOfMines, board);
            updateSurroundingMineCnt(i, j, -1, board);
            currNumberOfMines.addWith(-1);
        }

        //try free
        isMine[i][j] = false;
        if (checkSurroundingConditions(i, j, component.get(pos), 0, board)) {
            solveComponent(pos + 1, component, currIterations, currNumberOfMines, board);
        }
    }

    private void updateSurroundingMineCnt(int i, int j, int delta, Board<TileNoFlagsForSolver> board) throws Exception {
        for (int[] adj : board.getAdjacentIndexes(i, j)) {
            final int adjI = adj[0], adjJ = adj[1];
            if (board.getCell(adjI, adjJ).isVisible) {
                final int cnt = cntSurroundingMines[adjI][adjJ];
                cntSurroundingMines[adjI][adjJ] = cnt + delta;
            }
        }
    }

    private boolean checkSurroundingConditions(int i, int j, Pair<Integer, Integer> currSpot, int arePlacingAMine, Board<TileNoFlagsForSolver> board) throws Exception {
        for (int[] adj : board.getAdjacentIndexes(i, j)) {
            final int adjI = adj[0], adjJ = adj[1];
            TileNoFlagsForSolver adjTile = board.getCell(adjI, adjJ);
            if (!adjTile.isVisible) {
                continue;
            }
            final int currBacktrackingCount = cntSurroundingMines[adjI][adjJ];
            if (currBacktrackingCount + arePlacingAMine > adjTile.numberSurroundingMines) {
                return false;
            }
            if (
                    lastUnvisitedSpot[adjI][adjJ][0] == currSpot.first &&
                            lastUnvisitedSpot[adjI][adjJ][1] == currSpot.second &&
                            currBacktrackingCount + arePlacingAMine != adjTile.numberSurroundingMines) {
                return false;
            }
        }
        return true;
    }

    private void checkSolution(int currNumberOfMines, Board<TileNoFlagsForSolver> board) throws Exception {
        if (!checkPositionValidity(currNumberOfMines, board)) {
            return;
        }
        if (currNumberOfMines != board.getMines()) {
            return;
        }
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                if (board.getCell(i, j).isVisible) {
                    continue;
                }
                if (isMine[i][j]) {
                    solutionBoard.getCell(i, j).mineProbability.addWith(1);
                }
                numberOfTotalConfigs[i][j].addWith(1);
            }
        }
    }

    //returns true if valid
    private boolean checkPositionValidity(int currNumberOfMines, Board<TileNoFlagsForSolver> board) throws Exception {
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                for (int[] adj : board.getAdjacentIndexes(i, j)) {
                    final int adjI = adj[0], adjJ = adj[1];
                    TileNoFlagsForSolver adjTile = board.getCell(adjI, adjJ);
                    if (!adjTile.isVisible) {
                        continue;
                    }
                    if (cntSurroundingMines[adjI][adjJ] != adjTile.numberSurroundingMines) {
                        return false;
                    }
                }
            }
        }
        int prevNumberOfMines = 0;
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                if (isMine[i][j]) {
                    ++prevNumberOfMines;
                }
            }
        }
        if (prevNumberOfMines != currNumberOfMines) {
            throw new Exception("number of mines doesn't match");
        }
        return true;
    }
}
