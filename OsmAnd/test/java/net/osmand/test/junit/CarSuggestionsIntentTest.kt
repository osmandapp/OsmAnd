package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.CarSuggestionsHelper
import net.osmand.plus.auto.CarTestUtils
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.HistorySource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Verifies that suggestion card coordinates, passed as Double extras, survive the
 * [CarSuggestionsHelper.createCardIntent] / [CarSuggestionsHelper.getSuggestionLatLon] round trip
 * exactly, including values near 0° (issue #25977).
 */
@RunWith(AndroidJUnit4::class)
class CarSuggestionsIntentTest {

	companion object {
		/** Latitude for Greenwich test location. */
		private const val GREENWICH_LAT = 51.4779

		/** Longitude east of the Prime Meridian at Greenwich (tiny positive value). */
		private const val GREENWICH_LON = 0.0005

		/** Longitude west of the Prime Meridian at Greenwich (tiny negative value). */
		private const val WEST_GREENWICH_LON = -0.0005

		/** Latitude near the equator (tiny positive value). */
		private const val EQUATOR_LAT = 0.0005

		/** Longitude near the equator test location. */
		private const val EQUATOR_LON = 9.3

		/** Tiny negative latitude coordinate. */
		private const val TINY_LAT = -0.0001

		/** Tiny positive longitude coordinate. */
		private const val TINY_LON = 0.0002

		/** Latitude for Berlin control location. */
		private const val BERLIN_LAT = 52.52

		/** Longitude for Berlin control location. */
		private const val BERLIN_LON = 13.405

		/** Test card title string. */
		private const val TEST_CARD_TITLE = "Test Destination"
	}

	private data class CoordinateCase(
		val name: String,
		val lat: Double,
		val lon: Double
	)

	private lateinit var app: OsmandApplication
	private lateinit var settings: OsmandSettings
	private lateinit var helper: CarSuggestionsHelper

	private var savedNavigationHistory = true
	private val testPrefix = "25977-test-" + UUID.randomUUID().toString() + "-"
	private val createdDescriptions = mutableListOf<PointDescription>()

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		settings = app.settings
		helper = CarSuggestionsHelper(app)

		savedNavigationHistory = settings.NAVIGATION_HISTORY.get()
		settings.NAVIGATION_HISTORY.set(true)

		CarTestUtils.waitForAppInitialization(app)
	}

	@After
	fun tearDown() {
		try {
			CarTestUtils.cleanupHistoryEntries(
				app = app,
				testPrefix = testPrefix,
				createdDescriptions = createdDescriptions
			)
		} finally {
			settings.NAVIGATION_HISTORY.set(savedNavigationHistory)
		}
	}

	/**
	 * Verifies that createCardIntent and getSuggestionLatLon round-trip preserves exact double
	 * coordinate values without precision loss or exponential notation corruption.
	 */
	@Test
	fun roundTripPreservesExactCoordinates() {
		val testCases = listOf(
			CoordinateCase("Greenwich", GREENWICH_LAT, GREENWICH_LON),
			CoordinateCase("West of Greenwich", GREENWICH_LAT, WEST_GREENWICH_LON),
			CoordinateCase("Near the equator", EQUATOR_LAT, EQUATOR_LON),
			CoordinateCase("Both tiny", TINY_LAT, TINY_LON),
			CoordinateCase("Berlin control", BERLIN_LAT, BERLIN_LON)
		)

		for (case in testCases) {
			val pd = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}roundtrip-${case.name}")
			val entry = HistoryEntry(case.lat, case.lon, pd, HistorySource.NAVIGATION)
			val intent = CarSuggestionsHelper.createCardIntent(app, entry, TEST_CARD_TITLE)

			val resolvedLatLon = CarSuggestionsHelper.getSuggestionLatLon(intent)
			assertNotNull("Resolved LatLon must not be null for case '${case.name}'", resolvedLatLon)
			assertEquals(
				"Latitude must match exactly with 0.0 delta for case '${case.name}'",
				case.lat,
				resolvedLatLon!!.latitude,
				0.0
			)
			assertEquals(
				"Longitude must match exactly with 0.0 delta for case '${case.name}'",
				case.lon,
				resolvedLatLon.longitude,
				0.0
			)
		}
	}

}
