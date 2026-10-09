package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.StateChangedListener
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.CarTestUtils
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI
import net.osmand.plus.search.history.SearchHistoryHelper
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.HistorySource
import net.osmand.search.core.SearchPhrase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verifies [SearchHistoryHelper] change listener notifications used to refresh the Android Auto
 * suggestion cards (issue #25977): on add, on remove and none after [SearchHistoryHelper.removeListener].
 */
@RunWith(AndroidJUnit4::class)
class SearchHistoryHelperListenerTest {

	companion object {
		/** Base latitude for test points (Berlin center). */
		private const val BERLIN_LAT = 52.5200

		/** Base longitude for test points (Berlin center). */
		private const val BERLIN_LON = 13.4050

		/** Prefix used to identify and safely clean up test-created entries. */
		private const val TEST_NAME_PREFIX = "25977-test-"

	}

	private lateinit var app: OsmandApplication
	private lateinit var settings: OsmandSettings
	private lateinit var helper: SearchHistoryHelper

	private var savedNavigationHistory: Boolean = true
	private var savedSearchHistory: Boolean = true

	private val listenerCount = AtomicInteger(0)
	private val countingListener = StateChangedListener<Void?> {
		listenerCount.incrementAndGet()
	}

	private val createdPoints = mutableListOf<PointDescription>()

	@Before
	fun setUp() {
		app = InstrumentationRegistry.getInstrumentation()
			.targetContext.applicationContext as OsmandApplication
		settings = app.settings

		CarTestUtils.waitForAppInitialization(app)

		helper = app.searchHistoryHelper

		savedNavigationHistory = settings.NAVIGATION_HISTORY.get()
		savedSearchHistory = settings.SEARCH_HISTORY.get()

		settings.NAVIGATION_HISTORY.set(true)
		settings.SEARCH_HISTORY.set(true)

		listenerCount.set(0)
		helper.addListener(countingListener)
	}

	@After
	fun tearDown() {
		try {
			for (pd in createdPoints) {
				for (source in listOf(HistorySource.NAVIGATION, HistorySource.SEARCH)) {
					val entry = helper.getEntryByName(pd, source)
					if (entry != null) {
						val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
						val searchResult = SearchHistoryAPI.createSearchResult(app, entry, phrase)
						helper.remove(searchResult)
					}
				}
			}
			createdPoints.clear()
		} finally {
			try {
				helper.removeListener(countingListener)
			} finally {
				settings.NAVIGATION_HISTORY.set(savedNavigationHistory)
				settings.SEARCH_HISTORY.set(savedSearchHistory)
			}
		}
	}

	@Test
	fun addNewNavigationEntryNotifiesListenerExactlyOnce() {
		val pd = createTestPointDescription()
		listenerCount.set(0)

		helper.addNewItemToHistory(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION)

		assertEquals("Adding a new navigation entry must notify listeners exactly once", 1, listenerCount.get())
		assertNotNull("Navigation entry should be present in history", helper.getEntryByName(pd, HistorySource.NAVIGATION))
	}

	@Test
	fun removingExistingEntryViaSearchResultNotifiesAndRemovesEntry() {
		val pd = createTestPointDescription()
		helper.addNewItemToHistory(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION)
		val entry = helper.getEntryByName(pd, HistorySource.NAVIGATION)
		assertNotNull("Test entry must exist before removal", entry)

		val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
		val searchResult = SearchHistoryAPI.createSearchResult(app, entry!!, phrase)

		listenerCount.set(0)
		helper.remove(searchResult)

		assertTrue("Removing an existing entry must notify listeners at least once", listenerCount.get() >= 1)
		assertNull("Entry should no longer exist in navigation history", helper.getEntryByName(pd, HistorySource.NAVIGATION))
		assertNull("Entry should no longer exist in history", helper.getEntryByName(pd))
	}

	@Test
	fun afterRemoveListenerAddingEntryDoesNotNotify() {
		helper.removeListener(countingListener)
		listenerCount.set(0)

		val pd = createTestPointDescription()
		helper.addNewItemToHistory(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION)

		assertEquals("Adding an entry after removing listener must not notify listener", 0, listenerCount.get())
		assertNotNull("Navigation entry should still be added to history", helper.getEntryByName(pd, HistorySource.NAVIGATION))
	}

	private fun createTestPointDescription(nameSuffix: String = UUID.randomUUID().toString()): PointDescription {
		val pd = PointDescription(PointDescription.POINT_TYPE_LOCATION, "$TEST_NAME_PREFIX$nameSuffix")
		createdPoints.add(pd)
		return pd
	}
}
