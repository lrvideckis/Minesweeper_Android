package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

public class GetAdjacentCells {
    public static int[][] getAdjacentCells(int i, int j, int rows, int cols) {
        int cntAdjacent = 0;
        for (int di = -1; di <= 1; ++di) {
            for (int dj = -1; dj <= 1; ++dj) {
                if (di == 0 && dj == 0) {
                    continue;
                }
                final int adjI = i + di;
                final int adjJ = j + dj;
                if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
                    continue;
                }
                ++cntAdjacent;
            }
        }
        int[][] adjCells = new int[cntAdjacent][2];
        cntAdjacent = 0;
        for (int di = -1; di <= 1; ++di) {
            for (int dj = -1; dj <= 1; ++dj) {
                if (di == 0 && dj == 0) {
                    continue;
                }
                final int adjI = i + di;
                final int adjJ = j + dj;
                if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
                    continue;
                }
                adjCells[cntAdjacent][0] = adjI;
                adjCells[cntAdjacent][1] = adjJ;
                ++cntAdjacent;
            }
        }
        return adjCells;
    }
}
