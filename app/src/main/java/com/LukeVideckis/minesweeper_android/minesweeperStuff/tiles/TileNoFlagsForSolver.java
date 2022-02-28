package com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles;

public class TileNoFlagsForSolver {
    /*these members are not source of truth, source of truth is TileWithFlags. Tile class is used
    only by solvers as a temporary class to not give solvers access to flags*/
    public boolean isVisible;
    public int numberSurroundingMines;

    public TileNoFlagsForSolver() {
        isVisible = false;
        numberSurroundingMines = 0;
    }

    public void set(boolean _isVisible, int _numberSurroundingMines) {
        isVisible = _isVisible;
        numberSurroundingMines = _numberSurroundingMines;
    }

    public void set(TileNoFlagsForSolver rhs) {
        isVisible = rhs.isVisible;
        numberSurroundingMines = rhs.numberSurroundingMines;
    }

    public void set(Tile rhs) {
        isVisible = (rhs.state == TileState.VISIBLE);//lose information on purpose: solvers should not know about flags
        numberSurroundingMines = rhs.numberSurroundingMines;
    }
}
