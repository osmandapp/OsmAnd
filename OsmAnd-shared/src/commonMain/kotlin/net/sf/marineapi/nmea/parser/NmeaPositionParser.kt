package net.sf.marineapi.nmea.parser

import net.sf.marineapi.nmea.util.Position

internal object NmeaPositionParser {
    fun parse(
        latitude: String,
        latitudeHemisphere: String,
        longitude: String,
        longitudeHemisphere: String,
        altitude: Double = 0.0
    ): Position {
        var lat = parseDegrees(latitude, 2)
        var lon = parseDegrees(longitude, 3)
        if (latitudeHemisphere == "S") {
            lat *= -1
        }
        if (longitudeHemisphere == "W") {
            lon *= -1
        }
        return Position(lat, lon, altitude)
    }

    private fun parseDegrees(value: String, degreeDigits: Int): Double {
        if (value.length <= degreeDigits) {
            throw ParseException("Position field does not contain degrees and minutes")
        }
        val degrees = value.take(degreeDigits).toDoubleOrNull()
            ?: throw ParseException("Position degrees field is invalid")
        val minutes = value.drop(degreeDigits).toDoubleOrNull()
            ?: throw ParseException("Position minutes field is invalid")
        return degrees + minutes / 60.0
    }
}
