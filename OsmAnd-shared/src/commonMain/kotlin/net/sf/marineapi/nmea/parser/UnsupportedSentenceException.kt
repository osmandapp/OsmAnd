/*
 * UnsupportedSentenceException.java
 * Copyright (C) 2018 Michael Skogberg
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

/**
 * Thrown when an unsupported sentence is encountered.
 *
 * @author Michael Skogberg
 */
class UnsupportedSentenceException
/**
 * Constructor
 *
 * @param msg Exception message
 */
    (msg: String?) : RuntimeException(msg) {
    companion object {
        private const val serialVersionUID = 7618916517933110942L
    }
}