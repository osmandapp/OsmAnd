package net.osmand.plus.plugins.topoprint

import androidx.test.ext.junit.runners.AndroidJUnit4

import net.osmand.plus.plugins.topoprint.compose.Geomagnetism
import net.osmand.plus.plugins.topoprint.compose.UtmMath

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Parity tests against reference values produced by the desktop pipeline
 * (osmand_topo_map_generator/tests/reference/, utm + pygeomag/WMMHR2025).
 *
 * Tolerances:
 * - UTM: UTMPoint converts through a float LatLonPoint (~1 m slack).
 * - Declination: GeomagneticField uses WMM, reference uses WMMHR2025.
 */
@RunWith(AndroidJUnit4::class)
class TopoPrintMathTest {

	private companion object {
		// 2026-01-01 00:00:00 UTC, matching the reference generation epoch.
		const val EPOCH_2026 = 1767225600000L

		const val UTM_METERS_DELTA = 2.5
		const val LATLON_DEGREES_DELTA = 0.00005
		const val CONVERGENCE_DEGREES_DELTA = 0.001
		const val DECLINATION_DEGREES_DELTA = 0.75
	}

	@Test
	fun testLatLonToUtm() {
		val beijing = UtmMath.fromLatLon(40.0665, 116.183)
		assertEquals(50, beijing.zoneNumber)
		// The built-in UTMPoint only carries the hemisphere letter; the printed
		// latitude band comes from UtmMath.latitudeBand (see below).
		assertEquals('N', beijing.zoneLetter)
		assertEquals(430328.41, beijing.easting, UTM_METERS_DELTA)
		assertEquals(4435457.86, beijing.northing, UTM_METERS_DELTA)

		val newYork = UtmMath.fromLatLon(40.7128, -74.006)
		assertEquals(18, newYork.zoneNumber)
		assertEquals('N', newYork.zoneLetter)
		assertEquals(583959.37, newYork.easting, UTM_METERS_DELTA)
		assertEquals(4507351.00, newYork.northing, UTM_METERS_DELTA)

		val sydney = UtmMath.fromLatLon(-33.8688, 151.2093)
		assertEquals(56, sydney.zoneNumber)
		assertEquals('S', sydney.zoneLetter)
		assertEquals(334368.63, sydney.easting, UTM_METERS_DELTA)
		assertEquals(6250948.35, sydney.northing, UTM_METERS_DELTA)
	}

	@Test
	fun testLatitudeBands() {
		// Reference latitude-band letters from the desktop pipeline (50T, 32V, 33X, 56H).
		assertEquals('T', UtmMath.latitudeBand(40.0665))
		assertEquals('V', UtmMath.latitudeBand(60.39))
		assertEquals('X', UtmMath.latitudeBand(78.22))
		assertEquals('H', UtmMath.latitudeBand(-33.8688))
	}

	@Test
	fun testNorwayAndSvalbardSpecialZones() {
		val bergen = UtmMath.fromLatLon(60.39, 5.32)
		assertEquals(32, bergen.zoneNumber)
		assertEquals('N', bergen.zoneLetter)
		assertEquals(297230.22, bergen.easting, UTM_METERS_DELTA)
		assertEquals(6700510.18, bergen.northing, UTM_METERS_DELTA)

		val svalbard = UtmMath.fromLatLon(78.22, 15.63)
		assertEquals(33, svalbard.zoneNumber)
		assertEquals('N', svalbard.zoneLetter)
		assertEquals(514357.74, svalbard.easting, UTM_METERS_DELTA)
		assertEquals(8682999.17, svalbard.northing, UTM_METERS_DELTA)
	}

	@Test
	fun testUtmToLatLonRoundTrip() {
		val cases = listOf(
			Triple(40.0665, 116.183, 430328.41 to 4435457.86),
			Triple(0.5, 120.0, 166034.10 to 55341.39),
			Triple(-33.8688, 151.2093, 334368.63 to 6250948.35)
		)
		for ((lat, lon, utm) in cases) {
			val coordinate = UtmMath.fromLatLon(lat, lon)
			val (backLat, backLon) = UtmMath.toLatLon(coordinate)
			assertEquals(lat, backLat, LATLON_DEGREES_DELTA)
			assertEquals(lon, backLon, LATLON_DEGREES_DELTA)
			assertEquals(utm.first, coordinate.easting, UTM_METERS_DELTA)
			assertEquals(utm.second, coordinate.northing, UTM_METERS_DELTA)
		}
	}

	@Test
	fun testGridConvergence() {
		// Reference values computed as atan(tan(lon - centralMeridian) * sin(lat)).
		assertEquals(-0.5259, UtmMath.gridConvergenceDegrees(40.0665, 116.183, 50), CONVERGENCE_DEGREES_DELTA)
		assertEquals(0.9982, UtmMath.gridConvergenceDegrees(-33.8688, 151.2093, 56), CONVERGENCE_DEGREES_DELTA)
		assertEquals(-3.2005, UtmMath.gridConvergenceDegrees(60.39, 5.32, 32), CONVERGENCE_DEGREES_DELTA)
		assertEquals(0.6167, UtmMath.gridConvergenceDegrees(78.22, 15.63, 33), CONVERGENCE_DEGREES_DELTA)
		// On the central meridian grid north equals true north.
		assertEquals(0.0, UtmMath.gridConvergenceDegrees(40.0, 117.0, 50), 1e-9)
	}

	@Test
	fun testDeclination() {
		// WMMHR2025 reference: beijing -7.50, sydney +12.81, new york -12.49 degrees.
		val beijing = Geomagnetism.declinationDegrees(40.0665, 116.183, 0.0, EPOCH_2026)
		assertTrue("declination $beijing", Math.abs(beijing - (-7.5045)) < DECLINATION_DEGREES_DELTA)

		val sydney = Geomagnetism.declinationDegrees(-33.8688, 151.2093, 0.0, EPOCH_2026)
		assertTrue("declination $sydney", Math.abs(sydney - 12.8100) < DECLINATION_DEGREES_DELTA)

		val newYork = Geomagnetism.declinationDegrees(40.7128, -74.006, 0.0, EPOCH_2026)
		assertTrue("declination $newYork", Math.abs(newYork - (-12.4943)) < DECLINATION_DEGREES_DELTA)
	}
}
