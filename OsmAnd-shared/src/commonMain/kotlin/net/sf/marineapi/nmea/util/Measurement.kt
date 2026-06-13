/*
 * Measurement.java
 * Copyright (C) 2013 Robert Huitema, Kimmo Tuukkanen
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
 * Sensor measurement data delivered by
 * [net.sf.marineapi.nmea.sentence.XDRSentence]. Notice that any of the
 * fields may be empty (`null`), depending on sentence and sensor
 * that produced it.
 *
 * @author Robert Huitema, Kimmo Tuukkanen
 */
class Measurement {
    /**
     * Returns the name of transducer.
     *
     * @return Sensor name String
     */
    /**
     * Sets the name of transducer.
     *
     * @param name Transducer name to set
     */
    var name: String? = null
    /**
     * Returns the type of transducer.
     *
     * @return Type String
     */
    /**
     * Sets the type of measurement.
     *
     * @param type Type to set
     */
    var type: String? = null
    /**
     * Returns the units of measurement.
     *
     * @return Units String
     */
    /**
     * Sets the units of measurement.
     *
     * @param units Units to set.
     */
    var units: String? = null
    var value: Double? = null

    /**
     * Creates a new empty instance of Measurement.
     */
    constructor()

    /**
     * Creates a new instance of Measurement with given values.
     *
     * @param type Type of measurement
     * @param value The measured value
     * @param units Unit of measurement
     * @param name Name of measurement
     */
    constructor(type: String?, value: Double, units: String?, name: String?) {
        this.type = type
        this.value = value
        this.units = units
        this.name = name
    }

    /**
     * Returns the measurement value.
     *
     * @return Double value
     */
    fun getValue(): Double {
        return value!!
    }

    /**
     * Sets the measurement value.
     *
     * @param value Value to set
     */
    fun setValue(value: Double) {
        this.value = value
    }

    /**
     * Tells if all fields in this measurement are empty (null).
     *
     * @return true if empty, otherwise false.
     */
    val isEmpty: Boolean
        get() = name == null && type == null && value == null && units == null
}