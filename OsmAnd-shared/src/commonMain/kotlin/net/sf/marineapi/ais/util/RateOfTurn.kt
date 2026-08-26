/*
 * RateOfTurn.java
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

import kotlin.math.abs

/**
 * Checks a rate-of-turn value for validity.
 *
 * @author Lázár József
 */
object RateOfTurn {
    private const val DEFAULTVALUE = -0x80
    private const val MINVALUE = -126
    private const val MAXVALUE = 126

    /**
     * Checks if the ROT value is available.
     *
     * @param value Int value to check
     * @return true if the ROT is not the default value
     */
    fun isTurnInformationAvailable(value: Int): Boolean {
        return value != DEFAULTVALUE
    }

    /**
     * Checks if a turn indicator is available.
     *
     * @param value Int value to check
     * @return true if the turn indicator is available
     */
    fun isTurnIndicatorAvailable(value: Int): Boolean {
        return value in MINVALUE..MAXVALUE
    }

    /**
     * Converts the rate-of-turn value to a estimate degrees/minute value.
     *
     * @param value Int value to convert
     * @return degrees/minute value (positive sign indicates turning right)
     */
    fun toDegreesPerMinute(value: Int): Double {
        return if (isTurnIndicatorAvailable(value)) {
            val v = value / 4.733
            val v2 = v * v
            if (value < 0) -v2 else v2
        } else 0.0
    }

    /**
     * Converts given rate of turn value to String presentation.
     *
     * @param value Int value to stringify
     * @return string representation of the ROT information
     */
    fun toString(value: Int): String {
        val direction: String = if (value < 0) "left" else "right"
        return when (abs(value)) {
            128 -> "no turn information available (default)"
            127 -> "turning $direction at more than 5 degrees per 30 s (No TI available)"
            126 -> "turning $direction at 708 degrees per min or higher"
            0 -> "not turning"
            else -> "turning " + direction + " at " +
                    abs(toDegreesPerMinute(value)) + " degrees per min"
        }
    }
}