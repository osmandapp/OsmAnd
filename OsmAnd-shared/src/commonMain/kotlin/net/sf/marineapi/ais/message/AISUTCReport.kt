/*
 * AISUTCReport.java
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
package net.sf.marineapi.ais.message

/**
 * Common interface for all UTC time and position providing AIS messages.
 *
 * @author Lázár József
 */
interface AISUTCReport : AISMessage {
    /**
     * Returns the UTC year.
     * @return an integer value representing the UTC year (1-9999)
     */
    val utcYear: Int

    /**
     * Returns the UTC month.
     * @return an integer value representing the UTC month (1-12)
     */
    val utcMonth: Int

    /**
     * Returns the UTC day.
     * @return an integer value representing the UTC day (1-31)
     */
    val utcDay: Int

    /**
     * Returns the UTC hour.
     * @return an integer value representing the UTC hour (0-23)
     */
    val utcHour: Int

    /**
     * Returns the UTC minute.
     * @return an integer value representing the UTC minute (0-59)
     */
    val utcMinute: Int

    /**
     * Returns the UTC second.
     * @return an integer value representing the UTC second (0-59)
     */
    val utcSecond: Int

    /**
     * Returns the type of electronic position fixing device.
     * @return an integer value of the position device
     */
    val typeOfEPFD: Int
}