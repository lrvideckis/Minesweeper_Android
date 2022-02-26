package com.LukeVideckis.minesweeper_android.miscHelpers;

public class MyPair implements Comparable<MyPair> {
    public final Integer first, second;

    public MyPair(Integer first, Integer second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int compareTo(MyPair myPair) {
        if (first.equals(myPair.first)) {
            return second.compareTo(myPair.second);
        }
        return first.compareTo(myPair.first);
    }
}
