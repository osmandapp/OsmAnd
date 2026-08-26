/*
 * AISPositionReportParser.java
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

import net.sf.marineapi.ais.message.AISPositionReport
import net.sf.marineapi.ais.util.*

/**
 * Parser for all position report messages.
 *
 * <pre>
 * Field Name                                    Bits    (from,  to)
 * ------------------------------------------------------------------------
 * 1    messageID                                  6    (   1,   6)
 * 2    repeatIndicator                            2    (   7,   8)
 * 3    userID                                    30    (   9,  38)
 * 4    navigationalStatus                         4    (  39,  42)
 * 5    rateOfTurn                                 8    (  43,  50)
 * 6    speedOverGround                           10    (  51,  60)
 * 7    positionAccuracy                           1    (  61,  61)
 * 8    longitude                                 28    (  62,  89)
 * 9    latitude                                  27    (  90, 116)
 * 10    courseOverGround                          12    ( 117, 128)
 * 11    trueHeading                                9    ( 129, 137)
 * 12    timeStamp                                  6    ( 138, 143)
 * 13    specialManoeuvre                           2    ( 144, 145)
 * 14    spare                                      3    ( 146, 148)
 * 15    raimFlag                                   1    ( 149, 149)
 * 16    communicationState                        19    ( 150, 168)
 * ---- +
 * sum   168
</pre> *
 *
 * @author Lázár József
 */
internal open class AISPositionReportParser(content: Sixbit) : AISMessageParser(content, 168, 204), AISPositionReport {
    override val navigationalStatus: Int = content.getInt(FROM[NAVIGATIONALSTATUS], TO[NAVIGATIONALSTATUS])
    private val fRateOfTurn: Int = content.getAs8BitInt(FROM[RATEOFTURN], TO[RATEOFTURN])
    private val fSOG: Int = content.getInt(FROM[SPEEDOVERGROUND], TO[SPEEDOVERGROUND])
    override val isAccurate: Boolean = content.getBoolean(TO[POSITIONACCURACY])
    private val fLongitude: Int = content.getAs28BitInt(FROM[LONGITUDE], TO[LONGITUDE])
    private val fLatitude: Int = content.getAs27BitInt(FROM[LATITUDE], TO[LATITUDE])
    private val fCOG: Int = content.getInt(FROM[COURSEOVERGROUND], TO[COURSEOVERGROUND])
    override val trueHeading: Int = content.getInt(FROM[TRUEHEADING], TO[TRUEHEADING])
    override val timeStamp: Int = content.getInt(FROM[TIMESTAMP], TO[TIMESTAMP])
    override val manouverIndicator: Int = content.getInt(FROM[MANOEUVER], TO[MANOEUVER])

    /**
     * Constructs an AIS Message Position Report parser.
     *
     * @param content Six-bit message content.
     */
    init {
        if (!NavigationalStatus.isCorrect(navigationalStatus)) addViolation(
            AISRuleViolation(
                "NavigationalStatus",
                navigationalStatus,
                NavigationalStatus.RANGE
            )
        )

        // FIXME check indices, should be 61-61?
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
        if (!Angle12.isCorrect(fCOG)) addViolation(AISRuleViolation("CourseOverGround", fCOG, Angle12.RANGE))
        if (!Angle9.isCorrect(trueHeading)) addViolation(AISRuleViolation("TrueHeading", trueHeading, Angle9.RANGE))
        if (!ManeuverIndicator.isCorrect(manouverIndicator)) addViolation(
            AISRuleViolation(
                "ManouverIndicator",
                manouverIndicator,
                ManeuverIndicator.RANGE
            )
        )
    }

    override val rateOfTurn: Double
        get() = RateOfTurn.toDegreesPerMinute(fRateOfTurn)
    override val speedOverGround: Double
        get() = SpeedOverGround.toKnots(fSOG)
    override val longitudeInDegrees: Double
        get() = Longitude28.toDegrees(fLongitude)
    override val latitudeInDegrees: Double
        get() = Latitude27.toDegrees(fLatitude)
    override val courseOverGround: Double
        get() = Angle12.toDegrees(fCOG)

    override fun hasRateOfTurn(): Boolean {
        return RateOfTurn.isTurnIndicatorAvailable(fRateOfTurn)
    }

    override fun hasSpeedOverGround(): Boolean {
        return SpeedOverGround.isAvailable(fSOG)
    }

    override fun hasCourseOverGround(): Boolean {
        return Angle12.isAvailable(fCOG)
    }

    override fun hasTrueHeading(): Boolean {
        return Angle9.isAvailable(trueHeading)
    }

    override fun hasTimeStamp(): Boolean {
        return TimeStamp.isAvailable(timeStamp)
    }

    override fun hasLongitude(): Boolean {
        return Longitude28.isAvailable(fLongitude)
    }

    override fun hasLatitude(): Boolean {
        return Latitude27.isAvailable(fLatitude)
    }

    override fun toString(): String {
        var result = "\tNav st:  " + NavigationalStatus.toString(navigationalStatus)
        result += SEPARATOR + "ROT:     " + RateOfTurn.toString(fRateOfTurn)
        result += SEPARATOR + "SOG:     " + SpeedOverGround.toString(fSOG)
        result += SEPARATOR + "Pos acc: " + (if (isAccurate) "high" else "low") + " accuracy"
        result += SEPARATOR + "Lon:     " + Longitude28.toString(fLongitude)
        result += SEPARATOR + "Lat:     " + Latitude27.toString(fLatitude)
        result += SEPARATOR + "COG:     " + Angle12.toString(fCOG)
        result += SEPARATOR + "Heading: " + Angle9.getTrueHeadingString(trueHeading)
        result += SEPARATOR + "Time:    " + TimeStamp.toString(timeStamp)
        result += SEPARATOR + "Man ind: " + ManeuverIndicator.toString(
            manouverIndicator
        )
        return result
    }

    companion object {
        private const val SEPARATOR = "\n\t"
        private const val NAVIGATIONALSTATUS = 0
        private const val RATEOFTURN = 1
        private const val SPEEDOVERGROUND = 2
        private const val POSITIONACCURACY = 3
        private const val LONGITUDE = 4
        private const val LATITUDE = 5
        private const val COURSEOVERGROUND = 6
        private const val TRUEHEADING = 7
        private const val TIMESTAMP = 8
        private const val MANOEUVER = 9
        private val FROM = intArrayOf(
            38, 42, 50, 60, 61, 89, 116, 128, 137, 143
        )
        private val TO = intArrayOf(
            42, 50, 60, 61, 89, 116, 128, 137, 143, 145
        )
    }
}