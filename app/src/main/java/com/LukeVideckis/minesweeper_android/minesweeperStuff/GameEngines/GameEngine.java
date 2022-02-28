package com.LukeVideckis.minesweeper_android.minesweeperStuff.GameEngines;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.Tile;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileState;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithMine;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;
import java.util.Collections;


public class GameEngine {
    protected final Board<TileWithMine> grid;
    protected final boolean hasAn8;
    protected int rowWith8 = -1;
    protected int colWith8 = -1;//TODO: refactor these into EngineForCreatingSolvableBoard
    protected boolean firstClick = true, isGameLost = false;

    //***public members***
    public GameEngine(int rows, int cols, int numberOfMines, boolean _hasAn8) throws Exception {
        if (numberOfMines > rows * cols - 9) {
            throw new Exception("too many mines for zero start, UI shouldn't allow for this to happen");
        }
        if(_hasAn8 && numberOfMines < 8) {
            throw new Exception("too few mines for an 8, UI shouldn't allow for this to happen");
        }
        hasAn8 = _hasAn8;
        TileWithMine[][] tmpGrid = new TileWithMine[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                tmpGrid[i][j] = new TileWithMine();
            }
        }
        grid = new Board<>(tmpGrid, numberOfMines);
    }

    //copy constructor - deep copy of board
    public GameEngine(GameEngine rhs) throws Exception {
        hasAn8 = rhs.hasAn8;
        rowWith8 = rhs.rowWith8;
        colWith8 = rhs.colWith8;
        firstClick = rhs.firstClick;
        isGameLost = rhs.isGameLost;
        {
            TileWithMine[][] tmpGrid = new TileWithMine[rhs.getRows()][rhs.getCols()];
            for (int i = 0; i < rhs.getRows(); ++i) {
                for (int j = 0; j < rhs.getCols(); ++j) {
                    tmpGrid[i][j] = new TileWithMine(rhs.grid.getGrid()[i][j]);
                }
            }
            grid = new Board<>(tmpGrid, rhs.getNumberOfMines());
        }

        //only error checking below!
        boolean foundAn8 = false;
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                if (grid.getCell(i, j).isMine) {
                    continue;
                }
                if (grid.getCell(i, j).numberSurroundingMines == 8) {
                    foundAn8 = true;
                }
                int cntSurroundingMines = 0;
                for (TileWithMine adjTile : grid.getAdjacentCells(i, j)) {
                    if (adjTile.isMine) {
                        ++cntSurroundingMines;
                    }
                }
                if (cntSurroundingMines != grid.getCell(i, j).numberSurroundingMines) {
                    throw new Exception("number of surrounding mines doesn't match");
                }
            }
        }
        if (hasAn8 && !foundAn8) {
            throw new Exception("game should have an 8, but no 8 was found");
        }
    }

    //initialize new game engine with a given board (including mines), and starting click (reveal starting cell)
    //TODO: I hate that this exists, let's find a better way to initialize the engine after finding a solvable board
    public GameEngine(Board<TileWithMine> startBoard, int firstClickI, int firstClickJ, boolean _hasAn8) throws Exception {
        grid = startBoard;
        //TODO: when hint mode can have an 8, re-thing about how to initialize row/col with 8
        hasAn8 = _hasAn8;
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                if (grid.getCell(i, j).state == TileState.VISIBLE) {
                    grid.getCell(i, j).state = TileState.NOT_FLAGGED;
                }
            }
        }
        if (grid.getCell(firstClickI, firstClickJ).isMine) {
            throw new Exception("first clicked cell shouldn't be a mine");
        }
        if (grid.getCell(firstClickI, firstClickJ).numberSurroundingMines != 0) {
            throw new Exception("first clicked cell isn't a zero start");
        }
        firstClick = false;
        revealCell(firstClickI, firstClickJ);
    }

    public int getRows() {
        return grid.getRows();
    }

    public int getCols() {
        return grid.getCols();
    }

    public int getNumberOfMines() {
        return grid.getMines();
    }

    public int getNumberOfFlags() throws Exception {
        int numFlags = 0;
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                if (grid.getCell(i, j).state == TileState.FLAGGED) {
                    numFlags++;
                }
            }
        }
        return numFlags;
    }

    public boolean isBeforeFirstClick() {
        return firstClick;
    }

    //throws if game is still going (not lost nor won)
    public TileWithMine getCellWithMine(int row, int col) throws Exception {
        if (getGameState() == GameState.STILL_GOING) {
            throw new Exception("You cannot know mine locations until game is over");
        }
        return grid.getCell(row, col);
    }

    public Tile getCell(int row, int col) throws Exception {
        return grid.getCell(row, col);
    }

    public void clickCell(int row, int col, boolean toggleFlags) throws Exception {
        if (firstClick && !toggleFlags) {
            firstClick = false;
            if (hasAn8) {
                initializeMineLocationsAfterFirstClickedCellWith8(row, col);
            } else {
                initializeMineLocationsAfterFirstClickedCell(row, col);
            }
            return;
        }

        if (getGameState() != GameState.STILL_GOING) {
            return;
        }
        TileWithMine curr = grid.getCell(row, col);
        if (curr.state == TileState.VISIBLE) {
            checkForChords(row, col);
            return;
        }
        if (toggleFlags) {
            curr.toggleFlag();
            return;
        }
        if (curr.state == TileState.FLAGGED) {
            return;
        }
        if (curr.isMine) {
            isGameLost = true;
            return;
        }
        revealCell(row, col);
    }

    //game is won if all free cells are visible
    //so it's possible to have un-flagged mines for a winning game state
    public GameState getGameState() throws Exception {
        if (isGameLost) {
            return GameState.LOST;
        }
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                TileWithMine currCell = grid.getCell(i, j);
                if (!currCell.isMine && currCell.state != TileState.VISIBLE) {
                    return GameState.STILL_GOING;
                }
            }
        }
        return GameState.WON;
    }

    //***private members***

    //Definition of chord: if the surrounding tiles have correct # of flags, then reveal all un-flagged neighbors
    private void checkForChords(int row, int col) throws Exception {
        boolean revealSurroundingCells = true;
        for (TileWithMine adj : grid.getAdjacentCells(row, col)) {
            if (adj.state == TileState.VISIBLE) {
                continue;
            }
            if (adj.state == TileState.FLAGGED && !adj.isMine) {
                isGameLost = true;
                return;
            }
            if ((adj.state == TileState.FLAGGED) != adj.isMine) {
                revealSurroundingCells = false;
            }
        }
        if (!revealSurroundingCells) {
            return;
        }
        for (int[] adjIdx : grid.getAdjacentIndexes(row, col)) {
            final int adjI = adjIdx[0], adjJ = adjIdx[1];
            TileWithMine adj = grid.getCell(adjI, adjJ);
            if (adj.isMine) {
                continue;
            }
            revealCell(adjI, adjJ);
        }
    }

    private void initializeMineLocationsAfterFirstClickedCellWith8(int row, int col) throws Exception {
        ArrayList<Pair<Integer, Integer>> spots = new ArrayList<>();
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                if (Math.abs(row - i) <= 1 && Math.abs(col - j) <= 1) {
                    continue;
                }
                spots.add(new Pair<>(i, j));
            }
        }

        if (spots.size() < grid.getMines()) {
            throw new Exception("too many mines to have a zero start");
        }

        if (grid.getMines() < 8) {
            throw new Exception("too few mines for an 8");
        }

        Collections.shuffle(spots);

        for (int pos = 0; pos < spots.size(); ++pos) {
            final int i = spots.get(pos).first;
            final int j = spots.get(pos).second;
            if (i == 0 || j == 0 || i == grid.getRows() - 1 || j == grid.getCols() - 1) {
                continue;
            }
            rowWith8 = i;
            colWith8 = j;
            for (int[] adj : grid.getAdjacentIndexes(i, j)) {
                changeMineStatus(adj[0], adj[1], true);
            }
            break;
        }
        if (rowWith8 == -1 || colWith8 == -1) {
            throw new Exception("didn't find a spot for an 8, but there should be one");
        }

        spots.clear();
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                if (Math.abs(row - i) <= 1 && Math.abs(col - j) <= 1) {
                    continue;
                }
                if (Math.abs(rowWith8 - i) <= 1 && Math.abs(colWith8 - j) <= 1) {
                    continue;
                }
                spots.add(new Pair<>(i, j));
            }
        }

        Collections.shuffle(spots);

        if (spots.size() < grid.getMines() - 8) {
            throw new Exception("too many mines to have a zero start with an 8");
        }

        for (int pos = 0; pos < grid.getMines() - 8; ++pos) {
            final int i = spots.get(pos).first;
            final int j = spots.get(pos).second;
            changeMineStatus(i, j, true);
        }
        if (grid.getCell(row, col).isMine) {
            throw new Exception("starting click shouldn't be a mine");
        }
        revealCell(row, col);
    }

    //TODO: combine these 2 functions with logic around 8's existence
    protected void initializeMineLocationsAfterFirstClickedCell(int row, int col) throws Exception {
        ArrayList<Pair<Integer, Integer>> spots = new ArrayList<>();
        for (int i = 0; i < grid.getRows(); ++i) {
            for (int j = 0; j < grid.getCols(); ++j) {
                if (Math.abs(row - i) <= 1 && Math.abs(col - j) <= 1) {
                    continue;
                }
                spots.add(new Pair<>(i, j));
            }
        }

        if (spots.size() < grid.getMines()) {
            throw new Exception("too many mines to have a zero start");
        }

        Collections.shuffle(spots);

        for (int pos = 0; pos < grid.getMines(); ++pos) {
            final int mineRow = spots.get(pos).first;
            final int mineCol = spots.get(pos).second;
            changeMineStatus(mineRow, mineCol, true);
        }
        if (grid.getCell(row, col).isMine) {
            throw new Exception("starting click shouldn't be a mine");
        }
        revealCell(row, col);
    }

    //update mine as well as surround tile mine counts
    protected void changeMineStatus(int i, int j, boolean isMine) throws Exception {
        if (grid.getCell(i, j).isMine == isMine) {
            return;
        }
        grid.getCell(i, j).isMine = isMine;
        for (TileWithMine adjTile : grid.getAdjacentCells(i, j)) {
            if (isMine) {
                adjTile.numberSurroundingMines++;
            } else {
                adjTile.numberSurroundingMines--;
            }
        }
    }

    //does a dfs to reveal component of 0 tiles (bounded by non-0 tiles)
    //assumes cell is not a mine
    protected void revealCell(int row, int col) throws Exception {
        TileWithMine curr = grid.getCell(row, col);
        if (curr.isMine) {
            throw new Exception("can't reveal a mine");
        }
        curr.state = TileState.VISIBLE;//acts as visited array in dfs
        if (curr.numberSurroundingMines > 0) {
            return;
        }
        for (int[] adj : grid.getAdjacentIndexes(row, col)) {
            final int adjRow = adj[0];
            final int adjCol = adj[1];
            if (grid.getCell(adjRow, adjCol).state != TileState.VISIBLE) {
                revealCell(adjRow, adjCol);
            }
        }
    }
}
