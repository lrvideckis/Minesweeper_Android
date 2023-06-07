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
        BfsState state1 = new BfsState(0, 0, 2, false);
        BfsState state2 = new BfsState(0, 1, 1, false);
        if(!state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(!state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        if(!state1.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        state1 = new BfsState(0, 0, 128, false);
        state2 = new BfsState(2, 2, 1, false);
        if(!state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(!state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        state1 = new BfsState(0, 0, 95, false);
        state2 = new BfsState(4, 4, 86, false);
        if(state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        state1 = new BfsState(3, 5, 37, false);
        state2 = new BfsState(3, 5, 255 - 37, false);
        if(state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        state1 = new BfsState(3, 5, 56, false);
        state2 = new BfsState(3, 5, 57, false);
        if(state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(!state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        System.out.println("passed");
    }
}
