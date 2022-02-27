package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;

public class TileWithLogistics extends TileNoFlagsForSolver {
    public boolean isLogicalMine = false, isLogicalFree = false;

    public void set(TileWithProbability tileProb) {
        super.set(tileProb);
        isLogicalFree = tileProb.mineProbability.equals(0);
        isLogicalMine = tileProb.mineProbability.equals(1);
    }
}
