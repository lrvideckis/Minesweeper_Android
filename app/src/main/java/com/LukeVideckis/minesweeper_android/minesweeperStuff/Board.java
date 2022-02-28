package com.LukeVideckis.minesweeper_android.minesweeperStuff;

import java.util.ArrayList;

public class Board<tile> {
    private final int rows;
    private final int cols;
    private final int mines;
    private final tile[][] grid;

    public Board(tile[][] _grid, int _mines) throws Exception {
        if (_grid.length == 0 || _grid[0].length == 0) {
            throw new Exception("grid can't be empty");
        }
        rows = _grid.length;
        cols = _grid[0].length;
        for (int i = 0; i < rows; i++) {
            if (_grid[i].length != cols) {
                throw new Exception("grid isn't rectangular");
            }
        }
        grid = _grid;
        mines = _mines;
    }

    public boolean outOfBounds(int i, int j) {
        return (i < 0 || j < 0 || i >= rows || j >= cols);
    }

    public tile getCell(int i, int j) throws Exception {
        if (outOfBounds(i, j)) {
            throw new Exception("index out of bounds");
        }
        return grid[i][j];
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getMines() {
        return mines;
    }

    public tile[][] getGrid() {
        return grid;
    }

    public ArrayList<tile> getAdjacentCells(int i, int j) throws Exception {
        ArrayList<tile> adjCells = new ArrayList<>();
        for (int di = -1; di <= 1; ++di) {
            for (int dj = -1; dj <= 1; ++dj) {
                if (di == 0 && dj == 0) {
                    continue;
                }
                final int adjI = i + di;
                final int adjJ = j + dj;
                if (outOfBounds(adjI, adjJ)) {
                    continue;
                }
                adjCells.add(getCell(adjI, adjJ));
            }
        }
        return adjCells;
    }

    public ArrayList<int[]> getAdjacentIndexes(int i, int j) {
        ArrayList<int[]> adjIndexes = new ArrayList<>();
        for (int di = -1; di <= 1; ++di) {
            for (int dj = -1; dj <= 1; ++dj) {
                if (di == 0 && dj == 0) {
                    continue;
                }
                final int adjI = i + di;
                final int adjJ = j + dj;
                if (outOfBounds(adjI, adjJ)) {
                    continue;
                }
                adjIndexes.add(new int[]{adjI, adjJ});
            }
        }
        return adjIndexes;
    }
}

