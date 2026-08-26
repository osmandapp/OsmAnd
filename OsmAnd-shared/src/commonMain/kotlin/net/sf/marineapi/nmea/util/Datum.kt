/* 
 * Datum.java
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
 * Defines the supported datums, i.e. the coordinate systems used to specify
 * geographic positions.
 *
 * @author Kimmo Tuukkanen
 * @see net.sf.marineapi.nmea.util.Position
 */
enum class Datum {
    /** World Geodetic System 1984, the default datum in GPS systems.  */
    WGS84,

    /** North American Datum 1983  */
    NAD83,

    /** North American Datum 1927  */
    NAD27
}