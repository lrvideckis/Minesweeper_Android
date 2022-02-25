package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;


import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;

public class VisibleTile {
    public boolean isVisible, isLogicalMine, isLogicalFree;
    public int numberSurroundingMines;

    public VisibleTile() {
        reset();
    }

    //copy constructor
    public VisibleTile(VisibleTile other) {
        set(other);
    }

    public VisibleTile(char c) throws Exception {
        reset();
        if (c == '.') {
            updateVisibilityAndSurroundingMines(true, 0);
        } else if (c == 'U') {
            updateVisibilityAndSurroundingMines(false, 0);
        } else if (c == 'B') {
            updateVisibilityAndSurroundingMines(false, 0);
            isLogicalMine = true;
        } else {
            updateVisibilityAndSurroundingMines(true, c - '0');
        }
    }

    public void set(VisibleTile other) {
        isVisible = other.isVisible;
        isLogicalMine = other.isLogicalMine;
        isLogicalFree = other.isLogicalFree;
        numberSurroundingMines = other.numberSurroundingMines;
    }

    public boolean isNonLogicalStuffEqual(VisibleTile other) {
        return isVisible == other.isVisible &&
                numberSurroundingMines == other.numberSurroundingMines;
    }

    public boolean isEverythingEqual(VisibleTile other) {
        return isVisible == other.isVisible &&
                isLogicalMine == other.isLogicalMine &&
                isLogicalFree == other.isLogicalFree &&
                numberSurroundingMines == other.numberSurroundingMines;
    }

    private void reset() {
        isLogicalFree = isLogicalMine = isVisible = false;
        numberSurroundingMines = 0;
    }

    public boolean getIsVisible() {
        return isVisible;
    }

    public boolean getIsLogicalMine() {
        return isLogicalMine;
    }

    public boolean getIsLogicalFree() {
        return isLogicalFree;
    }

    public int getNumberSurroundingMines() {
        return numberSurroundingMines;
    }

    public void updateVisibilityAndSurroundingMines(MinesweeperGame.Tile tile) throws Exception {
        reset();
        isVisible = tile.isVisible;
        numberSurroundingMines = tile.numberSurroundingMines;
    }

    public void updateVisibilitySurroundingMinesAndLogicalStuff(MinesweeperGame.Tile tile) throws Exception {
        if (tile.isLogicalFree && tile.isLogicalMine) {
            throw new Exception("tile can't be both logical free and mine");
        }
        if (tile.isVisible) {
            if (tile.isLogicalFree || tile.isLogicalMine) {
                throw new Exception("visible tiles can't be logical stuff");
            }
        }
        reset();
        isVisible = tile.isVisible;
        numberSurroundingMines = tile.numberSurroundingMines;
        isLogicalMine = tile.isLogicalMine;
        isLogicalFree = tile.isLogicalFree;
    }

    public void updateVisibilityAndSurroundingMines(boolean isVisible, int numberSurroundingMines) throws Exception {
        reset();
        this.isVisible = isVisible;
        this.numberSurroundingMines = numberSurroundingMines;
    }
}
