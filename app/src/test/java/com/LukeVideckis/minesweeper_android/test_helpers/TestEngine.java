package com.LukeVideckis.minesweeper_android.test_helpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines.GameEngine;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithMine;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;
import java.util.Collections;

public class TestEngine extends GameEngine {
    public TestEngine(int rows, int cols, int numberOfMines, boolean _hasAn8) throws Exception {
        super(rows, cols, numberOfMines, _hasAn8);
    }

    //used in tests: some tests play a game out to completion - and compare holy grail solver to naive backtracking on each stage
    //we need to reveal a random cell if we get stuck along the way
    public void revealRandomFreeCell() throws Exception {
        ArrayList<Pair<Integer, Integer>> freeCells = new ArrayList<>();
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                TileWithMine curr = grid.getCell(i, j);
                if (curr.state != TileState.VISIBLE && !curr.isMine) {
                    freeCells.add(new Pair<>(i, j));
                }
            }
        }
        if (freeCells.isEmpty()) {
            throw new Exception("there should have been at least 1 free cell since the game isn't won");
        }
        Collections.shuffle(freeCells);
        final int i = freeCells.get(0).first;
        final int j = freeCells.get(0).second;
        revealCell(i, j);
    }
}
