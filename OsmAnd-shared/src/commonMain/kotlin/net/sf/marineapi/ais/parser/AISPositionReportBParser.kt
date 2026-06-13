/*
 * AISPositionReportBParser.java
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

import net.sf.marineapi.ais.message.AISPositionReportB
import net.sf.marineapi.ais.util.*


/**
 * Implementation for AIS Message 18 and 19:  Class B Equipment Position Report.
 *
 * <pre>
 * Field    Name                                    Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1       messageID                                  6    (   1,   6)
 * 2       repeatIndicator                            2    (   7,   8)
 * 3       userID                                    30    (   9,  38)
 * 4       spare1                                     8    (  39,  46)
 * 5       speedOverGround                           10    (  47,  56)
 * 6       positionAccuracy                           1    (  57,  57)
 * 7       longitude                                 28    (  58,  85)
 * 8       latitude                                  27    (  86, 112)
 * 9       courseOverGround                          12    ( 113, 124)
 * 10       trueHeading                                9    ( 125, 133)
 * 11       timeStamp                                  6    ( 134, 139)
</pre> *
 *
 * TODO: missing "Class B" flags 13 - 20.
 *
 * @author Lázár József
 */
internal open class AISPositionReportBParser : AISMessageParser, AISPositionReportB {
    private var fSOG = 0
    override var isAccurate = false
    private var fLongitude = 0
    private var fLatitude = 0
    private var fCOG = 0
    override var trueHeading = 0
    override var timeStamp = 0

    /**
     * Constructor.
     *
     * @param content Six-bit message content.
     */
    constructor(content: Sixbit) : super(content) {
        parse(content)
    }

    /**
     * Constructor with message length validation.
     *
     * @param content Six-bit message content.
     * @param len Expected content length (bits)
     * @throws IllegalArgumentException If content length is not as expected.
     */
    constructor(content: Sixbit, len: Int) : super(content, len) {
        parse(content)
    }

    private fun parse(content: Sixbit) {
        fSOG = content.getInt(FROM[SPEEDOVERGROUND], TO[SPEEDOVERGROUND])
        isAccurate = content.getBoolean(FROM[POSITIONACCURACY])
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
        if (!Angle12.isCorrect(fCOG)) addViolation(AISRuleViolation("getCourseOverGround", fCOG, Angle12.RANGE))
        trueHeading = content.getInt(FROM[TRUEHEADING], TO[TRUEHEADING])
        if (!Angle9.isCorrect(trueHeading)) addViolation(AISRuleViolation("getTrueHeading", trueHeading, Angle9.RANGE))
        timeStamp = content.getInt(FROM[TIMESTAMP], TO[TIMESTAMP])
    }

    override val speedOverGround: Double
        get() = SpeedOverGround.toKnots(fSOG)
    override val longitudeInDegrees: Double
        get() = Longitude28.toDegrees(fLongitude)
    override val latitudeInDegrees: Double
        get() = Latitude27.toDegrees(fLatitude)
    override val courseOverGround: Double
        get() = Angle12.toDegrees(fCOG)

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
        var result = "\tSOG:     " + SpeedOverGround.toString(fSOG)
        result += SEPARATOR + "Pos acc: " + (if (isAccurate) "high" else "low") + " accuracy"
        result += SEPARATOR + "Lon:     " + Longitude28.toString(fLongitude)
        result += SEPARATOR + "Lat:     " + Latitude27.toString(fLatitude)
        result += SEPARATOR + "COG:     " + Angle12.toString(fCOG)
        result += SEPARATOR + "Heading: " + Angle9.getTrueHeadingString(trueHeading)
        result += SEPARATOR + "Time:    " + TimeStamp.toString(timeStamp)
        return result
    }

    companion object {
        const val SEPARATOR = "\n\t"
        private const val SPEEDOVERGROUND = 0
        private const val POSITIONACCURACY = 1
        private const val LONGITUDE = 2
        private const val LATITUDE = 3
        private const val COURSEOVERGROUND = 4
        private const val TRUEHEADING = 5
        private const val TIMESTAMP = 6
        private val FROM = intArrayOf(
            46, 56, 57, 85, 112, 124, 133
        )
        private val TO = intArrayOf(
            56, 57, 85, 112, 124, 133, 139
        )
    }
}