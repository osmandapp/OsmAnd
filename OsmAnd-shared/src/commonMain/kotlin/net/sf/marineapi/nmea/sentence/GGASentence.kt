package net.sf.marineapi.nmea.sentence

/**
 * Global Navigation Satellite System fix data sentence.
 */
interface GGASentence : PositionSentence {
    fun hasFix(): Boolean
}
