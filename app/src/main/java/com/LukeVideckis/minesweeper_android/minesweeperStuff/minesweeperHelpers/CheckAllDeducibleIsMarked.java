package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithMine;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;

import java.util.concurrent.atomic.AtomicBoolean;

public class CheckAllDeducibleIsMarked {

    public static boolean checkAllDeducibleStuffIsMarked(Board<TileWithProbability> boardFromSolver, Board<Tile> boardWithFlags) {
        const int ROWS = boardFromSolver.getRows();
        const int COLS = boardFromSolver.getCols();
        if(rows != boardWithFlags.getRows() || cols != boardWithFlags.getCols()) {
            throw new Exception("either rows or cols does not match.");
        }
        for(int i = 0; i < ROWS; i++) {
            for(int j = 0; j < COLS; j++) {
                if(boardFromSolver[i][j].isVisible) {
                    continue;
                }
                if(boardFromSolver[i][j].mineProbability.equals(0)) {
                    // Any deducible free left un-tapped means the user hasn't correctly marked all
                    // deducible stuff, as they should have tapped this deducible free.
                    return false;
                }
                if(boardFromSolver[i][j].mineProbability.equals(1) && boardWithFlags.state == TileState.NOT_FLAGGED) {

                }

            }
        }
        return true;
    }
}
