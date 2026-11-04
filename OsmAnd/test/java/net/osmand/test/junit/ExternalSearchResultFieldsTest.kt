package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.aidl.AidlSearchResultWrapper
import net.osmand.aidl.search.SearchParams
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.CarTestUtils
import net.osmand.plus.helpers.ExternalApiHelper
import net.osmand.search.SearchUICore
import net.osmand.search.SearchUICore.SearchResultMatcher
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchCoreFactory.SearchBaseAPI
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchResult
import net.osmand.search.core.SearchSettings
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The external search (AIDL `search`, `navigate_search` intent) must answer even when the search
 * returns rows without a point (POI categories, "did you mean") or without other names.
 */
@RunWith(AndroidJUnit4::class)
class ExternalSearchResultFieldsTest {

	companion object {
		private const val QUERY = "zq26588fields"
		private const val LAT = 50.4501
		private const val LON = 30.5234
		private const val TIMEOUT_SEC = 30L
	}

	private lateinit var app: OsmandApplication
	private lateinit var core: SearchUICore
	private lateinit var savedSettings: SearchSettings
	private lateinit var probe: ProbeSearchAPI

	@Before
	fun setUp() {
		app = InstrumentationRegistry.getInstrumentation()
			.targetContext.applicationContext as OsmandApplication
		CarTestUtils.waitForAppInitialization(app)
		core = app.searchUICore.core
		savedSettings = core.searchSettings
		probe = ProbeSearchAPI(QUERY)
		core.registerAPI(probe)
	}

	@After
	fun tearDown() {
		probe.active = false
		core.updateSettings(savedSettings)
	}

	@Test
	fun resultWithoutLocationIsSkipped() {
		probe.rows = listOf(
			Row("no location", null, emptyList()),
			Row("located", LatLon(LAT, LON), emptyList())
		)
		val names = runExternalSearch().map { it.localName }
		assertTrue("located row is missing: $names", "located" in names)
		assertFalse("row without location was returned: $names", "no location" in names)
	}

	@Test
	fun resultWithoutOtherNamesIsReturned() {
		probe.rows = listOf(Row("no other names", LatLon(LAT, LON), null))
		val result = runExternalSearch().firstOrNull { it.localName == "no other names" }
		assertNotNull("row without other names is missing", result)
		assertTrue(result!!.otherNames.isEmpty())
	}

	private fun runExternalSearch(): List<AidlSearchResultWrapper> {
		val results = AtomicReference<List<AidlSearchResultWrapper>>()
		val answered = CountDownLatch(1)
		ExternalApiHelper.runSearch(app, QUERY, SearchParams.SEARCH_TYPE_ALL, LAT, LON, 1, -1) {
			results.set(it)
			answered.countDown()
		}
		assertTrue("external search callback was not called", answered.await(TIMEOUT_SEC, TimeUnit.SECONDS))
		return results.get()
	}

	private data class Row(val name: String, val location: LatLon?, val otherNames: List<String>?)

	private class ProbeSearchAPI(private val query: String) : SearchBaseAPI(ObjectType.POSTCODE) {

		@Volatile
		var active = true

		@Volatile
		var rows: List<Row> = emptyList()

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			for (row in rows) {
				resultMatcher.publish(SearchResult(phrase).apply {
					objectType = ObjectType.POSTCODE
					localeName = row.name
					location = row.location
					otherNames = row.otherNames
				})
			}
			return true
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			return if (active && p.fullSearchPhrase.trim() == query) 1 else -1
		}

		override fun isSearchMoreAvailable(phrase: SearchPhrase) = false
	}
}
