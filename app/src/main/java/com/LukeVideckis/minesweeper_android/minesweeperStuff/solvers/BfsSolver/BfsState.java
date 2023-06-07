package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BfsSolver;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;

public class BfsState {
    //represents location of center-cell
    final int centerI, centerJ;
    //number in range [0, 2^8)
    //012
    //3 4
    //567
    //each on-bit means that that relative tile is in the subset
    int subsetSurroundingSquares;
    boolean centerIsVisible;

    public BfsState(int centerI, int centerJ, int subsetSurroundingSquares, boolean centerIsVisible) {
        this.centerI = centerI;
        this.centerJ = centerJ;
        this.subsetSurroundingSquares = subsetSurroundingSquares;
        this.centerIsVisible = centerIsVisible;
    }

    //TODO: optimize to O(1) instead of O(8)?
    private boolean containsCell(int cellI, int cellJ) {
        for (int dir = 0; dir < 8; dir++) {
            if (((this.subsetSurroundingSquares >> dir) & 1) == 0) {
                continue;
            }
            final int adjI = this.centerI + Board.deltas[dir][0];
            final int adjJ = this.centerJ + Board.deltas[dir][1];
            if (adjI == cellI && adjJ == cellJ) {
                return true;
            }
        }
        return false;
    }

    //returns true iff otherState is a subset of me
    public boolean isSubsetOfMe(BfsState otherState) {
        for (int dir = 0; dir < 8; dir++) {
            if (((otherState.subsetSurroundingSquares >> dir) & 1) == 0) {
                continue;
            }
            final int adjI = otherState.centerI + Board.deltas[dir][0];
            final int adjJ = otherState.centerJ + Board.deltas[dir][1];
            if(!this.containsCell(adjI, adjJ)) {
                return false;
            }
        }
        return true;
    }
    //returns the set difference: a new BfsState, where each square in it's subset is in me and not in otherState
    public BfsState inMeNotInThem(BfsState otherState) {
        return null;//TODO
    }
}
