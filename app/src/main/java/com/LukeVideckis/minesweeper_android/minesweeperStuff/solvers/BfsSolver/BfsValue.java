package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BfsSolver;

import java.util.ArrayList;

public class BfsValue {
    //[inclusive, inclusive] range
    int minNumMines, maxNumMines;
    BfsTransitionType type;
    //may need to change to arraylist of previous states
    ArrayList<BfsState> prevStates;

    public BfsValue(int minNumMines, int maxNumMines, BfsTransitionType type, ArrayList<BfsState> prevStates) {
        this.minNumMines = minNumMines;
        this.maxNumMines = maxNumMines;
        this.type = type;
        this.prevStates = prevStates;
    }
}
