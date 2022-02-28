package com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithMine;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;
import java.util.Collections;

//game engine with essentially functionality to handle when hint button is pressed
public class EngineGetHelpMode extends GameEngine {
    private int getHelpRow = -1, getHelpCol = -1;

    public EngineGetHelpMode(int rows, int cols, int numberOfMines, boolean _hasAn8) throws Exception {
        super(rows, cols, numberOfMines, _hasAn8);
    }

    public EngineGetHelpMode(Board<TileWithMine> startBoard, int firstClickI, int firstClickJ, boolean hasAn8) throws Exception {
        super(startBoard, firstClickI, firstClickJ, hasAn8);
    }

    public int getHelpRow() {
        return getHelpRow;
    }

    public int getHelpCol() {
        return getHelpCol;
    }

    //assumes solver finished successfully
    public boolean userIdentifiedAllLogicalStuffCorrectly(Board<TileWithProbability> solverBoard) throws Exception {
        if (getGameState() != GameState.STILL_GOING) {
            throw new Exception("shouldn't be running solver on finished board");
        }
        if (solverBoard.getRows() != getRows() || solverBoard.getCols() != getCols()) {
            throw new Exception("solver board dimensions don't match game engine dimensions");
        }
        if(firstClick && getNumberOfMines() == 0) {
            return false;
        }
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                TileWithMine currTile = grid.getCell(i, j);
                if (currTile.state == TileState.VISIBLE) {
                    continue;
                }
                final boolean isLogicalFree = solverBoard.getCell(i, j).mineProbability.equals(0);
                final boolean isLogicalMine = solverBoard.getCell(i, j).mineProbability.equals(1);
                if (isLogicalFree || (isLogicalMine != (currTile.state == TileState.FLAGGED))) {
                    return false;
                }
            }
        }
        return true;
    }

    //when user presses getHint, without correctly identifying the logical stuff, the game is over
    public void endGameFromFailedHint() throws Exception {
        if(getGameState() == GameState.WON) {
            throw new Exception("can't end game when it's won");
        }
        isGameLost = true;
    }

    public void revealRandomCell() throws Exception {
        if (getGameState() != GameState.STILL_GOING) {
            return;
        }
        if (firstClick) {
            firstClick = false;
            getHelpRow = MyMath.getRand(0, grid.getRows() - 1);
            getHelpCol = MyMath.getRand(0, grid.getCols() - 1);
            //TODO: when 8-mode is changed to be a setting, this will have to be refactored
            initializeMineLocationsAndClickStartCell(getHelpRow, getHelpCol);
            return;
        }
        ArrayList<ArrayList<Pair<Integer, Integer>>> freeCells = new ArrayList<>(3);
        for (int i = 0; i < 3; ++i) {
            freeCells.add(new ArrayList<>());
        }
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                TileWithMine curr = grid.getCell(i, j);
                if (curr.state != TileState.VISIBLE && !curr.isMine) {
                    if (curr.numberSurroundingMines == 0) {
                        freeCells.get(0).add(new Pair<>(i, j));
                    } else {
                        boolean cellIsNextToANumber = false;
                        for (TileWithMine adjTile : grid.getAdjacentCells(i, j)) {
                            if (adjTile.state == TileState.VISIBLE) {
                                cellIsNextToANumber = true;
                                break;
                            }
                        }
                        if (cellIsNextToANumber) {
                            freeCells.get(1).add(new Pair<>(i, j));
                        } else {
                            freeCells.get(2).add(new Pair<>(i, j));
                        }
                    }
                }
            }
        }
        for (int type = 0; type < 3; ++type) {
            if (freeCells.get(type).isEmpty()) {
                continue;
            }
            Collections.shuffle(freeCells.get(type));
            final int i = freeCells.get(type).get(0).first;
            final int j = freeCells.get(type).get(0).second;
            revealCell(i, j);
            getHelpRow = i;
            getHelpCol = j;
            return;
        }
        throw new Exception("there should have been at least 1 free cell since the game isn't won");
    }
}
