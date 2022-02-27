package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;

public class TileWithLogistics extends TileNoFlagsForSolver {
    public boolean isLogicalMine, isLogicalFree;

    public TileWithLogistics() {
        super();
        isLogicalFree = isLogicalMine = false;
    }

    public TileWithLogistics(TileWithLogistics rhs) {
        super(rhs);
        isLogicalFree = rhs.isLogicalFree;
        isLogicalMine = rhs.isLogicalMine;
    }

    public TileWithLogistics(char c) {
        super(c);
        isLogicalFree = isLogicalMine = false;
    }

    public void set(TileWithProbability tileProb) {
        super.set(tileProb);
        isLogicalFree = tileProb.mineProbability.equals(0);
        isLogicalMine = tileProb.mineProbability.equals(1);
    }
}
