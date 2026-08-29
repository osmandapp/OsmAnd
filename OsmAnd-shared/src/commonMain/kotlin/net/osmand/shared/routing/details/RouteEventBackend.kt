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

/** One event selected by Android alarm rules, including its announcement flag. */
data class RouteEventSelection(
	val event: RouteEvent,
	val announce: Boolean,
)

/**
 * Shared home for ports of Android route-event methods; Android has no class named
 * `RouteEventBackend`.
 *
 * Tag classification follows `AlarmInfo.createAlarmInfo`. Event selection follows
 * `WaypointHelper.calculateAlarms` and the alarm-specific result of `WaypointHelper.sortList`.
 * Native direction applicability remains in the later Android adapter because it requires a
 * `RouteDataObject`.
 */
object RouteEventBackend {

	private const val DUPLICATE_CAMERA_DISTANCE_METERS = 150.0
	private const val DUPLICATE_RAILWAY_DISTANCE_METERS = 50.0

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
		val type = when (tag) {
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
		return type?.let {
			RouteEvent(
				type = it,
				location = location,
				locationIndex = locationIndex,
			)
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
		for (event in events) {
			when {
				event.type.isTrafficCamera() -> {
					if (options.showCameras || options.speakSpeedCameras) {
						val distance = previousCamera?.let { previous ->
							KMapUtils.getDistance(previous.location, event.location)
						}
						if (distance == null || distance >= DUPLICATE_CAMERA_DISTANCE_METERS) {
							selected.add(RouteEventSelection(event, options.speakSpeedCameras))
							previousCamera = event
						}
					}
				}
				event.type == RouteEventType.TUNNEL -> {
					if (options.showTunnels || options.speakTunnels) {
						selected.add(RouteEventSelection(event, options.speakTunnels))
					}
				}
				event.type == RouteEventType.PEDESTRIAN -> {
					if (options.showPedestrian || options.speakPedestrian) {
						selected.add(RouteEventSelection(event, options.speakPedestrian))
					}
				}
				event.type == RouteEventType.RAILWAY -> {
					val distance = previousRailway?.let { previous ->
						KMapUtils.getDistance(previous.location, event.location)
					}
					if (distance == null || distance >= DUPLICATE_RAILWAY_DISTANCE_METERS) {
						selected.add(RouteEventSelection(event, options.speakTrafficWarnings))
						previousRailway = event
					}
				}
				options.showTrafficWarnings || options.speakTrafficWarnings -> {
					selected.add(RouteEventSelection(event, options.speakTrafficWarnings))
				}
			}
		}
		// MutableList.sortWith is stable, so events with the same route index retain input order.
		selected.sortWith(compareBy { it.event.locationIndex })
		return selected
	}
}
