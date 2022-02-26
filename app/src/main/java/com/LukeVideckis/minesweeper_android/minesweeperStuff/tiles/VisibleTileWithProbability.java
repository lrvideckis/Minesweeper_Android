package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;


import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;

public class VisibleTileWithProbability extends VisibleTile {

    public BigFraction mineProbability;

    public VisibleTileWithProbability() {
        super();
        mineProbability = new BigFraction(0);
    }

    public VisibleTileWithProbability(VisibleTile other) {
        super(other);
        mineProbability = new BigFraction(0);
    }

    //copy constructor
    public VisibleTileWithProbability(VisibleTileWithProbability other) {
        super(other);
        mineProbability = new BigFraction(other.mineProbability);
    }

    public VisibleTileWithProbability(char c) throws Exception {
        super(c);
        mineProbability = new BigFraction(0);
    }

    public boolean isEverythingEqual(VisibleTileWithProbability other) {
        return super.isEverythingEqual(other) && other.mineProbability.equals(mineProbability);
    }

    public BigFraction getMineProbability() {
        return mineProbability;
    }

    public void updateVisibilityAndSurroundingMines(Tile tile) throws Exception {
        super.updateVisibilityAndSurroundingMines(tile);
        mineProbability.setValues(0, 1);
    }

    public void updateVisibilitySurroundingMinesAndLogicalStuff(Tile tile) throws Exception {
        if (tile.isLogicalFree && !tile.mineProbability.equals(0)) {
            throw new Exception("logical free tile with non-zero probability");
        }
        if (tile.isLogicalMine && !tile.mineProbability.equals(1)) {
            throw new Exception("logical mine tile with non-1 probability");
        }
        super.updateVisibilitySurroundingMinesAndLogicalStuff(tile);
        mineProbability.setValue(tile.mineProbability);
    }

    public void updateVisibilityAndSurroundingMines(boolean isVisible, int numberSurroundingMines) throws Exception {
        super.updateVisibilityAndSurroundingMines(isVisible, numberSurroundingMines);
        if (mineProbability == null) {
            mineProbability = new BigFraction(0);
        } else {
            mineProbability.setValues(0, 1);
        }
    }
}
