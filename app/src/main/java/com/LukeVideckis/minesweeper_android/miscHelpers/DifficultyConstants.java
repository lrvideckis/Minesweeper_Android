package com.LukeVideckis.minesweeper_android.miscHelpers;

public abstract class DifficultyConstants {
    public static final int BEGINNER_ROWS = 10;
    public static final int BEGINNER_COLS = 10;
    public static final int BEGINNER_MINES = 10;
    public static final int INTERMEDIATE_ROWS = 14;
    public static final int INTERMEDIATE_COLS = 16;
    public static final int INTERMEDIATE_MINES = 40;
    public static final int EXPERT_ROWS = 16;
    public static final int EXPERT_COLS = 30;
    public static final int EXPERT_MINES = 99;

    private DifficultyConstants() throws Exception {
        throw new Exception("No instances allowed!");
    }
}