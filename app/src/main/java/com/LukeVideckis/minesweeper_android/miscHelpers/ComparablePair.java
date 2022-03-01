package com.LukeVideckis.minesweeper_android.miscHelpers;

// This is needed in addition to the other `Pair` class because TreeSet<ComparablePair> is used
// somewhere. Then when I tried to make `Pair` comparable, it fails as `Pair` is used to return 2
// things from a function which aren't comparable.
public class ComparablePair implements Comparable<ComparablePair> {
    public final Integer first, second;

    public ComparablePair(Integer first, Integer second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int compareTo(ComparablePair myPair) {
        if (first.equals(myPair.first)) {
            return second.compareTo(myPair.second);
        }
        return first.compareTo(myPair.first);
    }
}
