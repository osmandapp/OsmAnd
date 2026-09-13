package net.osmand.shared.routing.details

import kotlinx.serialization.Serializable

/** One route encoding rule, preserving source order, duplicate tags, and nullable values. */
@Serializable
data class RouteTypeAttribute(
	val tag: String,
	val value: String?,
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
)
