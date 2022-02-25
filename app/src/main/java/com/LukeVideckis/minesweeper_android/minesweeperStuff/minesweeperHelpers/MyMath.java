package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import java.math.BigInteger;

public class MyMath {
    public static final double EPSILON = 1e-8;

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

    public static int getRand(int min, int max) throws Exception {
        if (min > max) {
            throw new Exception("invalid parameters: min > max");
        }
        return (int) (Math.random() * ((max - min) + 1)) + min;
    }

    public static void performGaussianElimination(double[][] matrix) {
        if (matrix.length == 0 || matrix[0].length == 0) {
            return;
        }
        final int rows = matrix.length;
        final int cols = matrix[0].length;
        for (int col = 0, row = 0; col < cols && row < rows; ++col) {
            int sel = row;
            for (int i = row + 1; i < rows; ++i) {
                if (Math.abs(matrix[i][col]) > Math.abs(matrix[sel][col])) {
                    sel = i;
                }
            }
            if (Math.abs(matrix[sel][col]) < EPSILON) {
                continue;
            }
            for (int j = 0; j < cols; ++j) {
                double temp = matrix[sel][j];
                matrix[sel][j] = matrix[row][j];
                matrix[row][j] = temp;
            }
            double s = (1.0 / matrix[row][col]);
            for (int j = 0; j < cols; ++j) {
                matrix[row][j] = matrix[row][j] * s;
            }
            for (int i = 0; i < rows; ++i) {
                if (i != row && Math.abs(matrix[i][col]) > EPSILON) {
                    double t = matrix[i][col];
                    for (int j = 0; j < cols; ++j) {
                        matrix[i][j] = matrix[i][j] - (matrix[row][j] * t);
                    }
                }
            }
            ++row;
        }
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
