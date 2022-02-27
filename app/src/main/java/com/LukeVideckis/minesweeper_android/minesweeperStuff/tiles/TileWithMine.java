package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;

public class TileWithMine extends Tile {
    public boolean isMine = false;

    public void set(TileWithMine rhs) {
        super.set(rhs);
        isMine = rhs.isMine;
    }
}
