/* 
 * Waypoint.java
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
 * Waypoint represents a named geographic location.
 *
 * @author Kimmo Tuukkanen
 * @see net.sf.marineapi.nmea.util.Position
 */
class Waypoint : Position {
    /**
     * Get id of Waypoint
     *
     * @return id
     */
    /**
     * Set the id of Waypoint
     *
     * @param id the id to set
     */
    var id: String?
    /**
     * Gets the waypoint description/comment.
     *
     * @return the description
     */
    /**
     * Sets the waypoint description.
     *
     * @param description the description to set
     */
    var description = ""

    /**
     * Returns the time stamp when `Waypoint` was created.
     *
     * @return Date
     */
    val timeStamp = Date()

    /**
     * Creates a new instance of `Waypoint` with default WGS84 datum.
     *
     * @param id Waypoint identifier
     * @param lat Latitude degrees of the waypoint location
     * @param lon Longitude degrees of waypoint location
     */
    constructor(id: String?, lat: Double, lon: Double) : super(lat, lon) {
        this.id = id
    }

    /**
     * Creates a new instance of `Waypoint` with default WGS84 datum.
     *
     * @param id Waypoint identifier
     * @param lat Latitude degrees of the waypoint location
     * @param lon Longitude degrees of waypoint location
     * @param alt Altitude value, in meters above/below mean sea level
     */
    constructor(id: String?, lat: Double, lon: Double, alt: Double) : super(lat, lon, alt) {
        this.id = id
    }

    /**
     * Creates a new instance of Waypoint with explicitly specified datum.
     *
     * @param id Waypoint identifier
     * @param lat Latitude degrees of the waypoint location
     * @param lon Longitude degrees of waypoint location
     * @param datum Position datum, i.e. the coordinate system.
     */
    constructor(id: String?, lat: Double, lon: Double, datum: Datum) : super(lat, lon, datum) {
        this.id = id
    }

    /**
     * Creates a new instance of `Waypoint` with explicitly specified
     * datum.
     *
     * @param id Waypoint identifier/name
     * @param lat Latitude degrees of the waypoint location
     * @param lon Longitude degrees of waypoint location
     * @param alt Altitude value, in meters above/below mean sea level
     * @param datum Position datum, i.e. the coordinate system.
     */
    constructor(id: String?, lat: Double, lon: Double, alt: Double, datum: Datum) : super(lat, lon, alt, datum) {
        this.id = id
    }
}