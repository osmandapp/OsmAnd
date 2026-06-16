/*
 * Angle12.java
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
 * Checks a 12-bit signed integer angular value for validity.
 *
 * @author Lázár József
 */
object Angle12 {
    private const val DEFAULTVALUE = 3600
    private const val MINVALUE = 0
    private const val MAXVALUE = 3599

    /** Valid range with default value for "no value"  */
    const val RANGE = "[$MINVALUE,$MAXVALUE] + {$DEFAULTVALUE}"

    /**
     * Tells if the angular value is correct, i.e. within the range 0..3599 or
     * the default value 3600.
     *
     * @param value Angular value to validate.
     * @return `true` if correct, otherwise `false`.
     */
    fun isCorrect(value: Int): Boolean {
        return value in MINVALUE..MAXVALUE || value == DEFAULTVALUE
    }

    /**
     * Checks if the angular value is available.
     *
     * @param value Angular value to check.
     * @return true if the angular is not the default value (3600)
     */
    fun isAvailable(value: Int): Boolean {
        return value != DEFAULTVALUE
    }

    /**
     * Converts the angular value to degrees.
     *
     * @param value Angular value to convert, in 1/10 degrees.
     * @return The angular value in degrees.
     */
    fun toDegrees(value: Int): Double {
        return value / 10.0
    }

    /**
     * Returns the String representation of given angular value.
     *
     * @param value Angular value to convert to String.
     * @return a string representing the angular value
     */
    fun toString(value: Int): String {
        val msg: String = if (isCorrect(value)) {
            if (isAvailable(value)) {
                val degrees = toDegrees(value)
                val sign = if (degrees < 0) "-" else ""
                val absDeg = kotlin.math.abs(degrees)
                val intPart = absDeg.toInt()
                val fracPart = ((absDeg - intPart) * 10).toInt()
                "$sign$intPart.$fracPart"
            } else "not available"
        } else "illegal value"
        return msg
    }
}