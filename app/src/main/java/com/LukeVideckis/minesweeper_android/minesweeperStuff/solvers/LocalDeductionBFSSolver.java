package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverNothingToLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.TreeMap;

public class LocalDeductionBFSSolver implements SolverNothingToLogistics {
    private final int rows, cols;
    private Queue<bfsState> q;
    //gridLocationToStates[i][j] = list of bfsState's which include cell (i,j) in their subset
    //used to efficiently retrieve all intersecting states
    private List<List<List<bfsState>>> gridLocationToStates;

    //stateToValue[i][j][subset] = bfsValue, used like a visited array
    private List<List<TreeMap<Integer, bfsValue>>> stateToValue;

    private List<List<bfsValue>> endValue;//TODO: initialize in that init function

    public LocalDeductionBFSSolver(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        q = new LinkedList<>();
        gridLocationToStates = new ArrayList<>(rows);
        stateToValue = new ArrayList<>(rows);
        endValue = new ArrayList<>(rows);
        for (int i = 0; i < rows; ++i) {
            gridLocationToStates.add(new ArrayList<>(cols));
            stateToValue.add(new ArrayList<>(cols));
            endValue.add(new ArrayList<>(cols));
            for (int j = 0; j < cols; ++j) {
                gridLocationToStates.get(i).add(new ArrayList<>());
                stateToValue.get(i).add(new TreeMap<>());
                endValue.get(i).add(null);
            }
        }
    }

    @Override
    public Board<TileWithLogistics> solvePosition(Board<TileNoFlagsForSolver> board) throws Exception {
        if (board.getRows() != rows || board.getCols() != cols) {
            throw new Exception("array bounds don't match");
        }
        initializeStructures(board);
        //TODO: a-star style approach when destination square is given
        while (!q.isEmpty()) {
            bfsState currState = q.remove();
            bfsValue currValue = getValue(currState);
            int sizeSubset = Integer.bitCount(currState.subsetSurroundingSquares);

            if(!(0 <= currValue.minNumMines &&
                    currValue.minNumMines <= currValue.maxNumMines &&
                    currValue.maxNumMines <= sizeSubset)) {
                throw new Exception("invalid min and max mines for current value");
            }

            //or maybe check if there's already deducible stuff in the subset, and if so,
            //immediately push a new state onto the q without this deducable stuff

            int numKnownMinesInSubset = 0, numKnownFreesInSubset = 0;
            for (int dir = 0; dir < 8; dir++) {
                if (((currState.subsetSurroundingSquares >> dir) & 1) == 0) {
                    continue;
                }
                final int adjI = currState.centerI + Board.deltas[dir][0];
                final int adjJ = currState.centerJ + Board.deltas[dir][1];
                if (board.outOfBounds(adjI, adjJ) || board.getCell(adjI, adjJ).isVisible) {
                    throw new Exception("subset of squares from BFS queue should always be inbounds and invisible");
                }
                if (Objects.isNull(endValue.get(adjI).get(adjJ))) {
                    continue;
                }

            }


            //handle case where size of subset == 0 or min number of mines
            if (sizeSubset == currValue.minNumMines) {
                //all squares in subset are deducible mines, let's mark them
                for (int dir = 0; dir < 8; dir++) {
                    if (((currState.subsetSurroundingSquares >> dir) & 1) == 0) {
                        continue;
                    }
                    final int adjI = currState.centerI + Board.deltas[dir][0];
                    final int adjJ = currState.centerJ + Board.deltas[dir][1];
                    //only set if null to keep shortest path
                    if (Objects.isNull(endValue.get(adjI).get(adjJ))) {
                        //final int minNumMines, maxNumMines;
                        //bfsState prevState1 = null, prevState2 = null, prevState3 = null;
                        endValue.get(adjI).set(adjJ, new bfsValue(/*TODO*/));
                    }
                }
                continue;
            }

            //TODO make this more generalized: max mines == num already logical mines in subset -> then rest are free
            if (currValue.maxNumMines == 0) {
                //all squares in subset are deducible frees
                //TODO: mark them as such
                continue;
            }

            //TODO: for each intersecting bfsState, check deduction 2,3

            //TODO: for each pair of intersecting bfsStates, check deduction 4, 4.5
        }

        //always allocate new board to avoid any potential issues with shallow copies between solver runs
        //initialize return board from endValue
        TileWithLogistics[][] tmpBoard = new TileWithLogistics[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                tmpBoard[i][j] = new TileWithLogistics();
                tmpBoard[i][j].set(board.getCell(i, j));
            }
        }
        return new Board<>(tmpBoard, board.getMines());
    }

    public class bfsTransition {
        //TODO: decide fields
    }
    public ArrayList<bfsTransition> getListOfTransitions(int tileI, int tileJ) {
        //idea to retrieve this: dfs-style topo sort:
        //call dfs initially on endValue[tileI][tileJ], then push state/value onto return ArrayList
        //only after all dfs recursive calls return
        //
        //this gives reverse topo order, but the adjacency list is already reversed, giving it in order

        ArrayList<bfsTransition> transitionList = new ArrayList<>();
        return transitionList;
    }

    private bfsValue getValue(bfsState state) {
        return Objects.requireNonNull(stateToValue.get(state.centerI).get(state.centerJ).get(state.subsetSurroundingSquares));
    }

    private void initializeStructures(Board<TileNoFlagsForSolver> board) throws Exception {
        q.clear();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                //can't clear as you go because you might clear a newly added bfsState
                //so instead clear all at once in the beginning
                gridLocationToStates.get(i).get(j).clear();
                stateToValue.get(i).get(j).clear();
                endValue.get(i).set(j, null);
            }
        }
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
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
                    bfsValue currValue = new bfsValue(numMines, numMines, null, null, null);
                    q.add(currState);
                    stateToValue.get(i).get(j).put(dirMask, currValue);
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
    }

    private class bfsState {
        //represents location of center-cell
        final int centerI, centerJ;
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
        //[inclusive, inclusive] range
        final int minNumMines, maxNumMines;
        bfsState prevState1 = null, prevState2 = null, prevState3 = null;

        public bfsValue(int minNumMines, int maxNumMines, bfsState prevState1, bfsState prevState2, bfsState prevState3) {
            this.minNumMines = minNumMines;
            this.maxNumMines = maxNumMines;
            this.prevState1 = prevState1;
            this.prevState2 = prevState2;
            this.prevState3 = prevState3;
        }
    }
}