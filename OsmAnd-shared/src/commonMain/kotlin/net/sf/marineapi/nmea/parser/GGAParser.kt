package net.sf.marineapi.nmea.parser

import net.sf.marineapi.nmea.sentence.GGASentence
import net.sf.marineapi.nmea.sentence.SentenceId
import net.sf.marineapi.nmea.sentence.TalkerId
import net.sf.marineapi.nmea.util.Position

/**
 * Global Navigation Satellite System fix data parser.
 */
internal class GGAParser : SentenceParser, GGASentence {
    constructor(nmea: String) : super(nmea, SentenceId.GGA)

    constructor(talker: TalkerId?) : super(talker, SentenceId.GGA, FIELD_COUNT)

    override fun hasFix(): Boolean {
        return getIntValue(FIX_QUALITY) > 0
    }

    override fun hasValidPosition(): Boolean {
        return hasFix()
    }

    override fun getPosition(): Position {
        val altitude = if (hasValue(ALTITUDE)) getDoubleValue(ALTITUDE) else 0.0
        return NmeaPositionParser.parse(
            getStringValue(LATITUDE),
            getStringValue(LATITUDE_HEMISPHERE),
            getStringValue(LONGITUDE),
            getStringValue(LONGITUDE_HEMISPHERE),
            altitude
        )
    }

    companion object {
        private const val FIELD_COUNT = 14
        private const val LATITUDE = 1
        private const val LATITUDE_HEMISPHERE = 2
        private const val LONGITUDE = 3
        private const val LONGITUDE_HEMISPHERE = 4
        private const val FIX_QUALITY = 5
        private const val ALTITUDE = 8
    }
}
