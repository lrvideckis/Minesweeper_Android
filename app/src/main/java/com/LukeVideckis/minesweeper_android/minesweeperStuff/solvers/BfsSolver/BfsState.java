package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BfsSolver;

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

    public boolean intersects(BfsState otherState) {
        return false;//TODO
    }
}
