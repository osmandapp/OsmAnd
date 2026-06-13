/*
 * VDOParser.java
 * Copyright (C) 2016 Henri Laurent
 *
 * This file is part of Java Marine API.
 * <http://sourceforge.net/projects/marineapi/>
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
package net.sf.marineapi.nmea.parser

import net.sf.marineapi.nmea.sentence.AISSentence
import net.sf.marineapi.nmea.sentence.SentenceId
import net.sf.marineapi.nmea.sentence.TalkerId

/**
 * AIS VDO sentence parser, contains only the NMEA layer. The actual payload
 * message is parsed by AIS message parsers.
 *
 * @author Henri Laurent
 * @see AISSentence
 *
 * @see AISParser
 */
internal class VDOParser : AISParser {
    /**
     * Creates a new instance of VDOParser.
     *
     * @param nmea NMEA sentence String.
     */
    constructor(nmea: String) : super(nmea, SentenceId.VDO)

    /**
     * Creates a new empty VDOParser.
     *
     * @param talker TalkerId to set
     */
    constructor(talker: TalkerId?) : super(talker, SentenceId.VDO)
}