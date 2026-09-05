package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils

/** Platform-owned alarm preferences consumed by the shared Android-compatible selection logic. */
data class RouteEventSelectionOptions(
	val routingAlarmsEnabled: Boolean,
	val showCameras: Boolean,
	val speakSpeedCameras: Boolean,
	val showTunnels: Boolean,
	val speakTunnels: Boolean,
	val showPedestrian: Boolean,
	val speakPedestrian: Boolean,
	val showTrafficWarnings: Boolean,
	val speakTrafficWarnings: Boolean,
)

/** One event selected by Android alarm rules, identified by its index in the input event list. */
data class RouteEventSelection(
	val sourceIndex: Int,
	val announce: Boolean,
)

/**
 * Shared home for ports of Android route-event methods; there is no legacy Android class named
 * `RouteEventHelper` to port wholesale.
 *
 * Tag classification follows `AlarmInfo.createAlarmInfo`. Priority follows
 * `AlarmInfo.updateDistanceAndGetPriority`. Event selection follows `WaypointHelper.calculateAlarms`
 * and the alarm-specific result of `WaypointHelper.sortList`. Native direction applicability
 * remains in the later Android adapter because it requires a `RouteDataObject`.
 */
object RouteEventHelper {

	private const val DUPLICATE_CAMERA_DISTANCE_METERS = 150.0
	private const val DUPLICATE_RAILWAY_DISTANCE_METERS = 50.0

	/** Exact equivalent of Android `AlarmInfo.updateDistanceAndGetPriority(float, float)`. */
	fun updateDistanceAndGetPriority(
		type: RouteEventType,
		timeSeconds: Float,
		distanceMeters: Float,
	): Int {
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

	/** Exact immutable equivalent of Android `AlarmInfo.createSpeedLimit`. */
	fun createSpeedLimit(
		speed: Int,
		location: KLatLon,
		speedMetersPerSecond: Float,
	): RouteEvent {
		return RouteEvent(
			type = RouteEventType.SPEED_LIMIT,
			location = location,
			locationIndex = 0,
			intValue = speed,
			floatValue = speedMetersPerSecond,
		)
	}

	/** Exact semantic mapping from Android `AlarmInfo.createAlarmInfo`. */
	fun createFromRouteTag(
		tag: String?,
		value: String?,
		locationIndex: Int,
		location: KLatLon,
	): RouteEvent? {
		val type = classifyType(tag, value)
		return type?.let {
			RouteEvent(
				type = it,
				location = location,
				locationIndex = locationIndex,
			)
		}
	}

	/** Type-only form used by Android before its native direction-applicability check. */
	fun classifyType(tag: String?, value: String?): RouteEventType? {
		return when (tag) {
			"highway" -> when (value) {
				"speed_camera" -> RouteEventType.SPEED_CAMERA
				"stop" -> RouteEventType.STOP
				else -> null
			}
			"enforcement" -> when (value) {
				"traffic_signals" -> RouteEventType.RED_LIGHT_CAMERA
				else -> null
			}
			"barrier" -> when (value) {
				"toll_booth" -> RouteEventType.TOLL_BOOTH
				"border_control" -> RouteEventType.BORDER_CONTROL
				else -> null
			}
			"traffic_calming" -> when (value) {
				"island", "choked_island", "painted_island" -> null
				else -> RouteEventType.TRAFFIC_CALMING
			}
			"hazard" -> RouteEventType.HAZARD
			"railway" -> when (value) {
				"level_crossing" -> RouteEventType.RAILWAY
				else -> null
			}
			"crossing" -> when (value) {
				"uncontrolled" -> RouteEventType.PEDESTRIAN
				else -> null
			}
			else -> null
		}
	}

	/**
	 * Immutable port of Android `WaypointHelper.calculateAlarms` followed by `sortList`.
	 *
	 * Camera and railway duplicate checks deliberately compare only with the last accepted event of
	 * the same Android group and run before route-index sorting, matching Android.
	 */
	fun select(
		events: List<RouteEvent>,
		options: RouteEventSelectionOptions,
	): List<RouteEventSelection> {
		if (!options.routingAlarmsEnabled) {
			return emptyList()
		}
		var previousCamera: RouteEvent? = null
		var previousRailway: RouteEvent? = null
		val selected = ArrayList<RouteEventSelection>()
		for (sourceIndex in events.indices) {
			val event = events[sourceIndex]
			when {
				event.type.isTrafficCamera() -> {
					if (options.showCameras || options.speakSpeedCameras) {
						val distance = previousCamera?.let { previous ->
							KMapUtils.getDistance(previous.location, event.location)
						}
						if (distance == null || distance >= DUPLICATE_CAMERA_DISTANCE_METERS) {
							selected.add(RouteEventSelection(sourceIndex, options.speakSpeedCameras))
							previousCamera = event
						}
					}
				}
				event.type == RouteEventType.TUNNEL -> {
					if (options.showTunnels || options.speakTunnels) {
						selected.add(RouteEventSelection(sourceIndex, options.speakTunnels))
					}
				}
				event.type == RouteEventType.PEDESTRIAN -> {
					if (options.showPedestrian || options.speakPedestrian) {
						selected.add(RouteEventSelection(sourceIndex, options.speakPedestrian))
					}
				}
				event.type == RouteEventType.RAILWAY -> {
					val distance = previousRailway?.let { previous ->
						KMapUtils.getDistance(previous.location, event.location)
					}
					if (distance == null || distance >= DUPLICATE_RAILWAY_DISTANCE_METERS) {
						selected.add(RouteEventSelection(sourceIndex, options.speakTrafficWarnings))
						previousRailway = event
					}
				}
				options.showTrafficWarnings || options.speakTrafficWarnings -> {
					selected.add(RouteEventSelection(sourceIndex, options.speakTrafficWarnings))
				}
			}
		}
		// MutableList.sortWith is stable, so events with the same route index retain input order.
		selected.sortWith(compareBy { events[it.sourceIndex].locationIndex })
		return selected
	}
}
