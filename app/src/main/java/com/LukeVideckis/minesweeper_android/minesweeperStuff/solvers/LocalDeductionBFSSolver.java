package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverNothingToLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;

public class LocalDeductionBFSSolver implements SolverNothingToLogistics {
    private final int rows, cols;
    private final Queue<bfsState> q;
    //gridLocationToStates[i][j] = list of bfsState's which include cell (i,j) in their subset
    private final List<List<List<bfsState>>> gridLocationToStates;
    //stateToValue[i][j][subset] = bfsValue, used like a visited array
    private final List<List<TreeMap<Integer, bfsValue>>> stateToValue;

    public LocalDeductionBFSSolver(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        q = new LinkedList<>();
        gridLocationToStates = new ArrayList<>(rows);
        stateToValue = new ArrayList<>(rows);
        for (int i = 0; i < rows; ++i) {
            gridLocationToStates.add(new ArrayList<>(cols));
            stateToValue.add(new ArrayList<>(cols));
            for (int j = 0; j < cols; ++j) {
                gridLocationToStates.get(i).add(new ArrayList<>());
                stateToValue.get(i).add(new TreeMap<>());
            }
        }
    }

    @Override
    public Board<TileWithLogistics> solvePosition(Board<TileNoFlagsForSolver> board) throws Exception {
        if (board.getRows() != rows || board.getCols() != cols) {
            throw new Exception("array bounds don't match");
        }

        Board<TileWithLogistics> boardWithLogistics = initializeStructures(board);

        //TODO: a-star style approach when destination square is given
        while (!q.isEmpty()) {
            bfsState currState = q.remove();
            int sizeSubset = Integer.bitCount(currState.subsetSurroundingSquares);

        }

        return boardWithLogistics;
    }

    private Board<TileWithLogistics> initializeStructures(Board<TileNoFlagsForSolver> board) throws Exception {
        q.clear();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                //can't clear as you go because you might clear a newly added bfsState
                //so instead clear all at once in the beginning
                gridLocationToStates.get(i).get(j).clear();
                stateToValue.get(i).get(j).clear();
            }
        }
        TileWithLogistics[][] tmpBoard = new TileWithLogistics[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                tmpBoard[i][j] = new TileWithLogistics();
                tmpBoard[i][j].set(board.getCell(i, j));
                if (!board.getCell(i, j).isVisible) {
                    continue;
                }
                int dirMask = 0;
                for (int dir = 0; dir < 8; dir++) {
                    final int adjI = i + Board.deltas[dir][0];
                    final int adjJ = j + Board.deltas[dir][1];
                    if (board.outOfBounds(adjI, adjJ)) {
                        continue;
                    }
                    if (board.getCell(adjI, adjJ).isVisible) {
                        continue;
                    }
                    dirMask += (1 << dir);
                }
                if (dirMask > 0) {
                    int numMines = board.getCell(i, j).numberSurroundingMines;
                    if (numMines == 0) {
                        throw new Exception("visible squares with at least 1 non-visible neighbor should have non-zero number of mines.");
                    }
                    bfsState currState = new bfsState(i, j, dirMask);
                    q.add(currState);
                    for (int dir = 0; dir < 8; dir++) {
                        if (((dirMask >> dir) & 1) == 0) {
                            continue;
                        }
                        final int adjI = i + Board.deltas[dir][0];
                        final int adjJ = j + Board.deltas[dir][1];
                        gridLocationToStates.get(adjI).get(adjJ).add(currState);
                    }
                }
            }
        }

        //always allocate new board to avoid any potential issues with shallow copies between solver runs
        return new Board<>(tmpBoard, board.getMines());
    }

    private class bfsState {
        //represents index of center-cell
        private final int centerI;
        private final int centerJ;
        //number in range [0, 2^8)
        //012
        //3 4
        //567
        //each on-bit means that that relative tile is in the subset
        private final int subsetSurroundingSquares;

        public bfsState(int centerI, int centerJ, int subsetSurroundingSquares) {
            this.centerI = centerI;
            this.centerJ = centerJ;
            this.subsetSurroundingSquares = subsetSurroundingSquares;
        }

        public boolean intersects(bfsState otherState) {
            return false;//TODO
        }
    }

    private class bfsValue {
        private final int minNumMines;
        private final int maxNumMines;
        private bfsState prevState1 = null, prevState2 = null, prevState3 = null;

        public bfsValue(int minNumMines, int maxNumMines, bfsState prevState1, bfsState prevState2, bfsState prevState3) {
            this.minNumMines = minNumMines;
            this.maxNumMines = maxNumMines;
            this.prevState1 = prevState1;
            this.prevState2 = prevState2;
            this.prevState3 = prevState3;
        }
    }
}