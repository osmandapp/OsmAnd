package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.StateChangedListener
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.CarTestUtils
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.search.history.SearchHistoryHelper
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.HistorySource
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SearchHistoryHelperListenerTest {

	companion object {
		private const val BERLIN_LAT = 52.5200
		private const val BERLIN_LON = 13.4050
		private const val TEST_NAME_PREFIX = "25977-test-"
	}

	private lateinit var app: OsmandApplication
	private lateinit var settings: OsmandSettings
	private lateinit var helper: SearchHistoryHelper

	private var savedNavigationHistory: Boolean = true
	private var savedSearchHistory: Boolean = true
	private var savedLastModifiedTime: Long = 0L

	private val listenerCount = AtomicInteger(0)
	private val lastSource = AtomicReference<HistorySource?>()
	private val countingListener = StateChangedListener<HistorySource?> { source ->
		listenerCount.incrementAndGet()
		lastSource.set(source)
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
		savedLastModifiedTime = helper.lastModifiedTime

		settings.NAVIGATION_HISTORY.set(true)
		settings.SEARCH_HISTORY.set(true)

		listenerCount.set(0)
		lastSource.set(null)
		helper.addListener(countingListener)
		CarTestUtils.assumeHistoryHasRoom(app)
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
				helper.lastModifiedTime = savedLastModifiedTime
				settings.NAVIGATION_HISTORY.set(savedNavigationHistory)
				settings.SEARCH_HISTORY.set(savedSearchHistory)
			}
		}
	}

	@Test
	fun addNewNavigationEntryNotifiesListenerExactlyOnce() {
		val pd = createTestPointDescription()
		listenerCount.set(0)
		lastSource.set(null)

		helper.addNewItemToHistory(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION)

		assertEquals("Adding a new navigation entry must notify listeners exactly once", 1, listenerCount.get())
		assertEquals(HistorySource.NAVIGATION, lastSource.get())
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

		assertEquals("Removing an existing entry must notify listeners exactly once", 1, listenerCount.get())
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

	@Test
	fun listenerReceivesSearchSourceForSearchEntry() {
		val pd = createTestPointDescription()
		listenerCount.set(0)
		lastSource.set(null)

		helper.addNewItemToHistory(BERLIN_LAT, BERLIN_LON, pd, HistorySource.SEARCH)

		assertEquals(1, listenerCount.get())
		assertEquals(HistorySource.SEARCH, lastSource.get())
	}

	@Test
	fun batchRemoveOfThreeEntriesFiresExactlyOneNotification() {
		val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
		val searchResults = mutableListOf<SearchResult>()
		for (i in 1..3) {
			val pd = createTestPointDescription()
			helper.addNewItemToHistory(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION)
			val entry = helper.getEntryByName(pd, HistorySource.NAVIGATION)
			assertNotNull(entry)
			searchResults.add(SearchHistoryAPI.createSearchResult(app, entry!!, phrase))
		}

		listenerCount.set(0)
		lastSource.set(null)

		helper.remove(searchResults)

		assertEquals(1, listenerCount.get())
		assertEquals(HistorySource.NAVIGATION, lastSource.get())
	}

	@Test
	fun addItemsToHistoryFiresNotificationWithEntrySource() {
		val entries = mutableListOf<HistoryEntry>()
		for (i in 1..2) {
			val pd = createTestPointDescription()
			entries.add(HistoryEntry(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION))
		}

		listenerCount.set(0)
		lastSource.set(null)

		helper.addItemsToHistory(entries)

		assertEquals(1, listenerCount.get())
		assertEquals(HistorySource.NAVIGATION, lastSource.get())
	}

	private fun createTestPointDescription(nameSuffix: String = UUID.randomUUID().toString()): PointDescription {
		val pd = PointDescription(PointDescription.POINT_TYPE_LOCATION, "$TEST_NAME_PREFIX$nameSuffix")
		createdPoints.add(pd)
		return pd
	}
}
