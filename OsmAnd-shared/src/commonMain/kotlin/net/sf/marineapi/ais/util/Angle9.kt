/*
 * Angle9.java
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
 * Checks a 9-bit signed integer angular value for validity.
 *
 * @author Lázár József
 */
object Angle9 {
    private const val DEFAULTVALUE = 511
    private const val MINVALUE = 0
    private const val MAXVALUE = 359

    /** Valid range with default value for "no value"  */
    const val RANGE = "[$MINVALUE,$MAXVALUE] + {$DEFAULTVALUE}"

    /**
     * Tells if the given angular value is correct, i.e. within range of 0..359
     * or equals the default value (511).
     *
     * @param value Angular value to validate
     * @return `true` if correct, otherwise `false`.
     */
    fun isCorrect(value: Int): Boolean {
        return value in MINVALUE..MAXVALUE || value == DEFAULTVALUE
    }

    /**
     * Checks if the angular value is available, i.e. equals not the default
     * value (511).
     *
     * @param value Angular value to check
     * @return `true` if available, otherwise `false`.
     */
    fun isAvailable(value: Int): Boolean {
        return value != DEFAULTVALUE
    }

    /**
     * Returns the String representation of given angular value.
     *
     * @param value Angular value to stringify
     * @return Angular value as String or "no heading" or "invalid heading"
     */
    fun getTrueHeadingString(value: Int): String {
        return when {
            value == DEFAULTVALUE -> "no heading"
            value > MAXVALUE -> "invalid heading"
            else -> value.toString()
        }
    }

    /**
     * Returns the String representation of given angular value.
     *
     * @param value Angular value to stringify
     * @return Angular value as String or "not available" or "illegal value"
     */
    fun toString(value: Int): String {
        val msg: String = if (isCorrect(value)) {
            if (isAvailable(value)) value.toString() else "not available"
        } else "illegal value"
        return msg
    }
}