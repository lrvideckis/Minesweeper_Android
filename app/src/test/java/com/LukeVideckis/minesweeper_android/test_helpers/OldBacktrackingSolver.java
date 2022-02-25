package com.LukeVideckis.minesweeper_android.test_helpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AllCellsAreHidden;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MutableInt;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;

import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

//TODO: also break out early the moment we find a (conditioned) solution
public class OldBacktrackingSolver implements BacktrackingSolver {

	public final static int iterationLimit = 10000;

	private final int rows, cols;
	private final boolean[][] isMine;
	private final int[][] cntSurroundingMines, updatedNumberSurroundingMines;
	private final ArrayList<ArrayList<ArrayList<Pair<Integer, Integer>>>> lastUnvisitedSpot;
	private final ArrayList<TreeMap<Integer, MutableInt>> mineConfig = new ArrayList<>();
	private final ArrayList<TreeMap<Integer, ArrayList<MutableInt>>> mineProbPerCompPerNumMines = new ArrayList<>();
	private final ArrayList<TreeMap<Integer, TreeMap<Integer, BigFraction>>> numberOfConfigsForCurrent = new ArrayList<>();
	private final VisibleTileWithProbability[][] tempBoardWithProbability;
	private final GaussianEliminationSolver gaussianEliminationSolver;
	private int numberOfMines;
	private VisibleTile[][] board;
	private ArrayList<ArrayList<Pair<Integer, Integer>>> components;

	public OldBacktrackingSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		isMine = new boolean[rows][cols];
		cntSurroundingMines = new int[rows][cols];
		updatedNumberSurroundingMines = new int[rows][cols];
		lastUnvisitedSpot = new ArrayList<>(rows);
		tempBoardWithProbability = new VisibleTileWithProbability[rows][cols];
		for (int i = 0; i < rows; ++i) {
			ArrayList<ArrayList<Pair<Integer, Integer>>> currRow = new ArrayList<>(cols);
			for (int j = 0; j < cols; ++j) {
				ArrayList<Pair<Integer, Integer>> currSpot = new ArrayList<>();
				currRow.add(currSpot);
			}
			lastUnvisitedSpot.add(currRow);
		}
		gaussianEliminationSolver = new GaussianEliminationSolver(rows, cols);
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {
		//TODO: this can be optimized: have the option to not calculate probability, this option can be used for getMineConfiguration
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				tempBoardWithProbability[i][j] = new VisibleTileWithProbability(board[i][j]);
			}
		}
		solvePosition(tempBoardWithProbability, numberOfMines);
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				board[i][j].set(tempBoardWithProbability[i][j]);
			}
		}
	}

	@Override
	public void solvePosition(VisibleTileWithProbability[][] board, int numberOfMines) throws Exception {

		if (AllCellsAreHidden.allCellsAreHidden(board)) {
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					board[i][j].mineProbability.setValues(numberOfMines, rows * cols);
					if(numberOfMines == 0) {
						board[i][j].isLogicalFree = true;
					}
					if(numberOfMines == rows * cols) {
						board[i][j].isLogicalMine = true;
					}
				}
			}
			return;
		}

		gaussianEliminationSolver.solvePosition(board, numberOfMines);

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsVisible() && (board[i][j].getIsLogicalMine() || board[i][j].getIsLogicalFree())) {
					throw new Exception("visible cells can't be logical frees/mines");
				}
				if (board[i][j].getIsLogicalMine() && board[i][j].getIsLogicalFree()) {
					throw new Exception("cell can't be both logical free and logical mine");
				}
				if (board[i][j].getIsLogicalMine()) {
					if (!AwayCell.isAwayCell(board, i, j, rows, cols)) {
						--numberOfMines;
					}
					board[i][j].mineProbability.setValues(1, 1);
				} else if (board[i][j].getIsLogicalFree()) {
					board[i][j].mineProbability.setValues(0, 1);
				} else {
					board[i][j].mineProbability.setValues(0, 1);
				}
				if (board[i][j].getIsVisible()) {
					updatedNumberSurroundingMines[i][j] = board[i][j].getNumberSurroundingMines();
					for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
						VisibleTile adjCell = board[adj[0]][adj[1]];
						if (adjCell.getIsLogicalMine()) {
							--updatedNumberSurroundingMines[i][j];
						}
					}
				}
			}
		}

		initialize(board, numberOfMines);
		components = GetConnectedComponentsOld.getComponentsWithKnownCellsOld(board);
		initializeLastUnvisitedSpot(components);

		performBacktrackingSequentially();

		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);

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
					board[row][col].mineProbability.addWith(delta);
				}
			}
		}

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTileWithProbability curr = board[i][j];
				if (curr.getIsVisible() && (curr.isLogicalMine || curr.isLogicalFree)) {
					throw new Exception("visible cells shouldn't be logical");
				}
				if (curr.getIsVisible() && !curr.mineProbability.equals(0)) {
					throw new Exception("found a visible cell with non-zero mine probability: " + i + " " + j);
				}
				if (curr.getIsLogicalMine()) {
					if (!curr.mineProbability.equals(1)) {
						throw new Exception("found logical mine with mine probability != 1: " + i + " " + j);
					}
				}
				if (curr.getIsLogicalFree()) {
					if (!curr.mineProbability.equals(0)) {
						throw new Exception("found logical free cell with mine probability != 0: " + i + " " + j);
					}
				}

				if (AwayCell.isAwayCell(board, i, j, rows, cols)) {
					if (awayMineProbability == null) {
						throw new Exception("away probability is null, but this was checked above");
					}
					curr.mineProbability.setValue(awayMineProbability);
				}

				if (curr.getIsVisible() || curr.getIsLogicalMine() || curr.getIsLogicalFree()) {
					continue;
				}
				if (curr.mineProbability.equals(0)) {
					curr.isLogicalFree = true;
				} else if (curr.mineProbability.equals(1)) {
					curr.isLogicalMine = true;
				}
			}
		}
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

		final int numberAwayCells = AwayCell.getNumberOfAwayCells(board);
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
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);
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
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);
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
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);
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

	private void initialize(VisibleTile[][] board, int numberOfMines) throws Exception {
		this.board = board;
		this.numberOfMines = numberOfMines;
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		if (rows != dimensions.first || cols != dimensions.second) {
			throw new Exception("dimensions of board doesn't match what was passed in the constructor");
		}
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				isMine[i][j] = false;
				cntSurroundingMines[i][j] = 0;
			}
		}
	}

	private void initializeLastUnvisitedSpot(ArrayList<ArrayList<Pair<Integer, Integer>>> components) {
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
				for (int[] adj : GetAdjacentCells.getAdjacentCells(spot.first, spot.second, rows, cols)) {
					final int adjI = adj[0], adjJ = adj[1];
					if (board[adjI][adjJ].isVisible) {
						lastUnvisitedSpot.get(adjI).get(adjJ).add(spot);
					}
				}
			}
		}
	}

	private void performBacktrackingSequentially() throws Exception {
		for (int i = 0; i < components.size(); ++i) {
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfMines = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfMines);
		}
	}

	//TODO: only re-run component solve if the component has changed
	private void solveComponent(int pos, int componentPos, MutableInt currIterations, MutableInt currNumberOfMines) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		if (pos == component.size()) {
			handleSolution(componentPos, currNumberOfMines.get());
			return;
		}
		currIterations.addWith(1);
		if (currIterations.get() >= iterationLimit) {
			throw new HitIterationLimitException("too many iterations");
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;

		//try mine
		isMine[i][j] = true;
		if (checkSurroundingConditions(i, j, component.get(pos), 1)) {
			currNumberOfMines.addWith(1);
			updateSurroundingMineCnt(i, j, 1);
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfMines);
			updateSurroundingMineCnt(i, j, -1);
			currNumberOfMines.addWith(-1);
		}

		//try free
		isMine[i][j] = false;
		if (checkSurroundingConditions(i, j, component.get(pos), 0)) {
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfMines);
		}
	}

	private void updateSurroundingMineCnt(int i, int j, int delta) throws Exception {
		boolean foundAdjVis = false;
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			if (board[adj[0]][adj[1]].isVisible) {
				foundAdjVis = true;
				cntSurroundingMines[adj[0]][adj[1]] += delta;
			}
		}
		if (!foundAdjVis) {
			throw new Exception("hidden cell with no adjacent visible cell");
		}
	}

	private boolean checkSurroundingConditions(int i, int j, Pair<Integer, Integer> currSpot, int arePlacingAMine) throws Exception {
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			VisibleTile adjTile = board[adjI][adjJ];
			if (!adjTile.isVisible) {
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

	private void handleSolution(int componentPos, int currNumberOfMines) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		checkPositionValidity(component, currNumberOfMines);

		MutableInt count = mineConfig.get(componentPos).get(currNumberOfMines);
		if (count == null) {
			mineConfig.get(componentPos).put(currNumberOfMines, new MutableInt(1));
		} else {
			count.addWith(1);
		}

		if (!mineProbPerCompPerNumMines.get(componentPos).containsKey(currNumberOfMines)) {
			ArrayList<MutableInt> currSpotsArray = new ArrayList<>(component.size());
			for (int i = 0; i < component.size(); ++i) {
				currSpotsArray.add(new MutableInt(0));
			}
			mineProbPerCompPerNumMines.get(componentPos).put(currNumberOfMines, currSpotsArray);
		}
		ArrayList<MutableInt> currArrayList = Objects.requireNonNull(mineProbPerCompPerNumMines.get(componentPos).get(currNumberOfMines));
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			MutableInt curr = currArrayList.get(pos);

			if (isMine[i][j]) {
				curr.addWith(1);
			}
		}
	}

	private void checkPositionValidity(ArrayList<Pair<Integer, Integer>> component, int currNumberOfMines) throws Exception {
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
				final int adjI = adj[0], adjJ = adj[1];
				VisibleTile adjTile = board[adjI][adjJ];
				if (!adjTile.isVisible) {
					continue;
				}
				if (cntSurroundingMines[adjI][adjJ] != updatedNumberSurroundingMines[adjI][adjJ]) {
					throw new Exception("found bad solution - # mines doesn't match, but this should be pruned out");
				}
			}
		}
		int prevNumberOfMines = 0;
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if (isMine[i][j]) {
				++prevNumberOfMines;
			}
		}
		if (prevNumberOfMines != currNumberOfMines) {
			throw new Exception("number of mines doesn't match");
		}
	}
}
