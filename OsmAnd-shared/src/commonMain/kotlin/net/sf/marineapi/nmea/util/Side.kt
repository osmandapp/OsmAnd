/*
 * Side.java
 * Copyright (C) 2014 Lázár József
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

/**
 * Defines the sides of a boat, i.e. "port" and "starboard".
 *
 * @author Lázár József
 */
enum class Side(private val ch: Char) {
    /** Port  */
    PORT('P'),

    /** Right  */
    STARBOARD('S');

    /**
     * Returns the corresponding char constant.
     *
     * @return Char indicator for Direction
     */
    fun toChar(): Char {
        return ch
    }

    companion object {
        /**
         * Get the enum corresponding to specified char.
         *
         * @param c Char indicator for Side
         * @return Side
         */
        fun valueOf(c: Char): Side {
            for (d in values()) {
                if (d.toChar() == c) {
                    return d
                }
            }
            return valueOf(c.toString())
        }
    }
}