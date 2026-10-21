package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.ResultMatcher
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * The external search (AIDL `search`, `navigate_search` intent) runs on the core shared with the
 * search screen and Android Auto. Its callback must answer its own search only, not later ones.
 */
@RunWith(AndroidJUnit4::class)
class ExternalSearchCallbackScopeTest {

	companion object {
		private const val QUERY = "zq26588callback"
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
	fun externalSearchCallbackDoesNotRunForLaterSearches() {
		val calls = AtomicInteger()
		val answered = CountDownLatch(1)
		ExternalApiHelper.runSearch(app, QUERY, SearchParams.SEARCH_TYPE_ALL, LAT, LON, 1, -1) {
			calls.incrementAndGet()
			answered.countDown()
		}
		assertTrue("external search callback was not called", answered.await(TIMEOUT_SEC, TimeUnit.SECONDS))

		// ordinary searches on the same core, as the search screen runs them; the second one also
		// makes sure everything the first one triggered on the search thread has already run
		searchAndWait()
		searchAndWait()

		assertEquals("external search callback ran again for later searches", 1, calls.get())
	}

	private fun searchAndWait() {
		val finished = CountDownLatch(1)
		core.search(QUERY, false, object : ResultMatcher<SearchResult> {
			override fun publish(result: SearchResult): Boolean {
				if (result.objectType == ObjectType.SEARCH_FINISHED) {
					finished.countDown()
				}
				return true
			}

			override fun isCancelled() = false
		})
		assertTrue("search did not finish", finished.await(TIMEOUT_SEC, TimeUnit.SECONDS))
	}

	private class ProbeSearchAPI(private val query: String) : SearchBaseAPI(ObjectType.POSTCODE) {

		@Volatile
		var active = true

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			resultMatcher.publish(SearchResult(phrase).apply {
				objectType = ObjectType.POSTCODE
				localeName = "probe"
				location = LatLon(LAT, LON)
				otherNames = emptyList()
			})
			return true
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			return if (active && p.fullSearchPhrase.trim() == query) 1 else -1
		}

		override fun isSearchMoreAvailable(phrase: SearchPhrase) = false
	}
}
