/*
 * ExceptionListener.java
 * Copyright (C) 2014 Johan Riisberg-Jensen
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
 * A listener callback interface for listening to Exceptions in DataReaders.
 *
 * @author Johan Riisberg-Jensen
 */
interface ExceptionListener {
    /**
     * Invoked by [SentenceReader] when error has occured while reading
     * the data source.
     *
     * @param e Exception that was thrown while reading data.
     */
    fun onException(e: Exception?)
}