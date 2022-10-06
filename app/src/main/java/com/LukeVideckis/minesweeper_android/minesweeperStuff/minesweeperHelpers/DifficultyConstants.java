package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

public abstract class DifficultyConstants {
    private DifficultyConstants() throws Exception {
        throw new Exception("No instances allowed!");
    }

    public static final int BeginnerRows = 10;
    public static final int BeginnerCols = 10;
    public static final int BeginnerMines = 10;

    public static final int IntermediateRows = 14;
    public static final int IntermediateCols = 16;
    public static final int IntermediateMines = 40;

    public static final int ExpertRows = 16;
    public static final int ExpertCols = 30;
    public static final int ExpertMines = 99;
}
