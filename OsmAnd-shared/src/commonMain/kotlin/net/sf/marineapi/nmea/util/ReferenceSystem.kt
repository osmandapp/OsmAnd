/* 
 * ReferenceSystem.java
 * Copyright (C) 2020 Joshua Sweaney
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
 * Defines the various reference systems that can be used to calculate
 * a vessel's kinematics such as speed and course.
 *
 * @see net.sf.marineapi.nmea.sentence.OSDSentence
 *
 *
 * @author Joshua Sweaney
 */
enum class ReferenceSystem(private val ch: Char) {
    BOTTOM_TRACKING_LOG('B'), MANUALLY_ENTERED('M'), WATER_REFERENCED('W'), RADAR_TRACKING('R'), POSITIONING_SYSTEM_GROUND_REFERENCE(
        'P'
    );

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
         * @param ch Char indicator for reference
         * @return ReferenceSystem enum
         */
        fun valueOf(ch: Char): ReferenceSystem {
            for (r in values()) {
                if (r.toChar() == ch) {
                    return r
                }
            }
            return valueOf(ch.toString())
        }
    }
}