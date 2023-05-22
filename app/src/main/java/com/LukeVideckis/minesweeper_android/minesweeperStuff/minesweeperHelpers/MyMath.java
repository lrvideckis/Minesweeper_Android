package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.miscHelpers.BigFraction;

import java.math.BigInteger;

public class MyMath {
    public static final double EPSILON = 1e-8;

    private MyMath() throws Exception {
        throw new Exception("No instances allowed!");
    }

    //returns (n choose x) / (n choose y)
    public static BigFraction BinomialCoefficientFraction(int n, int x, int y) throws Exception {
        if (x < 0 || y < 0 || x > n || y > n) {
            throw new Exception("invalid parameters (n,x,y): " + n + " " + x + " " + y);
        }
        if (x == y) {
            return new BigFraction(1);
        }
        if (x > y) {
            return new BigFraction(productRange(n - x + 1, n - y), productRange(y + 1, x));
        }
        return new BigFraction(productRange(x + 1, y), productRange(n - y + 1, n - x));
    }

    private static BigInteger productRange(int min, int max) throws Exception {
        if (min > max) {
            throw new Exception("min > max");
        }
        BigInteger result = BigInteger.valueOf(min);
        for (int i = min + 1; i <= max; ++i) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    //random number in range [min, max] inclusive
    public static int getRand(int min, int max) throws Exception {
        if (min > max) {
            throw new Exception("invalid parameters: min > max");
        }
        return (int) (Math.random() * ((max - min) + 1)) + min;
    }

    public static int addExact(int x, int y) throws Exception {
        int r = x + y;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new Exception("integer overflow");
        }
        return r;
    }
}
