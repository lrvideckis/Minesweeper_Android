package com.LukeVideckis.minesweeper_android.miscHelpers;

public abstract class CompletionTimeFormatter {
    private CompletionTimeFormatter() throws Exception {
        throw new Exception("No instances allowed!");
    }
    public static String formatTime(long completionTimeNanoseconds) {
        return String.format("%.2f", completionTimeNanoseconds / 1000000000.0);
    }
}
