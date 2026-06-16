/*
 * Longitude18.java
 * Copyright (C) 2015 Lázár József
 *
 * This file is part of Java Marine API.
 * <http://ktuukkan.github.io/marine-api/>
 *
 * Java Marine API is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Java Marine API is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java Marine API. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.marineapi.ais.util



/**
 * Checks a 18-bit signed integer longitude value for validity.
 *
 */
object Longitude18 {
    private const val MINUTE_PART_MULTIPLIER = 60 * 10
    private const val MIN_VALUE = -180 * MINUTE_PART_MULTIPLIER
    private const val MAX_VALUE = 180 * MINUTE_PART_MULTIPLIER
    private const val DEFAULT_VALUE = 181 * MINUTE_PART_MULTIPLIER

    /** The range of valid longitude values with default for "no value".  */
    const val RANGE = "[$MIN_VALUE,$MAX_VALUE] + {$DEFAULT_VALUE}"

    /**
     * Converts the longitude value (in 1/10 minutes) to degrees.
     *
     * @param value Int value to convert
     * @return the longitude value in degrees
     */
    fun toDegrees(value: Int): Double {
        return value.toDouble() / MINUTE_PART_MULTIPLIER.toDouble()
    }

    /**
     * Tells if the given longitude is available, i.e. within expected range.
     *
     * @param value Longitude value to validate
     * @return `true` if available, otherwise `false`.
     */
    fun isAvailable(value: Int): Boolean {
        return value in MIN_VALUE..MAX_VALUE
    }

    /**
     * Tells if the given value is correct, i.e. within expected range and not
     * the no-value.
     *
     * @param value Longitude value to validate
     * @return `true` if correct, otherwise `false`.
     */
    fun isCorrect(value: Int): Boolean {
        return isAvailable(value) || value == DEFAULT_VALUE
    }

    /**
     * Returns the string representation of given longitude value.
     *
     * @param value Value to stringify
     * @return formatted value, "invalid longitude" or "longitude not available"
     */
    fun toString(value: Int): String {
        return when {
            !isCorrect(value) -> "invalid longitude"
            !isAvailable(value) -> "longitude not available"
            else -> {
                val degrees = toDegrees(value)
                val sign = if (degrees < 0) "-" else ""
                val absDeg = kotlin.math.abs(degrees)
                val intPart = absDeg.toInt()
                val fracPart = ((absDeg - intPart) * 1000000).toInt()
                "$sign$intPart.${fracPart.toString().padStart(6, '0')}"
            }
        }
    }
}