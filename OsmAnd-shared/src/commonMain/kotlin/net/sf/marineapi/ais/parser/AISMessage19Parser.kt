/*
 * AISMessage19Parser.java
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
package net.sf.marineapi.ais.parser

import net.sf.marineapi.ais.message.AISMessage19
import net.sf.marineapi.ais.util.PositioningDevice
import net.sf.marineapi.ais.util.ShipType
import net.sf.marineapi.ais.util.Sixbit

/**
 * AIS Message 19 implementation: Extended Class B Equipment Position Report.
 *
 * The first part of the message is handled by AISPositionReportBParser.
 *
 * <pre>
 * Field   Name                                    Bits    (from, to )
 * ------------------------------------------------------------------------
 * 12      spare2                                     4    ( 140, 143)
 * 13      name                                     120    ( 144, 263)
 * 14      typeOfShipAndCargoType                     8    ( 264, 271)
 * 15      dimension                                 30    ( 272, 301)
 * 16      typeOfElectronicPositionFixingDevice       4    ( 302, 305)
 * 17      raimFlag                                   1    ( 306, 306)
 * 18      dte                                        1    ( 307, 307)
 * 19      assignedModeFlag                           1    ( 308, 308)
 * 20      spare3                                     4    ( 309, 312)
 * ---- +
 * sum  312
</pre> *
 *
 * @author Lázár József
 */
internal class AISMessage19Parser(content: Sixbit) : AISPositionReportBParser(content, 312), AISMessage19 {
    override val name: String?
    override val typeOfShipAndCargoType: Int
    override val bow: Int
    override val stern: Int
    override val port: Int
    override val starboard: Int
    override val typeOfEPFD: Int

    /**
     * Constructor.
     *
     * @param content Six-bit message content.
     */
    init {
        name = content.getString(FROM[NAME], TO[NAME])
        typeOfShipAndCargoType = content.getInt(FROM[TYPEOFSHIPANDCARGO], TO[TYPEOFSHIPANDCARGO])
        bow = content.getInt(FROM[BOW], TO[BOW])
        stern = content.getInt(FROM[STERN], TO[STERN])
        port = content.getInt(FROM[PORT], TO[PORT])
        starboard = content.getInt(FROM[STARBOARD], TO[STARBOARD])
        typeOfEPFD = content.getInt(FROM[TYPEOFEPFD], TO[TYPEOFEPFD])
    }

    override fun toString(): String {
        var result = super.toString()
        result += SEPARATOR + "Name:    " + name
        result += SEPARATOR + "Type:    " + ShipType.shipTypeToString(
            typeOfShipAndCargoType
        )
        val dim = "Bow: " + bow + ", Stern: " + stern +
                ", Port: " + port + ", Starboard: " + starboard + " [m]"
        result += SEPARATOR + "Dim:     " + dim
        result += SEPARATOR + "EPFD:    " + PositioningDevice.toString(typeOfEPFD)
        return result
    }

    companion object {
        private const val NAME = 0
        private const val TYPEOFSHIPANDCARGO = 1
        private const val BOW = 2
        private const val STERN = 3
        private const val PORT = 4
        private const val STARBOARD = 5
        private const val TYPEOFEPFD = 6
        private val FROM = intArrayOf(
            143, 263, 271, 280, 289, 295, 301
        )
        private val TO = intArrayOf(
            263, 271, 280, 289, 295, 301, 305
        )
    }
}