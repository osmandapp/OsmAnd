/*
 * TimeStamp.java
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
 * Checks a 6-bit integer time stamp value for validity.
 *
 * @author Lázár József
 */
object TimeStamp {
    private const val MINVALUE = 0
    private const val MAXVALUE = 59

    /**
     * Checks if the time stamp value is available.
     *
     * @param value Timetamp value to check
     * @return true if the time stamp falls within a range
     */
    fun isAvailable(value: Int): Boolean {
        return value in MINVALUE..MAXVALUE
    }

    /**
     * Return a string representing the time stamp value.
     *
     * @param value Timetamp value to stringify
     * @return a string representing the time stamp
     */
    fun toString(value: Int): String {
        return when (value) {
            60 -> "no time stamp"
            61 -> "positioning system manual"
            62 -> "dead reckoning"
            63 -> "positioning system inoperative"
            else -> value.toString()
        }
    }
}