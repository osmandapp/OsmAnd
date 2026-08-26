/*
 * AISMessage09.java
 * Copyright (C) 2016 Henri Laurent
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
package net.sf.marineapi.ais.message

/**
 * Standard SAR Aircraft Position Report
 *
 * Tracking information for search-and-rescue aircraft.
 *
 * Total number of bits is 168.
 *
 * @author Henri Laurent
 */
interface AISMessage09 : AISPositionInfo {
    /**
     * Returns the altitude of the aircraft. The special value 4095 indicates
     * altitude is not available; 4094 indicates 4094 meters or higher.
     *
     * @return Altitude, in meters.
     */
    val altitude: Int

    /**
     * Returns the speed over ground. Not deciknots as in the common navigation
     * block; planes go faster. The special value 1023 indicates speed not
     * available, 1022 indicates 1022 knots or higher.
     *
     * @return Speed over ground, in knots.
     */
    val speedOverGround: Int

    /**
     * Returns the course over ground.
     *
     * @return Course over ground, in degrees.
     */
    val courseOverGround: Double

    /**
     * Returns the UTC second.
     *
     * @return An integer value representing the UTC second (0-59)
     */
    val timeStamp: Int

    /**
     * Data terminal ready (0 = available 1 = not available = default)
     *
     * @return `true` if available, otherwise false.
     */
    val dTEFlag: Boolean

    /**
     * Returns the Assigned-mode flag
     *
     * @return `true` if assigned mode, otherwise `false`.
     */
    val assignedModeFlag: Boolean

    /**
     * Returns the RAIM flag.
     *
     * @return `true` if RAIM in use, otherwise `false`.
     */
    val rAIMFlag: Boolean

    /**
     * Returns the Radio status.
     *
     * @return Radio status int
     */
    val radioStatus: Int
}