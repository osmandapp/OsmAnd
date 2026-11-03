package net.osmand.shared.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * A geographic fix: position at a point in time, optionally with altitude, speed, bearing and
 * accuracy. Port of `net.osmand.Location`, which in turn descends from the AOSP `Location` class.
 *
 * Optional values keep the Java shape of a primitive plus a `hasX` flag rather than a nullable
 * primitive, because a route holds thousands of fixes and `Float?` would box every one of them.
 * Setting a value raises its flag, `removeX()` clears both.
 *
 * [distanceTo] and [bearingTo] share one Vincenty solution through an immutable cache snapshot, so
 * calling both for the same pair costs a single computation without needing a lock.
 */
class KLocation {

	var provider: String? = null

	var time: Long = 0

	var latitude: Double = 0.0

	var longitude: Double = 0.0

	var hasAltitude: Boolean = false
		private set

	var altitude: Double = 0.0
		set(value) {
			field = value
			hasAltitude = true
		}

	var hasSpeed: Boolean = false
		private set

	var speed: Float = 0f
		set(value) {
			field = value
			hasSpeed = true
		}

	var hasBearing: Boolean = false
		private set

	var bearing: Float = 0f
		set(value) {
			field = value
			hasBearing = true
		}

	var hasAccuracy: Boolean = false
		private set

	var accuracy: Float = 0f
		set(value) {
			field = value
			hasAccuracy = true
		}

	var hasVerticalAccuracy: Boolean = false
		private set

	var verticalAccuracy: Float = 0f
		set(value) {
			field = value
			hasVerticalAccuracy = true
		}

	private var cache: DistanceBearing? = null

	constructor(provider: String?) {
		this.provider = provider
	}

	constructor(provider: String?, latitude: Double, longitude: Double) {
		this.provider = provider
		this.latitude = latitude
		this.longitude = longitude
	}

	constructor(other: KLocation) {
		set(other)
	}

	/** Copies every field of [other] into this fix. */
	fun set(other: KLocation) {
		provider = other.provider
		time = other.time
		latitude = other.latitude
		longitude = other.longitude
		altitude = other.altitude
		hasAltitude = other.hasAltitude
		speed = other.speed
		hasSpeed = other.hasSpeed
		bearing = other.bearing
		hasBearing = other.hasBearing
		accuracy = other.accuracy
		hasAccuracy = other.hasAccuracy
		verticalAccuracy = other.verticalAccuracy
		hasVerticalAccuracy = other.hasVerticalAccuracy
		cache = null
	}

	fun reset() {
		provider = null
		time = 0
		latitude = 0.0
		longitude = 0.0
		removeAltitude()
		removeSpeed()
		removeBearing()
		removeAccuracy()
		removeVerticalAccuracy()
		cache = null
	}

	fun removeAltitude() {
		altitude = 0.0
		hasAltitude = false
	}

	fun removeSpeed() {
		speed = 0f
		hasSpeed = false
	}

	fun removeBearing() {
		bearing = 0f
		hasBearing = false
	}

	fun removeAccuracy() {
		accuracy = 0f
		hasAccuracy = false
	}

	fun removeVerticalAccuracy() {
		verticalAccuracy = 0f
		hasVerticalAccuracy = false
	}

	/** Distance in meters to [dest] along the WGS84 ellipsoid. */
	fun distanceTo(dest: KLocation): Float = solve(dest).distance

	/** Initial bearing in degrees east of true north on the shortest path to [dest]. */
	fun bearingTo(dest: KLocation): Float = solve(dest).initialBearing

	fun toKLatLon(): KLatLon = KLatLon(latitude, longitude)

	private fun solve(dest: KLocation): DistanceBearing {
		val cached = cache
		if (cached != null &&
			cached.lat1 == latitude && cached.lon1 == longitude &&
			cached.lat2 == dest.latitude && cached.lon2 == dest.longitude
		) {
			return cached
		}
		val results = FloatArray(2)
		computeDistanceAndBearing(latitude, longitude, dest.latitude, dest.longitude, results)
		val computed = DistanceBearing(
			latitude, longitude, dest.latitude, dest.longitude, results[0], results[1]
		)
		// a whole immutable snapshot is published at once, so a racing reader never mixes
		// coordinates of one pair with the distance of another
		cache = computed
		return computed
	}

	override fun toString(): String =
		"Location[provider=$provider,time=$time,latitude=$latitude,longitude=$longitude," +
				"hasAltitude=$hasAltitude,altitude=$altitude,hasSpeed=$hasSpeed,speed=$speed," +
				"hasBearing=$hasBearing,bearing=$bearing,hasAccuracy=$hasAccuracy,accuracy=$accuracy," +
				"hasVerticalAccuracy=$hasVerticalAccuracy,verticalAccuracy=$verticalAccuracy]"

	private class DistanceBearing(
		val lat1: Double,
		val lon1: Double,
		val lat2: Double,
		val lon2: Double,
		val distance: Float,
		val initialBearing: Float
	)

	companion object {

		private const val MAX_ITERATIONS = 20

		/** WGS84 major axis, meters. */
		private const val WGS84_A = 6378137.0

		/** WGS84 semi-major axis, meters. */
		private const val WGS84_B = 6356752.3142

		/**
		 * Distance and, when [results] is long enough, initial and final bearing between two points.
		 *
		 * `results[0]` is the distance in meters, `results[1]` the initial bearing in degrees and
		 * `results[2]` the final bearing in degrees.
		 */
		fun distanceBetween(
			startLatitude: Double,
			startLongitude: Double,
			endLatitude: Double,
			endLongitude: Double,
			results: FloatArray
		) {
			require(results.isNotEmpty()) { "results must hold at least one value" }
			computeDistanceAndBearing(startLatitude, startLongitude, endLatitude, endLongitude, results)
		}

		/**
		 * Vincenty inverse solution on the WGS84 ellipsoid,
		 * see http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf section 4.
		 */
		private fun computeDistanceAndBearing(
			latitude1: Double,
			longitude1: Double,
			latitude2: Double,
			longitude2: Double,
			results: FloatArray
		) {
			val lat1 = latitude1 * PI / 180.0
			val lat2 = latitude2 * PI / 180.0
			val lon1 = longitude1 * PI / 180.0
			val lon2 = longitude2 * PI / 180.0

			val a = WGS84_A
			val b = WGS84_B
			val f = (a - b) / a
			val aSqMinusBSqOverBSq = (a * a - b * b) / (b * b)

			val bigL = lon2 - lon1
			var bigA = 0.0
			val u1 = atan((1.0 - f) * tan(lat1))
			val u2 = atan((1.0 - f) * tan(lat2))

			val cosU1 = cos(u1)
			val cosU2 = cos(u2)
			val sinU1 = sin(u1)
			val sinU2 = sin(u2)
			val cosU1cosU2 = cosU1 * cosU2
			val sinU1sinU2 = sinU1 * sinU2

			var sigma = 0.0
			var deltaSigma = 0.0
			var cosSqAlpha = 0.0
			var cos2SM = 0.0
			var cosSigma = 0.0
			var sinSigma = 0.0
			var cosLambda = 0.0
			var sinLambda = 0.0

			var lambda = bigL
			for (iteration in 0 until MAX_ITERATIONS) {
				val lambdaOrig = lambda
				cosLambda = cos(lambda)
				sinLambda = sin(lambda)
				val t1 = cosU2 * sinLambda
				val t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda
				val sinSqSigma = t1 * t1 + t2 * t2
				sinSigma = sqrt(sinSqSigma)
				cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda
				sigma = atan2(sinSigma, cosSigma)
				val sinAlpha = if (sinSigma == 0.0) 0.0 else cosU1cosU2 * sinLambda / sinSigma
				cosSqAlpha = 1.0 - sinAlpha * sinAlpha
				cos2SM = if (cosSqAlpha == 0.0) 0.0 else cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha

				val uSquared = cosSqAlpha * aSqMinusBSqOverBSq
				bigA = 1 + (uSquared / 16384.0) *
						(4096.0 + uSquared * (-768 + uSquared * (320.0 - 175.0 * uSquared)))
				val bigB = (uSquared / 1024.0) *
						(256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)))
				val bigC = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha))
				val cos2SMSq = cos2SM * cos2SM
				deltaSigma = bigB * sinSigma *
						(cos2SM + (bigB / 4.0) *
								(cosSigma * (-1.0 + 2.0 * cos2SMSq) -
										(bigB / 6.0) * cos2SM *
										(-3.0 + 4.0 * sinSigma * sinSigma) *
										(-3.0 + 4.0 * cos2SMSq)))

				lambda = bigL + (1.0 - bigC) * f * sinAlpha *
						(sigma + bigC * sinSigma *
								(cos2SM + bigC * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM)))

				val delta = (lambda - lambdaOrig) / lambda
				if (abs(delta) < 1.0e-12) {
					break
				}
			}

			results[0] = (b * bigA * (sigma - deltaSigma)).toFloat()
			if (results.size > 1) {
				val initialBearing = atan2(
					cosU2 * sinLambda,
					cosU1 * sinU2 - sinU1 * cosU2 * cosLambda
				) * 180.0 / PI
				results[1] = initialBearing.toFloat()
				if (results.size > 2) {
					val finalBearing = atan2(
						cosU1 * sinLambda,
						-sinU1 * cosU2 + cosU1 * sinU2 * cosLambda
					) * 180.0 / PI
					results[2] = finalBearing.toFloat()
				}
			}
		}
	}
}
