/* 
 * DataStatus.java
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
 * DataStatus defines the validity of data being broadcasted by an NMEA device.
 *
 * @author Kimmo Tuukkanen
 */
enum class DataStatus(private val character: Char) {
    /** Valid data available. May also indicate boolean value `true`.  */
    ACTIVE('A'),

    /**
     * No valid data available. May also indicate boolean value
     * `false`.
     */
    VOID('V');

    /**
     * Returns the character used in NMEA sentences to indicate the status.
     *
     * @return Char indicator for DataStatus
     */
    fun toChar(): Char {
        return character
    }

    companion object {
        /**
         * Returns the DataStatus enum for status char used in sentences.
         *
         * @param ch Status char
         * @return DataStatus
         */
        fun valueOf(ch: Char): DataStatus {
            for (ds in values()) {
                if (ds.toChar() == ch) {
                    return ds
                }
            }
            return valueOf(ch.toString())
        }
    }
}