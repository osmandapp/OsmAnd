/*
 * AISParser.java
 * Copyright (C) 2016 Kimmo Tuukkanen
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
package net.sf.marineapi.nmea.parser

import net.sf.marineapi.nmea.sentence.AISSentence
import net.sf.marineapi.nmea.sentence.SentenceId
import net.sf.marineapi.nmea.sentence.TalkerId

/**
 * Common AIS sentence parser. Handles only the NMEA layer for VDM and VDO
 * sentences. The actual payload message is parsed by AIS message parsers.
 *
 * @author Kimmo Tuukkanen
 * @see AISSentence
 *
 * @see VDOParser
 *
 * @see VDMParser
 */
internal abstract class AISParser : SentenceParser, AISSentence {
    /**
     * Creates a new instance of VDOParser.
     *
     * @param nmea NMEA sentence String.
     * @param sid Expected sentence ID
     */
    constructor(nmea: String, sid: SentenceId) : super(nmea, sid)

    /**
     * Creates a new empty VDOParser.
     *
     * @param tid TalkerId to set
     * @param sid SentenceId to set
     */
    constructor(tid: TalkerId?, sid: SentenceId) : super('!', tid, sid, 6)

    override fun getNumberOfFragments(): Int {
        return getIntValue(NUMBER_OF_FRAGMENTS)
    }

    override fun getFragmentNumber(): Int {
        return getIntValue(FRAGMENT_NUMBER)
    }

    override fun getMessageId(): String {
        return getStringValue(MESSAGE_ID)
    }

    override fun getRadioChannel(): String {
        return getStringValue(RADIO_CHANNEL)
    }

    override fun getPayload(): String {
        return getStringValue(PAYLOAD)
    }

    override fun getFillBits(): Int {
        return getIntValue(FILL_BITS)
    }

    override fun isFragmented(): Boolean {
        return getNumberOfFragments() > 1
    }

    override fun isFirstFragment(): Boolean {
        return getFragmentNumber() == 1
    }

    override fun isLastFragment(): Boolean {
        return getNumberOfFragments() == getFragmentNumber()
    }

    override fun isPartOfMessage(sentence: AISSentence?): Boolean {
        return if (getNumberOfFragments() == sentence!!.getNumberOfFragments() &&
            getFragmentNumber() < sentence.getFragmentNumber()
        ) {
            if (getFragmentNumber() + 1 == sentence.getFragmentNumber()) {
                getRadioChannel() == sentence.getRadioChannel() || getMessageId() == sentence.getMessageId()
            } else {
                getRadioChannel() == sentence.getRadioChannel() && getMessageId() == sentence.getMessageId()
            }
        } else {
            false
        }
    }

    companion object {
        // NMEA message fields
        private const val NUMBER_OF_FRAGMENTS = 0
        private const val FRAGMENT_NUMBER = 1
        private const val MESSAGE_ID = 2
        private const val RADIO_CHANNEL = 3
        private const val PAYLOAD = 4
        private const val FILL_BITS = 5
    }
}