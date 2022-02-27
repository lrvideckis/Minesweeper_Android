package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;

public class Tile {

    public TileState state;
    public int numberSurroundingMines;

    public Tile() {
        state = TileState.NOT_FLAGGED;
        numberSurroundingMines = 0;
    }

    public Tile(Tile rhs) {
        state = rhs.state;
        numberSurroundingMines = rhs.numberSurroundingMines;
    }

    public Tile(TileState _state, int _numberSurroundingMines) {
        state = _state;
        numberSurroundingMines = _numberSurroundingMines;
    }

    public void toggleFlag() {
        switch(state) {
            case FLAGGED:
                state = TileState.NOT_FLAGGED;
                break;
            case NOT_FLAGGED:
                state = TileState.FLAGGED;
                break;
            case VISIBLE:
                break;
        }
    }

    public void set(Tile rhs) {
        state = rhs.state;
        numberSurroundingMines = rhs.numberSurroundingMines;
    }
}
