package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteEventBackendTest {

	@Test
	fun routeTagsMapExactlyLikeAndroidAlarmInfo() {
		val location = KLatLon(51.5, -0.1)
		val mappings = listOf(
			Triple("highway", "speed_camera", RouteEventType.SPEED_CAMERA),
			Triple("highway", "stop", RouteEventType.STOP),
			Triple("enforcement", "traffic_signals", RouteEventType.RED_LIGHT_CAMERA),
			Triple("barrier", "toll_booth", RouteEventType.TOLL_BOOTH),
			Triple("barrier", "border_control", RouteEventType.BORDER_CONTROL),
			Triple("traffic_calming", "bump", RouteEventType.TRAFFIC_CALMING),
			Triple("hazard", "falling_rocks", RouteEventType.HAZARD),
			Triple("railway", "level_crossing", RouteEventType.RAILWAY),
			Triple("crossing", "uncontrolled", RouteEventType.PEDESTRIAN),
		)

		for ((tag, value, expectedType) in mappings) {
			val event = RouteEventBackend.createFromRouteTag(tag, value, 7, location)
			assertEquals(expectedType, event?.type)
			assertEquals(7, event?.locationIndex)
			assertEquals(location, event?.location)
			assertEquals(-1, event?.lastLocationIndex)
			assertEquals(0, event?.intValue)
			assertEquals(0f, event?.floatValue)
		}
	}

	@Test
	fun unsupportedAndIslandTagsDoNotCreateAndroidAlarms() {
		val location = KLatLon(0.0, 0.0)
		val unsupported = listOf(
			"highway" to "residential",
			"enforcement" to "speed_camera",
			"barrier" to "gate",
			"traffic_calming" to "island",
			"traffic_calming" to "choked_island",
			"traffic_calming" to "painted_island",
			"railway" to "tram",
			"crossing" to "traffic_signals",
			"Highway" to "speed_camera",
			"unknown" to "stop",
		)

		for ((tag, value) in unsupported) {
			assertNull(RouteEventBackend.createFromRouteTag(tag, value, 0, location))
		}
		assertNull(RouteEventBackend.createFromRouteTag("highway", null, 0, location))
		assertNull(RouteEventBackend.createFromRouteTag(null, "stop", 0, location))
		assertEquals(
			RouteEventType.TRAFFIC_CALMING,
			RouteEventBackend.createFromRouteTag("traffic_calming", null, 0, location)?.type,
		)
		assertEquals(
			RouteEventType.HAZARD,
			RouteEventBackend.createFromRouteTag("hazard", null, 0, location)?.type,
		)
	}

	@Test
	fun trafficCameraClassificationMatchesAndroid() {
		assertTrue(RouteEventType.SPEED_CAMERA.isTrafficCamera())
		assertTrue(RouteEventType.RED_LIGHT_CAMERA.isTrafficCamera())
		assertFalse(RouteEventType.SPEED_LIMIT.isTrafficCamera())
		assertFalse(RouteEventType.RAILWAY.isTrafficCamera())
	}

	@Test
	fun speedLimitCreationMatchesAndroidAlarmInfo() {
		val location = KLatLon(52.0, 13.0)

		val event = RouteEventBackend.createSpeedLimit(
			speed = 50,
			location = location,
			speedMetersPerSecond = 13.8889f,
		)

		assertEquals(RouteEventType.SPEED_LIMIT, event.type)
		assertEquals(location, event.location)
		assertEquals(0, event.locationIndex)
		assertEquals(-1, event.lastLocationIndex)
		assertEquals(50, event.intValue)
		assertEquals(13.8889f, event.floatValue)
	}

	@Test
	fun priorityThresholdsMatchAndroidAlarmInfo() {
		assertEquals(
			Int.MAX_VALUE,
			event(RouteEventType.SPEED_LIMIT).updateDistanceAndGetPriority(0f, 1501f),
		)
		assertEquals(2, event(RouteEventType.SPEED_LIMIT).updateDistanceAndGetPriority(100f, 1500f))
		assertEquals(7, event(RouteEventType.STOP).updateDistanceAndGetPriority(5.9f, 1500f))
		assertEquals(7, event(RouteEventType.STOP).updateDistanceAndGetPriority(100f, 74.9f))
		assertEquals(1, event(RouteEventType.SPEED_CAMERA).updateDistanceAndGetPriority(14.9f, 1000f))
		assertEquals(12, event(RouteEventType.RED_LIGHT_CAMERA).updateDistanceAndGetPriority(100f, 149f))
		assertEquals(6, event(RouteEventType.TOLL_BOOTH).updateDistanceAndGetPriority(29.9f, 1000f))
		assertEquals(6, event(RouteEventType.TOLL_BOOTH).updateDistanceAndGetPriority(100f, 499f))
		assertEquals(17, event(RouteEventType.STOP).updateDistanceAndGetPriority(6.9f, 500f))
		assertEquals(17, event(RouteEventType.STOP).updateDistanceAndGetPriority(100f, 99f))
		assertEquals(Int.MAX_VALUE, event(RouteEventType.STOP).updateDistanceAndGetPriority(7f, 100f))
		assertEquals(Int.MAX_VALUE, event(RouteEventType.SPEED_CAMERA).updateDistanceAndGetPriority(15f, 150f))
	}

	@Test
	fun selectionMatchesAndroidFilteringDeduplicationAndSortingOrder() {
		val cameraAtEight = event(RouteEventType.SPEED_CAMERA, index = 8, longitude = 0.0)
		val duplicateRedLightAtTwo = event(RouteEventType.RED_LIGHT_CAMERA, index = 2, longitude = 0.001)
		val cameraAtFive = event(RouteEventType.SPEED_CAMERA, index = 5, longitude = 0.002)
		val railwayAtSeven = event(RouteEventType.RAILWAY, index = 7, longitude = 0.0)
		val duplicateRailwayAtOne = event(RouteEventType.RAILWAY, index = 1, longitude = 0.0004)
		val railwayAtFour = event(RouteEventType.RAILWAY, index = 4, longitude = 0.001)
		val tunnelAtSix = event(
			RouteEventType.TUNNEL,
			index = 6,
			lastIndex = 9,
			floatValue = 321.5f,
		)
		val pedestrianAtThree = event(RouteEventType.PEDESTRIAN, index = 3)
		val stopAtZero = event(RouteEventType.STOP, index = 0)
		val input = listOf(
			cameraAtEight,
			duplicateRedLightAtTwo,
			cameraAtFive,
			railwayAtSeven,
			duplicateRailwayAtOne,
			railwayAtFour,
			tunnelAtSix,
			pedestrianAtThree,
			stopAtZero,
		)
		val options = options(
			showCameras = true,
			speakTunnels = true,
			showPedestrian = true,
			speakTrafficWarnings = true,
		)

		val selected = RouteEventBackend.select(input, options)

		assertEquals(listOf(0, 3, 4, 5, 6, 7, 8), selected.map { it.event.locationIndex })
		assertEquals(listOf(true, false, true, false, true, true, false), selected.map { it.announce })
		assertEquals(tunnelAtSix, selected.single { it.event.type == RouteEventType.TUNNEL }.event)
	}

	@Test
	fun cameraAndRailwayThresholdsUseLastAcceptedEventBeforeSorting() {
		val cameraEvents = listOf(
			event(RouteEventType.SPEED_CAMERA, index = 6, longitude = longitudeForMeters(0.0)),
			event(RouteEventType.RED_LIGHT_CAMERA, index = 1, longitude = longitudeForMeters(149.0)),
			event(RouteEventType.SPEED_CAMERA, index = 4, longitude = longitudeForMeters(151.0)),
		)
		val railwayEvents = listOf(
			event(RouteEventType.RAILWAY, index = 7, longitude = longitudeForMeters(0.0)),
			event(RouteEventType.RAILWAY, index = 2, longitude = longitudeForMeters(49.0)),
			event(RouteEventType.RAILWAY, index = 5, longitude = longitudeForMeters(51.0)),
		)

		val cameras = RouteEventBackend.select(cameraEvents, options(showCameras = true))
		val railways = RouteEventBackend.select(railwayEvents, options())

		assertEquals(listOf(4, 6), cameras.map { it.event.locationIndex })
		assertEquals(listOf(5, 7), railways.map { it.event.locationIndex })
	}

	@Test
	fun routingAlarmSwitchAndIndividualPreferencesMatchAndroid() {
		val events = listOf(
			event(RouteEventType.SPEED_CAMERA, index = 0),
			event(RouteEventType.TUNNEL, index = 1),
			event(RouteEventType.PEDESTRIAN, index = 2),
			event(RouteEventType.RAILWAY, index = 3),
			event(RouteEventType.STOP, index = 4),
		)

		assertEquals(emptyList(), RouteEventBackend.select(events, options(routingAlarmsEnabled = false)))

		val railwayOnly = RouteEventBackend.select(events, options())
		assertEquals(listOf(RouteEventType.RAILWAY), railwayOnly.map { it.event.type })
		assertEquals(listOf(false), railwayOnly.map(RouteEventSelection::announce))

		val spokenOnly = RouteEventBackend.select(
			events,
			options(
				speakSpeedCameras = true,
				speakTunnels = true,
				speakPedestrian = true,
				speakTrafficWarnings = true,
			),
		)
		assertEquals(events, spokenOnly.map(RouteEventSelection::event))
		assertTrue(spokenOnly.all(RouteEventSelection::announce))
	}

	@Test
	fun ordinaryWarningsAreNotDeduplicatedAndEqualIndexesRemainStable() {
		val events = listOf(
			event(RouteEventType.SPEED_LIMIT, index = 3, intValue = 50, floatValue = 13.8f),
			event(RouteEventType.BORDER_CONTROL, index = 3),
			event(RouteEventType.TRAFFIC_CALMING, index = 3),
			event(RouteEventType.TOLL_BOOTH, index = 3),
			event(RouteEventType.STOP, index = 3),
			event(RouteEventType.HAZARD, index = 3),
			event(RouteEventType.MAXIMUM, index = 3),
		)

		val selected = RouteEventBackend.select(events, options(showTrafficWarnings = true))

		assertEquals(events, selected.map(RouteEventSelection::event))
		assertTrue(selected.none(RouteEventSelection::announce))
	}

	@Test
	fun tunnelClosedAndOpenEndedRangesArePreservedVerbatim() {
		val closed = event(
			RouteEventType.TUNNEL,
			index = 2,
			lastIndex = 7,
			floatValue = 456.25f,
		)
		val openEnded = event(
			RouteEventType.TUNNEL,
			index = 8,
			lastIndex = -1,
			floatValue = 123.75f,
		)

		val result = RouteEventBackend.select(
			listOf(openEnded, closed),
			options(showTunnels = true),
		)

		assertEquals(listOf(closed, openEnded), result.map(RouteEventSelection::event))
	}

	private fun event(
		type: RouteEventType,
		index: Int = 0,
		lastIndex: Int = -1,
		longitude: Double = 0.0,
		intValue: Int = 0,
		floatValue: Float = 0f,
	): RouteEvent {
		return RouteEvent(
			type = type,
			location = KLatLon(0.0, longitude),
			locationIndex = index,
			lastLocationIndex = lastIndex,
			intValue = intValue,
			floatValue = floatValue,
		)
	}

	private fun options(
		routingAlarmsEnabled: Boolean = true,
		showCameras: Boolean = false,
		speakSpeedCameras: Boolean = false,
		showTunnels: Boolean = false,
		speakTunnels: Boolean = false,
		showPedestrian: Boolean = false,
		speakPedestrian: Boolean = false,
		showTrafficWarnings: Boolean = false,
		speakTrafficWarnings: Boolean = false,
	): RouteEventSelectionOptions {
		return RouteEventSelectionOptions(
			routingAlarmsEnabled = routingAlarmsEnabled,
			showCameras = showCameras,
			speakSpeedCameras = speakSpeedCameras,
			showTunnels = showTunnels,
			speakTunnels = speakTunnels,
			showPedestrian = showPedestrian,
			speakPedestrian = speakPedestrian,
			showTrafficWarnings = showTrafficWarnings,
			speakTrafficWarnings = speakTrafficWarnings,
		)
	}

	private fun longitudeForMeters(distanceMeters: Double): Double {
		return distanceMeters / KMapUtils.HAVERSINE_EARTH_RADIUS_METERS * 180.0 / PI
	}
}
