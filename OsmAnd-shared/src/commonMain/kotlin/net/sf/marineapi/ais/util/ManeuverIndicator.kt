/*
 * ManeuverIndicator.java
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
 * Checks a 2-bit signed integer maneuver value for validity.
 *
 * @author Lázár József
 */
object ManeuverIndicator {
    private const val DEFAULTVALUE = 0
    private const val MINVALUE = 1
    private const val MAXVALUE = 2

    /** Valid range with default value for "no value"  */
    const val RANGE = "[$MINVALUE,$MAXVALUE] + {$DEFAULTVALUE}"

    /**
     * Checks if the value is in the correct range.
     *
     * @param value Int value to check
     * @return true if the value is correct
     */
    fun isCorrect(value: Int): Boolean {
        return value in MINVALUE..MAXVALUE || value == DEFAULTVALUE
    }

    /**
     * Checks if the maneuver value is available.
     *
     * @param value Int value to check
     * @return true if the value is not the default value
     */
    fun isAvailable(value: Int): Boolean {
        return value != DEFAULTVALUE
    }

    /**
     * Rerturns the string representation of given int value.
     *
     * @param value Value to stringify
     * @return a string representing the maneuvre indicator value
     */
    fun toString(value: Int): String {
        return when (value) {
            0 -> "no special maneuver indicator"
            1 -> "not in special maneuver"
            2 -> "in special maneuver"
            else -> "invalid special maneuver indicator"
        }
    }
}