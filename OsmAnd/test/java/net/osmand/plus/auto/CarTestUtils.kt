package net.osmand.plus.auto

import android.annotation.SuppressLint
import androidx.car.app.CarContext
import androidx.car.app.HostDispatcher
import androidx.car.app.suggestion.SuggestionManager
import androidx.car.app.suggestion.model.Suggestion
import androidx.car.app.testing.TestLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.search.history.SearchHistoryHelper
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.HistorySource
import net.osmand.search.core.SearchPhrase
import net.osmand.util.MapUtils
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@SuppressLint("RestrictedApi")
object CarTestUtils {

	const val TEST_HOST_PACKAGE = "androidx.car.app.testing"

	const val APP_INIT_TIMEOUT_SEC = 60L

	const val INIT_POLL_MS = 200L

	const val ENTRY_TIMESTAMP_INTERVAL_MS = 25L

	private const val TEST_LOCATION_MATCH_RADIUS_METERS = 50.0

	fun waitForAppInitialization(app: OsmandApplication, timeoutSec: Long = APP_INIT_TIMEOUT_SEC) {
		val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(INIT_POLL_MS)
		}
		assertFalse("Application initialization timed out before tests could run", app.isApplicationInitializing)
	}

	/**
	 * The tests write to the real history: at its limit every added entry deletes the oldest entry of the user.
	 */
	fun assumeHistoryHasRoom(app: OsmandApplication) {
		val size = app.searchHistoryHelper.getHistoryEntries(null, false, true).size
		assumeTrue("History has $size entries, test entries would push out the user's entries",
			size + 10 <= SearchHistoryHelper.HISTORY_LIMIT)
	}

	fun <T> onMain(block: () -> T): T {
		var result: Result<T>? = null
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			result = runCatching(block)
		}
		return result!!.getOrThrow()
	}

	private val TARGET_POINTS_BACKUP_DOUBLE_KEYS = listOf(
		OsmandSettings.POINT_NAVIGATE_LAT_BACKUP,
		OsmandSettings.POINT_NAVIGATE_LON_BACKUP,
		OsmandSettings.START_POINT_LAT_BACKUP,
		OsmandSettings.START_POINT_LON_BACKUP
	)

	private val TARGET_POINTS_BACKUP_STRING_KEYS = listOf(
		OsmandSettings.POINT_NAVIGATE_DESCRIPTION_BACKUP,
		OsmandSettings.START_POINT_DESCRIPTION_BACKUP,
		OsmandSettings.INTERMEDIATE_POINTS_BACKUP,
		OsmandSettings.INTERMEDIATE_POINTS_DESCRIPTION_BACKUP
	)

	class TargetPointsBackup(val doubles: Map<String, Double?>, val strings: Map<String, String?>)

	fun saveTargetPointsBackup(app: OsmandApplication): TargetPointsBackup {
		val api = app.settings.settingsAPI
		val prefs = app.settings.globalPreferences
		val doubles = TARGET_POINTS_BACKUP_DOUBLE_KEYS.associateWith { key ->
			if (api.contains(prefs, key)) {
				api.getDouble(prefs, key, 0.0)
			} else {
				null
			}
		}
		val strings = TARGET_POINTS_BACKUP_STRING_KEYS.associateWith { key ->
			if (api.contains(prefs, key)) {
				api.getString(prefs, key, "")
			} else {
				null
			}
		}
		return TargetPointsBackup(doubles, strings)
	}

	fun restoreTargetPointsBackup(app: OsmandApplication, backup: TargetPointsBackup) {
		val editor = app.settings.settingsAPI.edit(app.settings.globalPreferences)
		for ((key, value) in backup.doubles) {
			if (value != null) {
				editor.putDouble(key, value)
			} else {
				editor.remove(key)
			}
		}
		for ((key, value) in backup.strings) {
			if (value != null) {
				editor.putString(key, value)
			} else {
				editor.remove(key)
			}
		}
		editor.commit()
	}

	fun navigationHistoryKeys(app: OsmandApplication): Set<String> {
		return app.searchHistoryHelper.getHistoryEntries(HistorySource.NAVIGATION, false, true)
			.map { entry ->
				PointDescription.serializeToString(entry.name)
			}
			.toSet()
	}

	fun cleanupHistoryEntries(
		app: OsmandApplication,
		testPrefix: String,
		createdDescriptions: MutableCollection<PointDescription> = mutableListOf(),
		historyBefore: Set<String>? = null,
		testStartTimeMs: Long = 0L,
		testPoints: Collection<LatLon> = emptyList()
	) {
		for (pd in createdDescriptions) {
			var entry = app.searchHistoryHelper.getEntryByName(pd, HistorySource.NAVIGATION)
			if (entry == null) {
				entry = app.searchHistoryHelper.getHistoryEntries(HistorySource.NAVIGATION, false, true)
					.firstOrNull { historyEntry ->
						historyEntry.name?.name == pd.name
					}
			}
			if (entry != null) {
				val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
				val result = SearchHistoryAPI.createSearchResult(app, entry, phrase)
				app.searchHistoryHelper.remove(result)
			}
		}
		createdDescriptions.clear()

		val allEntries = app.searchHistoryHelper.getHistoryEntries(HistorySource.NAVIGATION, false, true)
		for (entry in allEntries) {
			val entryName = entry.name?.name ?: ""
			val absentFromSnapshot = historyBefore?.let { before ->
				PointDescription.serializeToString(entry.name) !in before
			} ?: false
			val matchesTestLocation = testPoints.any { point ->
				MapUtils.getDistance(entry.lat, entry.lon, point.latitude, point.longitude) <= TEST_LOCATION_MATCH_RADIUS_METERS
			}
			val isTestReverseGeocoded = absentFromSnapshot && entry.lastAccessTime >= testStartTimeMs && matchesTestLocation
			if (entryName.contains(testPrefix) || isTestReverseGeocoded) {
				val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
				val result = SearchHistoryAPI.createSearchResult(app, entry, phrase)
				app.searchHistoryHelper.remove(result)
			}
		}
	}

	fun nextNewestTime(app: OsmandApplication): Long {
		val newestExisting = app.searchHistoryHelper.getHistoryEntries(HistorySource.NAVIGATION, false, true)
			.maxOfOrNull { entry ->
				entry.lastAccessTime
			} ?: 0L
		return maxOf(System.currentTimeMillis(), newestExisting + ENTRY_TIMESTAMP_INTERVAL_MS)
	}

	fun addNewestEntry(
		app: OsmandApplication,
		pd: PointDescription,
		lat: Double,
		lon: Double,
		createdDescriptions: MutableCollection<PointDescription>? = null
	): HistoryEntry {
		createdDescriptions?.add(pd)
		val entry = HistoryEntry(lat, lon, pd, HistorySource.NAVIGATION)
		entry.markAsAccessed(nextNewestTime(app))
		app.searchHistoryHelper.addItemsToHistory(listOf(entry))
		return entry
	}

	class RecordingSuggestionManager(carContext: CarContext) :
		SuggestionManager(carContext, HostDispatcher(), TestLifecycleOwner().lifecycle) {

		val publications = CopyOnWriteArrayList<List<Suggestion>>()

		override fun updateSuggestions(suggestions: List<Suggestion>) {
			publications.add(ArrayList(suggestions))
		}

		fun clear() {
			publications.clear()
		}
	}
}
