package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;

public class AllCellsAreHidden {
    private AllCellsAreHidden() throws Exception {
        throw new Exception("No instances allowed!");
    }

    public static boolean allCellsAreHidden(Board<TileNoFlagsForSolver> board) throws Exception {
        for (int i = 0; i < board.getRows(); ++i) {
            for (int j = 0; j < board.getCols(); ++j) {
                if (board.getCell(i, j).isVisible) {
                    return false;
                }
            }
        }
        return true;
    }
}
