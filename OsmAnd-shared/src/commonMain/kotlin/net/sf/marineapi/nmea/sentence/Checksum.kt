/*
 * Checksum.java
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
package net.sf.marineapi.nmea.sentence

/**
 * Provides Sentence checksum calculation and utilities.
 *
 * @author Kimmo Tuukkanen
 */
object Checksum {
    /**
     * Append or replace existing checksum in specified NMEA sentence.
     *
     * @param nmea Sentence in String representation
     * @return The specified String with checksum added.
     */
    fun add(nmea: String): String {
        val str = nmea.substring(0, index(nmea))
        val sum = calculate(str)
        return "$str${Sentence.CHECKSUM_DELIMITER}$sum"
    }

    /**
     * Calculates checksum for given NMEA sentence, i.e. XOR of each
     * character between '$' and '*' characters (exclusive).
     *
     * @param nmea Sentence String with or without checksum.
     * @return Hexadecimal checksum
     */
    fun calculate(nmea: String): String {
        return xor(nmea.substring(1, index(nmea)))
    }

    /**
     * Calculates XOR checksum of given String. Resulting hex value is returned
     * as a String in two digit format, padded with a leading zero if necessary.
     *
     * @param str String to calculate checksum for.
     * @return Hexadecimal checksum
     */
    fun xor(str: String): String {
        var sum = 0
        for (element in str) {
            sum = sum xor element.code.toByte().toInt()
        }
        return sum.toString(16).uppercase().padStart(2, '0')
    }

    /**
     * Returns the index of checksum separator char in specified NMEA sentence.
     * If separator is not found, returns the String length.
     *
     * @param nmea Sentence String
     * @return Index of checksum separator or String length.
     */
    fun index(nmea: String): Int {
        return if (nmea.indexOf(Sentence.CHECKSUM_DELIMITER) > 0) nmea.indexOf(Sentence.CHECKSUM_DELIMITER)
        else nmea.length
    }
}