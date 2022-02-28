package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;

public class TileWithMine extends Tile {
    public boolean isMine;

    public TileWithMine() {
        super();
        isMine = false;
    }

    public TileWithMine(TileWithMine tileWithMine) {
        super(tileWithMine);
        isMine = tileWithMine.isMine;
    }

    public void set(TileWithMine rhs) {
        super.set(rhs);
        isMine = rhs.isMine;
    }
}
