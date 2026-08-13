package net.sf.marineapi.nmea.parser

import net.sf.marineapi.nmea.sentence.RMCSentence
import net.sf.marineapi.nmea.sentence.SentenceId
import net.sf.marineapi.nmea.sentence.TalkerId
import net.sf.marineapi.nmea.util.Position

/**
 * Recommended Minimum Specific GNSS Data parser.
 */
internal class RMCParser : SentenceParser, RMCSentence {
    constructor(nmea: String) : super(nmea, SentenceId.RMC)

    constructor(talker: TalkerId?) : super(talker, SentenceId.RMC, FIELD_COUNT)

    override fun isActive(): Boolean {
        return getStringValue(STATUS).equals("A", ignoreCase = true)
    }

    override fun hasValidPosition(): Boolean {
        return isActive()
    }

    override fun getPosition(): Position {
        return NmeaPositionParser.parse(
            getStringValue(LATITUDE),
            getStringValue(LATITUDE_HEMISPHERE),
            getStringValue(LONGITUDE),
            getStringValue(LONGITUDE_HEMISPHERE)
        )
    }

    override fun hasSpeed(): Boolean {
        return hasValue(SPEED)
    }

    override fun getSpeed(): Double {
        return getDoubleValue(SPEED)
    }

    override fun hasCourse(): Boolean {
        return hasValue(COURSE)
    }

    override fun getCourse(): Double {
        return getDoubleValue(COURSE)
    }

    companion object {
        private const val FIELD_COUNT = 12
        private const val STATUS = 1
        private const val LATITUDE = 2
        private const val LATITUDE_HEMISPHERE = 3
        private const val LONGITUDE = 4
        private const val LONGITUDE_HEMISPHERE = 5
        private const val SPEED = 6
        private const val COURSE = 7
    }
}
