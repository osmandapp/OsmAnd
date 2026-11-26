package net.osmand.test.ui.search

import android.Manifest
import android.os.SystemClock
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.viewpager.widget.ViewPager
import net.osmand.Location
import net.osmand.data.PointDescription
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI
import net.osmand.plus.search.dialogs.QuickSearchAddressListFragment
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchTab
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchType
import net.osmand.plus.search.dialogs.QuickSearchHistoryListFragment
import net.osmand.plus.search.dialogs.QuickSearchListFragment
import net.osmand.plus.search.dialogs.QuickSearchMainListFragment
import net.osmand.plus.settings.enums.HistorySource
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.search.core.SearchPhrase
import net.osmand.test.common.AndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests for #25765: a distance in search is orange when it is measured from the map
 * centre and blue when it is measured from my location. The colour and the distance went out of
 * step after the my-location button on a cleared query, on a page of the pager that was not
 * opened yet, and after the return from the map.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class QuickSearchMyLocationColorTest : AndroidTest() {

	companion object {
		private const val MY_LAT = 50.4501
		private const val MY_LON = 30.5234
		private const val MAP_SHIFT_DEG = 0.05 // about 5.5 km, far beyond "map centre at my location"
		private const val HISTORY_SHIFT_DEG = 0.002
		private const val SEARCH_TIMEOUT_MS = 60_000L
		private const val HISTORY_NAME = "25765-test-history"
		// a query that finds a place with a distance without any map
		private const val COORDINATES_QUERY = "50.4521, 30.5234"
	}

	@get:Rule
	val locationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
		Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

	private val instrumentation = InstrumentationRegistry.getInstrumentation()
	private val historyPoint = PointDescription(PointDescription.POINT_TYPE_LOCATION, HISTORY_NAME)

	private var savedSearchHistory = true
	private var savedHistoryCollapsed = false
	private var savedNearbyCollapsed = false
	private var savedHistoryModifiedTime = 0L

	@Before
	fun addHistoryEntry() {
		savedSearchHistory = settings.SEARCH_HISTORY.get()
		savedHistoryCollapsed = settings.EXPLORE_HISTORY_ROW_COLLAPSED.get()
		savedNearbyCollapsed = settings.EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.get()
		savedHistoryModifiedTime = app.searchHistoryHelper.lastModifiedTime
		settings.SEARCH_HISTORY.set(true)
		settings.EXPLORE_HISTORY_ROW_COLLAPSED.set(false)
		settings.EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.set(true) // leaves the screen to the history rows
		app.searchHistoryHelper.addNewItemToHistory(MY_LAT + HISTORY_SHIFT_DEG, MY_LON,
			historyPoint, HistorySource.SEARCH)
	}

	@After
	fun removeHistoryEntry() {
		val helper = app.searchHistoryHelper
		val entry = helper.getEntryByName(historyPoint, HistorySource.SEARCH)
		if (entry != null) {
			val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
			helper.remove(SearchHistoryAPI.createSearchResult(app, entry, phrase))
		}
		helper.lastModifiedTime = savedHistoryModifiedTime
		settings.SEARCH_HISTORY.set(savedSearchHistory)
		settings.EXPLORE_HISTORY_ROW_COLLAPSED.set(savedHistoryCollapsed)
		settings.EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.set(savedNearbyCollapsed)
	}

	@Test
	fun myLocationButtonOnClearedQueryPaintsDistancesBlue() {
		ActivityScenario.launch(MapActivity::class.java).use { scenario ->
			val dialog = openSearch(scenario, MY_LAT + MAP_SHIFT_DEG)
			assertTrue("Precondition: the map is away from my location, so search starts around the map centre",
				onMain { dialog.isUseMapCenter })

			// A query that is searched and then cleared leaves the cancelled search behind
			onMain { searchEditText(dialog).setText("cafe") }
			waitForSearchToFinish(dialog)
			onMain { searchEditText(dialog).setText("") }

			// With an empty query the clear button is the my-location button
			onMain { dialog.requireView().findViewById<View>(R.id.clearButton).performClick() }
			instrumentation.waitForIdleSync()
			assertFalse("Precondition: the my-location button moves the search to my location",
				onMain { dialog.isUseMapCenter })

			val myLocationColor = ContextCompat.getColor(app, R.color.color_myloc_distance)
			val historyDistances = onMain { visibleDistances(dialog, QuickSearchHistoryListFragment::class.java) }
			assertTrue("No history row with a distance is shown", historyDistances.isNotEmpty())
			for ((color, _) in historyDistances) {
				assertEquals("A history distance is not painted in the my-location colour",
					String.format("#%08X", myLocationColor), String.format("#%08X", color))
			}
			onMain { searchEditText(dialog).setText(COORDINATES_QUERY) }
			waitForSearchToFinish(dialog)
			val resultDistances = onMain { visibleDistances(dialog, QuickSearchMainListFragment::class.java) }
			assertTrue("No result with a distance is shown", resultDistances.isNotEmpty())
			for ((color, _) in resultDistances) {
				assertEquals("A result of the next query is not painted in the my-location colour",
					String.format("#%08X", myLocationColor), String.format("#%08X", color))
			}
		}
	}

	@Test
	fun myLocationButtonReachesPageNotOpenedYet() {
		ActivityScenario.launch(MapActivity::class.java).use { scenario ->
			val dialog = openSearch(scenario, MY_LAT + MAP_SHIFT_DEG)
			assertTrue("Precondition: the map is away from my location, so search starts around the map centre",
				onMain { dialog.isUseMapCenter })

			// The Address page is already built off screen, but it is not resumed before it is opened
			onMain { dialog.requireView().findViewById<View>(R.id.clearButton).performClick() }
			instrumentation.waitForIdleSync()
			assertFalse("Precondition: the my-location button moves the search to my location",
				onMain { dialog.isUseMapCenter })

			onMain { dialog.requireView().findViewById<ViewPager>(R.id.pager).setCurrentItem(2, false) }
			instrumentation.waitForIdleSync()
			val addressPage = onMain {
				dialog.childFragmentManager.fragments
					.filterIsInstance<QuickSearchAddressListFragment>()
					.firstOrNull()
			}
			assertNotNull("Address page is not created", addressPage)
			assertTrue("Precondition: the Address page is shown", onMain { addressPage!!.isResumed })
			assertFalse("Distances of the Address page would be painted from the map centre",
				onMain { addressPage!!.listAdapter!!.isUseMapCenter })
		}
	}

	@Test
	fun historyDistancesMatchTheirColourAfterReturnFromMovedMap() {
		ActivityScenario.launch(MapActivity::class.java).use { scenario ->
			val dialog = openSearch(scenario, MY_LAT)
			assertFalse("Precondition: the map is at my location, so search starts around my location",
				onMain { dialog.isUseMapCenter })

			returnFromMap(scenario, dialog, MY_LAT + MAP_SHIFT_DEG)
			assertTrue("Precondition: search moved to the map centre", onMain { dialog.isUseMapCenter })

			val mapCentreColor = ContextCompat.getColor(app, R.color.color_distance)
			val searchOrigin = app.searchUICore.core.searchSettings.originalLocation
			val fromMapCentre = formattedDistanceToHistoryPoint(searchOrigin.latitude, searchOrigin.longitude)
			val fromMyLocation = formattedDistanceToHistoryPoint(MY_LAT, MY_LON)
			val historyDistances = onMain {
				visibleDistances(dialog, QuickSearchHistoryListFragment::class.java, HISTORY_NAME)
			}
			assertTrue("The history row of the test is not shown", historyDistances.isNotEmpty())
			for ((color, text) in historyDistances) {
				val expected = if (color == mapCentreColor) fromMapCentre else fromMyLocation
				assertEquals("A history distance is not measured from the place its colour stands for "
						+ String.format("(#%08X)", color), expected, text)
			}
		}
	}

	@Test
	fun resultsKeepTheirColourAfterReturnFromMovedMap() {
		ActivityScenario.launch(MapActivity::class.java).use { scenario ->
			val dialog = openSearch(scenario, MY_LAT)
			assertFalse("Precondition: the map is at my location, so search starts around my location",
				onMain { dialog.isUseMapCenter })

			onMain { searchEditText(dialog).setText(COORDINATES_QUERY) }
			waitForSearchToFinish(dialog)
			val distancesBefore = onMain { visibleDistances(dialog, QuickSearchMainListFragment::class.java) }
			assertTrue("No result with a distance is shown", distancesBefore.isNotEmpty())

			returnFromMap(scenario, dialog, MY_LAT + MAP_SHIFT_DEG)
			assertTrue("Precondition: search moved to the map centre", onMain { dialog.isUseMapCenter })

			// the results are still those found from my location
			val distancesAfter = onMain { visibleDistances(dialog, QuickSearchMainListFragment::class.java) }
			assertEquals("Results found from my location changed after the return from the map",
				describe(distancesBefore), describe(distancesAfter))
		}
	}

	@Test
	fun resultsTurnBlueWhenSearchReturnsToMyLocation() {
		ActivityScenario.launch(MapActivity::class.java).use { scenario ->
			val dialog = openSearch(scenario, MY_LAT + MAP_SHIFT_DEG)
			onMain { searchEditText(dialog).setText(COORDINATES_QUERY) }
			waitForSearchToFinish(dialog)
			val mapCentreColor = ContextCompat.getColor(app, R.color.color_distance)
			val distancesBefore = onMain { visibleDistances(dialog, QuickSearchMainListFragment::class.java) }
			assertTrue("Precondition: results found from the map centre are painted in its colour",
				distancesBefore.isNotEmpty() && distancesBefore.all { it.first == mapCentreColor })

			returnFromMap(scenario, dialog, MY_LAT)
			assertFalse("Precondition: search moved to my location", onMain { dialog.isUseMapCenter })

			val myLocationColor = ContextCompat.getColor(app, R.color.color_myloc_distance)
			val distancesAfter = onMain { visibleDistances(dialog, QuickSearchMainListFragment::class.java) }
			assertTrue("No result with a distance is shown", distancesAfter.isNotEmpty())
			for ((color, _) in distancesAfter) {
				assertEquals("A result is still painted as measured from the map centre",
					String.format("#%08X", myLocationColor), String.format("#%08X", color))
			}
		}
	}

	private fun openSearch(scenario: ActivityScenario<MapActivity>, mapLat: Double): QuickSearchDialogFragment {
		scenario.onActivity { activity ->
			// the device's own position must not replace the simulated one
			app.locationProvider.pauseAllUpdates()
			app.locationProvider.setLocationFromSimulation(createMyLocation())
			app.mapViewTrackingUtilities.setMapLinkedToLocation(false)
			activity.mapView.setLatLon(mapLat, MY_LON)
			QuickSearchDialogFragment.showInstance(activity, "", null,
				QuickSearchType.REGULAR, QuickSearchTab.HISTORY, null)
		}
		instrumentation.waitForIdleSync()
		return findDialog(scenario)
	}

	// As after "show on map": search is hidden, the map is moved, search is shown again
	private fun returnFromMap(scenario: ActivityScenario<MapActivity>, dialog: QuickSearchDialogFragment,
	                          mapLat: Double) {
		scenario.onActivity { activity ->
			dialog.hide()
			activity.mapView.setLatLon(mapLat, MY_LON)
			dialog.show()
		}
		instrumentation.waitForIdleSync()
	}

	private fun describe(distances: List<Pair<Int, String>>): String =
		distances.joinToString { (color, text) -> String.format("%s #%08X", text, color) }

	private fun formattedDistanceToHistoryPoint(fromLat: Double, fromLon: Double): String {
		val distance = FloatArray(2)
		Location.distanceBetween(MY_LAT + HISTORY_SHIFT_DEG, MY_LON, fromLat, fromLon, distance)
		return OsmAndFormatter.getFormattedDistance(distance[0], app)
	}

	private fun createMyLocation(): Location {
		val location = Location("test", MY_LAT, MY_LON)
		location.time = System.currentTimeMillis()
		location.accuracy = 5f
		return location
	}

	private fun findDialog(scenario: ActivityScenario<MapActivity>): QuickSearchDialogFragment {
		var dialog: QuickSearchDialogFragment? = null
		scenario.onActivity { activity ->
			dialog = activity.supportFragmentManager
				.findFragmentByTag(QuickSearchDialogFragment.TAG) as QuickSearchDialogFragment?
		}
		assertNotNull("Search dialog is not shown", dialog)
		return dialog!!
	}

	private fun searchEditText(dialog: QuickSearchDialogFragment): EditText =
		dialog.requireView().findViewById(R.id.searchEditText)

	private fun waitForSearchToFinish(dialog: QuickSearchDialogFragment) {
		val progressBar = onMain { dialog.requireView().findViewById<View>(R.id.searchProgressBar) }
		val deadline = SystemClock.uptimeMillis() + SEARCH_TIMEOUT_MS
		while (onMain { progressBar.visibility == View.VISIBLE }) {
			assertTrue("Search did not finish", SystemClock.uptimeMillis() < deadline)
			SystemClock.sleep(100)
		}
	}

	private fun visibleDistances(dialog: QuickSearchDialogFragment, list: Class<out QuickSearchListFragment>,
	                             title: String? = null): List<Pair<Int, String>> {
		val distances = mutableListOf<Pair<Int, String>>()
		for (fragment in dialog.childFragmentManager.fragments) {
			if (list.isInstance(fragment)) {
				fragment as QuickSearchListFragment
				val listView = fragment.listView
				for (i in 0 until listView.childCount) {
					if (listView.firstVisiblePosition + i < listView.headerViewsCount) {
						continue // the nearby places card in the header paints its own distances
					}
					val row = listView.getChildAt(i)
					if (title != null && row.findViewById<TextView>(R.id.title)?.text?.contains(title) != true) {
						continue
					}
					val distance = row.findViewById<TextView>(R.id.distance)
					val text = distance?.text
					if (text is Spanned && text.isNotEmpty()) {
						val spans = text.getSpans(0, text.length, ForegroundColorSpan::class.java)
						if (spans.isNotEmpty()) {
							distances.add(spans[0].foregroundColor to text.toString())
						}
					}
				}
			}
		}
		return distances
	}

	private fun <T> onMain(action: () -> T): T {
		var result: Result<T>? = null
		instrumentation.runOnMainSync { result = runCatching(action) }
		return result!!.getOrThrow()
	}
}
