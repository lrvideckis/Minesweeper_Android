package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTile;


public class ConvertGameBoardFormat {

    public static void convertToExistingBoard(MinesweeperGame minesweeperGame, VisibleTile[][] board, boolean convertLogicalStuff) throws Exception {
        final int rows = minesweeperGame.getRows();
        final int cols = minesweeperGame.getCols();
        if (board.length != rows) {
            throw new Exception("minesweeper game rows doesn't match board rows");
        }
        for (int i = 0; i < rows; ++i) {
            if (board[i].length != cols) {
                throw new Exception("minesweeper game cols doesn't match board cols");
            }
            for (int j = 0; j < cols; ++j) {
                if (board[i][j] == null) {
                    throw new Exception("cell is null");
                }
                if (convertLogicalStuff) {
                    board[i][j].updateVisibilitySurroundingMinesAndLogicalStuff(minesweeperGame.getCell(i, j));
                } else {
                    board[i][j].updateVisibilityAndSurroundingMines(minesweeperGame.getCell(i, j));
                }
            }
        }
    }
}
