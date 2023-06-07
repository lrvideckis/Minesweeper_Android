package com.LukeVideckis.minesweeper_android.minesweeper_tests;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BfsSolver.BfsState;

import org.junit.Test;

//012
//3 4
//567
//public BfsState(int centerI, int centerJ, int subsetSurroundingSquares, boolean centerIsVisible) {
public class BfsStateTests {
    @Test
    public void testBfsStateIsSubset() throws Exception {
        BfsState state1 = new BfsState(0, 0, 2);
        BfsState state2 = new BfsState(0, 1, 1);
        if(!state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(!state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        if(!state1.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        state1 = new BfsState(0, 0, 128);
        state2 = new BfsState(2, 2, 1);
        if(!state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(!state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        state1 = new BfsState(0, 0, 95);
        state2 = new BfsState(4, 4, 86);
        if(state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        for(int subset1 = 0; subset1 < 256; subset1++) {
            for(int subset2 = subset1 + 1; subset2 < 256; subset2++) {
                state1 = new BfsState(3, 5, subset1);
                state2 = new BfsState(3, 5, subset2);
                //larger numeric value for a subset will never be contained
                //think of the most significant differing bit:
                //   it will always be on in subset2 and off in subset1
                if(state1.isSubsetOfMe(state2)) {
                    throw new Exception("failed");
                }
            }
        }
        state1 = new BfsState(3, 5, 37);
        state2 = new BfsState(3, 5, 255 - 37);
        if(state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        state1 = new BfsState(3, 5, 56);
        state2 = new BfsState(3, 5, 57);
        if(state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(!state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        System.out.println("passed");
    }

    @Test
    public void testBfsStateSetDifference() throws Exception {
        BfsState state1 = new BfsState(3, 5, 57);
        BfsState state2 = new BfsState(3, 5, 56);
        BfsState diff = state1.inMeNotInThem(state2);
        if(diff.centerI != 3 || diff.centerJ != 5 || diff.subsetSurroundingSquares != 1) {
            throw new Exception("incorrect set difference");
        }

        state1 = new BfsState(0, 0, 95);
        state2 = new BfsState(4, 4, 86);
        diff = state1.inMeNotInThem(state2);
        if(diff.centerI != 0 || diff.centerJ != 0 || diff.subsetSurroundingSquares != 95) {
            throw new Exception("incorrect set difference");
        }

        state1 = new BfsState(1, 1, 255);
        state2 = new BfsState(2, 2, 255);
        diff = state1.inMeNotInThem(state2);
        if(diff.centerI != 1 || diff.centerJ != 1 || diff.subsetSurroundingSquares != 175) {
            throw new Exception("incorrect set difference ");
        }

        System.out.println("passed");
    }
}
