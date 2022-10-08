package com.LukeVideckis.minesweeper_android.activity.activityHelpers;

import com.LukeVideckis.minesweeper_android.miscHelpers.DifficultyConstants;

public class DifficultyDeterminer {
    private int numberOfRows, numberOfCols, numberOfMines;
    public DifficultyDeterminer(int numberOfRows, int numberOfCols, int numberOfMines) {
        this.numberOfRows = numberOfRows;
        this.numberOfCols = numberOfCols;
        this.numberOfMines = numberOfMines;
    }

    boolean isBeginner() {
        return numberOfRows == DifficultyConstants.BEGINNER_ROWS
                && numberOfCols == DifficultyConstants.BEGINNER_COLS
                && numberOfMines == DifficultyConstants.BEGINNER_MINES;
    }

    boolean isIntermediate() {
        return numberOfRows == DifficultyConstants.INTERMEDIATE_ROWS
                && numberOfCols == DifficultyConstants.INTERMEDIATE_COLS
                && numberOfMines == DifficultyConstants.INTERMEDIATE_MINES;
    }

    boolean isExpert() {
        return numberOfRows == DifficultyConstants.EXPERT_ROWS
                && numberOfCols == DifficultyConstants.EXPERT_COLS
                && numberOfMines == DifficultyConstants.EXPERT_MINES;
    }

    public boolean isStandardDifficulty() {
        return isBeginner() || isIntermediate() || isExpert();
    }

    public String getDifficultyAsString() throws Exception {
        if (isBeginner()) {
            return "beginner";
        }
        if (isIntermediate()) {
            return "intermediate";
        }
        if (isExpert()) {
            return "expert";
        }
        throw new Exception("non standard difficulty");
    }
}

