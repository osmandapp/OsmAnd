package net.osmand.shared.aistracker

import net.sf.marineapi.nmea.sentence.PositionSentence
import net.sf.marineapi.nmea.util.Position

internal object NmeaLocationParser {

    fun parse(sentence: PositionSentence): AisLocation? {
        if (!sentence.hasValidPosition()) {
            return null
        }
        val hasSpeed = sentence.hasSpeed()
        val hasBearing = sentence.hasCourse()
        return sentence.getPosition().toAisLocation(
            speed = if (hasSpeed) knotsToMetersPerSecond(sentence.getSpeed()).toFloat() else 0f,
            bearing = if (hasBearing) sentence.getCourse().toFloat() else 0f,
            hasSpeed = hasSpeed,
            hasBearing = hasBearing
        )
    }

    private fun Position.toAisLocation(speed: Float, bearing: Float, hasSpeed: Boolean, hasBearing: Boolean): AisLocation {
        return AisLocation(latitude, longitude, speed, bearing, hasSpeed, hasBearing)
    }

    private fun knotsToMetersPerSecond(knots: Double): Double {
        return knots * 0.514444
    }
}
