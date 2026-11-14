package net.osmand.plus.myplaces.favorites

import android.os.Build
import android.os.StrictMode
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.FavouritePoint
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * A favourite saved from a POI without an icon of its own shows the icon of that POI. Finding the
 * POI reads the map files, and it ran on the main thread while the favourites layer was drawn: an
 * ANR showed the main thread in `BinaryMapPoiReaderAdapter.searchPoiIndex` under
 * `FavoritesTileProvider.addToData`.
 *
 * Needs a map with POIs around [LAT], [LON] (Monaco_europe.obf); without one the test is skipped.
 * The point is never added to the favourites, so the user's favourites stay as they are.
 */
@RunWith(AndroidJUnit4::class)
class FavoriteOriginalIconTest {

	companion object {
		// Place du Casino, Monaco
		private const val LAT = 43.7393
		private const val LON = 7.4280
		private const val CATEGORY = "test-original-icon"
		private const val APP_INIT_TIMEOUT_SEC = 120L
		private const val ICON_TIMEOUT_MS = 10_000L
	}

	private lateinit var app: OsmandApplication

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext
			.applicationContext as OsmandApplication
		val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(APP_INIT_TIMEOUT_SEC)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(200)
		}
	}

	@Test
	fun poiIconIsNotSearchedOnTheMainThread() {
		assumeTrue("StrictMode listeners need Android 9", Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
		val (point, poiIconId) = pointSavedFromPoi()
			?: return assumeTrue("Needs a map with POIs around $LAT, $LON", false)

		val diskReads = CopyOnWriteArrayList<Throwable>()
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		var oldPolicy: StrictMode.ThreadPolicy? = null
		instrumentation.runOnMainSync {
			oldPolicy = StrictMode.getThreadPolicy()
			StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
				.detectDiskReads()
				.penaltyListener({ it.run() }) { violation ->
					if (violation.stackTrace.any { it.methodName == "getIconIdOrDefault" }) {
						diskReads.add(violation)
					}
				}
				.build())
			point.getIconIdOrDefault(app)
		}
		// StrictMode reports a main thread violation once the message that caused it has finished
		instrumentation.runOnMainSync { StrictMode.setThreadPolicy(oldPolicy) }
		assertTrue("getIconIdOrDefault read the disk on the main thread: ${diskReads.firstOrNull()?.stackTraceToString()}",
			diskReads.isEmpty())

		app.favoritesHelper.searchOriginalIcons(listOf(point))
		val deadline = SystemClock.uptimeMillis() + ICON_TIMEOUT_MS
		while (point.iconId != poiIconId && SystemClock.uptimeMillis() < deadline) {
			Thread.sleep(50)
		}
		assertEquals("The point gets the icon of its POI", poiIconId, point.iconId)
	}

	private fun pointSavedFromPoi(): Pair<FavouritePoint, Int>? {
		val resources = app.resourceManager
		val amenities = resources.amenitySearcher.searchAmenities(LatLon(LAT, LON), resources.defaultAmenitySearchSettings)
		for (amenity in amenities) {
			val name = amenity.toStringEn()
			if (name.isNullOrEmpty()) {
				continue
			}
			val point = FavouritePoint(amenity.location.latitude, amenity.location.longitude, name, CATEGORY)
			point.amenityOriginName = name
			val iconId = app.favoritesHelper.getOriginalIconId(point)
			if (iconId != 0) {
				return point to iconId
			}
		}
		return null
	}
}
