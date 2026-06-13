/*
 * ActivityMonitor.java
 * Copyright (C) 2012 Kimmo Tuukkanen
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

/**
 * Monitor for firing state change events events, i.e. reader started, paused or
 * stopped.
 *
 * @author Kimmo Tuukkanen
 */
internal class ActivityMonitor
/**
 * Creates a new instance for given [SentenceReader].
 *
 * @param parent Parent [SentenceReader] to monitor.
 */(private val parent: SentenceReader?) {
    private var lastUpdated: Long = -1

    /**
     * Resets the monitor in initial state.
     */
    fun reset() {
        lastUpdated = -1
    }

    /**
     * Refreshes the monitor timestamp and fires reading started event if
     * currently paused.
     */
    fun refresh() {
        if (lastUpdated < 0) {
            parent!!.fireReadingStarted()
        }
        lastUpdated = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }

    /**
     * Heartbeat method, checks the time out if not paused.
     */
    fun tick() {
        if (lastUpdated > 0) {
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val timeout = parent!!.pauseTimeout
            if (now - lastUpdated >= timeout) {
                parent.fireReadingPaused()
                reset()
            }
        }
    }
}