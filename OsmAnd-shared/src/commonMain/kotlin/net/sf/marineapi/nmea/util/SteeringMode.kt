/*
 * SteeringMode.java
 * Copyright (C) 2015 Paweł Kozioł, Kimmo Tuukkanen
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
 * Defines the steering mode reported in HTC sentence. Steering modes represent
 * steering as selected by a steering selector switch or by a preceding HTC
 * sentence. Priority levels of these inputs and usage/acceptance of related
 * fields are to be defined and documented by the manufacturer.
 *
 * @author Paweł Kozioł
 * @see net.sf.marineapi.nmea.sentence.HTCSentence
 *
 * @see net.sf.marineapi.nmea.sentence.HDTSentence
 */
enum class SteeringMode(private val character: Char) {
    /**
     * The main steering system is in use.
     */
    MANUAL('M'),

    /**
     * The system works as a stand-alone heading controller.
     * Field "Commanded heading to steer" is not accepted as an input.
     */
    STANDALONE('S'),

    /**
     * Input of commanded heading to steer is from external device
     * and the system works as a remotely controlled heading controller.
     * Field "Commanded heading to steer" is accepted as an input.
     */
    HEADING_CONTROL('H'),

    /**
     * The system wokrs as a track controller by correcting a course received
     * in field "Commanded track". Corrections are made based on additionally
     * received track errors (e.g. from sentence
     * [XTE][net.sf.marineapi.nmea.sentence.XTESentence],
     * [APB][net.sf.marineapi.nmea.sentence.APBSentence]).
     */
    TRACK_CONTROL('T'),

    /**
     * Input of commanded rudder angle and direction from an external device.
     * The system accepts values given in fields "Commanded rudder angle"
     * and "Commanded rudder direction" and controls the steering by the same
     * electronic means as used in modes [S][.STANDALONE],
     * [H][.HEADING_CONTROL] or [T][.TRACK_CONTROL].
     */
    RUDDER_CONTROL('R');

    /**
     * Returns the character used in NMEA sentences to indicate the status.
     *
     * @return Char indicator for SteeringMode
     */
    fun toChar(): Char {
        return character
    }

    companion object {
        /**
         * Returns the SteeringMode enum for status char used in sentences.
         *
         * @param ch Status char
         * @return SteeringMode
         */
        fun valueOf(ch: Char): SteeringMode {
            for (sm in values()) {
                if (sm.toChar() == ch) {
                    return sm
                }
            }
            return valueOf(ch.toString())
        }
    }
}