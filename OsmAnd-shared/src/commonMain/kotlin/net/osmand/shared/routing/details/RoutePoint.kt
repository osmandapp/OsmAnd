package net.osmand.shared.routing.details

import kotlinx.serialization.Serializable
import net.osmand.shared.data.KLatLon

/**
 * Immutable copy of one Android `RouteCalculationResult` location.
 *
 * [distanceToFinishMeters] mirrors the integer `listDistance` entry calculated by Android.
 * Raw location time is retained without interpreting it as route travel time.
 */
@Serializable
data class RoutePoint(
	val location: KLatLon,
	val distanceToFinishMeters: Int,
	val altitudeMeters: Double? = null,
	val speedMetersPerSecond: Float? = null,
	val timeMillis: Long = 0L,
	val provider: String? = null,
) {
	init {
		require(distanceToFinishMeters >= 0) { "Route point distance to finish must not be negative" }
		require(altitudeMeters == null || altitudeMeters.isFinite()) {
			"Route point altitude must be finite"
		}
		require(speedMetersPerSecond == null || speedMetersPerSecond.isFinite()) {
			"Route point speed must be finite"
		}
	}
}
