/*
 * AISPositionReport.java
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
 *
 *
 * Common interface for all messages providing position reports.
 *
 *
 * See [Class A AIS
 * position report](https://www.navcen.uscg.gov/?pageName=AISMessagesA) specification for more details on return values and
 * status codes.
 * @author Lázár József
 */
interface AISPositionReport : AISPositionReportB {
    /**
     * Returns the navigational status.
     *
     * @return The navigational status indicator between 0 - 15
     */
    val navigationalStatus: Int

    /**
     * Returns the rate of turn.
     *
     * @return Rate of turn, in degrees per min
     */
    val rateOfTurn: Double

    /**
     * Returns the manouver indicator.
     *
     * @return 0 = not available = default, 1 = not engaged in special maneuver
     * or 2 = engaged in special maneuver
     */
    val manouverIndicator: Int

    /**
     * Returns true if rate of turn is available in the message.
     *
     * @return true if has ROT, otherwise false.
     */
    fun hasRateOfTurn(): Boolean
}