package net.osmand.shared.routing.details

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.osmand.shared.data.KLatLon

/** Cross-platform route event type, including Android's `MAXIMUM` priority sentinel. */
@Serializable
enum class RouteEventType(val androidPriority: Int) {
	@SerialName("speed_camera")
	SPEED_CAMERA(1),

	@SerialName("speed_limit")
	SPEED_LIMIT(2),

	@SerialName("border_control")
	BORDER_CONTROL(3),

	@SerialName("railway")
	RAILWAY(4),

	@SerialName("traffic_calming")
	TRAFFIC_CALMING(5),

	@SerialName("toll_booth")
	TOLL_BOOTH(6),

	@SerialName("stop")
	STOP(7),

	@SerialName("pedestrian")
	PEDESTRIAN(8),

	@SerialName("hazard")
	HAZARD(9),

	@SerialName("maximum")
	MAXIMUM(10),

	@SerialName("tunnel")
	TUNNEL(11),

	@SerialName("red_light_camera")
	RED_LIGHT_CAMERA(12);

	/** Whether this event is one of the traffic-camera variants. */
	fun isTrafficCamera(): Boolean {
		return this == SPEED_CAMERA || this == RED_LIGHT_CAMERA
	}
}

/**
 * Immutable copy of Android `AlarmInfo` before filtering, sorting, or deduplication.
 *
 * Android sentinel and boundary values are retained: [lastLocationIndex] may be `-1`, and
 * [locationIndex] may equal the route point count for point alarms created during conversion.
 */
@Serializable
data class RouteEvent(
	val type: RouteEventType,
	val location: KLatLon,
	val locationIndex: Int,
	val lastLocationIndex: Int = -1,
	val intValue: Int = 0,
	val floatValue: Float = 0f,
) {
	/**
	 * Exact equivalent of Android `AlarmInfo.updateDistanceAndGetPriority(float, float)`.
	 *
	 * The Android method does not mutate the alarm despite its legacy name.
	 */
	fun updateDistanceAndGetPriority(timeSeconds: Float, distanceMeters: Float): Int {
		if (distanceMeters > 1500) {
			return Int.MAX_VALUE
		}
		if (timeSeconds < 6 || distanceMeters < 75 || type == RouteEventType.SPEED_LIMIT) {
			return type.androidPriority
		}
		if (type.isTrafficCamera() && (timeSeconds < 15 || distanceMeters < 150)) {
			return type.androidPriority
		}
		if (type == RouteEventType.TOLL_BOOTH && (timeSeconds < 30 || distanceMeters < 500)) {
			return type.androidPriority
		}
		if (timeSeconds < 7 || distanceMeters < 100) {
			return type.androidPriority + RouteEventType.MAXIMUM.androidPriority
		}
		return Int.MAX_VALUE
	}

	init {
		require(locationIndex >= -1) { "Route event location index must preserve a valid Android sentinel" }
		require(lastLocationIndex >= -1) { "Route event last location index must preserve a valid Android sentinel" }
		require(floatValue.isFinite()) { "Route event float value must be finite" }
	}
}
