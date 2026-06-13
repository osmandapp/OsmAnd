/*
 * TurnMode.java
 * Copyright (C) 2015 Paweł Kozioł, Kimmo Tuukkanen
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
package net.sf.marineapi.nmea.util

/**
 * Defines how the ship changes heading, as returned by HTC and HTD sentences.
 *
 * @author Paweł Kozioł
 * @see net.sf.marineapi.nmea.sentence.HTCSentence
 *
 * @see net.sf.marineapi.nmea.sentence.HTDSentence
 */
enum class TurnMode(private val character: Char) {
    /** Radius controlled  */
    RADIUS_CONTROLLED('R'),

    /** Turn rate controlled  */
    TURN_RATE_CONTROLLED('T'),

    /** Not controlled  */
    NOT_CONTROLLED('N');

    /**
     * Returns the character used in NMEA sentences to indicate the status.
     *
     * @return Char indicator for TurnMode
     */
    fun toChar(): Char {
        return character
    }

    companion object {
        /**
         * Returns the TurnMode enum for status char used in sentences.
         *
         * @param ch Status char
         * @return TurnMode
         */
        fun valueOf(ch: Char): TurnMode {
            for (tm in values()) {
                if (tm.toChar() == ch) {
                    return tm
                }
            }
            return valueOf(ch.toString())
        }
    }
}