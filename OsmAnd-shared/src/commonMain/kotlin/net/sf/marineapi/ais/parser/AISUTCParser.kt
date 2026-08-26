/*
 * AISUTCParser.java
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

import net.sf.marineapi.ais.message.AISUTCReport
import net.sf.marineapi.ais.util.*

/**
 * AIS Base station and Mobile Station UTC reporting
 *
 * <pre>
 * Field  Name                                          Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1	  messageID                        		       	   6	(   1,   6)
 * 2	  repeatIndicator                         		   2	(   7,   8)
 * 3	  userID                                  		  30	(   9,  38)
 * 4	  utcYear                        		          14	(  39,  52)
 * 5	  utcMonth                            	    	   4	(  53,  56)
 * 6	  utcDay                                  		   5	(  57,  61)
 * 7	  utcHour                                 		   5	(  62,  66)
 * 8	  utcMinute                              		   6	(  67,  72)
 * 9	  utcSecond                          	    	   6	(  73,  78)
 * 10	  positionAccuracy                    	    	   1	(  79,  79)
 * 11	  longitude                               		  28	(  80, 107)
 * 12	  latitude                              		  27	( 108, 134)
 * 13	  typeOfElectronicPositionFixingDevice    		   4	( 135, 138)
 * 14	  transmissionControlForLongRangeBroadcastMessage  1	( 139, 139)
 * 15	  spare                                   	 	   9	( 140, 148)
 * 16	  raimFlag                              		   1	( 149, 149)
 * 17	  communicationState                      		  19	( 150, 168)
 * ---- +
 * sum  168
</pre> *
 *
 * @author Lázár József
 */
internal open class AISUTCParser(content: Sixbit) : AISMessageParser(content, 168), AISUTCReport {
    override val utcYear: Int = content.getInt(FROM[UTC_YEAR], TO[UTC_YEAR])
    override val utcMonth: Int = content.getInt(FROM[UTC_MONTH], TO[UTC_MONTH])
    override val utcDay: Int = content.getInt(FROM[UTC_DAY], TO[UTC_DAY])
    override val utcHour: Int = content.getInt(FROM[UTC_HOUR], TO[UTC_HOUR])
    override val utcMinute: Int = content.getInt(FROM[UTC_MINUTE], TO[UTC_MINUTE])
    override val utcSecond: Int = content.getInt(FROM[UTC_SECOND], TO[UTC_SECOND])
    val isAccurate: Boolean = content.getBoolean(FROM[POSITIONACCURACY])
    private val fLongitude: Int = content.getAs28BitInt(FROM[LONGITUDE], TO[LONGITUDE])
    private val fLatitude: Int = content.getAs27BitInt(FROM[LATITUDE], TO[LATITUDE])
    override val typeOfEPFD: Int = content.getInt(FROM[FIXING_DEV_TYPE], TO[FIXING_DEV_TYPE])

    /**
     * Constructor.
     *
     * @param content Six-bit message content.
     */
    init {
        if (!Longitude28.isCorrect(fLongitude)) addViolation(
            AISRuleViolation(
                "LongitudeInDegrees",
                fLongitude,
                Longitude28.RANGE
            )
        )
        if (!Latitude27.isCorrect(fLatitude)) addViolation(
            AISRuleViolation(
                "LatitudeInDegrees",
                fLatitude,
                Latitude27.RANGE
            )
        )
    }

    val longitudeInDegrees: Double
        get() = Longitude28.toDegrees(fLongitude)
    val latitudeInDegrees: Double
        get() = Latitude27.toDegrees(fLatitude)

    fun hasLongitude(): Boolean {
        return Longitude28.isAvailable(fLongitude)
    }

    fun hasLatitude(): Boolean {
        return Latitude27.isAvailable(fLatitude)
    }

    override fun toString(): String {
        var result = "\tYear:    $utcYear"
        result += SEPARATOR + "Month:   " + utcMonth
        result += SEPARATOR + "Day:     " + utcDay
        result += SEPARATOR + "Hour:    " + utcHour
        result += SEPARATOR + "Minute:  " + utcMinute
        result += SEPARATOR + "Sec:     " + utcSecond
        result += SEPARATOR + "Pos acc: " + (if (isAccurate) "high" else "low") + " accuracy"
        result += SEPARATOR + "Lon:     " + Longitude28.toString(fLongitude)
        result += SEPARATOR + "Lat:     " + Latitude27.toString(fLatitude)
        result += SEPARATOR + "EPFD:    " + PositioningDevice.toString(typeOfEPFD)
        return result
    }

    companion object {
        private const val SEPARATOR = "\n\t"
        private const val UTC_YEAR = 0
        private const val UTC_MONTH = 1
        private const val UTC_DAY = 2
        private const val UTC_HOUR = 3
        private const val UTC_MINUTE = 4
        private const val UTC_SECOND = 5
        private const val POSITIONACCURACY = 6
        private const val LONGITUDE = 7
        private const val LATITUDE = 8
        private const val FIXING_DEV_TYPE = 9
        private val FROM = intArrayOf(
            38, 52, 56, 61, 66, 72, 78, 79, 107, 134
        )
        private val TO = intArrayOf(
            52, 56, 61, 66, 72, 78, 79, 107, 134, 138
        )
    }
}