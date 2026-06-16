package net.sf.marineapi.nmea.parser

import net.sf.marineapi.nmea.sentence.AISSentence
import net.sf.marineapi.nmea.sentence.SentenceId
import net.sf.marineapi.nmea.sentence.TalkerId

/**
 * AIS VDM sentence parser, contains only the NMEA layer. The actual payload
 * message is parsed by AIS message parsers.
 *
 * @author Lázár József
 * @see AISSentence
 *
 * @see AISParser
 */
internal class VDMParser : AISParser {
    /**
     * Creates a new instance of VDMParser.
     *
     * @param nmea NMEA sentence String.
     */
    constructor(nmea: String) : super(nmea, SentenceId.VDM)

    /**
     * Creates a new empty VDMParser.
     *
     * @param talker TalkerId to set
     */
    constructor(talker: TalkerId?) : super(talker, SentenceId.VDM)
}