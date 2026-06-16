/*
 * MMSI.java
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
 * Checks an MMSI value for validity.
 *
 * @author Lázár József
 */
object MMSI {
    private const val MINVALUE: Long = 0
    private const val MAXVALUE: Long = 999999999

    /**
     * Checks whether the MMSI value is correct, i.e. within valid range.
     *
     * @param value MMSI value to check
     * @return true if the value is semantically correct.
     */
    fun isCorrect(value: Long): Boolean {
        return value in MINVALUE..MAXVALUE
    }

    /**
     * Returns the origin associated with the MMSI number.
     *
     * @param value MMSI value to stringify
     * @return A String describing the region of the transmitter
     */
    fun toString(value: Long): String {
        return when ((value / 100000000L).toInt()) {
            0 -> "Ship group, coast station, or group of coast stations"
            1 -> "SAR aircraft"
            2 -> "Europe"
            3 -> "North and Central America and Caribbean"
            4 -> "Asia"
            5 -> "Oceana"
            6 -> "Africa"
            7 -> "South America"
            8 -> "Assigned for regional Use"
            9 -> "Nav aids or craft associated with a parent ship"
            else -> "Invalid MMSI number"
        }
    }
}