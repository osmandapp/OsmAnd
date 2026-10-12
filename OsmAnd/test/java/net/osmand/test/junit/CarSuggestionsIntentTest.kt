package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.CarSuggestionsHelper
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.settings.enums.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CarSuggestionsIntentTest {

	companion object {
		private const val GREENWICH_LAT = 51.4779
		private const val WEST_GREENWICH_LON = -0.0005
		private const val TINY_LAT = -0.0001
		private const val TINY_LON = 0.0002
	}

	private data class CoordinateCase(
		val name: String,
		val lat: Double,
		val lon: Double
	)

	private lateinit var app: OsmandApplication

	private val testPrefix = "25977-test-" + UUID.randomUUID().toString() + "-"

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
	}

	@Test
	fun roundTripPreservesExactCoordinates() {
		val testCases = listOf(
			CoordinateCase("West of Greenwich", GREENWICH_LAT, WEST_GREENWICH_LON),
			CoordinateCase("Both tiny", TINY_LAT, TINY_LON)
		)

		for (case in testCases) {
			val pd = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}roundtrip-${case.name}")
			val entry = HistoryEntry(case.lat, case.lon, pd, HistorySource.NAVIGATION)
			val intent = CarSuggestionsHelper.createCardIntent(app, entry)

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
