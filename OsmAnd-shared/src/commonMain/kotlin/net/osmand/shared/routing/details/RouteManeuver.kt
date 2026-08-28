package net.osmand.shared.routing.details

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Exact semantic mapping of Android `TurnType` integer constants. */
@Serializable
enum class RouteManeuverType(val legacyValue: Int) {
	@SerialName("straight")
	STRAIGHT(1),

	@SerialName("turn_left")
	TURN_LEFT(2),

	@SerialName("slight_left")
	SLIGHT_LEFT(3),

	@SerialName("sharp_left")
	SHARP_LEFT(4),

	@SerialName("turn_right")
	TURN_RIGHT(5),

	@SerialName("slight_right")
	SLIGHT_RIGHT(6),

	@SerialName("sharp_right")
	SHARP_RIGHT(7),

	@SerialName("keep_left")
	KEEP_LEFT(8),

	@SerialName("keep_right")
	KEEP_RIGHT(9),

	@SerialName("u_turn")
	U_TURN(10),

	@SerialName("right_u_turn")
	RIGHT_U_TURN(11),

	@SerialName("off_route")
	OFF_ROUTE(12),

	@SerialName("roundabout")
	ROUNDABOUT(13),

	@SerialName("roundabout_left")
	ROUNDABOUT_LEFT(14);

	companion object {
		fun fromLegacyValue(value: Int): RouteManeuverType? = entries.find { it.legacyValue == value }
	}
}

/** Exact backend fields from Android `ExitInfo`. */
@Serializable
data class RouteExitInfo(
	val ref: String? = null,
	val exitStreetName: String? = null,
)

/**
 * Immutable copy of Android `RouteDirectionInfo` and its `TurnType`.
 *
 * [turnTypeValue] retains the integer value verbatim; [type] is only a nullable convenience mapping
 * for the fourteen values currently declared by Android.
 *
 * Android uses `0` as the ordinary default for [routeEndPointOffset]. It is therefore preserved
 * verbatim and is not required to follow [routePointOffset].
 *
 * [averageSpeedMetersPerSecond] also remains verbatim. Android's direction aggregation can produce
 * `NaN` for a zero-distance, zero-time group, and `RouteDirectionInfo.getExpectedTime()` preserves
 * Java's `Math.round(NaN) == 0` behavior.
 */
@Serializable
data class RouteManeuver(
	val turnTypeValue: Int,
	val routePointOffset: Int,
	val routeEndPointOffset: Int,
	val distanceMeters: Int,
	val expectedTimeSeconds: Int,
	val afterLeftTimeSeconds: Int,
	val averageSpeedMetersPerSecond: Float,
	val turnAngleDegrees: Float,
	val exitNumber: Int,
	val lanes: List<Int>? = null,
	val skipToSpeak: Boolean = false,
	val possibleLeftTurn: Boolean = false,
	val possibleRightTurn: Boolean = false,
	val otherTurnAngles: List<Float>? = null,
	val streetName: String? = null,
	val ref: String? = null,
	val destinationName: String? = null,
	val destinationRef: String? = null,
	val exitInfo: RouteExitInfo? = null,
) {
	/** Semantic view that does not replace or normalize [turnTypeValue]. */
	val type: RouteManeuverType?
		get() = RouteManeuverType.fromLegacyValue(turnTypeValue)

	init {
		require(routePointOffset >= 0) { "Maneuver route point offset must not be negative" }
		require(routeEndPointOffset >= 0) { "Maneuver end point offset must not be negative" }
		require(distanceMeters >= 0) { "Maneuver distance must not be negative" }
		require(expectedTimeSeconds >= 0) { "Maneuver expected time must not be negative" }
		require(afterLeftTimeSeconds >= 0) { "Maneuver time to finish must not be negative" }
		require(turnAngleDegrees.isFinite()) { "Maneuver turn angle must be finite" }
		require(otherTurnAngles?.all { it.isFinite() } != false) { "Other maneuver angles must be finite" }
	}
}
