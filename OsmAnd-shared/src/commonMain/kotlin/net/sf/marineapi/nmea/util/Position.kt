/* 
 * Position.java
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


import kotlin.math.*

/**
 * Represents a geographic position. Default datum is WGS84 as generally in NMEA
 * 0183. Notice that datum is only informative and it does not affect
 * calculations or handling of other values.
 *
 * @author Kimmo Tuukkanen
 */
open class Position(lat: Double, lon: Double) {
    /**
     * Get latitude value of Position
     *
     * @return latitude degrees
     */
    /**
     * Set the latitude degrees of Position
     *
     * @param latitude the latitude to set
     * @throws IllegalArgumentException If specified latitude value is out of
     * range 0..90 degrees.
     */
    // latitude degrees
    var latitude = 0.0
        set(latitude) {
            require(!(latitude < -90 || latitude > 90)) { "Latitude out of bounds -90..90 degrees" }
            field = latitude
        }
    /**
     * Get longitude value of Position
     *
     * @return longitude degrees
     */
    /**
     * Set the longitude degrees of Position
     *
     * @param longitude the longitude to set
     * @throws IllegalArgumentException If specified longitude value is out of
     * range 0..180 degrees.
     */
    // longitude degrees
    var longitude = 0.0
        set(longitude) {
            require(!(longitude < -180 || longitude > 180)) { "Longitude out of bounds -180..180 degrees" }
            field = longitude
        }
    /**
     * Gets the position altitude from mean sea level. Notice that most
     * sentences with position don't provide this value. When missing, the
     * default value in `Position` is 0.0.
     *
     * @return Altitude value in meters
     */
    /**
     * Sets the altitude of position above or below mean sea level. Defaults to
     * zero (-0.0).
     *
     * @param altitude Altitude value to set, in meters.
     */
    // altitude
    var altitude = 0.0

    /**
     * Gets the datum, i.e. the coordinate system used to define geographic
     * position. Default is [Datum.WGS84], unless datum is specified in
     * the constructor. Notice also that datum cannot be set afterwards.
     *
     * @return Datum enum
     */
    // datum/coordinate system
    var datum = Datum.WGS84
        private set

    /**
     * Creates a new instance of Position. Notice that altitude defaults to -0.0
     * and may be set later.
     *
     * @param lat Latitude degrees
     * @param lon Longitude degrees
     * @see .setAltitude
     */
    init {
        latitude = lat
        longitude = lon
    }

    /**
     * Creates a new instance of position with latitude, longitude and altitude.
     *
     * @param lat Latitude degrees
     * @param lon Longitude degrees
     * @param alt Altitude value, in meters.
     */
    constructor(lat: Double, lon: Double, alt: Double) : this(lat, lon) {
        altitude = alt
    }

    /**
     * Creates new instance of Position with latitude, longitude and datum.
     * Notice that altitude defaults to -0.0 and may be set later.
     *
     * @param lat Latitude degrees
     * @param lon Longitude degrees
     * @param datum Datum to set
     * @see .setAltitude
     */
    constructor(lat: Double, lon: Double, datum: Datum) : this(lat, lon) {
        this.datum = datum
    }

    /**
     * Creates new instance of Position with latitude, longitude, altitude and
     * datum.
     *
     * @param lat Latitude degrees
     * @param lon Longitude degrees
     * @param alt Altitude in meters
     * @param datum Datum to set
     */
    constructor(lat: Double, lon: Double, alt: Double, datum: Datum) : this(lat, lon, alt) {
        this.datum = datum
    }

    /**
     * Calculates distance to specified `Position`.
     *
     *
     * The Distance is calculated using the [Haversine
 * formula](http://en.wikipedia.org/wiki/Haversine_formula). Implementation is based on example found at [codecodex.com](http://www.codecodex.com/wiki/Calculate_Distance_Between_Two_Points_on_a_Globe).
     *
     *
     * Earth radius [earth
 * radius](http://en.wikipedia.org/wiki/Earth_radius#Mean_radius) used in calculation is `6366.70702` km, based on
     * the assumption that 1 degrees is exactly 60 NM.
     *
     * @param pos Position to which the distance is calculated.
     * @return Distance to po`pos` in meters.
     */
    fun distanceTo(pos: Position): Double {
        return haversine(
            latitude, longitude, pos.latitude,
            pos.longitude
        )
    }

    /**
     * Get the hemisphere of latitude, North or South.
     *
     * @return CompassPoint.NORTH or CompassPoint.SOUTH
     */
    val latitudeHemisphere: CompassPoint
        get() = if (isLatitudeNorth) CompassPoint.NORTH else CompassPoint.SOUTH

    /**
     * Get the hemisphere of longitude, East or West.
     *
     * @return CompassPoint.EAST or CompassPoint.WEST
     */
    val longitudeHemisphere: CompassPoint
        get() = if (isLongitudeEast) CompassPoint.EAST else CompassPoint.WEST

    /**
     * Tells if the latitude is on northern hemisphere.
     *
     * @return true if northern, otherwise false (south).
     */
    val isLatitudeNorth: Boolean
        get() = latitude >= 0.0

    /**
     * Tells if the longitude is on eastern hemisphere.
     *
     * @return true if eastern, otherwise false (west).
     */
    val isLongitudeEast: Boolean
        get() = longitude >= 0.0

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[")
        
        val latVal = abs(latitude)
        val latInt = latVal.toInt()
        val latFrac = ((latVal - latInt) * 10000000).toInt()
        val latStr = latInt.toString().padStart(2, '0') + "." + latFrac.toString().padStart(7, '0')
        sb.append(latStr)
        
        sb.append(" ")
        sb.append(latitudeHemisphere.toChar())
        sb.append(", ")
        
        val lonVal = abs(longitude)
        val lonInt = lonVal.toInt()
        val lonFrac = ((lonVal - lonInt) * 10000000).toInt()
        val lonStr = lonInt.toString().padStart(3, '0') + "." + lonFrac.toString().padStart(7, '0')
        sb.append(lonStr)
        
        sb.append(" ")
        sb.append(longitudeHemisphere.toChar())
        sb.append(", ")
        sb.append(altitude)
        sb.append(" m]")
        return sb.toString()
    }

    /**
     * Convenience method for creating a waypoint based in the Position.
     *
     * @param id Waypoint ID or name
     * @return the created Waypoint
     */
    fun toWaypoint(id: String?): Waypoint {
        return Waypoint(id, latitude, longitude)
    }

    /**
     * Haversine formulae, implementation based on example at [codecodex](http://www.codecodex.com/wiki/Calculate_Distance_Between_Two_Points_on_a_Globe).
     *
     * @param lat1 Origin latitude
     * @param lon1 Origin longitude
     * @param lat2 Destination latitude
     * @param lon2 Destination longitude
     * @return Distance in meters
     */
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {

        // Mean earth radius (IUGG) = 6371.009
        // Meridional earth radius = 6367.4491
        // Earth radius by assumption that 1 degree equals exactly 60 NM:
        // 1.852 * 60 * 360 / (2 * Pi) = 6366.7 km
        val earthRadius = 6366.70702
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = (sin(dLat / 2) * sin(dLat / 2)
                + (cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0)
                * sin(dLon / 2) * sin(dLon / 2)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c * 1000
    }
}