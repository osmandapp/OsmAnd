/*
 * NavigationalStatus.java
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
 * Checks the navigational status for validity.
 *
 * @author Lázár József
 */
object NavigationalStatus {
    /** Valid range  */
    const val RANGE = "[0,8] + [14,15]"
    private val VALUES = arrayOf(
        "under way using engine",  // 0
        "at anchor",  // 1
        "not under command",  // 2
        "restricted manoeuvrability",  // 3
        "constrained by her draught",  // 4
        "moored",  // 5
        "aground",  // 6
        "engaged in fishing",  // 7
        "under way sailing",  // 8
        "reserved for future amendment",  // 9
        "reserved for future amendment",  //10
        "power driven vessel towing astern",  //11
        "power driven vessel pushing ahead or towing alongside",  //12
        "reserved for future use",  //13
        "AIS-SART MOB or EPIRB (active)",  //14
        "not defined" //15
    )

    /**
     * Returns the String representing the given status.
     *
     * @param value Navigational status value to stringify.
     * @return text string describing the navigational status
     */
    fun toString(value: Int): String {
        return if (value in 0..15) VALUES[value] else VALUES[15]
    }

    /**
     * Checks if the given status value is correct.
     *
     * @param value Navigational status value to check
     * @return true if the status falls within the range
     */
    fun isCorrect(value: Int): Boolean {
        return value in 0..15 && value != 9 && value != 10 && value != 13
    }
}