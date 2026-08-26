/* 
 * SentenceListener.java
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



/**
 * Base interface for listening to SentenceEvents.
 *
 * @author Kimmo Tuukkanen
 * @see net.sf.marineapi.nmea.io.SentenceReader
 *
 * @see SentenceEvent
 *
 * @see AbstractSentenceListener
 */
interface SentenceListener {
    /**
     *
     * Called after [net.sf.marineapi.nmea.io.SentenceReader] has timed
     * out for receiving new data. Indicates that the reader is still active and
     * waiting for new data which isn't currently available for some reason. For
     * example, the device may have stopped broadcasting or the end of a file
     * has been reached.
     *
     *
     * Default time for timeout is defined by
     * [net.sf.marineapi.nmea.io.SentenceReader.DEFAULT_TIMEOUT]. This
     * value can be overridden with
     * [net.sf.marineapi.nmea.io.SentenceReader.setPauseTimeout].
     */
    fun readingPaused()

    /**
     * Called before [net.sf.marineapi.nmea.io.SentenceReader] starts
     * dispatching events. Indicates that the reader is active and receiving
     * data. Also, this notification occurs when the dispatching continues
     * again after [.readingPaused] has occurred.
     */
    fun readingStarted()

    /**
     * Called after [net.sf.marineapi.nmea.io.SentenceReader] has
     * permanently stopped reading, either due to an error or by calling the
     * [net.sf.marineapi.nmea.io.SentenceReader.stop] method.
     */
    fun readingStopped()

    /**
     * Called by [net.sf.marineapi.nmea.io.SentenceReader] when a single
     * NMEA 0183 sentence has been read and parsed from the data stream. By
     * default, only supported sentences defined in
     * [net.sf.marineapi.nmea.sentence.SentenceId] are dispatched.
     *
     * @param event SentenceEvent containing the data.
     */
    fun sentenceRead(event: SentenceEvent)
}