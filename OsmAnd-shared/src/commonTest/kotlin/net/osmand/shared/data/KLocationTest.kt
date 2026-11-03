package net.osmand.shared.data

import net.osmand.shared.util.KMapUtils
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KLocationTest {

	@Test
	fun testOptionalValuesTrackTheirFlags() {
		val location = KLocation("test")
		assertFalse(location.hasAltitude)
		assertFalse(location.hasSpeed)
		assertFalse(location.hasBearing)
		assertFalse(location.hasAccuracy)
		assertFalse(location.hasVerticalAccuracy)

		location.altitude = 120.5
		location.speed = 13.4f
		location.bearing = 91f
		location.accuracy = 4f
		location.verticalAccuracy = 8f

		assertTrue(location.hasAltitude)
		assertTrue(location.hasSpeed)
		assertTrue(location.hasBearing)
		assertTrue(location.hasAccuracy)
		assertTrue(location.hasVerticalAccuracy)

		location.removeAltitude()
		location.removeSpeed()
		location.removeBearing()
		location.removeAccuracy()
		location.removeVerticalAccuracy()

		assertFalse(location.hasAltitude)
		assertEquals(0.0, location.altitude)
		assertFalse(location.hasSpeed)
		assertEquals(0f, location.speed)
		assertFalse(location.hasBearing)
		assertFalse(location.hasAccuracy)
		assertFalse(location.hasVerticalAccuracy)
	}

	@Test
	fun testCopyConstructorAndSet() {
		val source = KLocation("gps", 50.45, 30.52)
		source.time = 1_700_000_000_000L
		source.speed = 5f
		source.altitude = 180.0

		val copy = KLocation(source)
		assertEquals("gps", copy.provider)
		assertEquals(50.45, copy.latitude)
		assertEquals(30.52, copy.longitude)
		assertEquals(1_700_000_000_000L, copy.time)
		assertTrue(copy.hasSpeed)
		assertEquals(5f, copy.speed)
		assertTrue(copy.hasAltitude)
		assertFalse(copy.hasBearing)

		val target = KLocation("other")
		target.bearing = 33f
		target.set(source)
		assertFalse(target.hasBearing, "flags must be copied, not merged")
		assertEquals("gps", target.provider)
	}

	@Test
	fun testReset() {
		val location = KLocation("gps", 1.0, 2.0)
		location.speed = 9f
		location.reset()
		assertEquals(null, location.provider)
		assertEquals(0.0, location.latitude)
		assertEquals(0.0, location.longitude)
		assertEquals(0L, location.time)
		assertFalse(location.hasSpeed)
	}

	@Test
	fun testDistanceMatchesSphericalApproximationWithinEllipsoidError() {
		// Vincenty on WGS84 vs the haversine used by KMapUtils: they must agree to ~0.5%
		val pairs = listOf(
			doubleArrayOf(50.4501, 30.5234, 50.4600, 30.5300),
			doubleArrayOf(52.5200, 13.4050, 48.8566, 2.3522),
			doubleArrayOf(-33.8688, 151.2093, -37.8136, 144.9631),
			doubleArrayOf(0.0, 0.0, 0.0, 1.0)
		)
		for (pair in pairs) {
			val from = KLocation("a", pair[0], pair[1])
			val to = KLocation("b", pair[2], pair[3])
			val vincenty = from.distanceTo(to).toDouble()
			val haversine = KMapUtils.getDistance(pair[0], pair[1], pair[2], pair[3])
			assertTrue(
				abs(vincenty - haversine) / haversine < 0.005,
				"distance mismatch: vincenty=$vincenty haversine=$haversine"
			)
		}
	}

	@Test
	fun testKnownDistanceAndBearing() {
		// one degree of longitude on the equator, and due east
		val from = KLocation("a", 0.0, 0.0)
		val to = KLocation("b", 0.0, 1.0)
		assertTrue(abs(from.distanceTo(to) - 111_319.5f) < 50f, "got ${from.distanceTo(to)}")
		assertTrue(abs(from.bearingTo(to) - 90f) < 0.001f, "got ${from.bearingTo(to)}")

		// due north
		val north = KLocation("c", 1.0, 0.0)
		assertTrue(abs(from.bearingTo(north)) < 0.001f, "got ${from.bearingTo(north)}")

		assertEquals(0f, from.distanceTo(KLocation("d", 0.0, 0.0)))
	}

	@Test
	fun testDistanceIsSymmetric() {
		val a = KLocation("a", 50.45, 30.52)
		val b = KLocation("b", 48.85, 2.35)
		assertTrue(abs(a.distanceTo(b) - b.distanceTo(a)) < 0.01f)
	}

	@Test
	fun testCacheIsInvalidatedWhenCoordinatesChange() {
		val from = KLocation("a", 0.0, 0.0)
		val to = KLocation("b", 0.0, 1.0)
		val first = from.distanceTo(to)
		// bearing must reuse the very same solution
		assertTrue(abs(from.bearingTo(to) - 90f) < 0.001f)

		to.longitude = 2.0
		val second = from.distanceTo(to)
		assertTrue(second > first * 1.9, "cache was not invalidated: $first -> $second")

		from.latitude = 10.0
		val third = from.distanceTo(to)
		assertTrue(abs(third - second) > 1f, "cache was not invalidated on source change")
	}

	@Test
	fun testDistanceBetweenFillsResults() {
		val results = FloatArray(3)
		KLocation.distanceBetween(0.0, 0.0, 0.0, 1.0, results)
		assertTrue(abs(results[0] - 111_319.5f) < 50f)
		assertTrue(abs(results[1] - 90f) < 0.001f)
		assertTrue(abs(results[2] - 90f) < 0.001f)
		assertFailsWith<IllegalArgumentException> { KLocation.distanceBetween(0.0, 0.0, 1.0, 1.0, FloatArray(0)) }
	}

	@Test
	fun testToKLatLon() {
		val location = KLocation("gps", 50.45, 30.52)
		assertEquals(KLatLon(50.45, 30.52), location.toKLatLon())
	}
}
