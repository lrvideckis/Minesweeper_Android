package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.CheckForLocalStuff;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.MinesweeperSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.MyBacktrackingSolver;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.MinesweeperSolver.VisibleTile;

public class CreateSolvableBoard {
	private final MinesweeperSolver gaussSolver;
	private final VisibleTile[][] board;
	private final int rows;
	private final int cols;
	private final int mines;
	private final BacktrackingSolver myBacktrackingSolver;

	public CreateSolvableBoard(int rows, int cols, int mines) {
		myBacktrackingSolver = new MyBacktrackingSolver(rows, cols);
		gaussSolver = new GaussianEliminationSolver(rows, cols);
		board = new VisibleTile[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				board[i][j] = new VisibleTile();
			}
		}
		this.rows = rows;
		this.cols = cols;
		this.mines = mines;
	}

	private boolean clickedLogicalFrees(MinesweeperGame game) throws Exception {
		boolean clickedFree = false;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsVisible()) {
					if (board[i][j].getIsLogicalMine() || board[i][j].getIsLogicalFree()) {
						throw new Exception("visible tiles can't be logical");
					}
				}
				if (board[i][j].getIsLogicalFree() && board[i][j].getIsLogicalMine()) {
					throw new Exception("can't be both logical free and logical mine");
				}
				if (board[i][j].getIsLogicalMine() && !game.getCell(i, j).isMine()) {
					throw new Exception("found a logical mine which is free");
				}
				if (board[i][j].getIsLogicalFree()) {
					if (game.getCell(i, j).isMine()) {
						throw new Exception("found a logical free which is mine");
					}
					game.clickCell(i, j, false);
					clickedFree = true;
				}
			}
		}
		return clickedFree;
	}

	public MinesweeperGame getSolvableBoard(int firstClickI, int firstClickJ, boolean hasAn8, AtomicBoolean isInterrupted) throws Exception {
		if (ArrayBounds.outOfBounds(firstClickI, firstClickJ, rows, cols)) {
			throw new Exception("first click is out of bounds");
		}

		Stack<MinesweeperGame> gameStack = new Stack<>();

		while (!isInterrupted.get()) {
			MinesweeperGame game = new MinesweeperGame(rows, cols, mines);
			if (hasAn8) {
				game.setHavingAn8();
			}
			game.clickCell(firstClickI, firstClickJ, false);

			/* Main board generation loop.
			 * I'm calling an "interesting" mine a mine which is next to at least 1 clue
			 *
			 * In this loop, we try to create a solvable board by this
			 * algorithm:
			 *
			 * 		while(board isn't won yet) {
			 *	 		while (there exists a deducible non-mine square) {
			 * 				click all those deducible non-mine squares;
			 * 				continue;
			 *  		}
			 *
			 * 			//now there aren't any deducible mine-free squares and the game isn't won, so
			 * 			//we resort to moving positions of mines. Also, we'll change 1 mine to a
			 * 			//non-"interesting" square (a square not next to any clue).
			 *
			 * 			//Doing this has pros:
			 * 			//		- the entire board generation algorithm runs faster (fast enough to
			 * 			//		  execute in real time for the user).
			 * 			//		- this step will always eventually produce a deducible free square as
			 * 			//		  eventually one border clue will become 0, leading to more clues.
			 *
			 * 			//And cons:
			 * 			//		- many mines will be eventually moved to the outside of the board
			 * 			//		  effectively making the board smaller
			 * 			//		- the mine density of the inside of the board will be smaller, which
			 * 			//		  generally creates easier boards
			 *
			 * 			randomly move the positions of non-deducible "interesting" mines, and move 1
			 * 			"interesting" mine to a square not next to any mines;
			 *  	}
			 *
			 *
			 * The above algorithm can fail to generate a solvable board when there's a 50/50 at the
			 * very end (there may be other cases when the alg. fails). This is why there is an outer
			 * loop. So we can restart completely on a fresh random board.
			 */
			int cnt = 0;
			while (!game.getIsGameWon() && !isInterrupted.get()) {
				if (game.getIsGameLost()) {
					throw new Exception("game is lost, but board generator should never lose");
				}

				ConvertGameBoardFormat.convertToExistingBoard(game, board, true);

				/*try to deduce free squares with local rules. There is the
				 * possibility of not finding deducible free squares, even if they exist.
				 */
				if (CheckForLocalStuff.checkAndUpdateBoardForTrivialStuff(board)) {
					game.updateLogicalStuff(board);
					if (game.everyComponentHasLogicalFrees()) {
						gameStack.push(new MinesweeperGame(game));
					}
					if (clickedLogicalFrees(game)) {
						continue;
					}
				}

				/*try to deduce free squares with gauss solver. Gaussian Elimination has the
				 * possibility of not finding deducible free squares, even if they exist.
				 */
				gaussSolver.solvePosition(board, mines);
				game.updateLogicalStuff(board);
				if (game.everyComponentHasLogicalFrees()) {
					gameStack.push(new MinesweeperGame(game));
				}
				if (clickedLogicalFrees(game)) {
					continue;
				}

				try {
					myBacktrackingSolver.solvePosition(board, mines);
					game.updateLogicalStuff(board);
					if (game.everyComponentHasLogicalFrees()) {
						gameStack.push(new MinesweeperGame(game));
					}
					if (clickedLogicalFrees(game)) {
						continue;
					}
				} catch (HitIterationLimitException ignored) {
				}

				if ((cnt++) % 12 == 0) {
					try {
						game.shuffleInterestingMinesAndMakeOneAway(firstClickI, firstClickJ);
						gameStack.clear();
					} catch (Exception ignored) {
						break;
					}
				} else {
					if (!gameStack.empty()) {
						gameStack.pop();
					}
					while (!gameStack.empty() && !game.everyComponentHasLogicalFrees()) {
						game = new MinesweeperGame(gameStack.pop());
					}
					if (!game.everyComponentHasLogicalFrees()) {
						break;
					}
					try {
						game.shuffleAwayMines();
					} catch (Exception ignored) {
						break;
					}
				}
			}

			if (game.getIsGameWon()) {
				return new MinesweeperGame(game, firstClickI, firstClickJ);
			}
		}
		return new MinesweeperGame(rows, cols, mines);
	}
}
