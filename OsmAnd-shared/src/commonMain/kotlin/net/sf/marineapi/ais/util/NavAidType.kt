/*
 * NavAidType.java
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
 * Checks the NavAid type for validity.
 *
 * @author Henri Laurent
 */
object NavAidType {
    /**
     * Returns a text string for the NavAid.
     *
     * @param deviceType Device type value to Stringify.
     * @return a text string describing the Nav Aid type
     */
    fun toString(deviceType: Int): String {
        return when (deviceType) {
            0 -> "Default, Type of Aid to Navigation not specified"
            1 -> "Reference point"
            2 -> "RACON (radar transponder marking a navigation hazard)"
            3 -> "Fixed structure off shore, such as oil platforms, wind farms, rigs"
            4 -> "Spare, Reserved for future use"
            5 -> "Light, without sectors"
            6 -> "Light, with sectors"
            7 -> "Leading Light Front"
            8 -> "Leading Light Rear"
            9 -> "Beacon, Cardinal N"
            10 -> "Beacon, Cardinal E"
            11 -> "Beacon, Cardinal S"
            12 -> "Beacon, Cardinal W"
            13 -> "Beacon, Port hand"
            14 -> "Beacon, Starboard hand"
            15 -> "Beacon, Preferred Channel port hand"
            16 -> "Beacon, Preferred Channel starboard hand"
            17 -> "Beacon, Isolated danger"
            18 -> "Beacon, Safe water"
            19 -> "Beacon, Special mark"
            20 -> "Cardinal Mark N"
            21 -> "Cardinal Mark E"
            22 -> "Cardinal Mark S"
            23 -> "Cardinal Mark W"
            24 -> "Port hand Mark"
            25 -> "Starboard hand Mark"
            26 -> "Preferred Channel Port hand"
            27 -> "Preferred Channel Starboard hand"
            28 -> "Isolated danger"
            29 -> "Safe Water"
            30 -> "Special Mark"
            31 -> "Light Vessel / LANBY / Rigs"
            else -> "not used"
        }
    }
}