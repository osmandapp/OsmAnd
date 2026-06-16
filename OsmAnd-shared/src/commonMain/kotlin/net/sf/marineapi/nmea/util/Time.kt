/*
 * Time.java
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
package net.sf.marineapi.nmea.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Represents a time of day in 24-hour clock, i.e. the UTC time used as default
 * in NMEA 0183. Transmitted by
 * [net.sf.marineapi.nmea.sentence.TimeSentence].
 *
 * @author Kimmo Tuukkanen
 * @see net.sf.marineapi.nmea.sentence.TimeSentence
 *
 * @see net.sf.marineapi.nmea.util.NmeaDate
 */
class Time {
    // hour of day
    private var hour = 0

    // minute of hour
    private var minutes = 0

    // seconds of a minute, may include decimal sub-second in some sentences
    private var seconds = 0.0
    /**
     * Get time zone offset hours. Defaults to 0 (UTC).
     *
     * @return Offset hours as int.
     */
    /**
     * Set time zone offset hours.
     *
     * @param hours Offset to set (-13..13)
     * @throws IllegalArgumentException If offset out of bounds.
     */
    // time zone offset hours
    var offsetHours = 0
        set(hours) {
            require(!(hours < -13 || hours > 13)) { "Offset out of bounds [-13..13]" }
            field = hours
        }
    /**
     * Get time zone offset minutes. Defaults to 0 (UTC).
     *
     * @return Offset minutes as int.
     */
    /**
     * Set time zone offset minutes.
     *
     * @param minutes Offset to set (-59..59)
     * @throws IllegalArgumentException If offset out of bounds.
     */
    // time zone offset minutes
    var offsetMinutes = 0
        set(minutes) {
            require(!(minutes < -59 || minutes > 59)) { "Offset out of bounds [-59..59]" }
            field = minutes
        }

    /**
     * Creates a new instance of `Time` using the current system
     * time.
     */
    constructor() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        hour = now.hour
        minutes = now.minute
        seconds = now.second + now.nanosecond / 1_000_000_000.0
    }

    /**
     * Creates a new instance of `Time` based on given String.
     * Assumes the `hhmmss.sss` formatting used in NMEA sentences.
     *
     * @param time Timestamp String
     */
    constructor(time: String?) {
        setHour(time!!.substring(0, 2).toInt())
        setMinutes(time.substring(2, 4).toInt())
        setSeconds(time.substring(4).toDouble())
    }

    /**
     * Creates a new instance of Time with hours, minutes and seconds.
     *
     * @param hour Hour of day
     * @param min Minute of hour
     * @param sec Second of minute
     */
    constructor(hour: Int, min: Int, sec: Double) {
        setHour(hour)
        setMinutes(min)
        setSeconds(sec)
    }

    /**
     * Creates a new instance of Time with time zone offsets.
     *
     * @param hour Hour of day
     * @param min Minute of hour
     * @param sec Second of minute
     * @param offsetHrs Time zone offset hours
     * @param offsetMin Time zone offset minutes
     */
    constructor(hour: Int, min: Int, sec: Double, offsetHrs: Int, offsetMin: Int) {
        setHour(hour)
        setMinutes(min)
        setSeconds(sec)
        offsetHours = offsetHrs
        offsetMinutes = offsetMin
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return if (other is Time) {
            (other.getHour() == getHour() && other.getMinutes() == getMinutes()
                    && other.getSeconds() == getSeconds() && other.offsetHours == offsetHours
                    && other.offsetMinutes == offsetMinutes)
        } else false
    }

    /**
     * Get the hour of day.
     *
     * @return the hour
     */
    fun getHour(): Int {
        return hour
    }

    /**
     * Get time as milliseconds since beginning of day.
     *
     * @return Milliseconds
     */
    val milliseconds: Long
        get() {
            var m = (getSeconds() * 1000).roundToLong()
            m += (getMinutes() * 60 * 1000).toLong()
            m += (getHour() * 3600 * 1000).toLong()
            return m
        }

    /**
     * Get the minute of hour.
     *
     * @return the minute
     */
    fun getMinutes(): Int {
        return minutes
    }

    /**
     * Get the second of minute.
     *
     * @return the second
     */
    fun getSeconds(): Double {
        return seconds
    }

    override fun hashCode(): Int {
        val s = "$hour$minutes$seconds"
        return s.hashCode()
    }

    /**
     * Set the hour of day.
     *
     * @param hour the hour to set
     * @throws IllegalArgumentException If hour value out of bounds 0..23
     */
    fun setHour(hour: Int) {
        require(!(hour < 0 || hour > 23)) { "Valid hour value is between 0..23" }
        this.hour = hour
    }

    /**
     * Set the minute of hour.
     *
     * @param minutes the minute to set
     * @throws IllegalArgumentException If minutes value out of bounds 0..59
     */
    fun setMinutes(minutes: Int) {
        require(!(minutes < 0 || minutes > 59)) { "Valid minutes value is between 0..59" }
        this.minutes = minutes
    }

    /**
     * Set seconds of minute.
     *
     * @param seconds Seconds to set
     * @throws IllegalArgumentException If seconds out of bounds (
     * `0 &lt; seconds &lt; 60`)
     */
    fun setSeconds(seconds: Double) {
        require(!(seconds < 0 || seconds >= 60)) { "Invalid value for second (0 < seconds < 60)" }
        this.seconds = seconds
    }

    /**
     * Set the time by [kotlinx.datetime.LocalDateTime]. The date information of is
     * ignored, only hours, minutes and seconds are relevant. Notice also that
     * time zone offset is not affected by this method because
     * [kotlinx.datetime.LocalDateTime] does not contain zone offset.
     *
     * @param d LocalDateTime
     */
    fun setTime(d: LocalDateTime?) {
        if (d == null) return
        val seconds = d.second + d.nanosecond / 1_000_000_000.0
        setHour(d.hour)
        setMinutes(d.minute)
        setSeconds(seconds)
    }

    /**
     * Convert to [kotlinx.datetime.LocalDateTime]. Notice that time zone information is
     * lost in conversion.
     *
     * @param d LocalDate that defines year, month and day for time.
     * @return A LocalDateTime that is combination of specified Date and Time
     */
    fun toLocalDateTime(d: LocalDate): LocalDateTime {
        val seconds = getSeconds()
        val fullSeconds = floor(seconds).toInt()
        val nanoseconds = ((seconds - fullSeconds) * 1_000_000_000).roundToLong().toInt()
        return d.atTime(getHour(), getMinutes(), fullSeconds, nanoseconds)
    }

    /**
     * Returns the String representation of `Time`. Formats the time
     * in `hhmmss.sss` format used in NMEA 0183 sentences. Seconds
     * are presented with three decimals regardless of precision returned by
     * [.getSeconds].
     */
    override fun toString(): String {
        val hr = getHour().toString().padStart(2, '0')
        val min = getMinutes().toString().padStart(2, '0')
        
        // Format seconds to "00.000"
        val secInt = floor(getSeconds()).toInt()
        val secDec = ((getSeconds() - secInt) * 1000).roundToLong()
        val secStr = secInt.toString().padStart(2, '0') + "." + secDec.toString().padStart(3, '0')
        
        return hr + min + secStr
    }

    /**
     * Returns the ISO 8601 representation of time (`hh:mm:ss+hh:mm`).
     *
     * @return ISO 8601 formatted time String.
     */
    fun toISO8601(): String {
        val hr = getHour().toString().padStart(2, '0')
        val min = getMinutes().toString().padStart(2, '0')
        val sec = floor(getSeconds()).toInt().toString().padStart(2, '0')
        val tzSign = if (offsetHours >= 0) "+" else "-"
        val tzHr = kotlin.math.abs(offsetHours).toString().padStart(2, '0')
        val tzMin = offsetMinutes.toString().padStart(2, '0')
        return "$hr:$min:$sec$tzSign$tzHr:$tzMin"
    }

    companion object {
    }
}
