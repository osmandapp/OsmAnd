/*
 * AISMessage21Parser.java
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
package net.sf.marineapi.ais.parser

import net.sf.marineapi.ais.message.AISMessage21
import net.sf.marineapi.ais.util.*

/**
 * AIS Message 21 implementation: Aid-to-Navigation Report.
 *
 * This message is unusual in that it varies in length depending on the presence
 * and size of the Name Extension field. May vary between 272 and 360 bits.
 *
 * <pre>
 * Field  Name                                      Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1	  messageID                               	   6	(   1,   6)
 * 2	  repeatIndicator                         	   2	(   7,   8)
 * 3	  userID                                  	  30	(   9,  38)
 * 4	  aid_type                               	   5	(  39,  43)
 * 5	  name                                    	 120	( 44,  163)
 * 6	  positionAccuracy                        	   1	( 164, 164)
 * 7	  longitude                               	  28	( 165, 192)
 * 8	  latitude                                	  27	( 193, 219)
 * 9	  dimension                               	  30	( 220, 249)
 * 10	  typeOfElectronicPositionFixingDevice    	   4	( 250, 253)
 * 12	  timeStamp                               	   6	( 254, 259)
 * 13     off_position                                 1	( 260, 260)
 * 14     regional                                     8    ( 261, 268)
 * 15     raim                                         1    ( 269, 269)
 * 16     virtual_aid                                  1    ( 270, 270)
 * 17     assigned                                     1    ( 271, 271)
 * 18     spare                                        1    ( 272, 272)
 * 19	  name extension                          	  88	( 273, 360)
 * ---- +
 * sum 360
</pre> *
 *
 * @author Henri Laurent
 */
internal class AISMessage21Parser(content: Sixbit) : AISMessageParser(content, 272, 361), AISMessage21 {
    override val aidType: Int
    override val name: String?
    override val isAccurate: Boolean
    private val fLongitude: Int
    private val fLatitude: Int
    override val bow: Int
    override val stern: Int
    override val port: Int
    override val starboard: Int
    override val typeOfEPFD: Int
    override val utcSecond: Int
    override val offPositionIndicator: Boolean
    override val regional: Int
    override val rAIMFlag: Boolean
    override val virtualAidFlag: Boolean
    override val assignedModeFlag: Boolean
    override val nameExtension: String

    /**
     * Constructor.
     *
     * @param content Six-bit message content.
     */
    init {
        aidType = content.getInt(FROM[AIDTYPE], TO[AIDTYPE])
        name = content.getString(FROM[NAME], TO[NAME])
        isAccurate = content.getBoolean(TO[POSITIONACCURACY])
        fLongitude = content.getAs28BitInt(FROM[LONGITUDE], TO[LONGITUDE])
        if (!Longitude28.isCorrect(fLongitude)) addViolation(
            AISRuleViolation(
                "LongitudeInDegrees",
                fLongitude,
                Longitude28.RANGE
            )
        )
        fLatitude = content.getAs27BitInt(FROM[LATITUDE], TO[LATITUDE])
        if (!Latitude27.isCorrect(fLatitude)) addViolation(
            AISRuleViolation(
                "LatitudeInDegrees",
                fLatitude,
                Latitude27.RANGE
            )
        )
        bow = content.getInt(FROM[BOW], TO[BOW])
        stern = content.getInt(FROM[STERN], TO[STERN])
        port = content.getInt(FROM[PORT], TO[PORT])
        starboard = content.getInt(FROM[STARBOARD], TO[STARBOARD])
        typeOfEPFD = content.getInt(FROM[TYPEOFEPFD], TO[TYPEOFEPFD])
        utcSecond = content.getInt(FROM[UTC_SECOND], TO[UTC_SECOND])
        offPositionIndicator = content.getBoolean(TO[OFFPOSITIONINDICATOR])
        regional = content.getInt(FROM[REGIONAL], TO[REGIONAL])
        rAIMFlag = content.getBoolean(TO[RAIMFLAG])
        virtualAidFlag = content.getBoolean(TO[VIRTUALAIDFLAG])
        assignedModeFlag = content.getBoolean(TO[ASSIGNEDMODEFLAG])
        nameExtension = content.getString(FROM[NAMEEXTENSION], TO[NAMEEXTENSION]).trim { it <= ' ' }
    }

    override val longitudeInDegrees: Double
        get() = Longitude28.toDegrees(fLongitude)
    override val latitudeInDegrees: Double
        get() = Latitude27.toDegrees(fLatitude)

    override fun hasLongitude(): Boolean {
        return Longitude28.isAvailable(fLongitude)
    }

    override fun hasLatitude(): Boolean {
        return Latitude27.isAvailable(fLatitude)
    }

    override fun toString(): String {
        var result = "\tAid Type:      " + NavAidType.toString(aidType)
        result += SEPARATOR + "Name:      " + name
        result += SEPARATOR + "Pos acc: " + (if (isAccurate) "high" else "low") + " accuracy"
        result += SEPARATOR + "Lon:     " + Longitude28.toString(fLongitude)
        result += SEPARATOR + "Lat:     " + Latitude27.toString(fLatitude)
        val dim = "Bow: $bow, Stern: $stern, Port: $port, Starboard: $starboard [m]"
        result += SEPARATOR + "Dim:       " + dim
        result += SEPARATOR + "Sec:     " + utcSecond
        result += SEPARATOR + "Off Position Indicator: " + if (offPositionIndicator) "yes" else "no"
        result += SEPARATOR + "Regional:     " + regional
        result += SEPARATOR + "RAIM Flag: " + if (rAIMFlag) "yes" else "no"
        result += SEPARATOR + "Virtual Aid Flag: " + if (virtualAidFlag) "yes" else "no"
        result += SEPARATOR + "Assigned Mode Flag: " + if (assignedModeFlag) "yes" else "no"
        result += SEPARATOR + "Name Extension:      " + nameExtension
        return result
    }

    companion object {
        private const val SEPARATOR = "\n\t"
        private const val AIDTYPE = 0
        private const val NAME = 1
        private const val POSITIONACCURACY = 2
        private const val LONGITUDE = 3
        private const val LATITUDE = 4
        private const val BOW = 5
        private const val STERN = 6
        private const val PORT = 7
        private const val STARBOARD = 8
        private const val TYPEOFEPFD = 9
        private const val UTC_SECOND = 10
        private const val OFFPOSITIONINDICATOR = 11
        private const val REGIONAL = 12
        private const val RAIMFLAG = 13
        private const val VIRTUALAIDFLAG = 14
        private const val ASSIGNEDMODEFLAG = 15
        private const val SPARE = 16
        private const val NAMEEXTENSION = 17
        private val FROM =
            intArrayOf(38, 43, 163, 164, 192, 219, 228, 237, 243, 249, 253, 259, 260, 268, 269, 270, 271, 272)
        private val TO =
            intArrayOf(43, 163, 164, 192, 219, 228, 237, 243, 249, 253, 259, 260, 268, 269, 270, 271, 272, 360)
    }
}