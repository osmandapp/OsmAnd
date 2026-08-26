/*
 * AISMessage.java
 * Copyright (C) 2015 Kimmo Tuukkanen
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
 * Common base interface of AIS messages.
 *
 * @author Kimmo Tuukkanen
 */
interface AISMessage {
    /**
     * Returns the message type.
     *
     * @return Message type in range from 1 to 27.
     */
    val messageType: Int

    /**
     * Tells how many times the message has been repeated.
     *
     * @return the number of repeats
     */
    val repeatIndicator: Int

    /**
     * Returns the unique identifier of the transmitting ship (MMSI, Maritime
     * Mobile Service Identity).
     *
     * @return MMSI identifier
     */
    val mMSI: Int
}