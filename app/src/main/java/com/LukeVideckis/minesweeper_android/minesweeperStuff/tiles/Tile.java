package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;

public class Tile extends VisibleTileWithProbability {
    private boolean isFlagged, isMine;

    public Tile() {
        super();
        isFlagged = isMine = false;
    }

    //copy constructor
    public Tile(Tile other) {
        super(other);
        isMine = other.isMine;
        isFlagged = other.isFlagged;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setIsMine(boolean _isMine) {
        isMine = _isMine;
    }

    //TODO: breaks single responsibility
    public boolean isFlagged() {
        if (isVisible) {
            isFlagged = false;
        }
        return isFlagged;
    }

    public void setIsFlagged(boolean _isFlagged) {
        isFlagged = _isFlagged;
    }

    public void resetLogicalStuffAndVisibility() throws Exception {
        isVisible = isLogicalMine = isLogicalFree = false;
        mineProbability.setValues(0, 1);
    }

    //returns whether this tile is revealed - it's revealed if previously not visible
    public boolean revealTile() throws Exception {
        boolean revealed = !isVisible;
        isVisible = true;
        isFlagged = isLogicalFree = false;
        mineProbability.setValues(0, 1);
        if (isMine) {
            throw new Exception("can't reveal a mine");
        }
        if (isLogicalMine) {
            throw new Exception("can't reveal a logical mine");
        }
        return revealed;
    }

    public void toggleFlag() {
        if (isVisible) {
            isFlagged = false;
            return;
        }
        isFlagged = !isFlagged;
    }
}
