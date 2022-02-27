package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;

public class TileWithMine extends Tile {
    public boolean isMine;

    public TileWithMine() {
        super();
        isMine = false;
    }

    //copy constructor
    public TileWithMine(TileWithMine rhs) {
        super(rhs);
        isMine = rhs.isMine;
    }

    public void set(TileWithMine rhs) {
        super.set(rhs);
        isMine = rhs.isMine;
    }
}
