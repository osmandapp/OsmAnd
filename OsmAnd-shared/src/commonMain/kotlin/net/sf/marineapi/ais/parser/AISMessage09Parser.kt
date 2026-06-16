/*
 * AISMessage09Parser.java
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

import net.sf.marineapi.ais.message.AISMessage09
import net.sf.marineapi.ais.util.*



/**
 * AIS Message 9 implementation: Standard SAR Aircraft Position Report
 *
 * <pre>
 * Field  Name                                      Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1	  messageID                               	   6	(   1,   6)
 * 2	  repeatIndicator                         	   2	(   7,   8)
 * 3	  userID                                  	  30	(   9,  38)
 * 4	  Altitude                               	  12	(  39,  50)
 * 5	  speedOverGround                         	  10	(  51,  60)
 * 6	  positionAccuracy                        	   1	(  61,  61)
 * 7	  longitude                               	  28	(  62,  89)
 * 8	  latitude                                	  27	(  90, 116)
 * 9	  courseOverGround                        	  12	( 117, 128)
 * 10	  timeStamp                               	   6	( 129, 134)
 * 11     regional                                     8    ( 135, 142)
 * 12	  dte                                    	   1	( 143, 143)
 * 13     spare                                        3    ( 144, 146)
 * 14     assigned                                     1    ( 147, 147)
 * 15     raim                                         1    ( 148, 148)
 * 16     radio                                       20    ( 149, 168)
 * ---- +
 * sum 168
</pre> *
 *
 * @author Henri Laurent
 */
internal class AISMessage09Parser(content: Sixbit) : AISMessageParser(content, 168), AISMessage09 {
    override val altitude: Int
    override val speedOverGround: Int
    override val isAccurate: Boolean
    private val fLongitude: Int
    private val fLatitude: Int
    private val fCOG: Int
    override val timeStamp: Int

    /**
     * Regional reserved (spare)
     *
     * @return Int value
     */
    val regional: Int
    override val dTEFlag: Boolean
    override val assignedModeFlag: Boolean
    override val rAIMFlag: Boolean
    override val radioStatus: Int

    /**
     * Constructor.
     *
     * @param content Sib-bit message content
     */
    init {
        altitude = content.getInt(FROM[ALTITUDE], TO[ALTITUDE])
        speedOverGround = content.getInt(FROM[SPEEDOVERGROUND], TO[SPEEDOVERGROUND])
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
        fCOG = content.getInt(FROM[COURSEOVERGROUND], TO[COURSEOVERGROUND])
        if (!Angle12.isCorrect(fCOG)) addViolation(AISRuleViolation("CourseOverGround", fCOG, Angle12.RANGE))
        timeStamp = content.getInt(FROM[TIMESTAMP], TO[TIMESTAMP])
        regional = content.getInt(FROM[REGIONAL], TO[REGIONAL])
        dTEFlag = content.getBoolean(TO[DTE])
        assignedModeFlag = content.getBoolean(TO[ASSIGNEDMODEFLAG])
        rAIMFlag = content.getBoolean(TO[RAIMFLAG])
        radioStatus = content.getInt(FROM[RADIOSTATUS], TO[RADIOSTATUS])
    }

    /**
     * Returns the String representation of speed over ground.
     *
     * @return formatted value, "no SOG" or ">=1022"
     */
    val sOGString: String
        get() {
            return SpeedOverGround.toString(speedOverGround)
        }
    override val longitudeInDegrees: Double
        get() = Longitude28.toDegrees(fLongitude)
    override val latitudeInDegrees: Double
        get() = Latitude27.toDegrees(fLatitude)
    override val courseOverGround: Double
        get() = Angle12.toDegrees(fCOG)

    override fun hasLongitude(): Boolean {
        return Longitude28.isAvailable(fLongitude)
    }

    override fun hasLatitude(): Boolean {
        return Latitude27.isAvailable(fLatitude)
    }

    override fun toString(): String {
        var result = "\tAlt:      $altitude"
        result += SEPARATOR + "SOG:     " + SpeedOverGround.toString(speedOverGround)
        result += SEPARATOR + "Pos acc: " + (if (isAccurate) "high" else "low") + " accuracy"
        result += SEPARATOR + "Lon:     " + Longitude28.toString(fLongitude)
        result += SEPARATOR + "Lat:     " + Latitude27.toString(fLatitude)
        result += SEPARATOR + "COG:     " + Angle12.toString(fCOG)
        result += SEPARATOR + "Time:    " + TimeStamp.toString(timeStamp)
        result += SEPARATOR + "Regional:     " + regional
        result += SEPARATOR + "DTE: " + if (dTEFlag) "yes" else "no"
        result += SEPARATOR + "Assigned Mode Flag: " + if (assignedModeFlag) "yes" else "no"
        result += SEPARATOR + "RAIM Flag: " + if (rAIMFlag) "yes" else "no"
        result += SEPARATOR + "RadioStatus:     " + radioStatus
        return result
    }

    companion object {
        private const val SEPARATOR = "\n\t"
        private const val ALTITUDE = 0
        private const val SPEEDOVERGROUND = 1
        private const val POSITIONACCURACY = 2
        private const val LONGITUDE = 3
        private const val LATITUDE = 4
        private const val COURSEOVERGROUND = 5
        private const val TIMESTAMP = 6
        private const val REGIONAL = 7 // spare 1
        private const val DTE = 8
        private const val SPARE = 9 // spare 2
        private const val ASSIGNEDMODEFLAG = 10
        private const val RAIMFLAG = 11
        private const val RADIOSTATUS = 12
        private val FROM = intArrayOf(38, 50, 60, 61, 89, 116, 128, 134, 142, 43, 146, 147, 149)
        private val TO = intArrayOf(50, 60, 61, 89, 116, 128, 134, 142, 43, 146, 147, 149, 167)
    }
}