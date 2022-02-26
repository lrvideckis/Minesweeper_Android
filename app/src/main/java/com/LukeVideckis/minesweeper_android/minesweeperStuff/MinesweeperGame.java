package com.LukeVideckis.minesweeper_android.minesweeperStuff;

import com.LukeVideckis.minesweeper_android.customExceptions.NoAwayCellsToMoveAMineToException;
import com.LukeVideckis.minesweeper_android.customExceptions.NoInterestingMinesException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.Dsu;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetConnectedComponents;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.RowColToIndex;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTile;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.VisibleTileWithProbability;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;
import java.util.Collections;


public class MinesweeperGame {
    private final int rows, cols, numberOfMines;
    private final Tile[][] grid;
    private int numberOfFlags, rowWith8 = -1, colWith8 = -1, getHelpRow, getHelpCol;
    private boolean firstClick, isGameLost, hasAn8 = false, revealedAHiddenCell = false;

    //***public members***
    public MinesweeperGame(int rows, int cols, int numberOfMines) throws Exception {
        if (tooManyMinesForZeroStart(rows, cols, numberOfMines)) {
            throw new Exception("too many mines for zero start, UI doesn't allow for this to happen");
        }

        this.rows = rows;
        this.cols = cols;
        this.numberOfMines = numberOfMines;
        numberOfFlags = 0;
        firstClick = true;
        isGameLost = false;
        grid = new Tile[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                grid[i][j] = new Tile();
            }
        }
    }

    //copy constructor
    public MinesweeperGame(MinesweeperGame minesweeperGame) throws Exception {
        rows = minesweeperGame.getRows();
        cols = minesweeperGame.getCols();
        getHelpRow = minesweeperGame.getHelpRow;
        getHelpCol = minesweeperGame.getHelpCol;
        numberOfMines = minesweeperGame.getNumberOfMines();
        hasAn8 = minesweeperGame.hasAn8;
        rowWith8 = minesweeperGame.rowWith8;
        colWith8 = minesweeperGame.colWith8;
        numberOfFlags = minesweeperGame.numberOfFlags;
        firstClick = minesweeperGame.firstClick;
        isGameLost = minesweeperGame.isGameLost;
        revealedAHiddenCell = minesweeperGame.revealedAHiddenCell;
        grid = new Tile[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                grid[i][j] = new Tile(minesweeperGame.grid[i][j]);
            }
        }

        boolean foundAn8 = false;
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (grid[i][j].isMine) {
                    continue;
                }

                if (grid[i][j].numberSurroundingMines == 8) {
                    foundAn8 = true;
                }
                int cntSurroundingMines = 0;
                for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
                    final int adjI = adj[0], adjJ = adj[1];
                    if (grid[adjI][adjJ].isMine) {
                        ++cntSurroundingMines;
                    }
                }
                if (cntSurroundingMines != grid[i][j].numberSurroundingMines) {
                    throw new Exception("number of surrounding mines doesn't match");
                }
            }
        }
        if (hasAn8 && !foundAn8) {
            throw new Exception("game should have an 8, but no 8 was found");
        }
    }

    public MinesweeperGame(MinesweeperGame game, int firstClickI, int firstClickJ) throws Exception {
        this(game);
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                grid[i][j].isVisible = false;
            }
        }
        if (grid[firstClickI][firstClickJ].isMine) {
            throw new Exception("first clicked cell shouldn't be a mine");
        }
        if (grid[firstClickI][firstClickJ].numberSurroundingMines != 0) {
            throw new Exception("first clicked cell isn't a zero start");
        }
        revealCell(firstClickI, firstClickJ);
    }

    public static boolean tooManyMinesForZeroStart(int rows, int cols, int numberOfMines) {
        return (numberOfMines > rows * cols - 9);
    }

    public void setHavingAn8() {
        hasAn8 = true;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getNumberOfMines() {
        return numberOfMines;
    }

    public int getNumberOfFlags() {
        return numberOfFlags;
    }

    public boolean isBeforeFirstClick() {
        return firstClick;
    }

    //TODO: find way to now expose Tile which contains isMine field
    //goal: only expose mine locations once game is over
    public Tile getCell(int row, int col) {
        if (ArrayBounds.outOfBounds(row, col, rows, cols)) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return grid[row][col];
    }

    //returns true if the board has changed
    public boolean clickCell(int row, int col, boolean toggleMines) throws Exception {
        revealedAHiddenCell = false;
        if (firstClick && !toggleMines) {
            firstClick = false;
            if (hasAn8) {
                firstClickedCellWith8(row, col);
            } else {
                firstClickedCell(row, col);
            }
            return revealedAHiddenCell;
        }
        if (isGameLost || getIsGameWon()) {
            return revealedAHiddenCell;
        }
        final Tile curr = grid[row][col];
        if (curr.getIsVisible()) {
            checkToRevealAdjacentMines(row, col);
        }
        if (toggleMines) {
            if (!curr.getIsVisible()) {
                if (curr.isFlagged()) {
                    --numberOfFlags;
                } else {
                    ++numberOfFlags;
                }
                curr.toggleFlag();
            }
            return revealedAHiddenCell;
        }
        if (curr.isMine && !curr.isFlagged()) {
            isGameLost = true;
            return revealedAHiddenCell;
        }
        if (curr.isFlagged()) {
            return revealedAHiddenCell;
        }
        revealCell(row, col);
        return revealedAHiddenCell;
    }

    public void shuffleInterestingMinesAndMakeOneAway(int firstClickI, int firstClickJ) throws Exception {
        int interestingMines = 0;
        ArrayList<Pair<Integer, Integer>>
                interestingSpots = new ArrayList<>(),
                freeAwayCells = new ArrayList<>();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (isInterestingCell(i, j) && AwayCell.isNextToAnAwayCell(this, i, j)) {
                    if (grid[i][j].isMine) {
                        ++interestingMines;
                    }
                    interestingSpots.add(new Pair<>(i, j));
                }
                if (AwayCell.isAwayCell(this, i, j) &&
                        !grid[i][j].isMine &&
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
            if (grid[i][j].isMine) {
                ++tempInterestingCount;
                changeMineStatus(i, j, false);
            }
            resetLogicalStuff(i, j);
        }
        if (tempInterestingCount != interestingMines) {
            throw new Exception("sanity check that # interesting mines matches");
        }
        Collections.shuffle(interestingSpots);
        for (int pos = 0; pos < interestingMines - 1; ++pos) {
            final int i = interestingSpots.get(pos).first;
            final int j = interestingSpots.get(pos).second;
            changeMineStatus(i, j, true);
        }
        Collections.shuffle(freeAwayCells);
        int i = freeAwayCells.get(0).first;
        int j = freeAwayCells.get(0).second;
        changeMineStatus(i, j, true);

        resetAllLogicalAndVisibleStuff();
        revealCell(firstClickI, firstClickJ);
    }

    public void updateLogicalStuff(VisibleTile[][] visibleBoard) throws Exception {
        if (visibleBoard.length != rows) {
            throw new Exception("visibleBoard has wrong dimensions");
        }
        for (int i = 0; i < rows; ++i) {
            if (visibleBoard[i].length != cols) {
                throw new Exception("visibleBoard has wrong dimensions");
            }
            for (int j = 0; j < cols; ++j) {
                if (visibleBoard[i][j].isLogicalFree || visibleBoard[i][j].isLogicalMine) {
                    if (grid[i][j].isVisible) {
                        throw new Exception("visible cells can't be logical");
                    }
                }
                if (visibleBoard[i][j].isLogicalFree && visibleBoard[i][j].isLogicalMine) {
                    throw new Exception("cell can't be both logical free and logical mine");
                }
                if (visibleBoard[i][j].isLogicalMine && !grid[i][j].isMine) {
                    throw new Exception("logical mine which isn't a mine");
                }
                if (visibleBoard[i][j].isLogicalFree && grid[i][j].isMine) {
                    throw new Exception("logical free which is a mine");
                }
                grid[i][j].isLogicalFree = visibleBoard[i][j].isLogicalFree;
                grid[i][j].isLogicalMine = visibleBoard[i][j].isLogicalMine;
            }
        }
    }

    public void updateLogicalStuff(VisibleTileWithProbability[][] visibleBoard) throws Exception {
        updateLogicalStuff((VisibleTile[][]) visibleBoard);
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (visibleBoard[i][j].isLogicalMine && !visibleBoard[i][j].mineProbability.equals(1)) {
                    throw new Exception("logical mine with non-1 mine probability " + i + " " + j);
                }
                if (visibleBoard[i][j].isLogicalFree && !visibleBoard[i][j].mineProbability.equals(0)) {
                    throw new Exception("logical free with non-zero mine probability " + i + " " + j);
                }
                grid[i][j].mineProbability.setValue(visibleBoard[i][j].mineProbability);
            }
        }
    }

    public boolean getIsGameLost() {
        return isGameLost;
    }

    //game is won if all free cells are visible
    public boolean getIsGameWon() {
        if (isGameLost) {
            return false;
        }
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                Tile currCell = grid[i][j];
                if (!currCell.isMine() && !currCell.isVisible) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean everyComponentHasLogicalFrees() throws Exception {
        Dsu disjointSet = GetConnectedComponents.getDsuOfComponentsWithKnownMines(grid);
        boolean[] hasLogicalFree = new boolean[rows * cols];
        boolean hasAtLeastOneLogicalFree = false;
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (grid[i][j].isLogicalFree) {
                    hasAtLeastOneLogicalFree = true;
                    hasLogicalFree[disjointSet.find(RowColToIndex.rowColToIndex(i, j, rows, cols))] = true;
                }
            }
        }
        if (!hasAtLeastOneLogicalFree) {
            return false;
        }
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (isInterestingCell(i, j) &&
                        !grid[i][j].isLogicalMine &&
                        !hasLogicalFree[disjointSet.find(RowColToIndex.rowColToIndex(i, j, rows, cols))]) {
                    return false;
                }
            }
        }
        return true;
    }

    public void shuffleAwayMines() throws Exception {
        ArrayList<Pair<Integer, Integer>> awayCells = new ArrayList<>();
        int mineCount = 0;
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (AwayCell.isAwayCell(this, i, j) && notPartOfThe8(i, j)) {
                    awayCells.add(new Pair<>(i, j));
                    if (grid[i][j].isMine) {
                        ++mineCount;
                    }
                }
            }
        }
        if (mineCount == 0 || mineCount == awayCells.size()) {
            throw new Exception("can't shuffle away mines");
        }
        for (Pair<Integer, Integer> cell : awayCells) {
            if (grid[cell.first][cell.second].isMine) {
                changeMineStatus(cell.first, cell.second, false);
            }
        }
        Collections.shuffle(awayCells);
        for (int pos = 0; pos < mineCount; ++pos) {
            final int i = awayCells.get(pos).first;
            final int j = awayCells.get(pos).second;
            changeMineStatus(i, j, true);
        }
    }

    public void setFlagsForHiddenCells(MinesweeperGame game) throws Exception {
        if (game.getRows() != rows || game.getCols() != cols) {
            throw new Exception("board dimensions don't match");
        }
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (grid[i][j].isVisible) {
                    continue;
                }
                grid[i][j].isFlagged = game.grid[i][j].isFlagged;
            }
        }
    }

    //TODO: refactor this - It breaks single responsibility
    public void revealRandomCellIfAllLogicalStuffIsCorrect(boolean solverHitIterationLimit) throws Exception {
        if (isGameLost || getIsGameWon()) {
            return;
        }
        if (firstClick) {
            firstClick = false;
            getHelpRow = MyMath.getRand(0, rows - 1);
            getHelpCol = MyMath.getRand(0, cols - 1);
            firstClickedCell(getHelpRow, getHelpCol);
            return;
        }
        if (!solverHitIterationLimit) {
            for (int i = 0; i < rows; ++i) {
                for (int j = 0; j < cols; ++j) {
                    if (grid[i][j].isLogicalFree || grid[i][j].isLogicalMine != grid[i][j].isFlagged) {
                        isGameLost = true;
                        return;
                    }
                }
            }
        }
        ArrayList<ArrayList<Pair<Integer, Integer>>> freeCells = new ArrayList<>(3);
        for (int i = 0; i < 3; ++i) {
            freeCells.add(new ArrayList<>());
        }
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                final Tile curr = grid[i][j];
                if (!curr.isVisible && !curr.isMine) {
                    if (curr.numberSurroundingMines == 0) {
                        freeCells.get(0).add(new Pair<>(i, j));
                    } else {
                        boolean cellIsNextToANumber = false;
                        for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
                            final int adjI = adj[0], adjJ = adj[1];
                            if (grid[adjI][adjJ].isVisible) {
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

    public int getHelpRow() {
        return getHelpRow;
    }

    public int getHelpCol() {
        return getHelpCol;
    }

    public void checkCorrectnessOfSolverOutput(VisibleTile[][] SolverBoard) throws Exception {
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (SolverBoard[i][j].getIsVisible()) {
                    if (SolverBoard[i][j].getIsLogicalMine() || SolverBoard[i][j].getIsLogicalFree()) {
                        throw new Exception("visible tiles can't be logical");
                    }
                }
                if (SolverBoard[i][j].getIsLogicalFree() && SolverBoard[i][j].getIsLogicalMine()) {
                    throw new Exception("can't be both logical free and logical mine");
                }
                if (SolverBoard[i][j].getIsLogicalMine() && !grid[i][j].isMine) {
                    throw new Exception("found a logical mine which is free");
                }
                if (SolverBoard[i][j].getIsLogicalFree()) {
                    if (grid[i][j].isMine) {
                        throw new Exception("found a logical free which is mine");
                    }
                }
            }
        }
    }

    //***private members***
    private void checkToRevealAdjacentMines(int row, int col) throws Exception {
        boolean revealSurroundingCells = true;
        for (int[] adjCells : GetAdjacentCells.getAdjacentCells(row, col, rows, cols)) {
            Tile adj = grid[adjCells[0]][adjCells[1]];
            if (adj.getIsVisible()) {
                continue;
            }
            if (adj.isFlagged() && !adj.isMine) {
                isGameLost = true;
                return;
            }
            if (adj.isFlagged() != adj.isMine) {
                revealSurroundingCells = false;
            }
        }
        if (!revealSurroundingCells) {
            return;
        }
        for (int[] adjCells : GetAdjacentCells.getAdjacentCells(row, col, rows, cols)) {
            final int adjI = adjCells[0], adjJ = adjCells[1];
            Tile adj = grid[adjI][adjJ];
            if (adj.isMine) {
                continue;
            }
            revealCell(adjI, adjJ);
        }
    }

    private void firstClickedCell(int row, int col) throws Exception {
        ArrayList<Pair<Integer, Integer>> spots = new ArrayList<>();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (Math.abs(row - i) <= 1 && Math.abs(col - j) <= 1) {
                    continue;
                }
                spots.add(new Pair<>(i, j));
            }
        }

        if (spots.size() < numberOfMines) {
            throw new Exception("too many mines to have a zero start");
        }

        Collections.shuffle(spots);

        for (int pos = 0; pos < numberOfMines; ++pos) {
            final int mineRow = spots.get(pos).first;
            final int mineCol = spots.get(pos).second;
            changeMineStatus(mineRow, mineCol, true);
        }
        if (grid[row][col].isMine) {
            throw new Exception("starting click shouldn't be a mine");
        }
        revealCell(row, col);
    }

    private void firstClickedCellWith8(int row, int col) throws Exception {
        ArrayList<Pair<Integer, Integer>> spots = new ArrayList<>();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (Math.abs(row - i) <= 1 && Math.abs(col - j) <= 1) {
                    continue;
                }
                spots.add(new Pair<>(i, j));
            }
        }

        if (spots.size() < numberOfMines) {
            throw new Exception("too many mines to have a zero start");
        }

        if (numberOfMines < 8) {
            throw new Exception("too few mines for an 8");
        }

        Collections.shuffle(spots);

        for (int pos = 0; pos < spots.size(); ++pos) {
            final int i = spots.get(pos).first;
            final int j = spots.get(pos).second;
            if (i == 0 || j == 0 || i == rows - 1 || j == cols - 1) {
                continue;
            }
            rowWith8 = i;
            colWith8 = j;
            for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
                final int adjI = adj[0], adjJ = adj[1];
                changeMineStatus(adjI, adjJ, true);
            }
            break;
        }
        if (rowWith8 == -1 || colWith8 == -1) {
            throw new Exception("didn't find a spot for an 8, but there should be one");
        }

        spots.clear();
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
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

        if (spots.size() < numberOfMines - 8) {
            throw new Exception("too many mines to have a zero start with an 8");
        }

        for (int pos = 0; pos < numberOfMines - 8; ++pos) {
            final int i = spots.get(pos).first;
            final int j = spots.get(pos).second;
            changeMineStatus(i, j, true);
        }
        if (grid[row][col].isMine) {
            throw new Exception("starting click shouldn't be a mine");
        }
        revealCell(row, col);
    }

    private void revealCell(int row, int col) throws Exception {
        Tile curr = grid[row][col];
        if (curr.isMine) {
            throw new Exception("can't reveal a mine");
        }
        if (curr.isLogicalMine) {
            throw new Exception("can't reveal a logical mine");
        }
        if (curr.isFlagged()) {
            --numberOfFlags;
        }
        curr.revealTile();
        if (curr.numberSurroundingMines > 0) {
            return;
        }
        for (int[] adj : GetAdjacentCells.getAdjacentCells(row, col, rows, cols)) {
            final int adjRow = adj[0];
            final int adjCol = adj[1];
            Tile adjacent = grid[adjRow][adjCol];
            if (!adjacent.getIsVisible()) {
                revealCell(adjRow, adjCol);
            }
        }
    }

    private boolean notPartOfThe8(int i, int j) {
        return !(hasAn8 && Math.abs(i - rowWith8) <= 1 && Math.abs(j - colWith8) <= 1);
    }

    private boolean isInterestingCell(int i, int j) {
        if (grid[i][j].isVisible) {
            return false;
        }
        if (AwayCell.isAwayCell(this, i, j)) {
            return false;
        }
        return notPartOfThe8(i, j);
    }

    private void resetLogicalStuff(int i, int j) throws Exception {
        grid[i][j].isLogicalFree = false;
        grid[i][j].isLogicalMine = false;
        grid[i][j].mineProbability.setValues(0, 1);
    }

    private void changeMineStatus(int i, int j, boolean isMine) throws Exception {
        grid[i][j].resetLogicalStuffAndVisibility();
        for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
            grid[adj[0]][adj[1]].resetLogicalStuffAndVisibility();
        }

        if (grid[i][j].isMine == isMine) {
            return;
        }

        grid[i][j].isMine = isMine;
        for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
            final int adjI = adj[0], adjJ = adj[1];
            if (isMine) {
                grid[adjI][adjJ].numberSurroundingMines++;
            } else {
                grid[adjI][adjJ].numberSurroundingMines--;
            }
        }
    }

    private void resetAllLogicalAndVisibleStuff() throws Exception {
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                grid[i][j].resetLogicalStuffAndVisibility();
            }
        }
    }

    public class Tile extends VisibleTileWithProbability {
        private boolean isFlagged, isMine;

        private Tile() {
            super();
            isFlagged = isMine = false;
        }

        //copy constructor
        private Tile(Tile other) {
            super(other);
            isMine = other.isMine;
            isFlagged = other.isFlagged;
        }

        public boolean isMine() {
            return isMine;
        }

        public boolean isFlagged() {
            if (isVisible) {
                isFlagged = false;
            }
            return isFlagged;
        }

        private void resetLogicalStuffAndVisibility() throws Exception {
            isVisible = isLogicalMine = isLogicalFree = false;
            mineProbability.setValues(0, 1);
        }

        private void revealTile() throws Exception {
            if (!isVisible) {
                MinesweeperGame.this.revealedAHiddenCell = true;
            }
            isVisible = true;
            isFlagged = isLogicalFree = false;
            mineProbability.setValues(0, 1);
            if (isMine) {
                throw new Exception("can't reveal a mine");
            }
            if (isLogicalMine) {
                throw new Exception("can't reveal a logical mine");
            }
        }

        private void toggleFlag() {
            if (isVisible) {
                isFlagged = false;
                return;
            }
            isFlagged = !isFlagged;
        }
    }
}
