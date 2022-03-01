package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.Board;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AllCellsAreHidden;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper_android.miscHelpers.BigFraction;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.CutNodes;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.EdgePair;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetConnectedComponents;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetSubComponentByRemovedNodes;
import com.LukeVideckis.minesweeper_android.miscHelpers.MutableInt;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.RowColToIndex;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.interfaces.SolverStartingWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileNoFlagsForSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithLogistics;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.tiles.TileWithProbability;
import com.LukeVideckis.minesweeper_android.miscHelpers.ComparablePair;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

//TODO: also break out early the moment we find a (conditioned) solution
//implementation of the intense recursive alg described in my pdf
public class IntenseRecursiveSolver implements SolverStartingWithLogistics {

    public final static int iterationLimit = 10000;
    //TODO: play around with this number
    private static final int maxNumberOfRemovedCells = 6;
    private final int rows, cols;
    private final boolean[][] isMine;
    private final int[][] cntSurroundingMines, updatedNumberSurroundingMines;
    private final ArrayList<ArrayList<ArrayList<Pair<Integer, Integer>>>> lastUnvisitedSpot;
    private final ArrayList<TreeMap<Integer, MutableInt>> mineConfig = new ArrayList<>();
    private final ArrayList<TreeMap<Integer, ArrayList<MutableInt>>> mineProbPerCompPerNumMines = new ArrayList<>();
    private final ArrayList<TreeMap<Integer, TreeMap<Integer, BigFraction>>> numberOfConfigsForCurrent = new ArrayList<>();
    private int numberOfMines;
    private Board<TileWithLogistics> board;
    private ArrayList<ArrayList<Pair<Integer, Integer>>> components;
    private ArrayList<ArrayList<SortedSet<Integer>>> adjList;

    public IntenseRecursiveSolver(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        isMine = new boolean[rows][cols];
        cntSurroundingMines = new int[rows][cols];
        updatedNumberSurroundingMines = new int[rows][cols];
        lastUnvisitedSpot = new ArrayList<>(rows);
        for (int i = 0; i < rows; ++i) {
            ArrayList<ArrayList<Pair<Integer, Integer>>> currRow = new ArrayList<>(cols);
            for (int j = 0; j < cols; ++j) {
                ArrayList<Pair<Integer, Integer>> currSpot = new ArrayList<>();
                currRow.add(currSpot);
            }
            lastUnvisitedSpot.add(currRow);
        }
    }

    @Override
    public Board<TileWithProbability> solvePositionWithLogistics(Board<TileWithLogistics> board) throws Exception {
        //always allocate new board to avoid any potential issues with shallow copies between solver runs
        TileWithProbability[][] tmpBoard = new TileWithProbability[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                tmpBoard[i][j] = new TileWithProbability();
            }
        }
        Board<TileWithProbability> boardWithProbability = new Board<>(tmpBoard, board.getMines());
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                TileWithProbability curr = boardWithProbability.getCell(i, j);
                curr.set(board.getCell(i, j));
                curr.mineProbability = new BigFraction(0);
            }
        }

        numberOfMines = board.getMines();
        if (AllCellsAreHidden.allCellsAreHidden(new Board<>(board.getGrid(), board.getMines()))) {
            for (int i = 0; i < rows; ++i) {
                for (int j = 0; j < cols; ++j) {
                    boardWithProbability.getCell(i, j).mineProbability.setValues(numberOfMines, rows * cols);
                    if (numberOfMines >= rows * cols) {
                        throw new Exception("too many mines, but this shouldn't happen");
                    }
                }
            }
            return boardWithProbability;
        }

        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (board.getCell(i, j).isVisible && (board.getCell(i, j).isLogicalMine || board.getCell(i, j).isLogicalFree)) {
                    throw new Exception("visible cells can't be logical frees/mines");
                }
                if (board.getCell(i, j).isLogicalMine && board.getCell(i, j).isLogicalFree) {
                    throw new Exception("cell can't be both logical free and logical mine");
                }
                if (board.getCell(i, j).isLogicalMine) {
                    if (!AwayCell.isAwayCellSolver(new Board<>(board.getGrid(), board.getMines()), i, j)) {
                        --numberOfMines;
                    }
                    boardWithProbability.getCell(i, j).mineProbability.setValues(1, 1);
                } else if (board.getCell(i, j).isLogicalFree) {
                    boardWithProbability.getCell(i, j).mineProbability.setValues(0, 1);
                } else {
                    boardWithProbability.getCell(i, j).mineProbability.setValues(0, 1);
                }
                if (board.getCell(i, j).isVisible) {
                    updatedNumberSurroundingMines[i][j] = board.getCell(i, j).numberSurroundingMines;
                    for (TileWithLogistics adjCell : board.getAdjacentCells(i, j)) {
                        if (adjCell.isLogicalMine) {
                            --updatedNumberSurroundingMines[i][j];
                        }
                    }
                }
            }
        }

        initialize(board, numberOfMines/*intentionally not passed in #-of-mines*/);

        Pair<ArrayList<ArrayList<Pair<Integer, Integer>>>, ArrayList<ArrayList<SortedSet<Integer>>>> result = GetConnectedComponents.getComponentsWithKnownCells(new Board<>(board.getGrid(), board.getMines()));
        components = result.first;
        adjList = result.second;
        if (components.size() != adjList.size()) {
            throw new Exception("components size doesn't match adjList size");
        }
        for (int i = 0; i < components.size(); ++i) {
            if (components.get(i).size() != adjList.get(i).size()) {
                throw new Exception("components[i] size doesn't match adjList[i] size");
            }
        }
        initializeLastUnvisitedSpot(components);

        findMineProbAndNumConfigsForEachComponent();

        final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(new Board<>(board.getGrid(), board.getMines()));

        removeMineNumbersFromComponent();
        BigFraction awayMineProbability = null;
        if (numberOfAwayCells > 0) {
            awayMineProbability = calculateAwayMineProbability();
        }
        updateNumberOfConfigsForCurrent();


        TreeMap<Integer, BigFraction> configsPerMineCount = calculateNumberOfMineConfigs();

        for (int i = 0; i < components.size(); ++i) {
            for (TreeMap.Entry<Integer, ArrayList<MutableInt>> entry : mineProbPerCompPerNumMines.get(i).entrySet()) {
                final int mines = entry.getKey();
                final ArrayList<MutableInt> mineProbPerSpot = entry.getValue();

                TreeMap<Integer, BigFraction> configsPerMine = numberOfConfigsForCurrent.get(i).get(mines);
                BigFraction currWeight = new BigFraction(0);
                for (TreeMap.Entry<Integer, BigFraction> currEntry : Objects.requireNonNull(configsPerMine).entrySet()) {
                    BigFraction currTerm = new BigFraction(0);
                    for (TreeMap.Entry<Integer, BigFraction> total : configsPerMineCount.entrySet()) {
                        BigFraction delta = MyMath.BinomialCoefficientFraction(numberOfAwayCells, numberOfMines - total.getKey(), numberOfMines - currEntry.getKey());
                        delta.multiplyWith(total.getValue());

                        currTerm.addWith(delta);
                    }

                    currTerm.invert();
                    currTerm.multiplyWith(currEntry.getValue());

                    currWeight.addWith(currTerm);
                }

                for (int j = 0; j < components.get(i).size(); ++j) {
                    final int numerator = mineProbPerSpot.get(j).get();
                    final int row = components.get(i).get(j).first;
                    final int col = components.get(i).get(j).second;

                    BigFraction delta = new BigFraction(numerator);
                    delta.multiplyWith(currWeight);
                    boardWithProbability.getCell(row, col).mineProbability.addWith(delta);
                }
            }
        }

        // set probabilities for away cells
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                if (AwayCell.isAwayCellSolver(new Board<>(board.getGrid(), board.getMines()), i, j)) {
                    if (awayMineProbability == null) {
                        throw new Exception("away probability is null, but this was checked above");
                    }
                    boardWithProbability.getCell(i, j).mineProbability.setValue(awayMineProbability);
                }
            }
        }

        //just error checking below
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                TileWithLogistics boardCell = board.getCell(i, j);
                TileWithProbability probCell = boardWithProbability.getCell(i, j);
                if (boardCell.isVisible && (boardCell.isLogicalMine || boardCell.isLogicalFree)) {
                    throw new Exception("visible cells shouldn't be logical");
                }
                if (boardCell.isVisible && !probCell.mineProbability.equals(0)) {
                    throw new Exception("found a visible cell with non-zero mine probability: " + i + " " + j);
                }
                if (boardCell.isLogicalMine) {
                    if (!probCell.mineProbability.equals(1)) {
                        throw new Exception("found logical mine with mine probability != 1: " + i + " " + j);
                    }
                }
                if (boardCell.isLogicalFree) {
                    if (!probCell.mineProbability.equals(0)) {
                        throw new Exception("found logical free cell with mine probability != 0: " + i + " " + j);
                    }
                }
            }
        }

        return boardWithProbability;
    }

    //for each component, and for each # mines for that component: this calculates the number of mine configurations, and saves it in numberOfConfigsForCurrent
    private void updateNumberOfConfigsForCurrent() throws Exception {

        ArrayList<TreeMap<Integer, BigFraction>> prefix = new ArrayList<>(components.size() + 1);
        for (int i = 0; i <= components.size(); ++i) {
            prefix.add(new TreeMap<>());
        }
        prefix.get(0).put(0, new BigFraction(1));
        for (int i = 0; i < components.size(); ++i) {
            for (TreeMap.Entry<Integer, MutableInt> mineVal : mineConfig.get(i).entrySet()) {
                for (TreeMap.Entry<Integer, BigFraction> waysVal : prefix.get(i).entrySet()) {
                    final int nextKey = mineVal.getKey() + waysVal.getKey();
                    BigFraction nextValueDiff = new BigFraction(mineVal.getValue().get());
                    nextValueDiff.multiplyWith(waysVal.getValue());
                    BigFraction nextVal = prefix.get(i + 1).get(nextKey);
                    if (nextVal == null) {
                        prefix.get(i + 1).put(nextKey, nextValueDiff);
                    } else {
                        nextVal.addWith(nextValueDiff);
                    }
                }
            }
        }

        ArrayList<TreeMap<Integer, BigFraction>> suffix = new ArrayList<>(components.size() + 1);
        for (int i = 0; i <= components.size(); ++i) {
            suffix.add(new TreeMap<>());
        }
        suffix.get(components.size()).put(0, new BigFraction(1));
        for (int i = components.size() - 1; i >= 0; --i) {
            for (TreeMap.Entry<Integer, MutableInt> mineVal : mineConfig.get(i).entrySet()) {
                for (TreeMap.Entry<Integer, BigFraction> waysVal : suffix.get(i + 1).entrySet()) {
                    final int nextKey = mineVal.getKey() + waysVal.getKey();
                    BigFraction nextValueDiff = new BigFraction(mineVal.getValue().get());
                    nextValueDiff.multiplyWith(waysVal.getValue());
                    BigFraction nextVal = suffix.get(i).get(nextKey);
                    if (nextVal == null) {
                        suffix.get(i).put(nextKey, nextValueDiff);
                    } else {
                        nextVal.addWith(nextValueDiff);
                    }
                }
            }
        }

        final int numberAwayCells = AwayCell.getNumberOfAwayCells(new Board<>(board.getGrid(), board.getMines()));
        for (int i = 0; i < components.size(); ++i) {
            if (!numberOfConfigsForCurrent.get(i).isEmpty()) {
                throw new Exception("numberOfConfigsForCurrent should be cleared from previous run, but isn't");
            }
            for (TreeMap.Entry<Integer, MutableInt> waysCurr : mineConfig.get(i).entrySet()) {
                numberOfConfigsForCurrent.get(i).put(waysCurr.getKey(), new TreeMap<>());
                for (TreeMap.Entry<Integer, BigFraction> waysPrefix : prefix.get(i).entrySet()) {
                    for (TreeMap.Entry<Integer, BigFraction> waysSuffix : suffix.get(i + 1).entrySet()) {
                        final int currKey = waysCurr.getKey() + waysPrefix.getKey() + waysSuffix.getKey();
                        if (currKey > numberOfMines || currKey < numberOfMines - numberAwayCells) {
                            continue;
                        }
                        BigFraction currVal = new BigFraction(1);
                        currVal.multiplyWith(waysPrefix.getValue());
                        currVal.multiplyWith(waysSuffix.getValue());
                        BigFraction curr = Objects.requireNonNull(numberOfConfigsForCurrent.get(i).get(waysCurr.getKey())).get(currKey);
                        if (curr == null) {
                            Objects.requireNonNull(numberOfConfigsForCurrent.get(i).get(waysCurr.getKey())).put(currKey, currVal);
                        } else {
                            curr.addWith(currVal);
                        }
                    }
                }
            }
        }
    }

    private void removeMineNumbersFromComponent() throws Exception {
        ArrayList<TreeSet<Integer>> dpTable = new ArrayList<>(components.size() + 1);
        for (int i = 0; i <= components.size(); ++i) {
            dpTable.add(new TreeSet<>());
        }

        dpTable.get(0).add(0);
        for (int i = 0; i < components.size(); ++i) {
            for (int entry : mineConfig.get(i).keySet()) {
                for (int val : dpTable.get(i)) {
                    dpTable.get(i + 1).add(val + entry);
                }
            }
        }
        TreeSet<Integer> validSpots = new TreeSet<>();
        final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(new Board<>(board.getGrid(), board.getMines()));
        for (int mineCnt : dpTable.get(components.size())) {
            if (mineCnt <= numberOfMines && numberOfMines <= mineCnt + numberOfAwayCells) {
                validSpots.add(mineCnt);
            }
        }
        dpTable.get(components.size()).clear();
        dpTable.set(components.size(), validSpots);

        for (int i = components.size() - 1; i >= 0; --i) {
            TreeSet<Integer> spotsToRemove = new TreeSet<>();
            for (int entry : mineConfig.get(i).keySet()) {
                boolean found = false;
                for (int val : dpTable.get(i)) {
                    if (dpTable.get(i + 1).contains(val + entry)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    spotsToRemove.add(entry);
                }
            }
            for (int val : spotsToRemove) {
                mineConfig.get(i).remove(val);
                mineProbPerCompPerNumMines.get(i).remove(val);
            }

            spotsToRemove.clear();
            for (int val : dpTable.get(i)) {
                boolean found = false;
                for (int entry : mineConfig.get(i).keySet()) {
                    if (dpTable.get(i + 1).contains(val + entry)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    spotsToRemove.add(val);
                }
            }
            for (int val : spotsToRemove) {
                dpTable.get(i).remove(val);
            }
        }
    }

    private BigFraction calculateAwayMineProbability() throws Exception {
        final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(new Board<>(board.getGrid(), board.getMines()));
        TreeMap<Integer, BigFraction> configsPerMineCount = calculateNumberOfMineConfigs();
        BigFraction awayMineProbability = new BigFraction(0);
        for (TreeMap.Entry<Integer, BigFraction> entry : configsPerMineCount.entrySet()) {
            if (numberOfMines - entry.getKey() < 0 || numberOfMines - entry.getKey() > numberOfAwayCells) {
                throw new Exception("number of remaining mines is more than number of away cells (or negative)");
            }

            //calculate # configs / # total configs - the probability that the configuration of mines has the current specific # of mines
            BigFraction numberOfConfigs = new BigFraction(0);
            for (TreeMap.Entry<Integer, BigFraction> val : configsPerMineCount.entrySet()) {
                BigFraction currDelta = MyMath.BinomialCoefficientFraction(numberOfAwayCells, numberOfMines - val.getKey(), numberOfMines - entry.getKey());
                currDelta.multiplyWith(val.getValue());
                numberOfConfigs.addWith(currDelta);
            }
            numberOfConfigs.invert();
            numberOfConfigs.multiplyWith(entry.getValue());

            //actual probability that a single away cell is a mine, the above is just a weight - "how often is this probability the case - # configs / # total configs"
            numberOfConfigs.multiplyWith(numberOfMines - entry.getKey(), numberOfAwayCells);

            awayMineProbability.addWith(numberOfConfigs);
        }
        return awayMineProbability;
    }

    private TreeMap<Integer, BigFraction> calculateNumberOfMineConfigs() throws Exception {
        final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(new Board<>(board.getGrid(), board.getMines()));
        TreeMap<Integer, BigFraction> prevWays = new TreeMap<>(), newWays = new TreeMap<>();
        prevWays.put(0, new BigFraction(1));
        for (int i = 0; i < components.size(); ++i) {
            for (TreeMap.Entry<Integer, MutableInt> mineVal : mineConfig.get(i).entrySet()) {
                for (TreeMap.Entry<Integer, BigFraction> waysVal : prevWays.entrySet()) {
                    final int nextKey = mineVal.getKey() + waysVal.getKey();
                    BigFraction nextValueDiff = new BigFraction(mineVal.getValue().get());
                    nextValueDiff.multiplyWith(waysVal.getValue());
                    if (i + 1 == components.size() && (nextKey > numberOfMines || nextKey + numberOfAwayCells < numberOfMines)) {
                        continue;
                    }
                    BigFraction nextVal = newWays.get(nextKey);
                    if (nextVal == null) {
                        newWays.put(nextKey, nextValueDiff);
                    } else {
                        nextVal.addWith(nextValueDiff);
                    }
                }
            }
            prevWays.clear();
            prevWays.putAll(newWays);
            newWays.clear();
        }
        return prevWays;
    }

    private void initialize(Board<TileWithLogistics> board, int numberOfMines) throws Exception {
        this.board = board;
        this.numberOfMines = numberOfMines;
        if (rows != board.getRows() || cols != board.getCols()) {
            throw new Exception("dimensions of board doesn't match what was passed in the constructor");
        }
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                isMine[i][j] = false;
                cntSurroundingMines[i][j] = 0;
            }
        }
    }

    private void initializeLastUnvisitedSpot(ArrayList<ArrayList<Pair<Integer, Integer>>> components) throws Exception {
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                lastUnvisitedSpot.get(i).get(j).clear();
            }
        }
        mineConfig.clear();
        numberOfConfigsForCurrent.clear();
        mineProbPerCompPerNumMines.clear();
        for (ArrayList<Pair<Integer, Integer>> component : components) {
            mineConfig.add(new TreeMap<>());
            numberOfConfigsForCurrent.add(new TreeMap<>());
            mineProbPerCompPerNumMines.add(new TreeMap<>());
            for (Pair<Integer, Integer> spot : component) {
                for (int[] adj : board.getAdjacentIndexes(spot.first, spot.second)) {
                    final int adjI = adj[0], adjJ = adj[1];
                    if (board.getCell(adjI, adjJ).isVisible) {
                        lastUnvisitedSpot.get(adjI).get(adjJ).add(spot);
                    }
                }
            }
        }
    }

    private void findMineProbAndNumConfigsForEachComponent() throws Exception {
        for (int i = 0; i < components.size(); ++i) {
            TreeSet<Integer> subComponent = new TreeSet<>();
            for (int j = 0; j < components.get(i).size(); ++j) {
                subComponent.add(j);
            }
            boolean[] isRemoved = new boolean[components.get(i).size()];
            ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>> result =
                    solveComponent(i, Collections.unmodifiableSortedSet(subComponent), isRemoved);

            if (Objects.requireNonNull(result).size() != 1) {
                throw new Exception("result has size != 1, but it should be 1, size is: " + Objects.requireNonNull(result).size());
            }
            if (!mineConfig.get(i).isEmpty()) {
                throw new Exception("mine config isn't empty, but it should be, component: " + i);
            }
            if (!mineProbPerCompPerNumMines.get(i).isEmpty()) {
                throw new Exception("mine probabilities isn't empty, but it should be, component: " + i);
            }
            mineConfig.get(i).putAll(result.get(0).first);
            mineProbPerCompPerNumMines.get(i).putAll(result.get(0).second);
        }
    }

    private ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>> solveComponent(
            final int componentPos,
            final SortedSet<Integer> subComponent, //list of indexes into components[componentPos]
            boolean[] isRemoved //also list of indexes into components[componentPos]
    ) throws Exception {
        ArrayList<Integer> removedCellsList = new ArrayList<>();
        TreeMap<Integer, Integer> toIndexOriginal = new TreeMap<>();
        int pos = 0;
        for (int node : subComponent) {
            if (isRemoved[node]) {
                removedCellsList.add(node);
                toIndexOriginal.put(node, pos);
                ++pos;
            }
        }
        final int startNumRemoved = removedCellsList.size();
        if (startNumRemoved > maxNumberOfRemovedCells) {
            throw new Exception("starting with too many removed cells " + startNumRemoved);
        }
        if (subComponent.size() <= 8) {//TODO: play around with this number
            return solveComponentWithBacktracking(componentPos, subComponent, toIndexOriginal);
        }

        //find split cells
        //1st try: find articulation nodes
        TreeSet<Integer> allCutNodes = CutNodes.getCutNodes(subComponent, adjList.get(componentPos), isRemoved);
        for (int node : allCutNodes) {
            if (removedCellsList.size() < maxNumberOfRemovedCells) {
                removedCellsList.add(node);
                if (isRemoved[node]) {
                    throw new Exception("node is already removed, but this shouldn't happen");
                }
                isRemoved[node] = true;
            }
        }

        //TODO: these
        //2nd try: find pairs of articulation nodes

        //3rd try: find pairs of edges - only once there are no cut nodes, if there's >= 1 cut node, then we'll recurse again
        if (allCutNodes.isEmpty()) {
            Pair<ComparablePair, ComparablePair> edgePair = EdgePair.getPairOfEdges(subComponent, componentPos, isRemoved, adjList);
            TreeSet<Integer> uniqueNodes = new TreeSet<>();
            if (edgePair != null) {
                uniqueNodes.add(edgePair.first.first);
                uniqueNodes.add(edgePair.first.second);
                uniqueNodes.add(edgePair.second.first);
                uniqueNodes.add(edgePair.second.second);
                if (removedCellsList.size() + uniqueNodes.size() <= maxNumberOfRemovedCells) {
                    for (int node : uniqueNodes) {
                        removedCellsList.add(node);
                        if (isRemoved[node]) {
                            throw new Exception("node is already removed, but this shouldn't happen");
                        }
                        isRemoved[node] = true;
                    }
                }
            }
        }
        //4th try: approximation algorithm

        ArrayList<SortedSet<Integer>> newSubComponents = GetSubComponentByRemovedNodes.getSubComponentByRemovedNodes(subComponent, adjList.get(componentPos), isRemoved);

        if (newSubComponents.size() <= 1) {
            for (int i = startNumRemoved; i < removedCellsList.size(); ++i) {
                isRemoved[removedCellsList.get(i)] = false;
            }
            return solveComponentWithBacktracking(componentPos, subComponent, toIndexOriginal);
        }

        //recursing on new sub components
        ArrayList<ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>>> resultsSub = new ArrayList<>(newSubComponents.size());
        for (SortedSet<Integer> currSubComponent : newSubComponents) {
            resultsSub.add(solveComponent(componentPos, currSubComponent, isRemoved));
        }

        ArrayList<Integer> removedCellsListSorted = new ArrayList<>(removedCellsList);
        Collections.sort(removedCellsListSorted);
        ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>> result =
                combineResultsFromRecursing(subComponent, componentPos, newSubComponents, toIndexOriginal, isRemoved, startNumRemoved, removedCellsListSorted, resultsSub);

        //restore isRemoved to was it was at the beginning of the recursive call
        for (int i = startNumRemoved; i < removedCellsList.size(); ++i) {
            isRemoved[removedCellsList.get(i)] = false;
        }

        return result;
    }


    private ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>> combineResultsFromRecursing(
            SortedSet<Integer> subComponent,
            final int componentPos,
            ArrayList<SortedSet<Integer>> newSubComponents,
            TreeMap<Integer, Integer> toIndexOriginal,
            boolean[] isRemoved,
            final int startNumRemoved,
            ArrayList<Integer> removedCellsListSorted,
            ArrayList<ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>>> resultsSub
    ) throws Exception {
        TreeMap<Integer, Integer> toIndexOriginalAfterAddingNewRemoved = new TreeMap<>();
        int pos = 0;
        for (int node : subComponent) {
            if (isRemoved[node]) {
                toIndexOriginalAfterAddingNewRemoved.put(node, pos);
                ++pos;
            }
        }

        TreeMap<Integer, Integer> gridToNode = new TreeMap<>();
        TreeMap<Integer, Integer> nodeToIndex = new TreeMap<>();
        pos = 0;
        for (int node : subComponent) {
            nodeToIndex.put(node, pos);
            final int i = components.get(componentPos).get(node).first;
            final int j = components.get(componentPos).get(node).second;
            gridToNode.put(RowColToIndex.rowColToIndex(i, j, rows, cols), node);
            ++pos;
        }

        TreeSet<Integer> cluesWithAllRemovedNeighbors = new TreeSet<>();
        for (int node : subComponent) {
            if (!isRemoved[node]) {
                continue;
            }
            final int i = components.get(componentPos).get(node).first;
            final int j = components.get(componentPos).get(node).second;
            for (int[] adj : board.getAdjacentIndexes(i, j)) {
                if (!board.getCell(adj[0], adj[1]).isVisible) {
                    continue;
                }
                boolean allNeighborsAreRemoved = true;
                for (int[] adj2 : board.getAdjacentIndexes(adj[0], adj[1])) {
                    final int adjI = adj2[0], adjJ = adj2[1];
                    final int currKey = RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols);
                    if (board.getCell(adjI, adjJ).isVisible || board.getCell(adjI, adjJ).isLogicalMine || board.getCell(adjI, adjJ).isLogicalFree) {
                        continue;
                    }
                    if (!gridToNode.containsKey(currKey) || !isRemoved[Objects.requireNonNull(gridToNode.get(currKey))]) {
                        allNeighborsAreRemoved = false;
                        break;
                    }
                }
                if (allNeighborsAreRemoved) {
                    cluesWithAllRemovedNeighbors.add(RowColToIndex.rowColToIndex(adj[0], adj[1], rows, cols));
                }
            }
        }

        ArrayList<TreeMap<Integer, Integer>> toIndex = new ArrayList<>(newSubComponents.size());
        for (int subC = 0; subC < newSubComponents.size(); ++subC) {
            TreeMap<Integer, Integer> currIndex = new TreeMap<>();
            pos = 0;
            for (int node : newSubComponents.get(subC)) {
                if (isRemoved[node]) {
                    currIndex.put(node, pos);
                    ++pos;
                }
            }
            toIndex.add(currIndex);
        }

        ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>> result = new ArrayList<>(1 << startNumRemoved);
        for (int i = 0; i < (1 << startNumRemoved); ++i) {
            result.add(new Pair<>(new TreeMap<>(), new TreeMap<>()));
        }

        //combine results
        final int pow2 = (1 << removedCellsListSorted.size());
        for (int mask = 0; mask < pow2; ++mask) {//<32
            /*
             * there could be some clues which are next to **only** removed cells
             * for these clues, we need to check that they are satisfied
             */
            boolean allCluesMatch = true;
            for (int cluesWithAllRemoved : cluesWithAllRemovedNeighbors) {
                final int i = RowColToIndex.indexToRowCol(cluesWithAllRemoved, rows, cols).first;
                final int j = RowColToIndex.indexToRowCol(cluesWithAllRemoved, rows, cols).second;
                if (!board.getCell(i, j).isVisible) {
                    throw new Exception("clue which is not visible");
                }
                int surroundingMineCount = 0;
                for (int[] adj : board.getAdjacentIndexes(i, j)) {
                    final int adjI = adj[0], adjJ = adj[1];
                    if (!gridToNode.containsKey(RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols))) {
                        continue;
                    }
                    if (board.getCell(adjI, adjJ).isVisible || board.getCell(adjI, adjJ).isLogicalMine || board.getCell(adjI, adjJ).isLogicalFree) {
                        throw new Exception("node in component which is either visible, or logical");
                    }
                    final int currNode = Objects.requireNonNull(gridToNode.get(RowColToIndex.rowColToIndex(adjI, adjJ, rows, cols)));
                    if (!isRemoved[currNode]) {
                        throw new Exception("node should be removed, but it isn't");
                    }
                    if ((mask & (1 << Objects.requireNonNull(toIndexOriginalAfterAddingNewRemoved.get(currNode)))) > 0) {
                        ++surroundingMineCount;
                    }
                }
                if (surroundingMineCount != updatedNumberSurroundingMines[i][j]) {
                    allCluesMatch = false;
                    break;
                }
            }
            if (!allCluesMatch) {
                continue;
            }

            /*
             * For mines in removed cells, we only want to count then once in the total # of mines
             */
            int cntToAdjustForDuplicates = 0;
            for (int bit = 0; bit < removedCellsListSorted.size(); ++bit) {
                if ((mask & (1 << bit)) == 0) {
                    continue;
                }
                final int node = removedCellsListSorted.get(bit);
                int cntSubContains = 0;
                for (SortedSet<Integer> currSubComponent : newSubComponents) {
                    if (currSubComponent.contains(node)) {
                        ++cntSubContains;
                    }
                }
                if (cntSubContains == 0) {
                    ++cntToAdjustForDuplicates;
                }
                cntToAdjustForDuplicates -= Math.max(0, cntSubContains - 1);
            }

            ArrayList<TreeMap<Integer, MutableInt>> prefixMineConfigs = new ArrayList<>(newSubComponents.size() + 1);
            for (int i = 0; i <= newSubComponents.size(); ++i) {
                prefixMineConfigs.add(new TreeMap<>());
            }
            prefixMineConfigs.get(0).put(0, new MutableInt(1));

            ArrayList<Integer> maskSubC = new ArrayList<>(newSubComponents.size());
            for (int subC = 0; subC < newSubComponents.size(); ++subC) {
                int currMaskSubC = 0;
                for (int bit = 0; bit < removedCellsListSorted.size(); ++bit) {
                    if ((mask & (1 << bit)) == 0) {
                        continue;
                    }
                    if (!toIndex.get(subC).containsKey(removedCellsListSorted.get(bit))) {
                        continue;
                    }
                    final int currIndex = Objects.requireNonNull(toIndex.get(subC).get(removedCellsListSorted.get(bit)));
                    currMaskSubC = (currMaskSubC | (1 << currIndex));
                }
                maskSubC.add(currMaskSubC);
                final TreeMap<Integer, MutableInt> currSubMineConfig = resultsSub.get(subC).get(maskSubC.get(subC)).first;

                //update new mine configs
                for (TreeMap.Entry<Integer, MutableInt> prevMineConfigs : prefixMineConfigs.get(subC).entrySet()) {
                    for (TreeMap.Entry<Integer, MutableInt> currMineConfigs : currSubMineConfig.entrySet()) {
                        final int newKey = prevMineConfigs.getKey() + currMineConfigs.getKey();
                        if (!prefixMineConfigs.get(subC + 1).containsKey(newKey)) {
                            prefixMineConfigs.get(subC + 1).put(newKey, new MutableInt(0));
                        }
                        MutableInt curr = Objects.requireNonNull(prefixMineConfigs.get(subC + 1).get(newKey));
                        //TODO: consider switching here to big integers
                        curr.addWith(prevMineConfigs.getValue().get() * currMineConfigs.getValue().get());
                    }
                }
            }

            int currMask = 0;
            for (int bit = 0; bit < removedCellsListSorted.size(); ++bit) {
                if ((mask & (1 << bit)) == 0) {
                    continue;
                }
                if (!toIndexOriginal.containsKey(removedCellsListSorted.get(bit))) {
                    continue;
                }
                final int currIndex = Objects.requireNonNull(toIndexOriginal.get(removedCellsListSorted.get(bit)));
                currMask = (currMask | (1 << currIndex));
            }

            TreeMap<Integer, MutableInt> resultMineConfigs = result.get(currMask).first;
            TreeMap<Integer, ArrayList<MutableInt>> resultMineProbs = result.get(currMask).second;

            for (TreeMap.Entry<Integer, MutableInt> mineConfigs : prefixMineConfigs.get(newSubComponents.size()).entrySet()) {
                final int currKey = mineConfigs.getKey() + cntToAdjustForDuplicates;
                if (!resultMineConfigs.containsKey(currKey)) {
                    resultMineConfigs.put(currKey, new MutableInt(0));
                }
                Objects.requireNonNull(resultMineConfigs.get(currKey)).addWith(mineConfigs.getValue().get());

                pos = 0;
                for (int node : subComponent) {
                    if (isRemoved[node]) {
                        final int removedNodeIndex = Objects.requireNonNull(toIndexOriginalAfterAddingNewRemoved.get(node));
                        if ((mask & (1 << removedNodeIndex)) > 0) {
                            if (!resultMineProbs.containsKey(currKey)) {
                                ArrayList<MutableInt> currArray = new ArrayList<>(subComponent.size());
                                for (int i = 0; i < subComponent.size(); ++i) {
                                    currArray.add(new MutableInt(0));
                                }
                                resultMineProbs.put(currKey, currArray);
                            }
                            MutableInt curr = Objects.requireNonNull(resultMineProbs.get(currKey)).get(pos);
                            curr.addWith(mineConfigs.getValue().get());
                        }
                    }
                    ++pos;
                }
            }

            TreeMap<Integer, MutableInt> suffixMineConfigs = new TreeMap<>();
            suffixMineConfigs.put(0, new MutableInt(1));

            for (int subC = newSubComponents.size() - 1; subC >= 0; --subC) {
                final TreeMap<Integer, MutableInt> currSubMineConfig = resultsSub.get(subC).get(maskSubC.get(subC)).first;
                final TreeMap<Integer, ArrayList<MutableInt>> currSubProb = resultsSub.get(subC).get(maskSubC.get(subC)).second;

                //update mine probabilities
                //TODO: not sure if it's correct, but try to optimize this with the trick: total ways / curr ways
                for (TreeMap.Entry<Integer, MutableInt> currPrefixMineConfigs : prefixMineConfigs.get(subC).entrySet()) {
                    for (TreeMap.Entry<Integer, MutableInt> currSuffixMineConfigs : suffixMineConfigs.entrySet()) {
                        for (TreeMap.Entry<Integer, ArrayList<MutableInt>> currMineProbs : currSubProb.entrySet()) {
                            if (newSubComponents.get(subC).size() != currMineProbs.getValue().size()) {
                                throw new Exception("new sub component size doesn't match currMineProb size");
                            }
                            final int totalMines = currPrefixMineConfigs.getKey() + currSuffixMineConfigs.getKey() + currMineProbs.getKey() + cntToAdjustForDuplicates;
                            final int waysPrefixAndSuffix = currPrefixMineConfigs.getValue().get() * currSuffixMineConfigs.getValue().get();
                            pos = 0;
                            for (int node : newSubComponents.get(subC)) {
                                final int posOrig = Objects.requireNonNull(nodeToIndex.get(node));
                                if (!isRemoved[node]) {
                                    if (!resultMineProbs.containsKey(totalMines)) {
                                        ArrayList<MutableInt> currArray = new ArrayList<>(subComponent.size());
                                        for (int i = 0; i < subComponent.size(); ++i) {
                                            currArray.add(new MutableInt(0));
                                        }
                                        resultMineProbs.put(totalMines, currArray);
                                    }
                                    final int currWays = waysPrefixAndSuffix * currMineProbs.getValue().get(pos).get();
                                    Objects.requireNonNull(resultMineProbs.get(totalMines)).get(posOrig).addWith(currWays);
                                }
                                ++pos;
                            }
                        }
                    }
                }

                //update prevMineConfigs
                TreeMap<Integer, MutableInt> tempMineConfigs = new TreeMap<>();
                for (TreeMap.Entry<Integer, MutableInt> currSuffixMineConfigs : suffixMineConfigs.entrySet()) {
                    for (TreeMap.Entry<Integer, MutableInt> currMineConfigs : currSubMineConfig.entrySet()) {
                        final int currKey = currSuffixMineConfigs.getKey() + currMineConfigs.getKey();
                        if (!tempMineConfigs.containsKey(currKey)) {
                            tempMineConfigs.put(currKey, new MutableInt(0));
                        }
                        MutableInt curr = Objects.requireNonNull(tempMineConfigs.get(currKey));
                        curr.addWith(currSuffixMineConfigs.getValue().get() * currMineConfigs.getValue().get());
                    }
                }
                suffixMineConfigs.clear();
                suffixMineConfigs.putAll(tempMineConfigs);
            }
        }

        return result;
    }

    private ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>> solveComponentWithBacktracking(
            int componentPos,
            SortedSet<Integer> nodes,
            TreeMap<Integer, Integer> toIndexOriginal
    ) throws Exception {
        ArrayList<Integer> allNodes = new ArrayList<>(nodes.size());
        allNodes.addAll(nodes);
        MutableInt currNumberOfMines = new MutableInt(0);
        final int pow2 = (1 << toIndexOriginal.size());
        ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>> result = new ArrayList<>(pow2);
        for (int i = 0; i < pow2; ++i) {
            result.add(new Pair<>(new TreeMap<>(), new TreeMap<>()));
        }
        solveComponentWithBacktracking(0, allNodes, toIndexOriginal, result, componentPos, new MutableInt(0), currNumberOfMines);
        return result;
    }


    //TODO: only re-run component solve if the component has changed
    private void solveComponentWithBacktracking(
            int pos,
            ArrayList<Integer> allNodes,
            TreeMap<Integer, Integer> toIndexOriginal,
            ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>> result,
            int componentPos,
            MutableInt currIterations,
            MutableInt currNumberOfMines
    ) throws Exception {
        ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
        if (pos == allNodes.size()) {
            handleSolution(componentPos, currNumberOfMines.get(), allNodes, toIndexOriginal, result);
            return;
        }
        currIterations.addWith(1);
        if (currIterations.get() >= iterationLimit) {
            throw new HitIterationLimitException("too many iterations");
        }
        final int i = component.get(allNodes.get(pos)).first;
        final int j = component.get(allNodes.get(pos)).second;

        //try mine
        isMine[i][j] = true;
        if (checkSurroundingConditions(i, j, component.get(allNodes.get(pos)), 1, allNodes, componentPos)) {
            currNumberOfMines.addWith(1);
            updateSurroundingMineCnt(i, j, 1);
            solveComponentWithBacktracking(pos + 1, allNodes, toIndexOriginal, result, componentPos, currIterations, currNumberOfMines);
            updateSurroundingMineCnt(i, j, -1);
            currNumberOfMines.addWith(-1);
        }

        //try free
        isMine[i][j] = false;
        if (checkSurroundingConditions(i, j, component.get(allNodes.get(pos)), 0, allNodes, componentPos)) {
            solveComponentWithBacktracking(pos + 1, allNodes, toIndexOriginal, result, componentPos, currIterations, currNumberOfMines);
        }
    }

    private void updateSurroundingMineCnt(int i, int j, int delta) throws Exception {
        boolean foundAdjVis = false;
        for (int[] adj : board.getAdjacentIndexes(i, j)) {
            if (board.getCell(adj[0], adj[1]).isVisible) {
                foundAdjVis = true;
                cntSurroundingMines[adj[0]][adj[1]] += delta;
            }
        }
        if (!foundAdjVis) {
            throw new Exception("hidden cell with no adjacent visible cell");
        }
    }

    private boolean checkSurroundingConditions(
            int i,
            int j,
            Pair<Integer, Integer> currSpot,
            int arePlacingAMine,
            ArrayList<Integer> allNodes,
            final int componentPos
    ) throws Exception {
        for (int[] adj : board.getAdjacentIndexes(i, j)) {
            final int adjI = adj[0], adjJ = adj[1];
            TileNoFlagsForSolver adjTile = board.getCell(adjI, adjJ);
            if (!adjTile.isVisible) {
                continue;
            }
            boolean foundAll = true;
            for (int[] adj2 : board.getAdjacentIndexes(adjI, adjJ)) {
                TileWithLogistics currTile = board.getCell(adj2[0], adj2[1]);
                if (currTile.isVisible || currTile.isLogicalMine || currTile.isLogicalFree) {
                    continue;
                }
                boolean found = false;
                for (int node : allNodes) {
                    if (adj2[0] == components.get(componentPos).get(node).first && adj2[1] == components.get(componentPos).get(node).second) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    foundAll = false;
                    break;
                }
            }
            if (!foundAll) {
                continue;
            }

            final int currBacktrackingCount = cntSurroundingMines[adjI][adjJ];
            if (currBacktrackingCount + arePlacingAMine > updatedNumberSurroundingMines[adjI][adjJ]) {
                return false;
            }
            ArrayList<Pair<Integer, Integer>> currAdj = lastUnvisitedSpot.get(adjI).get(adjJ);
            int spotsLeft = -1;
            for (int pos = 0; pos < currAdj.size(); ++pos) {
                if (currAdj.get(pos).equals(currSpot)) {
                    spotsLeft = currAdj.size() - pos - 1;
                    break;
                }
            }
            if (spotsLeft == -1) {
                throw new Exception("didn't find spot in lastUnvisitedSpot, but it should be there");
            }
            if (currBacktrackingCount + arePlacingAMine + spotsLeft < updatedNumberSurroundingMines[adjI][adjJ]) {
                return false;
            }
        }
        return true;
    }

    private void handleSolution(
            int componentPos,
            int currNumberOfMines,
            ArrayList<Integer> allNodes,
            TreeMap<Integer, Integer> toIndexOriginal,
            ArrayList<Pair<TreeMap<Integer, MutableInt>, TreeMap<Integer, ArrayList<MutableInt>>>> result
    ) throws Exception {
        ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
        int mask = 0;
        for (int node : allNodes) {
            if (!toIndexOriginal.containsKey(node)) {
                continue;
            }
            final int i = components.get(componentPos).get(node).first;
            final int j = components.get(componentPos).get(node).second;
            if (isMine[i][j]) {
                final int currIndex = Objects.requireNonNull(toIndexOriginal.get(node));
                mask = (mask | (1 << currIndex));
            }
        }

        MutableInt count = result.get(mask).first.get(currNumberOfMines);
        if (count == null) {
            result.get(mask).first.put(currNumberOfMines, new MutableInt(1));
        } else {
            count.addWith(1);
        }

        if (!result.get(mask).second.containsKey(currNumberOfMines)) {
            ArrayList<MutableInt> currSpotsArray = new ArrayList<>(allNodes.size());
            for (int i = 0; i < allNodes.size(); ++i) {
                currSpotsArray.add(new MutableInt(0));
            }
            result.get(mask).second.put(currNumberOfMines, currSpotsArray);
        }
        ArrayList<MutableInt> currArrayList = Objects.requireNonNull(result.get(mask).second.get(currNumberOfMines));
        for (int pos = 0; pos < allNodes.size(); ++pos) {
            final int i = component.get(allNodes.get(pos)).first;
            final int j = component.get(allNodes.get(pos)).second;
            MutableInt curr = currArrayList.get(pos);

            if (isMine[i][j]) {
                curr.addWith(1);
            }
        }
    }
}
