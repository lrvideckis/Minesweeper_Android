package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;

import com.LukeVideckis.minesweeper_android.miscHelpers.BigFraction;

public class TileWithLogistics extends TileNoFlagsForSolver {
    public LogisticState logic = LogisticState.UNKNOWN;

    public void setLogic(BigFraction mineProb) {
        if (mineProb.equals(1)) {
            logic = LogisticState.MINE;
        } else if (mineProb.equals(0)) {
            logic = LogisticState.FREE;
        } else {
            logic = LogisticState.UNKNOWN;
        }
    }
}
