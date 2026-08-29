package net.osmand.shared.routing.details

import kotlinx.serialization.Serializable

/** One route encoding rule, preserving source order and duplicate tags. */
@Serializable
data class RouteTypeAttribute(
	val tag: String,
	val value: String,
)

/**
 * Immutable copy of an Android `RouteSegmentResult` and the exact values used by Route Details.
 *
 * [heightValues] retains the flattened distance/height pairs returned by
 * `RouteSegmentResult.getHeightValues()`. They are deliberately not reconstructed from route points.
 */
@Serializable
data class RouteSegment(
	val routePointStartIndex: Int,
	val routePointEndIndex: Int,
	val nativeStartPointIndex: Int,
	val nativeEndPointIndex: Int,
	val distanceMeters: Float,
	val segmentTimeSeconds: Float,
	val segmentSpeedMetersPerSecond: Float,
	val roadId: Long,
	val forward: Boolean,
	val roadName: String? = null,
	val ref: String? = null,
	val destinationName: String? = null,
	val destinationRef: String? = null,
	val highway: String? = null,
	val maximumSpeedMetersPerSecond: Float = 0f,
	val lanes: Int = -1,
	val oneWayDirection: Int = 0,
	val roundabout: Boolean = false,
	val tunnel: Boolean = false,
	val routeTypes: List<RouteTypeAttribute> = emptyList(),
	val heightValues: FloatArray = floatArrayOf(),
) {
	init {
		require(routePointStartIndex >= 0) { "Route segment start index must not be negative" }
		require(routePointEndIndex >= routePointStartIndex) {
			"Route segment end index must follow its start index"
		}
		require(nativeStartPointIndex >= 0 && nativeEndPointIndex >= 0) {
			"Native route segment indices must not be negative"
		}
		require(distanceMeters.isFinite() && distanceMeters >= 0f) {
			"Route segment distance must be finite and non-negative"
		}
		require(segmentTimeSeconds.isFinite() && segmentTimeSeconds >= 0f) {
			"Route segment time must be finite and non-negative"
		}
		require(segmentSpeedMetersPerSecond.isFinite()) { "Route segment speed must be finite" }
		require(maximumSpeedMetersPerSecond.isFinite()) { "Route segment maximum speed must be finite" }
		require(heightValues.size % 2 == 0) { "Route segment height values must contain distance/height pairs" }
		require(heightValues.all { it.isFinite() }) { "Route segment height values must be finite" }
	}
}
