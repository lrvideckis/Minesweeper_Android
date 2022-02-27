package com.LukeVideckis.minesweeper_android.test_helpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;

public class TestTileNoFlagsForSolver extends TileNoFlagsForSolver {
    public TestTileNoFlagsForSolver(char c) {
        if (c == '.') {//visible and 0
            set(true, 0);
        } else if (c == 'U'/*un visible*/ || c == 'B' /*forced bomb (un visible)*/ || c == 'F'/*forced free (un visible)*/) {
            set(false, 0);
        } else {//visible and non-zero
            set(true, c - '0');
        }
    }
}
