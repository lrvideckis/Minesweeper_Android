package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.Tile;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTile;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTileWithProbability;

public class Board<tile_type> {
    private final int rows;
    private final int cols;
    private tile_type[][] grid;

    public Board(tile_type[][] _grid) throws Exception {
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
    }

    public Board(Board<Tile> rhs) {
        rows = rhs.rows;
        cols = rhs.cols;
        grid = (tile_type[][]) rhs.grid;
    }

    public boolean outOfBounds(int i, int j) {
        return (i < 0 || j < 0 || i >= rows || j >= cols);
    }

    public tile_type getCell(int i, int j) throws Exception {
        if(outOfBounds(i,j)) {
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

};

