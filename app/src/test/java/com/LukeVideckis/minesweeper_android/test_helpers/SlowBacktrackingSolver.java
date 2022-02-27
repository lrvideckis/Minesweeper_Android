package com.LukeVideckis.minesweeper_android.test_helpers;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AllCellsAreHidden;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MutableInt;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;

public class SlowBacktrackingSolver implements BacktrackingSolver {

    private final static int iterationLimit = 10000;
    private final int[][][] lastUnvisitedSpot;
    private final boolean[][] isMine;
    private final int[][] cntSurroundingMines;
    private final BigFraction[][] numberOfTotalConfigs;
    private final TileWithProbability[][] tempBoardWithProbability;
    private int rows, cols;
    private TileWithProbability[][] board;
    private int numberOfMines;

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
        tempBoardWithProbability = new TileWithProbability[rows][cols];
    }

    @Override
    public void solvePosition(TileNoFlagsForSolver[][] board, int numberOfMines) throws Exception {
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                tempBoardWithProbability[i][j] = new TileWithProbability(board[i][j]);
            }
        }
        solvePosition(tempBoardWithProbability, numberOfMines);
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                board[i][j] = new TileNoFlagsForSolver(tempBoardWithProbability[i][j]);
            }
        }
    }

    @Override
    public void solvePosition(TileWithProbability[][] board, int numberOfMines) throws Exception {
        initialize(board, numberOfMines);

        if (AllCellsAreHidden.allCellsAreHidden(board)) {
            for (int i = 0; i < rows; ++i) {
                for (int j = 0; j < cols; ++j) {
                    board[i][j].mineProbability.setValues(numberOfMines, rows * cols);
                    if (numberOfMines == 0) {
                        board[i][j].isLogicalFree = true;
                    }
                    if (numberOfMines == rows * cols) {
                        board[i][j].isLogicalMine = true;
                    }
                }
            }
            return;
        }

        ArrayList<Pair<Integer, Integer>> component = new ArrayList<>();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (!board[i][j].getIsVisible()) {
                    component.add(new Pair<>(i, j));
                }
            }
        }
        initializeLastUnvisitedSpot(component);

        MutableInt currIterations = new MutableInt(0);
        MutableInt currNumberOfMines = new MutableInt(0);
        solveComponent(0, component, currIterations, currNumberOfMines);

        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                TileWithProbability curr = board[i][j];
                if (curr.getIsVisible()) {
                    continue;
                }
                if (numberOfTotalConfigs[i][j].equals(0)) {
                    throw new NoSolutionFoundException("There should be at least one mine configuration for non-visible cells");
                }
                curr.mineProbability.divideWith(numberOfTotalConfigs[i][j]);
                if (curr.mineProbability.equals(0)) {
                    curr.isLogicalFree = true;
                } else if (curr.mineProbability.equals(1)) {
                    curr.isLogicalMine = true;
                }
            }
        }
    }

    private void initializeLastUnvisitedSpot(ArrayList<Pair<Integer, Integer>> component) {
        for (Pair<Integer, Integer> spot : component) {
            for (int[] adj : GetAdjacentCells.getAdjacentCells(spot.first, spot.second, rows, cols)) {
                final int adjI = adj[0], adjJ = adj[1];
                if (board[adjI][adjJ].isVisible) {
                    lastUnvisitedSpot[adjI][adjJ][0] = spot.first;
                    lastUnvisitedSpot[adjI][adjJ][1] = spot.second;
                }
            }
        }
    }

    private void initialize(TileWithProbability[][] board, int numberOfMines) throws Exception {
        this.board = board;
        this.numberOfMines = numberOfMines;
        Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
        rows = dimensions.first;
        cols = dimensions.second;
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                isMine[i][j] = false;
                cntSurroundingMines[i][j] = 0;
                lastUnvisitedSpot[i][j][0] = 0;
                lastUnvisitedSpot[i][j][1] = 0;
                numberOfTotalConfigs[i][j].setValues(0, 1);
            }
        }
    }

    private void solveComponent(int pos, ArrayList<Pair<Integer, Integer>> component, MutableInt currIterations, MutableInt currNumberOfMines) throws Exception {
        if (pos == component.size()) {
            checkSolution(currNumberOfMines.get());
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
        if (checkSurroundingConditions(i, j, component.get(pos), 1)) {
            currNumberOfMines.addWith(1);
            updateSurroundingMineCnt(i, j, 1);
            solveComponent(pos + 1, component, currIterations, currNumberOfMines);
            updateSurroundingMineCnt(i, j, -1);
            currNumberOfMines.addWith(-1);
        }

        //try free
        isMine[i][j] = false;
        if (checkSurroundingConditions(i, j, component.get(pos), 0)) {
            solveComponent(pos + 1, component, currIterations, currNumberOfMines);
        }
    }

    private void updateSurroundingMineCnt(int i, int j, int delta) {
        for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
            final int adjI = adj[0], adjJ = adj[1];
            if (board[adjI][adjJ].isVisible) {
                final int cnt = cntSurroundingMines[adjI][adjJ];
                cntSurroundingMines[adjI][adjJ] = cnt + delta;
            }
        }
    }

    private boolean checkSurroundingConditions(int i, int j, Pair<Integer, Integer> currSpot, int arePlacingAMine) {
        for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
            final int adjI = adj[0], adjJ = adj[1];
            TileNoFlagsForSolver adjTile = board[adjI][adjJ];
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

    private void checkSolution(int currNumberOfMines) throws Exception {
        if (!checkPositionValidity(currNumberOfMines)) {
            return;
        }

        if (currNumberOfMines != numberOfMines) {
            return;
        }
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (board[i][j].getIsVisible()) {
                    continue;
                }
                if (isMine[i][j]) {
                    board[i][j].mineProbability.addWith(1);
                }
                numberOfTotalConfigs[i][j].addWith(1);
            }
        }
    }

    //returns true if valid
    private boolean checkPositionValidity(int currNumberOfMines) throws Exception {
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
                    final int adjI = adj[0], adjJ = adj[1];
                    TileNoFlagsForSolver adjTile = board[adjI][adjJ];
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
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
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
