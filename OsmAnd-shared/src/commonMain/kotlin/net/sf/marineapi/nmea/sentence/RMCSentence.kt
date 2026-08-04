package net.sf.marineapi.nmea.sentence

/**
 * Recommended Minimum Specific GNSS Data sentence.
 */
interface RMCSentence : PositionSentence {
    fun isActive(): Boolean
}
