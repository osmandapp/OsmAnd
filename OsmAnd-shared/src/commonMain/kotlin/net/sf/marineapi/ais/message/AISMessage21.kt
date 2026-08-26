/*
 * AISMessage21.java
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
 * Aid-to-Navigation Report
 *
 * Identification and location message to be emitted by aids to navigation such as buoys and lighthouses.
 *
 * This message is unusual in that it varies in length depending on the presence and size of the Name Extension field.
 * May vary between 272 and 360 bits
 *
 * @author Henri Laurent
 */
interface AISMessage21 : AISPositionInfo {
    /**
     * Returns the Aid type for the current message.
     * @return Aid type
     */
    val aidType: Int

    /**
     * Returns the name of the transmitting ship.
     * @return maximum 20 characters, representing the name
     */
    val name: String?

    /**
     * Returns the distance from the reference point to the bow.
     *
     * @return Distance to bow, in meters.
     */
    val bow: Int

    /**
     * Returns the distance from the reference point to the stern of the ship.
     *
     * @return Distance to stern, in meters.
     */
    val stern: Int

    /**
     * Returns the distance from the reference point to the port side of the
     * ship.
     *
     * @return Distance to port side, in meters.
     */
    val port: Int

    /**
     * Returns the distance from the reference point to the starboard side of
     * the ship.
     *
     * @return Distance to starboard side, in meters.
     */
    val starboard: Int

    /**
     * Returns the type of electronic position fixing device.
     *
     * @return an integer value of the position device
     */
    val typeOfEPFD: Int

    /**
     * Returns the UTC second.
     *
     * @return an integer value representing the UTC second (0-59)
     */
    val utcSecond: Int

    /**
     * Returns the Off-position indicator: 0 means on position; 1 means off
     * position. Only valid if UTC second is equal to or below 59.
     *
     * @return `true` if off-position, otherwise `false`.
     */
    val offPositionIndicator: Boolean

    /**
     * Returns a Regional integer (reserved)
     *
     * @return an integer value
     */
    val regional: Int

    /**
     * Returns the RAIM flag
     *
     * @return `true` if RAIM in use, otherwise `false`.
     */
    val rAIMFlag: Boolean

    /**
     * Returns the Virtual-aid flag
     *
     * @return `true` if virtual, otherwise `false`
     */
    val virtualAidFlag: Boolean

    /**
     * Returns the Assigned-mode flag
     *
     * @return `true` if assigned, otherwise `false`.
     */
    val assignedModeFlag: Boolean

    /**
     * Returns the name extension.
     *
     * @return maximum 14 characters, representing the name extension
     */
    val nameExtension: String
}