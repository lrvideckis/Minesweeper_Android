package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.bfsSolver;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverNothingToLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.LogisticState;
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
    private Queue<BfsState> q;
    //gridLocationToStates[i][j] = list of BfsState's which include cell (i,j) in their subset
    //used to efficiently retrieve all intersecting states
    private List<List<List<BfsState>>> gridLocationToStates;

    //stateToValue[i][j][subset] = BfsValue, used like a visited array
    private List<List<TreeMap<Integer, BfsValue>>> stateToValue;

    private List<List<BfsValue>> endValue;//TODO: initialize in that init function

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
        //always allocate new board to avoid any potential issues with shallow copies between solver runs
        Board<TileWithLogistics> boardLogistics = initializeStructures(board);
        //TODO: a-star style approach when destination square is given
        while (!q.isEmpty()) {
            BfsState currState = q.remove();
            BfsValue currValue = getValue(currState);
            int sizeSubset = Integer.bitCount(currState.subsetSurroundingSquares);

            if(!(0 <= currValue.minNumMines &&
                    currValue.minNumMines <= currValue.maxNumMines &&
                    currValue.maxNumMines <= sizeSubset)) {
                throw new Exception("invalid min and max mines for current value");
            }

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
                if (boardLogistics.getCell(adjI, adjJ).logic == LogisticState.MINE)
                    numKnownMinesInSubset++;
                if (boardLogistics.getCell(adjI, adjJ).logic == LogisticState.FREE)
                    numKnownFreesInSubset++;
            }

            //handle trivial case where the remaining cells in the subset are either all mines or all frees
            {
                boolean remainingAreFrees = (currValue.maxNumMines == numKnownMinesInSubset);
                boolean remainingAreMines = (currValue.minNumMines == sizeSubset - numKnownFreesInSubset);
                if (remainingAreFrees || remainingAreMines) {
                    //all squares in subset are deducible mines, let's mark them
                    for (int dir = 0; dir < 8; dir++) {
                        if (((currState.subsetSurroundingSquares >> dir) & 1) == 0) {
                            continue;
                        }
                        final int adjI = currState.centerI + Board.deltas[dir][0];
                        final int adjJ = currState.centerJ + Board.deltas[dir][1];
                        //only set if null to keep shortest path
                        boolean endValueInitialized = !Objects.isNull(endValue.get(adjI).get(adjJ));
                        boolean boardLogisticsInitialized = (boardLogistics.getCell(adjI, adjJ).logic != LogisticState.UNKNOWN);
                        if (endValueInitialized != boardLogisticsInitialized) {
                            throw new Exception("logistics board initialization should match endValue initialization");
                        }
                        if (endValueInitialized) {
                            continue;
                        }
                        if (remainingAreFrees && remainingAreMines) {
                            throw new Exception("remaining squares can't be both deducible frees and mines");
                        }
                        endValue.get(adjI).set(adjJ, currValue);
                        addBackAdjacentStatesToQueue(adjI, adjJ, board);
                        if (remainingAreMines) {
                            boardLogistics.getCell(adjI, adjJ).logic = LogisticState.MINE;
                        }
                        if (remainingAreFrees) {
                            boardLogistics.getCell(adjI, adjJ).logic = LogisticState.FREE;
                        }
                    }
                    continue;
                }
            }

            //TODO: for each intersecting BfsState, check deduction 2,3

            //TODO: for each pair of intersecting BfsStates, check deduction 4, 4.5
        }

        return boardLogistics;
    }

    //everytime we find a new deduction, we need to add back surrounding logical states to bfs queue
    //to make sure we don't miss anything.
    //this ultimately is because we don't know what order to visit states in
    private void addBackAdjacentStatesToQueue(int tileI, int tileJ, Board<TileNoFlagsForSolver> board) {
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++){
                int adjI = tileI + di;
                int adjJ = tileJ + dj;
                if (board.outOfBounds(adjI, adjJ)) {
                    continue;
                }
                //System.out.println("adding this many new states to q: " + gridLocationToStates.get(adjI).get(adjJ).size());
                for (BfsState state : gridLocationToStates.get(adjI).get(adjJ)) {
                    q.add(state);
                }
            }
        }
    }

    public ArrayList<BfsValue> getListOfTransitions(int tileI, int tileJ) {
        //idea to retrieve this: dfs-style topo sort:
        //call dfs initially on endValue[tileI][tileJ], then push state/value onto return ArrayList
        //only after all dfs recursive calls return
        //
        //this gives reverse topo order, but the adjacency list is already reversed, giving it in order

        ArrayList<BfsValue> transitionList = new ArrayList<>();
        return transitionList;
    }

    private BfsValue getValue(BfsState state) {
        return Objects.requireNonNull(stateToValue.get(state.centerI).get(state.centerJ).get(state.subsetSurroundingSquares));
    }

    private Board<TileWithLogistics> initializeStructures(Board<TileNoFlagsForSolver> board) throws Exception {
        q.clear();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                //can't clear as you go because you might clear a newly added BfsState
                //so instead clear all at once in the beginning
                gridLocationToStates.get(i).get(j).clear();
                stateToValue.get(i).get(j).clear();
                endValue.get(i).set(j, null);
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
                    BfsState currState = new BfsState(i, j, dirMask, true);
                    ArrayList<BfsState> tmpList = new ArrayList<>(1);
                    tmpList.add(currState);
                    BfsValue currValue = new BfsValue(numMines, numMines, BfsTransitionType.BASE_CASE_FROM_NUMBER, tmpList);
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
        return new Board<TileWithLogistics>(tmpBoard, board.getMines());
    }

}