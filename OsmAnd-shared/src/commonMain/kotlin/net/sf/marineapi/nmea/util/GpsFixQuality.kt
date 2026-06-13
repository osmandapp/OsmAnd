/* 
 * GpsFixQuality.java
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
 * GpsFixQuality defines the supported fix quality types.
 *
 * @author Kimmo Tuukkanen
 * @see FaaMode
 *
 * @see GpsFixStatus
 *
 * @see DataStatus
 */
enum class GpsFixQuality(private val value: Int) {
    /** No GPS fix acquired.  */
    INVALID(0),

    /** Normal GPS fix, Standard Position Service (SPS).  */
    NORMAL(1),

    /** Differential GPS fix.  */
    DGPS(2),

    /** Precise Positioning Service fix.  */
    PPS(3),

    /** Real Time Kinematic  */
    RTK(4),

    /** Float RTK  */
    FRTK(5),

    /** Estimated, dead reckoning (2.3 feature)  */
    ESTIMATED(6),

    /** Manual input mode  */
    MANUAL(7),

    /** Simulation mode  */
    SIMULATED(8);

    /**
     * Returns the corresponding int indicator for fix quality.
     *
     * @return Fix quality indicator value as indicated in sentences.
     */
    fun toInt(): Int {
        return value
    }

    companion object {
        /**
         * Get GpsFixQuality enum that corresponds the actual integer identifier
         * used in the sentences.
         *
         * @param val Status identifier value
         * @return GpsFixQuality enum
         */
        fun valueOf(`val`: Int): GpsFixQuality {
            for (gfq in values()) {
                if (gfq.toInt() == `val`) {
                    return gfq
                }
            }
            return valueOf(`val`.toString())
        }
    }
}