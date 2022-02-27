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

    public TileNoFlagsForSolver(boolean _isVisible, int _numberSurroundingMines) {
        isVisible = _isVisible;
        numberSurroundingMines = _numberSurroundingMines;
    }

    //copy constructor
    public TileNoFlagsForSolver(TileNoFlagsForSolver rhs) {
        isVisible = rhs.isVisible;
        numberSurroundingMines = rhs.numberSurroundingMines;
    }

    public TileNoFlagsForSolver(char c) {
        if (c == '.') {
            set(true, 0);
        } else if (c == 'U') {
            set(false, 0);
        } else if (c == 'B') {
            set(false, 0);
        } else {
            set(true, c - '0');
        }
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
        isVisible = (rhs.state == TileState.VISIBLE);
        numberSurroundingMines = rhs.numberSurroundingMines;
    }
}
