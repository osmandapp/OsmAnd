/*
 * PositioningDevice.java
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
 * Checks the positioning device type for validity.
 *
 * @author Lázár József
 */
object PositioningDevice {
    /**
     * Returns a text string for the EPFD.
     *
     * @param deviceType Device type value to Stringify.
     * @return a text string describing the positioning device type
     */
    fun toString(deviceType: Int): String {
        return when (deviceType) {
            0 -> "undefined device"
            1 -> "GPS"
            2 -> "GLONASS"
            3 -> "combined GPS/GLONASS"
            4 -> "Loran-C"
            5 -> "Chayka"
            6 -> "integrated navigation system"
            7 -> "surveyed"
            8 -> "Galileo"
            15 -> "internal GNSS"
            else -> "not used"
        }
    }
}