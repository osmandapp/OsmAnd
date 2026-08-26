package net.sf.marineapi.ais.parser

import net.sf.marineapi.ais.message.AISMessage27
import net.sf.marineapi.ais.util.*


/**
 * AIS Message 27 implementation - LONG-RANGE AUTOMATIC IDENTIFCATION SYSTEM BROADCAST MESSAGE
 * see: https://www.navcen.uscg.gov/?pageName=AISMessage27
 *
 *
 * This message is primarily intended for long-range detection of AIS Class A and Class B “SO” equipped vessels (typically by satellite).
 * This message has a similar content to Messages 1, 2 and 3,
 * but the total number of bits has been compressed
 * to allow for increased propagation delays associated with long-range detection.
 * Note there is no time stamp in this message.
 * The receiving system is expected to provide the time stamp when this message is received.
 *
 * <pre>
 * Field  Name                                      Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1	  messageID                                    6    (   1,   6) - always 27
 * 2	  repeatIndicator                              2    (   7,   8) - always 3
 * 3	  userID                                      30    (   9,  38)
 * 4	  positionAccuracy                             1    (  39,  39)
 * 5	  raimFlag                                     1    (  40,  40)
 * 6	  navigationalStatus                           4    (  41,  44)
 * 7	  longitude                                   18    (  45,  62)
 * 8	  latitude                                    17    (  63,  79)
 * 9	  speedOverGround                              6    (  80,  85)
 * 10	  courseOverGround                             9    (  86,  94)
 * 11	  positionLatency                              1    (  95,  95)
 * 12	  spare                                        1    (  96,  96) - always 0
 * ---- +
 * sum 96
</pre> *
 *
 * @author Krzysztof Borowski
 */
internal class AisMessage27Parser(content: Sixbit) : AISMessageParser(content, 96, 96), AISMessage27 {
    override val isAccurate: Boolean
    override val rAIMFlag: Boolean
    override val navigationalStatus: Int
    private val fLongitude: Int
    private val fLatitude: Int
    private val fSOG: Int
    private val fCOG: Int
    override val positionLatency: Int

    // not available in this Message27 Position Report, filled in with defaults
    override val trueHeading = 511
    private val fRateOfTurn = -128
    override val timeStamp = 60
    override val manouverIndicator = 0

    init {
        isAccurate = content.getBoolean(TO[POSITIONACCURACY])
        rAIMFlag = content.getBoolean(TO[RAIMFLAG])
        navigationalStatus = content.getInt(FROM[NAVIGATIONALSTATUS], TO[NAVIGATIONALSTATUS])
        if (!NavigationalStatus.isCorrect(navigationalStatus)) addViolation(
            AISRuleViolation(
                "NavigationalStatus",
                navigationalStatus,
                NavigationalStatus.RANGE
            )
        )
        fLongitude = content.getAs18BitInt(FROM[LONGITUDE], TO[LONGITUDE])
        if (!Longitude18.isCorrect(fLongitude)) addViolation(
            AISRuleViolation(
                "LongitudeInDegrees",
                fLongitude,
                Longitude18.RANGE
            )
        )
        fLatitude = content.getAs17BitInt(FROM[LATITUDE], TO[LATITUDE])
        if (!Latitude17.isCorrect(fLatitude)) addViolation(
            AISRuleViolation(
                "LatitudeInDegrees",
                fLatitude,
                Latitude17.RANGE
            )
        )
        fSOG = content.getInt(FROM[SPEEDOVERGROUND], TO[SPEEDOVERGROUND])
        fCOG = content.getInt(FROM[COURSEOVERGROUND], TO[COURSEOVERGROUND])
        if (!Angle9.isCorrect(fCOG)) addViolation(AISRuleViolation("CourseOverGround", fCOG, Angle9.RANGE))
        positionLatency = content.getInt(FROM[POSITIONLATENCY], TO[POSITIONLATENCY])
    }

    override val rateOfTurn: Double
        get() = RateOfTurn.toDegreesPerMinute(fRateOfTurn)
    override val speedOverGround: Double
        get() = fSOG.toDouble()
    override val longitudeInDegrees: Double
        get() = Longitude18.toDegrees(fLongitude)
    override val latitudeInDegrees: Double
        get() = Latitude17.toDegrees(fLatitude)
    override val courseOverGround: Double
        get() = fCOG.toDouble()

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
        return Longitude18.isAvailable(fLongitude)
    }

    override fun hasLatitude(): Boolean {
        return Latitude17.isAvailable(fLatitude)
    }

    override fun toString(): String {
        var result = "\tNav st:  " + NavigationalStatus.toString(navigationalStatus)
        result += SEPARATOR + "ROT:     " + RateOfTurn.toString(fRateOfTurn)
        result += SEPARATOR + "SOG:     " + SpeedOverGround.toString(fSOG)
        result += SEPARATOR + "Pos acc: " + (if (isAccurate) "high" else "low") + " accuracy"
        result += SEPARATOR + "Lon:     " + Longitude18.toString(fLongitude)
        result += SEPARATOR + "Lat:     " + Latitude17.toString(fLatitude)
        result += SEPARATOR + "COG:     " + Angle9.toString(fCOG)
        result += SEPARATOR + "Heading: " + Angle9.getTrueHeadingString(trueHeading)
        result += SEPARATOR + "Time:    " + TimeStamp.toString(timeStamp)
        result += SEPARATOR + "Man ind: " + ManeuverIndicator.toString(manouverIndicator)
        result += SEPARATOR + "Latency: " + if (positionLatency == 0) "<5s" else ">5s"
        return result
    }

    companion object {
        private const val SEPARATOR = "\n\t"
        private const val POSITIONACCURACY = 0
        private const val RAIMFLAG = 1
        private const val NAVIGATIONALSTATUS = 2
        private const val LONGITUDE = 3
        private const val LATITUDE = 4
        private const val SPEEDOVERGROUND = 5
        private const val COURSEOVERGROUND = 6
        private const val POSITIONLATENCY = 7
        private const val SPARE = 8
        private val FROM = intArrayOf(
            38, 38, 49, 44, 62, 79, 85, 94, 95
        )
        private val TO = intArrayOf(
            38, 39, 44, 62, 79, 85, 94, 95, 96
        )
    }
}