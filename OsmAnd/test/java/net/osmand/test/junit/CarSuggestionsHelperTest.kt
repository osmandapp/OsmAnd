package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.CarSuggestionsHelper
import net.osmand.plus.auto.CarTestUtils
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.HistorySource
import net.osmand.search.core.SearchPhrase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CarSuggestionsHelperTest {

	companion object {
		private const val ICON_AMENITY_FUEL = "amenity_fuel"

		private const val UNKNOWN_ICON_NAME = "no_such_icon_25977"

		private const val BERLIN_LAT_A = 52.5200

		private const val BERLIN_LON_A = 13.4050
	}

	private lateinit var app: OsmandApplication
	private lateinit var settings: OsmandSettings
	private lateinit var helper: CarSuggestionsHelper

	private var savedNavigationHistory = true
	private var savedLastModifiedTime: Long = 0L
	private val testPrefix = "25977-test-" + UUID.randomUUID().toString() + "-"
	private val createdDescriptions = mutableListOf<PointDescription>()

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		settings = app.settings
		helper = CarSuggestionsHelper(app)

		savedNavigationHistory = settings.NAVIGATION_HISTORY.get()
		savedLastModifiedTime = app.searchHistoryHelper.lastModifiedTime
		settings.NAVIGATION_HISTORY.set(true)

		CarTestUtils.waitForAppInitialization(app)
		CarTestUtils.assumeHistoryHasRoom(app)
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
			app.searchHistoryHelper.lastModifiedTime = savedLastModifiedTime
			settings.NAVIGATION_HISTORY.set(savedNavigationHistory)
		}
	}

	@Test
	fun entryAtZeroCoordinatesIsExcludedFromSuggestions() {
		val pdZero = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}Zero")
		createdDescriptions.add(pdZero)
		app.searchHistoryHelper.addNewItemToHistory(0.0, 0.0, pdZero, HistorySource.NAVIGATION)

		val pdValid = PointDescription(PointDescription.POINT_TYPE_LOCATION, "${testPrefix}Valid")
		createdDescriptions.add(pdValid)
		app.searchHistoryHelper.addNewItemToHistory(BERLIN_LAT_A, BERLIN_LON_A, pdValid, HistorySource.NAVIGATION)

		val testEntries = helper.getSuggestionEntries().filter { entry ->
			entry.name.name.startsWith(testPrefix)
		}

		assertEquals(1, testEntries.size)
		assertEquals(pdValid.name, testEntries[0].name.name)
		assertTrue(testEntries.none { entry ->
			entry.lat == 0.0 && entry.lon == 0.0
		})
	}

	@Test
	fun poiEntryWithFuelIconResolvesToBigIconResourceId() {
		val pd = PointDescription(PointDescription.POINT_TYPE_POI, "${testPrefix}Fuel")
		pd.iconName = ICON_AMENITY_FUEL
		createdDescriptions.add(pd)
		app.searchHistoryHelper.addNewItemToHistory(BERLIN_LAT_A, BERLIN_LON_A, pd, HistorySource.NAVIGATION)

		val entry = app.searchHistoryHelper.getEntryByName(pd, HistorySource.NAVIGATION)
		assertNotNull("History entry must be present in history helper", entry)

		val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
		val searchResult = SearchHistoryAPI.createSearchResult(app, entry!!, phrase)

		val expectedIconId = RenderingIcons.getBigIconResourceId(ICON_AMENITY_FUEL)
		assertTrue("RenderingIcons big icon resource ID for amenity_fuel must be non-zero", expectedIconId != 0)

		val resolvedIconId = helper.getIconId(entry, searchResult)
		assertEquals(expectedIconId, resolvedIconId)
	}

	@Test
	fun poiEntryWithUnknownIconFallsBackToItemIcon() {
		val pd = PointDescription(PointDescription.POINT_TYPE_POI, "${testPrefix}UnknownIconPoi")
		pd.iconName = UNKNOWN_ICON_NAME
		createdDescriptions.add(pd)
		app.searchHistoryHelper.addNewItemToHistory(BERLIN_LAT_A, BERLIN_LON_A, pd, HistorySource.NAVIGATION)

		val entry = app.searchHistoryHelper.getEntryByName(pd, HistorySource.NAVIGATION)
		assertNotNull("History entry must be present in history helper", entry)

		val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
		val searchResult = SearchHistoryAPI.createSearchResult(app, entry!!, phrase)

		val resolvedIconId = helper.getIconId(entry, searchResult)
		assertEquals(entry.name.itemIcon, resolvedIconId)
	}
}
