/* 
 * SentenceEvent.java
 * Copyright (C) 2010 Kimmo Tuukkanen
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
package net.sf.marineapi.nmea.event

import net.sf.marineapi.nmea.sentence.Sentence

import kotlinx.datetime.Clock


/**
 * Sentence events occur when a valid NMEA 0183 sentence has been read from the
 * data source.
 *
 * @author Kimmo Tuukkanen
 * @see SentenceListener
 *
 * @see net.sf.marineapi.nmea.io.SentenceReader
 */
class SentenceEvent(val source: Any?, s: Sentence?) {
    /**
     * Get system time when this event was created.
     *
     * @return Milliseconds timestamp
     */
    val timeStamp = Clock.System.now().toEpochMilliseconds()

    /**
     * Gets the Sentence object that triggered the event.
     *
     * @return Sentence object
     */
    val sentence: Sentence

    /**
     * Creates a new SentenceEvent object.
     *
     * @param src Object that fired the event
     * @param s Sentence that triggered the event
     * @throws IllegalArgumentException If specified sentence is `null`
     */
    init {
        requireNotNull(s) { "Sentence cannot be null" }
        sentence = s
    }


}