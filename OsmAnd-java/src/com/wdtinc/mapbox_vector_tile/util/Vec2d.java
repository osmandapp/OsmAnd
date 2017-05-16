
package com.wdtinc.mapbox_vector_tile.util;

/**
 * Mutable Vector with double-valued x and y dimensions.
 */
public final class Vec2d {

    public double x, y;

    /**
     * Construct instance with x = 0, y = 0.
     */
    public Vec2d() {
        set(0d, 0d);
    }

    /**
     * Construct instance with (x, y) values set to passed parameters
     *
     * @param x value in x
     * @param y value in y
     */
    public Vec2d(double x, double y) {
        set(x, y);
    }

    /**
     * Constructs instance with values from the input vector 'v'.
     *
     * @param v The vector
     */
    public Vec2d(Vec2d v) {
        set(v);
    }

    /**
     * Set the x and y values of this vector. Return this vector for chaining.
     *
     * @param x value in x
     * @param y value in y
     * @return this vector for chaining
     */
    public Vec2d set(double x, double y) {
        this.x = x;
        this.y = y;

        return this;
    }

    /**
     * Set the x and y values of this vector to match input vector 'v'. Return this vector for chaining.
     *
     * @param v contains values to copy
     * @return this vector for chaining
     */
    public Vec2d set(Vec2d v) {
        return set(v.x, v.y);
    }

    /**
     * Adds the given values to this vector. Return this vector for chaining.
     *
     * @param x value in x
     * @param y value in y
     * @return this vector for chaining
     */
    public Vec2d add(double x, double y) {
        this.x += x;
        this.y += y;

        return this;
    }

    /**
     * Adds the given vector 'v' to this vector. Return this vector for chaining.
     *
     * @param v vector to add
     * @return this vector for chaining
     */
    public Vec2d add(Vec2d v) {
        return add(v.x, v.y);
    }

    /**
     * Subtracts the given values from this vector. Return this vector for chaining.
     *
     * @param x value in x to subtract
     * @param y value in y to subtract
     * @return this vector for chaining
     */
    public Vec2d sub(double x, double y) {
        this.x -= x;
        this.y -= y;

        return this;
    }

    /**
     * Subtracts the given vector 'v' from this vector. Return this vector for chaining.
     *
     * @param v vector to subtract
     * @return this vector for chaining
     */
    public Vec2d sub(Vec2d v) {
        return sub(v.x, v.y);
    }

    /**
     * Scales this vector's values by a constant.
     *
     * @param scalar constant to scale this vector's values by
     * @return this vector for chaining
     */
    public Vec2d scale(double scalar) {
        this.x *= scalar;
        this.y *= scalar;

        return this;
    }

    @Override
    public String toString () {
        return "(" + x + "," + y + ")";
    }
}