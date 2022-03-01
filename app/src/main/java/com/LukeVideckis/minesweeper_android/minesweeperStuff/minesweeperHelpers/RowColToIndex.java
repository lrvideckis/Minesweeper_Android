package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

//TODO: try to remove this class entirely
public class RowColToIndex {
    private RowColToIndex() throws Exception {
        throw new Exception("No instances allowed!");
    }

    public static int rowColToIndex(int i, int j, int rows, int cols) {
        if (i < 0 || j < 0 || i >= rows || j >= cols) {
            throw new ArrayIndexOutOfBoundsException("throwing from getConnectedComponents.getNode()");
        }
        return i * cols + j;
    }

    public static Pair<Integer, Integer> indexToRowCol(int index, int rows, int cols) {
        if (index < 0 || index >= rows * cols) {
            throw new ArrayIndexOutOfBoundsException("index is out of bounds");
        }
        return new Pair<>(index / cols, index % cols);
    }
}
