package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverNothingToLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.LogisticState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;

public class LocalDeductionBFSSolver implements SolverNothingToLogistics {
    private final int rows, cols;

    public LocalDeductionBFSSolver(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    @Override
    public Board<TileWithLogistics> solvePosition(Board<TileNoFlagsForSolver> board) throws Exception {
        if (board.getRows() != rows || board.getCols() != cols) {
            throw new Exception("array bounds don't match");
        }

        Queue<bfsState> q = getInitialQueue(board);

        //always allocate new board to avoid any potential issues with shallow copies between solver runs
        TileWithLogistics[][] tmpBoard = new TileWithLogistics[rows][cols];
        List<List<TreeMap<Integer, bfsValue>>> stateToValue = new ArrayList<>(rows);
        for (int i = 0; i < rows; ++i) {
            stateToValue.add(new ArrayList<>(cols));
            for (int j = 0; j < cols; ++j) {
                stateToValue.get(i).add(new TreeMap<>());
                tmpBoard[i][j] = new TileWithLogistics();
                tmpBoard[i][j].set(board.getCell(i, j));
            }
        }
        Board<TileWithLogistics> boardWithLogistics = new Board<>(tmpBoard, board.getMines());

        while(!q.isEmpty()) {
            bfsState state = q.remove();


            //Integer.bitCount(i);
        }

        return boardWithLogistics;
    }

    private Queue<bfsState> getInitialQueue(Board<TileNoFlagsForSolver> board) throws Exception {
        Queue<bfsState> q = new LinkedList<>();
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
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
                    dirMask += (1<<dir);
                }
                if(dirMask > 0) {
                    int numMines = board.getCell(i, j).numberSurroundingMines;
                    q.add(new bfsState(i, j, dirMask));
                }
            }
        }
        return q;
    }

    private class bfsState {
        //represents index of center-cell
        private int centerI, centerJ;
        //number in range [0, 2^8)
        //012
        //3 4
        //567
        //each on-bit means that that relative tile is in the subset
        private int subsetSurroundingSquares;

        public bfsState(int centerI, int centerJ, int subsetSurroundingSquares) {
            this.centerI = centerI;
            this.centerJ = centerJ;
            this.subsetSurroundingSquares = subsetSurroundingSquares;
        }
    }
    private class bfsValue {
        private int minNumMines, maxNumMines;
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