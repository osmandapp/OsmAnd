/*
 * SatelliteInfo.java
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
 * SatelliteInfo represents the information about a single GPS satellite
 * vehicle.
 *
 * @author Kimmo Tuukkanen
 * @author Gunnar Hillert
 * @see net.sf.marineapi.nmea.sentence.GSVSentence
 */
open class SatelliteInfo
/**
 * Creates a new instance of SatelliteInfo
 *
 * @param id Satellite ID
 * @param elevation Current elevation of the satellite
 * @param azimuth Current azimuth of the satellite
 * @param noise Current noise ratio of the satellite signal
 */(
    /**
     * Set the ID of satellite vehicle, for example "05".
     *
     * @param id the id to set
     */
    var id: String?, private var elevation: Int, private var azimuth: Int, private var noise: Int
) {
    /**
     * Get the ID of satellite vehicle, for example "05".
     *
     * @return ID String
     */
    /**
     * Get satellite azimuth, in degrees from true north (0..359).
     *
     * @return azimuth value
     */
    fun getAzimuth(): Int {
        return azimuth
    }

    /**
     * Get satellite elevation, in degrees (max. 90).
     *
     * @return elevation value
     */
    fun getElevation(): Int {
        return elevation
    }

    /**
     * Get satellite the signal noise ratio, in dB (0-99 dB).
     *
     * @return Noise ratio
     */
    fun getNoise(): Int {
        return noise
    }

    /**
     * Set satellite azimuth, in degrees from true north (0..359).
     *
     * @param azimuth the azimuth to set
     * @throws IllegalArgumentException If value is out of bounds 0..360 deg.
     */
    fun setAzimuth(azimuth: Int) {
        require(!(azimuth < 0 || azimuth > 360)) { "Value out of bounds 0..360 deg" }
        this.azimuth = azimuth
    }

    /**
     * Set satellite elevation, in degrees (max. 90).
     *
     * @param elevation the elevation to set
     * @throws IllegalArgumentException If value is out of bounds 0..90 deg.
     */
    fun setElevation(elevation: Int) {
        require(!(elevation < 0 || elevation > 90)) { "Value out of bounds 0..90 deg" }
        this.elevation = elevation
    }

    /**
     * Set the satellite signal noise ratio, in dB (0-99 dB).
     *
     * @param noise the noise to set
     * @throws IllegalArgumentException If value is out of bounds 0..99 dB.
     */
    fun setNoise(noise: Int) {
        require(!(noise < 0 || noise > 99)) { "Value out of bounds 0..99 dB" }
        this.noise = noise
    }

    override fun toString(): String {
        return "SatelliteInfo [id=${id?.padStart(3, ' ')}, elevation=${elevation.toString().padStart(3, '0')} deg, azimuth=${azimuth.toString().padStart(3, '0')} deg, noise=${noise.toString().padStart(2, '0')} db]"
    }
}