package com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines;

import com.LukeVideckis.minesweeper_android.customExceptions.NoAwayCellsToMoveAMineToException;
import com.LukeVideckis.minesweeper_android.customExceptions.NoInterestingMinesException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithMine;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;
import java.util.Collections;

//game engine with extra functionality of moving mines around to help create solvable boards
public class EngineForCreatingSolvableBoard extends GameEngine {
    public EngineForCreatingSolvableBoard(int rows, int cols, int numberOfMines, boolean hasAn8) throws Exception {
        super(rows, cols, numberOfMines, hasAn8);
    }

    public EngineForCreatingSolvableBoard(EngineForCreatingSolvableBoard rhs) throws Exception {
        super(rhs);
    }

    public void shuffleInterestingMinesAndMakeOneAway(int firstClickI, int firstClickJ) throws Exception {
        int interestingMines = 0;
        ArrayList<Pair<Integer, Integer>>
                interestingSpots = new ArrayList<>(),
                freeAwayCells = new ArrayList<>();
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                if (isInterestingCell(i, j) && AwayCell.isNextToAnAwayCellEngine(new Board<>(grid.getGrid(), grid.getMines()), i, j)) {
                    if (grid.getCell(i, j).isMine) {
                        ++interestingMines;
                    }
                    interestingSpots.add(new Pair<>(i, j));
                }
                if (AwayCell.isAwayCellEngine(new Board<>(grid.getGrid(), grid.getMines()), i, j) &&
                        !grid.getCell(i, j).isMine &&
                        notPartOfThe8(i, j)
                ) {
                    freeAwayCells.add(new Pair<>(i, j));
                }
            }
        }
        if (interestingMines == 0) {
            throw new NoInterestingMinesException("no interesting mines, but there needs to be one to remove");
        }
        if (freeAwayCells.isEmpty()) {
            throw new NoAwayCellsToMoveAMineToException("no free away cells");
        }
        int tempInterestingCount = 0;
        for (Pair<Integer, Integer> interestingSpot : interestingSpots) {
            final int i = interestingSpot.first;
            final int j = interestingSpot.second;
            if (grid.getCell(i, j).isMine) {
                ++tempInterestingCount;
                changeMineStatusAndVisibility(i, j, false);
            }
        }
        if (tempInterestingCount != interestingMines) {
            throw new Exception("sanity check that # interesting mines matches");
        }
        Collections.shuffle(interestingSpots);
        for (int pos = 0; pos < interestingMines - 1; ++pos) {
            final int i = interestingSpots.get(pos).first;
            final int j = interestingSpots.get(pos).second;
            changeMineStatusAndVisibility(i, j, true);
        }
        Collections.shuffle(freeAwayCells);
        {
            int i = freeAwayCells.get(0).first;
            int j = freeAwayCells.get(0).second;
            changeMineStatusAndVisibility(i, j, true);
        }

        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                grid.getCell(i, j).state = TileState.NOT_FLAGGED;
            }
        }
        revealCell(firstClickI, firstClickJ);
    }

    public void shuffleAwayMines() throws Exception {
        ArrayList<Pair<Integer, Integer>> awayCells = new ArrayList<>();
        int mineCount = 0;
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                if (AwayCell.isAwayCellEngine(new Board<>(grid.getGrid(), grid.getMines()), i, j) && notPartOfThe8(i, j)) {
                    awayCells.add(new Pair<>(i, j));
                    if (grid.getCell(i, j).isMine) {
                        ++mineCount;
                    }
                }
            }
        }
        if (mineCount == 0 || mineCount == awayCells.size()) {
            throw new Exception("can't shuffle away mines");
        }
        for (Pair<Integer, Integer> cell : awayCells) {
            if (grid.getCell(cell.first, cell.second).isMine) {
                changeMineStatusAndVisibility(cell.first, cell.second, false);
            }
        }
        Collections.shuffle(awayCells);
        for (int pos = 0; pos < mineCount; ++pos) {
            final int i = awayCells.get(pos).first;
            final int j = awayCells.get(pos).second;
            changeMineStatusAndVisibility(i, j, true);
        }
    }

    public void checkCorrectnessOfSolverOutput(Board<TileWithLogistics> SolverBoard) throws Exception {
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                if (SolverBoard.getCell(i, j).isVisible) {
                    if (SolverBoard.getCell(i, j).isLogicalMine || SolverBoard.getCell(i, j).isLogicalFree) {
                        throw new Exception("visible tiles can't be logical");
                    }
                }
                if (SolverBoard.getCell(i, j).isLogicalFree && SolverBoard.getCell(i, j).isLogicalMine) {
                    throw new Exception("can't be both logical free and logical mine");
                }
                if (SolverBoard.getCell(i, j).isLogicalMine && !grid.getCell(i, j).isMine) {
                    throw new Exception("found a logical mine which is free");
                }
                if (SolverBoard.getCell(i, j).isLogicalFree) {
                    if (grid.getCell(i, j).isMine) {
                        throw new Exception("found a logical free which is mine");
                    }
                }
            }
        }
    }

    //hidden (non-visible) cell with visible neighbor
    public boolean isInterestingCell(int i, int j) throws Exception {
        if (grid.getCell(i, j).state == TileState.VISIBLE) {
            return false;
        }
        if (AwayCell.isAwayCellEngine(new Board<>(grid.getGrid(), grid.getMines()), i, j)) {
            return false;
        }
        return notPartOfThe8(i, j);
    }

    private void resetVisibilityOfMeAndNeighbors(int i, int j) throws Exception {
        grid.getCell(i, j).state = TileState.NOT_FLAGGED;
        for (TileWithMine adjTile : grid.getAdjacentCells(i, j)) {
            adjTile.state = TileState.NOT_FLAGGED;
        }
    }

    private void changeMineStatusAndVisibility(int i, int j, boolean isMine) throws Exception {
        resetVisibilityOfMeAndNeighbors(i, j);
        super.changeMineStatus(i, j, isMine);
    }

    private boolean notPartOfThe8(int i, int j) {
        return !(hasAn8 && Math.abs(i - rowWith8) <= 1 && Math.abs(j - colWith8) <= 1);
    }
}
