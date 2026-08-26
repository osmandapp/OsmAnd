package net.osmand.shared.aistracker

import net.osmand.shared.aistracker.AisObjectConstants.INVALID_CPA
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_TCPA
import net.osmand.shared.util.KMapUtils
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object AisTrackerMath {
    private var lastCorrectionUpdate: Long = 0
    private var correctionFactor: Double = 1.0
    private const val maxCorrectionUpdateAgeInMin: Long = 60

    private class Vector(val x: Double, val y: Double) {
        fun sub(a: Vector): Vector {
            return Vector(this.x - a.x, this.y - a.y)
        }
        fun dot(a: Vector): Double {
            return (this.x * a.x) + (this.y * a.y)
        }
    }

    fun getTcpa(ownLocation: AisLocation, otherLocation: AisLocation): Double {
        return getTcpa(ownLocation, otherLocation, getLonCorrection(ownLocation))
    }

    private fun getTcpa(ownLocation: AisLocation, otherLocation: AisLocation, lonCorrection: Double): Double {
        val vX = locationToVector(ownLocation, lonCorrection)
        val vY = locationToVector(otherLocation, lonCorrection)
        val vVX = courseToVector(ownLocation.bearing.toDouble(), getSpeedInKnots(ownLocation).toDouble())
        val vVY = courseToVector(otherLocation.bearing.toDouble(), getSpeedInKnots(otherLocation).toDouble())
        val vDXY = vX.sub(vY)
        val vDVXY = vVX.sub(vVY)
        val divisor = vDVXY.dot(vDVXY)

        return if (abs(divisor) < 1.0E-10 || lonCorrection < 1.0E-10) {
            INVALID_TCPA
        } else {
            val result = -(vDXY.dot(vDVXY)) / divisor
            if (result < 0.0) INVALID_TCPA else result
        }
    }

    fun getCpa1(x: AisLocation, y: AisLocation): AisLatLon? {
        return getCpa(x, y, true)
    }

    fun getCpa2(x: AisLocation, y: AisLocation): AisLatLon? {
        return getCpa(x, y, false)
    }

    fun getCpaDistance(x: AisLocation, y: AisLocation): Float {
        val cpaX = getCpa1(x, y)
        val cpaY = getCpa2(x, y)
        return if (cpaX != null && cpaY != null) {
            meterToMiles(KMapUtils.getDistance(cpaX.latitude, cpaX.longitude, cpaY.latitude, cpaY.longitude).toFloat())
        } else {
            INVALID_CPA
        }
    }

    fun getCpa(ownLocation: AisLocation, otherLocation: AisLocation, result: AisCpa) {
        if (!checkSpeedAndBearing(ownLocation, otherLocation)) {
            val tcpa = getTcpa(ownLocation, otherLocation)
            if (tcpa != INVALID_TCPA) {
                val cpaX = getNewPosition(ownLocation, tcpa)
                val cpaY = getNewPosition(otherLocation, tcpa)
                val crossingTimes = getCrossingTimes(ownLocation, otherLocation)
                if (crossingTimes != null) {
                    result.t1 = crossingTimes.first
                    result.t2 = crossingTimes.second
                }
                result.tcpa = tcpa
                result.cpaPos1 = cpaX
                result.cpaPos2 = cpaY
                if (cpaX != null && cpaY != null) {
                    result.cpa = meterToMiles(KMapUtils.getDistance(cpaX.latitude, cpaX.longitude, cpaY.latitude, cpaY.longitude).toFloat())
                    result.valid = true
                    result.hasCpa = true
                }
            }
        }
    }

    private fun getCpa(x: AisLocation, y: AisLocation, useFirstAsReference: Boolean): AisLatLon? {
        if (checkSpeedAndBearing(x, y)) {
            return null
        }
        val tcpa = getTcpa(x, y)
        return if (tcpa == INVALID_TCPA) {
            null
        } else {
            val base = if (useFirstAsReference) x else y
            getNewPosition(base, tcpa)
        }
    }

    fun getNewPosition(loc: AisLocation?, timeInHours: Double): AisLatLon? {
        if (loc != null) {
            val speed = loc.speed
            val bearing = loc.bearing
            // In original Java code, they did:
            // LatLonPoint b = a.getPoint(loc.getSpeed() * timeInHours * Math.PI / 5556.0, bearingInRad(loc.getBearing()));
            // Note: 1 nautical mile = 1852 meters. 1 hour = 3600 seconds.
            // loc.getSpeed() was in meters per second. 
            // Wait, their formula: loc.getSpeed() * timeInHours * Math.PI / 5556.0
            // Instead we can use KMapUtils.rhumbDestinationPoint
            // We need distance in meters.
            // loc.speed is m/s. timeInHours * 3600 = timeInSeconds.
            // distance = loc.speed * timeInHours * 3600.
            val distanceInMeters = speed * timeInHours * 3600.0
            val dest = KMapUtils.rhumbDestinationPoint(loc.latitude, loc.longitude, distanceInMeters, bearing.toDouble())
            return AisLatLon(dest.latitude, dest.longitude)
        }
        return null
    }

    private fun getCrossingTimes(x: AisLocation, y: AisLocation): Pair<Double, Double>? {
        val lonCorrection = getLonCorrection(x)
        val vX = locationToVector(x, lonCorrection)
        val vY = locationToVector(y, lonCorrection)
        val vVX = courseToVector(x.bearing.toDouble(), getSpeedInKnots(x).toDouble())
        val vVY = courseToVector(y.bearing.toDouble(), getSpeedInKnots(y).toDouble())
        val vDXY = vX.sub(vY)
        val divisor = vVX.x * vVY.y - vVX.y * vVY.x

        if (abs(divisor) < 1.0E-10 || lonCorrection < 1.0E-10) {
            return null
        }
        val t1 = (vVY.x * vDXY.y - vVY.y * vDXY.x) / divisor
        val t2 = (vVX.x * vDXY.y - vVX.y * vDXY.x) / divisor
        return Pair(t1, t2)
    }

    private fun getLonCorrection(loc: AisLocation?): Double {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (((now - lastCorrectionUpdate) / 1000 / 60) > maxCorrectionUpdateAgeInMin) {
            correctionFactor = calculateLonCorrection(loc)
            lastCorrectionUpdate = now
        }
        return correctionFactor
    }

    private fun calculateLonCorrection(loc: AisLocation?): Double {
        if (loc != null) {
            val x = AisLocation(loc.latitude, loc.longitude, knotsToMeterPerSecond(1.0f), 90.0f)
            val yEast = getNewPosition(x, 1.0)
            if (yEast != null) {
                val diffLon = yEast.longitude - x.longitude
                return diffLon * 60.0
            }
        }
        return 1.0
    }

    fun knotsToMeterPerSecond(speed: Float): Float {
        return speed * 1852 / 3600
    }

    fun meterPerSecondToKnots(speed: Float): Float {
        return speed * 3600 / 1852
    }

    fun meterToMiles(x: Float): Float {
        return x / 1852.0f
    }

    private fun courseToVector(cog: Double, sog: Double): Vector {
        var alpha = 450.0 - cog
        while (alpha < 0) { alpha += 360.0 }
        while (alpha >= 360.0) { alpha -= 360.0 }
        alpha = alpha * kotlin.math.PI / 180.0
        return Vector(cos(alpha) * sog, sin(alpha) * sog)
    }

    private fun locationToVector(loc: AisLocation, lonCorrection: Double): Vector {
        return Vector(loc.longitude * 60.0 / lonCorrection, loc.latitude * 60.0)
    }

    private fun checkSpeedAndBearing(x: AisLocation, y: AisLocation): Boolean {
        return !x.hasBearing || !y.hasBearing || !x.hasSpeed || !y.hasSpeed
    }

    private fun getSpeedInKnots(loc: AisLocation): Float {
        return meterPerSecondToKnots(loc.speed)
    }
}
