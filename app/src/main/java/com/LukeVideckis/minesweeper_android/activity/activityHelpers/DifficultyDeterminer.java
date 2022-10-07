package com.LukeVideckis.minesweeper_android.activity.activityHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.DifficultyConstants;

public class DifficultyDeterminer {
    private int numberOfRows, numberOfCols, numberOfMines;
    public DifficultyDeterminer(int numberOfRows, int numberOfCols, int numberOfMines) {
        this.numberOfRows = numberOfRows;
        this.numberOfCols = numberOfCols;
        this.numberOfMines = numberOfMines;
    }

    boolean isBeginner() {
        return numberOfRows == DifficultyConstants.BeginnerRows
                && numberOfCols == DifficultyConstants.BeginnerCols
                && numberOfMines == DifficultyConstants.BeginnerMines;
    }

    boolean isIntermediate() {
        return numberOfRows == DifficultyConstants.IntermediateRows
                && numberOfCols == DifficultyConstants.IntermediateCols
                && numberOfMines == DifficultyConstants.IntermediateMines;
    }

    boolean isExpert() {
        return numberOfRows == DifficultyConstants.ExpertRows
                && numberOfCols == DifficultyConstants.ExpertCols
                && numberOfMines == DifficultyConstants.ExpertMines;
    }

    public boolean isStandardDifficulty() {
        return isBeginner() || isIntermediate() || isExpert();
    }

    String getDifficultyAsString() throws Exception {
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

