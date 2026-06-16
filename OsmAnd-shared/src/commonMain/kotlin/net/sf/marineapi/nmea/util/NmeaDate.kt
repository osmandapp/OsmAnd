/*
 * Date.java
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
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant


/**
 * Represents a calendar date (day-month-year) transmitted in sentences that
 * implement [net.sf.marineapi.nmea.sentence.DateSentence].
 *
 * @author Kimmo Tuukkanen
 * @see net.sf.marineapi.nmea.sentence.DateSentence
 *
 * @see net.sf.marineapi.nmea.util.Time
 */
class NmeaDate {
    // day of month 1..31
    private var day = 0

    // month 1..12
    private var month = 0

    // four-digit year
    private var year = 0

    /**
     * Creates a new instance of `Date` using the current date.
     */
    constructor() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        year = now.year
        month = now.monthNumber
        day = now.dayOfMonth
    }

    /**
     * Creates a new instance of `Date`, assumes the default NMEA
     * 0183 date formatting, `ddmmyy` or `ddmmyyyy`.
     *
     * @param date Date String to parse.
     */
    constructor(date: String?) {
        setDay(date!!.substring(0, 2).toInt())
        setMonth(date.substring(2, 4).toInt())
        setYear(date.substring(4).toInt())
    }

    /**
     * Constructor with date values.
     *
     * @param year Year, two or four digit value [0..99] or [1000..9999]
     * @param month Month [1..12]
     * @param day Day [1..31]
     * @throws IllegalArgumentException If any of the parameter is out of
     * bounds.
     */
    constructor(year: Int, month: Int, day: Int) {
        setYear(year)
        setMonth(month)
        setDay(day)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is NmeaDate && other.getDay() == getDay() && other.getMonth() == getMonth() && other.getYear() == getYear()
    }

    /**
     * Get day of month.
     *
     * @return the day
     */
    fun getDay(): Int {
        return day
    }

    /**
     * Get month, valid values are 1-12 where 1 denotes January, 2 denotes
     * February etc.
     *
     * @return the month
     */
    fun getMonth(): Int {
        return month
    }

    /**
     * Get year. The date fields in NMEA 0183 may present year by using either
     * two or four digits. In case of only two digits, the century is determined
     * by comparing the value against [.PIVOT_YEAR]. Values lower than or
     * equal to pivot are added to 2000, while values greater than pivot are
     * added to 1900.
     *
     * @return The four-digit year
     * @see .PIVOT_YEAR
     */
    fun getYear(): Int {
        return year
    }

    override fun hashCode(): Int {
        return toISO8601().hashCode()
    }

    /**
     * Set day of month.
     *
     * @param day the day to set
     */
    fun setDay(day: Int) {
        require(!(day < 1 || day > 31)) { "Day out of bounds [1..31]" }
        this.day = day
    }

    /**
     * Get month, valid values are 1-12 where 1 denotes January, 2 denotes
     * February etc.
     *
     * @param month the month to set
     * @throws IllegalArgumentException If specified value is out of bounds
     * [1..12]
     */
    fun setMonth(month: Int) {
        require(!(month < 1 || month > 12)) { "Month value out of bounds [1..12]" }
        this.month = month
    }

    /**
     * Set year. The date fields in NMEA 0183 may present year by using either
     * two or four digits. In case of only two digits, the century is determined
     * by comparing the value against [.PIVOT_YEAR]. Values lower than or
     * equal to pivot are added to 2000, while values greater than pivot are
     * added to 1900.
     *
     * @param year Year to set, two or four digit value.
     * @see .PIVOT_YEAR
     *
     * @throws IllegalArgumentException If specified value is negative or
     * three-digit value.
     */
    fun setYear(year: Int) {
        require((year in 0..99) || (year in 1000..9999)) { "Year must be two or four digit value" }
        when {
            year in (PIVOT_YEAR + 1)..99 -> this.year = 1900 + year
            year < 100 && year <= PIVOT_YEAR -> this.year = 2000 + year
            else -> this.year = year
        }
    }

    /**
     * Returns the String representation of `Date`. Formats the date
     * in `ddmmyy` format used in NMEA 0183 sentences.
     */
    override fun toString(): String {
        val y = getYear().toString()
        val year = y.substring(2)
        val d = getDay().toString().padStart(2, '0')
        val m = getMonth().toString().padStart(2, '0')
        return "$d$m$year"
    }

    /**
     * Returns the date in ISO 8601 format (`yyyy-mm-dd`).
     *
     * @return Formatted date String
     */
    fun toISO8601(): String {
        val y = getYear().toString()
        val m = getMonth().toString().padStart(2, '0')
        val d = getDay().toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    /**
     * Returns a timestamp in ISO 8601 format
     * (`yyyy-mm-ddThh:mm:ss+hh:mm`).
     *
     * @param t [Time] to append in the date
     * @return The formatted date-time String.
     */
    fun toISO8601(t: Time): String {
        return toISO8601() + "T" + t.toISO8601()
    }

    /**
     * Converts to kotlinx.datetime.LocalDate.
     *
     * @return kotlinx.datetime.LocalDate
     */
    fun toLocalDate(): LocalDate {
        return LocalDate(getYear(), getMonth(), getDay())
    }

    companion object {
        /**
         * A pivot value that is used to determine century for two-digit year
         * values. Two-digit values lower than or equal to pivot are assigned to
         * 21th century, while greater values are assigned to 20th century.
         */
        const val PIVOT_YEAR = 50
    }
}
