// **********************************************************************
//
// <copyright>
//
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
//
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
//
// </copyright>
// **********************************************************************

package com.jwetherell.openmap.common;

public abstract class MoreMath {

    /**
     * 2*Math.PI
     */
    public static final transient float TWO_PI = (float) Math.PI * 2.0f;

    /**
     * 2*Math.PI
     */
    public static final transient double TWO_PI_D = Math.PI * 2.0d;

    /**
     * Math.PI/2
     */
    public static final transient float HALF_PI = (float) Math.PI / 2.0f;

    /**
     * Math.PI/2
     */
    public static final transient double HALF_PI_D = Math.PI / 2.0d;

    /**
     * Checks if a ~= b. Use this to test equality of floating point numbers.
     * <p>
     * 
     * @param a
     *            double
     * @param b
     *            double
     * @param epsilon
     *            the allowable error
     * @return boolean
     */
    public static final boolean approximately_equal(double a, double b, double epsilon) {
        return (Math.abs(a - b) <= epsilon);
    }

    /**
     * Checks if a ~= b. Use this to test equality of floating point numbers.
     * <p>
     * 
     * @param a
     *            float
     * @param b
     *            float
     * @param epsilon
     *            the allowable error
     * @return boolean
     */
    public static final boolean approximately_equal(float a, float b, float epsilon) {
        return (Math.abs(a - b) <= epsilon);
    }

    /**
     * Hyperbolic arcsin.
     * <p>
     * Hyperbolic arc sine: log (x+sqrt(1+x^2))
     * 
     * @param x
     *            float
     * @return float asinh(x)
     */
    public static final float asinh(float x) {
        return (float) Math.log(x + Math.sqrt(x * x + 1));
    }

    /**
     * Hyperbolic arcsin.
     * <p>
     * Hyperbolic arc sine: log (x+sqrt(1+x^2))
     * 
     * @param x
     *            double
     * @return double asinh(x)
     */
    public static final double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1));
    }

    /**
     * Hyperbolic sin.
     * <p>
     * Hyperbolic sine: (e^x-e^-x)/2
     * 
     * @param x
     *            float
     * @return float sinh(x)
     */
    public static final float sinh(float x) {
        return (float) (Math.pow(Math.E, x) - Math.pow(Math.E, -x)) / 2.0f;
    }

    /**
     * Hyperbolic sin.
     * <p>
     * Hyperbolic sine: (e^x-e^-x)/2
     * 
     * @param x
     *            double
     * @return double sinh(x)
     */
    public static final double sinh(double x) {
        return (Math.pow(Math.E, x) - Math.pow(Math.E, -x)) / 2.0d;
    }

    // HACK - are there functions that already exist?
    /**
     * Return sign of number.
     * 
     * @param x
     *            short
     * @return int sign -1, 1
     */
    public static final int sign(short x) {
        return (x < 0) ? -1 : 1;
    }

    /**
     * Return sign of number.
     * 
     * @param x
     *            int
     * @return int sign -1, 1
     */
    public static final int sign(int x) {
        return (x < 0) ? -1 : 1;
    }

    /**
     * Return sign of number.
     * 
     * @param x
     *            long
     * @return int sign -1, 1
     */
    public static final int sign(long x) {
        return (x < 0) ? -1 : 1;
    }

    /**
     * Return sign of number.
     * 
     * @param x
     *            float
     * @return int sign -1, 1
     */
    public static final int sign(float x) {
        return (x < 0f) ? -1 : 1;
    }

    /**
     * Return sign of number.
     * 
     * @param x
     *            double
     * @return int sign -1, 1
     */
    public static final int sign(double x) {
        return (x < 0d) ? -1 : 1;
    }

    /**
     * Check if number is odd.
     * 
     * @param x
     *            short
     * @return boolean
     */
    public static final boolean odd(short x) {
        return !even(x);
    }

    /**
     * Check if number is odd.
     * 
     * @param x
     *            int
     * @return boolean
     */
    public static final boolean odd(int x) {
        return !even(x);
    }

    /**
     * Check if number is odd.
     * 
     * @param x
     *            long
     * @return boolean
     */
    public static final boolean odd(long x) {
        return !even(x);
    }

    /**
     * Check if number is even.
     * 
     * @param x
     *            short
     * @return boolean
     */
    public static final boolean even(short x) {
        return ((x & 0x1) == 0);
    }

    /**
     * Check if number is even.
     * 
     * @param x
     *            int
     * @return boolean
     */
    public static final boolean even(int x) {
        return ((x & 0x1) == 0);
    }

    /**
     * Check if number is even.
     * 
     * @param x
     *            long
     * @return boolean
     */
    public static final boolean even(long x) {
        return ((x & 0x1) == 0);
    }

    /**
     * Converts a byte in the range of -128 to 127 to an int in the range 0 -
     * 255.
     * 
     * @param b
     *            (-128 &lt;= b &lt;= 127)
     * @return int (0 &lt;= b &lt;= 255)
     */
    public static final int signedToInt(byte b) {
        return (b & 0xff);
    }

    /**
     * Converts a short in the range of -32768 to 32767 to an int in the range 0
     * - 65535.
     * 
     * @param w
     *            (-32768 &lt;= b &lt;= 32767)
     * @return int (0 &lt;= b &lt;= 65535)
     */
    public static final int signedToInt(short w) {
        return (w & 0xffff);
    }

    /**
     * Convert an int in the range of -2147483648 to 2147483647 to a long in the
     * range 0 to 4294967295.
     * 
     * @param x
     *            (-2147483648 &lt;= x &lt;= 2147483647)
     * @return long (0 &lt;= x &lt;= 4294967295)
     */
    public static final long signedToLong(int x) {
        return (x & 0xFFFFFFFFL);
    }

    /**
     * Converts an int in the range of 0 - 65535 to an int in the range of 0 -
     * 255.
     * 
     * @param w
     *            int (0 &lt;= w &lt;= 65535)
     * @return int (0 &lt;= w &lt;= 255)
     */
    public static final int wordToByte(int w) {
        return w >> 8;
    }

    /**
     * Build short out of bytes (in big endian order).
     * 
     * @param bytevec
     *            bytes
     * @param offset
     *            byte offset
     * @return short
     */
    public static final short BuildShortBE(byte bytevec[], int offset) {
        return (short) (((bytevec[0 + offset]) << 8) | (signedToInt(bytevec[1 + offset])));
    }

    /**
     * Build short out of bytes (in little endian order).
     * 
     * @param bytevec
     *            bytes
     * @param offset
     *            byte offset
     * @return short
     */
    public static final short BuildShortLE(byte bytevec[], int offset) {
        return (short) (((bytevec[1 + offset]) << 8) | (signedToInt(bytevec[0 + offset])));
    }

    /**
     * Build short out of bytes.
     * 
     * @param bytevec
     *            bytes
     * @param offset
     *            byte offset
     * @param MSBFirst
     *            BE or LE?
     * @return short
     */
    public static final short BuildShort(byte bytevec[], int offset, boolean MSBFirst) {
        if (MSBFirst)
            return (BuildShortBE(bytevec, offset));
        // else
        return (BuildShortLE(bytevec, offset));
    }

    /**
     * Build short out of bytes (in big endian order).
     * 
     * @param bytevec
     *            bytes
     * @param MSBFirst
     *            BE or LE?
     * @return short
     */

    public static final short BuildShortBE(byte bytevec[], boolean MSBFirst) {
        return BuildShortBE(bytevec, 0);
    }

    /**
     * Build short out of bytes (in little endian order).
     * 
     * @param bytevec
     *            bytes
     * @param MSBFirst
     *            BE or LE?
     * @return short
     */
    public static final short BuildShortLE(byte bytevec[], boolean MSBFirst) {
        return BuildShortLE(bytevec, 0);
    }

    /**
     * Build short out of bytes.
     * 
     * @param bytevec
     *            bytes
     * @param MSBFirst
     *            BE or LE?
     * @return short
     */
    public static final short BuildShort(byte bytevec[], boolean MSBFirst) {
        return BuildShort(bytevec, 0, MSBFirst);
    }

    /**
     * Build int out of bytes (in big endian order).
     * 
     * @param bytevec
     *            bytes
     * @param offset
     *            byte offset
     * @return int
     */
    public static final int BuildIntegerBE(byte bytevec[], int offset) {
        return (((bytevec[0 + offset]) << 24) | (signedToInt(bytevec[1 + offset]) << 16) | (signedToInt(bytevec[2 + offset]) << 8) | (signedToInt(bytevec[3 + offset])));
    }

    /**
     * Build int out of bytes (in little endian order).
     * 
     * @param bytevec
     *            bytes
     * @param offset
     *            byte offset
     * @return int
     */
    public static final int BuildIntegerLE(byte bytevec[], int offset) {
        return (((bytevec[3 + offset]) << 24) | (signedToInt(bytevec[2 + offset]) << 16) | (signedToInt(bytevec[1 + offset]) << 8) | (signedToInt(bytevec[0 + offset])));
    }

    /**
     * Build int out of bytes.
     * 
     * @param bytevec
     *            bytes
     * @param offset
     *            byte offset
     * @param MSBFirst
     *            BE or LE?
     * @return int
     */
    public static final int BuildInteger(byte bytevec[], int offset, boolean MSBFirst) {
        if (MSBFirst) 
            return BuildIntegerBE(bytevec, offset);
        // else
        return BuildIntegerLE(bytevec, offset);
    }

    /**
     * Build int out of bytes (in big endian order).
     * 
     * @param bytevec
     *            bytes
     * @return int
     */
    public static final int BuildIntegerBE(byte bytevec[]) {
        return BuildIntegerBE(bytevec, 0);
    }

    /**
     * Build int out of bytes (in little endian order).
     * 
     * @param bytevec
     *            bytes
     * @return int
     */
    public static final int BuildIntegerLE(byte bytevec[]) {
        return BuildIntegerLE(bytevec, 0);
    }

    /**
     * Build int out of bytes.
     * 
     * @param bytevec
     *            bytes
     * @param MSBFirst
     *            BE or LE?
     * @return int
     */
    public static final int BuildInteger(byte bytevec[], boolean MSBFirst) {
        if (MSBFirst) 
            return BuildIntegerBE(bytevec, 0);
        //else 
        return BuildIntegerLE(bytevec, 0);
    }

    /**
     * Build long out of bytes (in big endian order).
     * 
     * @param bytevec
     *            bytes
     * @param offset
     *            byte offset
     * @return long
     */
    public static final long BuildLongBE(byte bytevec[], int offset) {
        return (((long) signedToInt(bytevec[0 + offset]) << 56) | ((long) signedToInt(bytevec[1 + offset]) << 48)
                | ((long) signedToInt(bytevec[2 + offset]) << 40) | ((long) signedToInt(bytevec[3 + offset]) << 32)
                | ((long) signedToInt(bytevec[4 + offset]) << 24) | ((long) signedToInt(bytevec[5 + offset]) << 16)
                | ((long) signedToInt(bytevec[6 + offset]) << 8) | (signedToInt(bytevec[7 + offset])));
    }

    /**
     * Build long out of bytes (in little endian order).
     * 
     * @param bytevec
     *            bytes
     * @param offset
     *            byte offset
     * @return long
     */
    public static final long BuildLongLE(byte bytevec[], int offset) {
        return (((long) signedToInt(bytevec[7 + offset]) << 56) | ((long) signedToInt(bytevec[6 + offset]) << 48)
                | ((long) signedToInt(bytevec[5 + offset]) << 40) | ((long) signedToInt(bytevec[4 + offset]) << 32)
                | ((long) signedToInt(bytevec[3 + offset]) << 24) | ((long) signedToInt(bytevec[2 + offset]) << 16)
                | ((long) signedToInt(bytevec[1 + offset]) << 8) | (signedToInt(bytevec[0 + offset])));
    }

    /**
     * Build long out of bytes.
     * 
     * @param bytevec
     *            bytes
     * @param offset
     *            byte offset
     * @param MSBFirst
     *            BE or LE?
     * @return long
     */
    public static final long BuildLong(byte bytevec[], int offset, boolean MSBFirst) {
        if (MSBFirst) 
            return BuildLongBE(bytevec, offset);
        // else 
        return BuildLongLE(bytevec, offset);
    }

    /**
     * Build long out of bytes (in big endian order).
     * 
     * @param bytevec
     *            bytes
     * @return long
     */
    public static final long BuildLongBE(byte bytevec[]) {
        return BuildLongBE(bytevec, 0);
    }

    /**
     * Build long out of bytes (in little endian order).
     * 
     * @param bytevec
     *            bytes
     * @return long
     */
    public static final long BuildLongLE(byte bytevec[]) {
        return BuildLongLE(bytevec, 0);
    }

    /**
     * Build long out of bytes.
     * 
     * @param bytevec
     *            bytes
     * @param MSBFirst
     *            BE or LE?
     * @return long
     */
    public static final long BuildLong(byte bytevec[], boolean MSBFirst) {
        if (MSBFirst) 
            return BuildLongBE(bytevec, 0);
        // else 
        return BuildLongLE(bytevec, 0);
    }
}