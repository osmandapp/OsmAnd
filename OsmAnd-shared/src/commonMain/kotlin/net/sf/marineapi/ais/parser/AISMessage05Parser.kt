/*
 * AISMessage05Parser.java
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

import net.sf.marineapi.ais.message.AISMessage05
import net.sf.marineapi.ais.util.PositioningDevice
import net.sf.marineapi.ais.util.ShipType
import net.sf.marineapi.ais.util.Sixbit

/**
 * AIS Message 5 implementation: Ship Static and Voyage Related Data.
 *
 * <pre>
 * Field  Name                                      Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1	  messageID                               	   6	(   1,   6)
 * 2	  repeatIndicator                         	   2	(   7,   8)
 * 3	  userID                                  	  30	(   9,  38)
 * 4	  aisVersionIndicator                     	   2	(  39,  40)
 * 5	  imoNumber                               	  30	(  41,  70)
 * 6	  callSign                                	  42	(  71, 112)
 * 7	  name                                    	 120	( 113, 232)
 * 8	  typeOfShipAndCargoType                  	   8	( 233, 240)
 * 9	  dimension                               	  30	( 241, 270)
 * 10	  typeOfElectronicPositionFixingDevice    	   4	( 271, 274)
 * 11	  eta month                                    4	( 275, 278)
 * 11	  eta day                                      5	( 279, 283)
 * 11	  eta hour                                 	   5	( 284, 288)
 * 11	  eta minute                                   6	( 289, 294)
 * 12	  maximumPresentStaticDraught             	   8	( 295, 302)
 * 13	  destination                             	 120	( 303, 422)
 * 14	  dte                                     	   1	( 423, 423)
 * 15	  spare                                   	   1	( 424, 424)
 * ---- +
 * sum 424
</pre> *
 *
 * @author Lázár József
 */
internal class AISMessage05Parser(content: Sixbit) : AISMessageParser(content, 424), AISMessage05 {
    override val aISVersionIndicator: Int
    override val iMONumber: Int
    override val callSign: String
    override val name: String
    override val typeOfShipAndCargoType: Int
    override val bow: Int
    override val stern: Int
    override val port: Int
    override val starboard: Int
    override val typeOfEPFD: Int
    override val eTAMinute: Int
    override val eTAHour: Int
    override val eTADay: Int
    override val eTAMonth: Int
    private val fMaximumDraught: Int
    override val destination: String
    override val isDteReady: Boolean

    /**
     * Constructor.
     *
     * @param content Six-bit message content to parse.
     */
    init {
        aISVersionIndicator = content.getInt(FROM[AISVERSION], TO[AISVERSION])
        iMONumber = content.getInt(FROM[IMONUMBER], TO[IMONUMBER])
        callSign = content.getString(FROM[CALLSIGN], TO[CALLSIGN]).trim { it <= ' ' }
        name = content.getString(FROM[NAME], TO[NAME]).trim { it <= ' ' }
        typeOfShipAndCargoType = content.getInt(FROM[TYPEOFSHIPANDCARGO], TO[TYPEOFSHIPANDCARGO])
        bow = content.getInt(FROM[BOW], TO[BOW])
        stern = content.getInt(FROM[STERN], TO[STERN])
        port = content.getInt(FROM[PORT], TO[PORT])
        starboard = content.getInt(FROM[STARBOARD], TO[STARBOARD])
        typeOfEPFD = content.getInt(FROM[TYPEOFEPFD], TO[TYPEOFEPFD])
        eTAMonth = content.getInt(FROM[MONTH], TO[MONTH])
        eTADay = content.getInt(FROM[DAY], TO[DAY])
        eTAHour = content.getInt(FROM[HOUR], TO[HOUR])
        eTAMinute = content.getInt(FROM[MINUTE], TO[MINUTE])
        fMaximumDraught = content.getInt(FROM[DRAUGHT], TO[DRAUGHT])
        destination = content.getString(FROM[DESTINATION], TO[DESTINATION]).trim { it <= ' ' }
        isDteReady = content.getBoolean(TO[DTE])
    }

    override val maximumDraught: Double
        get() = fMaximumDraught / 10.0

    override fun toString(): String {
        var result = "\tIMO:       $iMONumber"
        result += SEPARATOR + "Call sign: " + callSign
        result += SEPARATOR + "Name:      " + name
        result += SEPARATOR + "Type:      " + ShipType.shipTypeToString(typeOfShipAndCargoType)
        val dim = "Bow: " + bow + ", Stern: " + stern +
                ", Port: " + port + ", Starboard: " + starboard + " [m]"
        result += SEPARATOR + "Dim:       " + dim
        result += SEPARATOR + "ETA:       " + "Month: " + eTAMonth + ", D: " + eTADay +
                ", H: " + eTAHour + ", M: " + eTAMinute
        result += SEPARATOR + "Draft:     " + (fMaximumDraught / 10f).toString()
        result += SEPARATOR + "EPFD:      " + PositioningDevice.toString(typeOfEPFD)
        result += SEPARATOR + "Dest:      " + destination
        result += SEPARATOR + "DTE:       " + isDteReady
        return result
    }

    companion object {
        private const val SEPARATOR = "\n\t"
        private const val AISVERSION = 0
        private const val IMONUMBER = 1
        private const val CALLSIGN = 2
        private const val NAME = 3
        private const val TYPEOFSHIPANDCARGO = 4
        private const val BOW = 5
        private const val STERN = 6
        private const val PORT = 7
        private const val STARBOARD = 8
        private const val TYPEOFEPFD = 9
        private const val MONTH = 10
        private const val DAY = 11
        private const val HOUR = 12
        private const val MINUTE = 13
        private const val DRAUGHT = 14
        private const val DESTINATION = 15
        private const val DTE = 16
        private val FROM = intArrayOf(
            38, 40, 70, 112, 232, 240, 249, 258, 264, 270, 274, 278, 283, 288, 294, 302
        )
        private val TO = intArrayOf(
            40, 70, 112, 232, 240, 249, 258, 264, 270, 274, 278, 283, 288, 294, 302, 422, 423
        )
    }
}