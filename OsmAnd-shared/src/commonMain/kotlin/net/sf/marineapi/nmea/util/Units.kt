/* 
 * Units.java
 * Copyright (C) 2010 Kimmo Tuukkanen
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
 * Defines the supported units of measure.
 *
 * @author Kimmo Tuukkanen
 */
enum class Units(private val ch: Char) {
    /** Pressure in bars  */
    BARS('B'),

    /** Temperature in degrees Celsius (centigrade)  */
    CELSIUS('C'),

    /** Depth in fathoms  */
    FATHOMS('F'),

    /** Length in feet  */
    FEET('f'),

    /** Distance/pressure in inches   */
    INCHES('I'),

    /** Kilometers - used for distance, and speed (as kilometers per hour)  */
    KILOMETERS('K'),

    /** Length in meter  */
    METER('M'),

    /** Nautical miles - used for distance, and for speed (nautical miles per hour, which are knots)  */
    NAUTICAL_MILES('N'),

    /** Statute miles - used for distance, and for speed (as miles per hour/mph)  */
    STATUTE_MILES('S');

    /**
     * Returns the corresponding char constant.
     *
     * @return Char indicator of enum
     */
    fun toChar(): Char {
        return ch
    }

    companion object {
        /**
         * Get the enum corresponding to specified char.
         *
         * @param ch Char indicator for unit
         * @return Units enum
         */
        fun valueOf(ch: Char): Units {
            for (u in values()) {
                if (u.toChar() == ch) {
                    return u
                }
            }
            return valueOf(ch.toString())
        }
    }
}