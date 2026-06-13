/* 
 * GpsFixStatus.java
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
 * GpsFixStatus defines the status of current GPS fix.
 *
 * @author Kimmo Tuukkanen
 * @see FaaMode
 *
 * @see GpsFixQuality
 *
 * @see DataStatus
 */
enum class GpsFixStatus(private val status: Int) {
    /** No GPS fix available  */
    GPS_NA(1),

    /** 2D GPS fix (lat/lon)  */
    GPS_2D(2),

    /** 3D GPS fix (lat/lon/alt)  */
    GPS_3D(3);

    /**
     * Returns the corresponding int value for fix status enum.
     *
     * @return Fix status integer values as in sentences
     */
    fun toInt(): Int {
        return status
    }

    companion object {
        /**
         * Returns the GpsFixStatus enum corresponding to actual int identifier used
         * in the sentences.
         *
         * @param val Fix status indentifier int
         * @return GpsFixStatus enum
         */
        fun valueOf(`val`: Int): GpsFixStatus {
            for (st in values()) {
                if (st.toInt() == `val`) {
                    return st
                }
            }
            return valueOf(`val`.toString())
        }
    }
}