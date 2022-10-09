package com.LukeVideckis.minesweeper_android.miscHelpers;

import java.util.Locale;

public abstract class CompletionTimeFormatter {
    private CompletionTimeFormatter() throws Exception {
        throw new Exception("No instances allowed!");
    }
    public static String formatTime(long completionTimeNanoseconds) {
        return String.format(Locale.getDefault(), "%.2f", completionTimeNanoseconds / 1000000000.0);
    }
}
