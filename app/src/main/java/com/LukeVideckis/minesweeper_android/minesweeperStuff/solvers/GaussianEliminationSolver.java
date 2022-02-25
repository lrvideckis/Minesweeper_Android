package com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers;

import com.LukeVideckis.minesweeper_android.miscHelpers.Pair;;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;

import java.util.ArrayList;
import java.util.Arrays;

//TODO: run this independently on each component
public class GaussianEliminationSolver implements MinesweeperSolver {

	private static final int maxAwayCellsToIncludeThem = 10;
	private final int rows, cols;
	private final int[][] hiddenNodeToId, idToHiddenNode, newSurroundingMineCounts;

	public GaussianEliminationSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		hiddenNodeToId = new int[rows][cols];
		idToHiddenNode = new int[rows * cols][2];
		newSurroundingMineCounts = new int[rows][cols];
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		if (rows != dimensions.first || cols != dimensions.second) {
			throw new Exception("dimensions of board doesn't match what was passed in the constructor");
		}

		//noinspection StatementWithEmptyBody
		while (runGaussSolverOnce(board, numberOfMines))
			;
	}

	//returns true if extra stuff is found
	public boolean runGaussSolverOnce(VisibleTile[][] board, int numberOfMines) throws Exception {
		final boolean includeAwayCells = (AwayCell.getNumberOfAwayCells(board) <= maxAwayCellsToIncludeThem);
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				final VisibleTile cell = board[i][j];
				if (cell.isLogicalMine && cell.isLogicalFree) {
					throw new Exception("cell can't be both logical mine and free");
				}
				if (cell.getIsLogicalMine()) {
					--numberOfMines;
				}
				newSurroundingMineCounts[i][j] = cell.getNumberSurroundingMines();
				hiddenNodeToId[i][j] = -1;
			}
		}

		int numberOfHiddenNodes = 0, numberOfClues = 0;
		ArrayList<Pair<Integer, Integer>> clueSpots = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				final VisibleTile cell = board[i][j];
				if (cell.getIsVisible()) {
					boolean foundAdjacentUnknown = false;
					for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
						final int adjI = adj[0], adjJ = adj[1];
						if (board[adjI][adjJ].isLogicalMine) {
							--newSurroundingMineCounts[i][j];
						}
						//noinspection IfStatementMissingBreakInLoop
						if (!board[adjI][adjJ].isLogicalMine && !board[adjI][adjJ].isLogicalFree) {
							foundAdjacentUnknown = true;
						}
					}
					if (newSurroundingMineCounts[i][j] > 0 && foundAdjacentUnknown) {
						++numberOfClues;
						clueSpots.add(new Pair<>(i, j));
					}
					continue;
				}
				if (AwayCell.isAwayCell(board, i, j, rows, cols) && !includeAwayCells) {
					continue;
				}
				if (cell.isLogicalFree || cell.isLogicalMine) {
					continue;
				}
				hiddenNodeToId[i][j] = numberOfHiddenNodes;
				idToHiddenNode[numberOfHiddenNodes][0] = i;
				idToHiddenNode[numberOfHiddenNodes][1] = j;
				numberOfHiddenNodes++;
			}
		}
		if (includeAwayCells) {
			++numberOfClues;
		}

		double[][] matrix = new double[numberOfClues][numberOfHiddenNodes + 1];
		for (int currentClue = 0; currentClue < clueSpots.size(); ++currentClue) {
			final int i = clueSpots.get(currentClue).first;
			final int j = clueSpots.get(currentClue).second;
			for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
				final int adjI = adj[0], adjJ = adj[1];
				if (board[adjI][adjJ].getIsVisible() || board[adjI][adjJ].isLogicalMine || board[adjI][adjJ].isLogicalFree) {
					continue;
				}
				if (hiddenNodeToId[adjI][adjJ] == -1) {
					throw new Exception("adjacent node should have an id");
				}
				matrix[currentClue][hiddenNodeToId[adjI][adjJ]] = 1;
			}
			matrix[currentClue][numberOfHiddenNodes] = newSurroundingMineCounts[i][j];
		}

		if (includeAwayCells) {
			if (clueSpots.size() != numberOfClues - 1) {
				throw new Exception("wrong number of clues");
			}
			for (int j = 0; j < numberOfHiddenNodes; ++j) {
				matrix[numberOfClues - 1][j] = 1;
			}
			matrix[numberOfClues - 1][numberOfHiddenNodes] = numberOfMines;
		}

		MyMath.performGaussianElimination(matrix);

		boolean foundNewStuff = false;
		boolean[] isMine = new boolean[numberOfHiddenNodes];
		boolean[] isFree = new boolean[numberOfHiddenNodes];
		for (double[] currRow : matrix) {
			Arrays.fill(isMine, false);
			Arrays.fill(isFree, false);
			checkRowForSolvableStuff(currRow, isMine, isFree);
			for (int i = 0; i + 1 < currRow.length; ++i) {
				if (isMine[i] && isFree[i]) {
					throw new Exception("can't be both a mine and free");
				}
				final int gridI = idToHiddenNode[i][0];
				final int gridJ = idToHiddenNode[i][1];
				if (isMine[i] && !board[gridI][gridJ].isLogicalMine) {
					foundNewStuff = true;
					board[gridI][gridJ].isLogicalMine = true;
				}
				if (isFree[i] && !board[gridI][gridJ].isLogicalFree) {
					foundNewStuff = true;
					board[gridI][gridJ].isLogicalFree = true;
				}
			}
		}

		return (foundNewStuff || CheckForLocalStuff.checkAndUpdateBoardForTrivialStuff(board));
	}

	private void checkRowForSolvableStuff(double[] currRow, boolean[] isMine, boolean[] isFree) {
		if (Math.abs(currRow[currRow.length - 1]) < MyMath.EPSILON) {
			return;
		}
		double sumPos = 0, sumNeg = 0;
		for (int i = 0; i + 1 < currRow.length; ++i) {
			if (Math.abs(currRow[i]) < MyMath.EPSILON) {
				continue;
			}
			if (currRow[i] > 0.0) {
				sumPos += currRow[i];
			}
			if (currRow[i] < 0.0) {
				sumNeg += currRow[i];
			}
		}
		if (Math.abs(sumPos - currRow[currRow.length - 1]) < MyMath.EPSILON) {
			for (int i = 0; i + 1 < currRow.length; ++i) {
				if (Math.abs(currRow[i]) < MyMath.EPSILON) {
					continue;
				}
				if (currRow[i] > 0.0) {
					isMine[i] = true;
				}
				if (currRow[i] < 0.0) {
					isFree[i] = true;
				}
			}
			return;
		}
		if (Math.abs(sumNeg - currRow[currRow.length - 1]) < MyMath.EPSILON) {
			for (int i = 0; i + 1 < currRow.length; ++i) {
				if (Math.abs(currRow[i]) < MyMath.EPSILON) {
					continue;
				}
				if (currRow[i] > 0.0) {
					isFree[i] = true;
				}
				if (currRow[i] < 0.0) {
					isMine[i] = true;
				}
			}
		}
	}
}
