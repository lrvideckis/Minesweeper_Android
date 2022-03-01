package com.LukeVideckis.minesweeper_android.miscHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;

public class MutableInt {
    private int value;

    public MutableInt(int startVal) {
        value = startVal;
    }

    public void addWith(int delta) throws Exception {
        value = MyMath.addExact(value, delta);
    }

    public int get() {
        return value;
    }
}
