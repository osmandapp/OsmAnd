package net.sf.marineapi.nmea.sentence

import net.sf.marineapi.nmea.util.Position

/**
 * Base interface for NMEA sentences that can provide receiver position.
 */
interface PositionSentence : Sentence {
    fun hasValidPosition(): Boolean
    fun getPosition(): Position
    fun hasSpeed(): Boolean = false
    fun getSpeed(): Double = 0.0
    fun hasCourse(): Boolean = false
    fun getCourse(): Double = 0.0
}
