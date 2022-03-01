package com.LukeVideckis.minesweeper_android.miscHelpers;

import java.math.BigInteger;

public class BigFraction {

    private BigInteger numerator, denominator;

    public BigFraction(int value) {
        numerator = BigInteger.valueOf(value);
        denominator = BigInteger.ONE;
    }

    //this should only throw if denominator is 0
    public BigFraction(BigInteger numerator, BigInteger denominator) throws Exception {
        reduceAndSet(numerator, denominator);
    }

    //this should only throw if denominator is 0
    public void setValues(int numerator, int denominator) throws Exception {
        reduceAndSet(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public void addWith(int delta) {
        BigInteger currNumerator = numerator.add(BigInteger.valueOf(delta).multiply(denominator));
        try {
            reduceAndSet(currNumerator, denominator);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addWith(BigFraction delta) {
        try {
            BigInteger newNumerator = numerator.multiply(delta.getDenominator()).add(denominator.multiply(delta.getNumerator()));
            BigInteger newDenominator = denominator.multiply(delta.getDenominator());
            reduceAndSet(newNumerator, newDenominator);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //this will only throw if denominator == 0
    public void multiplyWith(int numerator, int denominator) throws Exception {
        this.numerator = this.numerator.multiply(BigInteger.valueOf(numerator));
        this.denominator = this.denominator.multiply(BigInteger.valueOf(denominator));
        reduceAndSet(this.numerator, this.denominator);
    }

    public void multiplyWith(BigFraction other) {
        try {
            BigInteger newNumerator, newDenominator;
            newNumerator = numerator.multiply(other.getNumerator());
            newDenominator = denominator.multiply(other.getDenominator());
            reduceAndSet(newNumerator, newDenominator);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //only will throw if quotient == 0 (divide by zero error)
    public void divideWith(BigFraction quotient) throws Exception {
        this.numerator = this.numerator.multiply(quotient.getDenominator());
        this.denominator = this.denominator.multiply(quotient.getNumerator());
        reduceAndSet(this.numerator, this.denominator);
    }

    //should only throw if fraction initially equals 0
    public void invert() throws Exception {
        reduceAndSet(denominator, numerator);
    }

    public BigInteger getNumerator() throws Exception {
        if (!numerator.gcd(denominator).equals(BigInteger.ONE)) {
            throw new Exception("fraction isn't in reduced form, but I reduced after every operation");
        }
        return numerator;
    }

    public BigInteger getDenominator() throws Exception {
        if (!numerator.gcd(denominator).equals(BigInteger.ONE)) {
            throw new Exception("fraction isn't in reduced form, but I reduced after every operation");
        }
        return denominator;
    }

    public void setValue(BigFraction other) throws Exception {
        numerator = other.numerator;
        denominator = other.denominator;
        if (!numerator.gcd(denominator).equals(BigInteger.ONE)) {
            throw new Exception("fraction isn't in reduced form, but I reduced after every operation");
        }
        if (denominator.equals(BigInteger.ZERO)) {
            throw new Exception("given fraction has a 0 denominator, but this shouldn't happen");
        }
    }

    public boolean equals(BigFraction other) {
        try {
            return numerator.multiply(other.getDenominator()).equals(denominator.multiply(other.getNumerator()));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean equals(int other) {
        return numerator.equals(denominator.multiply(BigInteger.valueOf(other)));
    }

    public double getDoubleValue() throws Exception {
        if (denominator.equals(BigInteger.ZERO)) {
            throw new Exception("fraction with 0 as denominator");
        }
        return numerator.doubleValue() / denominator.doubleValue();
    }

    private void reduceAndSet(BigInteger numerator, BigInteger denominator) throws Exception {
        final BigInteger gcd = numerator.gcd(denominator);
        this.numerator = numerator.divide(gcd);
        this.denominator = denominator.divide(gcd);
        if (this.denominator.equals(BigInteger.ZERO)) {
            throw new Exception("fraction with 0 as denominator");
        }
    }
}
