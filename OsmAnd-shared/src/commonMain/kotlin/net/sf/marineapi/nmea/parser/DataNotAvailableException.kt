/* 
 * DataNotAvailableException.java
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
package net.sf.marineapi.nmea.parser

/**
 * Thrown to indicate that requested data is not available. For example, when
 * invoking a getter for sentence data field that contains no value.
 *
 * @author Kimmo Tuukkanen
 */
open class DataNotAvailableException : RuntimeException {
    /**
     * Constructor
     *
     * @param msg Exception message
     */
    constructor(msg: String?) : super(msg)

    /**
     * Constructor
     *
     * @param msg Exception message
     * @param cause Throwable that caused the exception
     */
    constructor(msg: String?, cause: Throwable?) : super(msg, cause)

    companion object {
        private const val serialVersionUID = -3672061046826633631L
    }
}