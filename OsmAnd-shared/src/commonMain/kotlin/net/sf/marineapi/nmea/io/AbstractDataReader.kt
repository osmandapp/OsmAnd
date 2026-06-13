/*
 * AbstractDataReader.java
 * Copyright (C) 2014-2018 Kimmo Tuukkanen
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
package net.sf.marineapi.nmea.io

import net.sf.marineapi.nmea.parser.SentenceFactory
import net.sf.marineapi.nmea.parser.UnsupportedSentenceException
import net.sf.marineapi.nmea.sentence.SentenceValidator



/**
 * Abstract base class for data readers, with common methods and run loop
 * for firing events to listeners managed by the parent [SentenceReader].
 *
 *
 * Extend this class to implement custom readers, for example when NMEA data
 * is delivered embedded in a proprietary format. Otherwise, it is recommended
 * to use `SentenceReader` directly with `InputStream`
 * or `DatagramSocket`.
 *
 *
 * @author Kimmo Tuukkanen
 * @see SentenceReader
 */
abstract class AbstractDataReader(var parent: SentenceReader? = null) {

    /**
     * Tells if the reader is running and actively scanning the data source for
     * new data.
     *
     * @return `true` if running, otherwise `false`.
     */
    var isRunning = true
        private set


    /**
     * Read one NMEA-0183 sentence and return it.
     *
     * @return Sentence String or `null` if nothing was read.
     * @throws Exception On read failure.
     */
    @Throws(Exception::class)
    abstract fun read(): String?

    open suspend fun run() {
        val monitor = ActivityMonitor(parent)
        val factory: SentenceFactory = SentenceFactory.instance
        while (isRunning) {
            try {
                val data = read()
                if (data == null) {
                    kotlinx.coroutines.delay(SLEEP_TIME.toLong())
                } else if (SentenceValidator.isValid(data)) {
                    monitor.refresh()
                    val s = factory.createParser(data)
                    parent!!.fireSentenceEvent(s)
                } else if (!SentenceValidator.isSentence(data)) {
                    parent!!.fireDataEvent(data)
                }
            } catch (use: UnsupportedSentenceException) {
                println(use.message)
            } catch (e: Exception) {
                parent!!.handleException("Data read failed", e)
                try {
                    kotlinx.coroutines.delay(SLEEP_TIME.toLong())
                } catch (_: Exception) {
                }
            } finally {
                monitor.tick()
            }
        }
        monitor.reset()
        parent!!.fireReadingStopped()
    }

    /**
     * Stops the reader permanently.
     */
    fun stop() {
        isRunning = false
    }

    companion object {
        // Sleep time between failed read attempts to prevent busy-looping
        private const val SLEEP_TIME = 100

    }
}