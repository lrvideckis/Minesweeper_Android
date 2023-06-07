package com.LukeVideckis.minesweeper_android.minesweeper_tests;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.solvers.BfsSolver.BfsState;

import org.junit.Test;

public class BfsStateTests {
    @Test
    public void testBfsStateIsSubset() throws Exception {

        //012
        //3 4
        //567
    //public BfsState(int centerI, int centerJ, int subsetSurroundingSquares, boolean centerIsVisible) {
        BfsState state1 = new BfsState(0, 0, 2, false);
        BfsState state2 = new BfsState(1, 0, 1, false);
        if(!state1.isSubsetOfMe(state2)) {
            throw new Exception("failed");
        }
        if(!state2.isSubsetOfMe(state1)) {
            throw new Exception("failed");
        }
        System.out.println("passed");
    }
}
