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
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.HistorySource
import net.osmand.search.core.SearchPhrase
import net.osmand.util.MapUtils
import org.junit.Assert.assertFalse
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Shared test utilities for Android Auto suggestion and navigation tests (issue #25977).
 */
@SuppressLint("RestrictedApi")
object CarTestUtils {

	/** Mock host package name for Car App testing handshake. */
	const val TEST_HOST_PACKAGE = "androidx.car.app.testing"

	/** Maximum time in seconds to wait for OsmandApplication initialization before executing tests. */
	const val APP_INIT_TIMEOUT_SEC = 60L

	/** Polling interval in milliseconds while waiting for OsmandApplication initialization. */
	const val INIT_POLL_MS = 200L

	/** Brief pause in milliseconds between adding history entries so they receive strictly ascending timestamps. */
	const val ENTRY_TIMESTAMP_INTERVAL_MS = 25L

	/**
	 * Waits for [OsmandApplication] initialization to complete by polling [OsmandApplication.isApplicationInitializing].
	 *
	 * @param app the target application instance.
	 * @param timeoutSec maximum wait time in seconds.
	 */
	fun waitForAppInitialization(app: OsmandApplication, timeoutSec: Long = APP_INIT_TIMEOUT_SEC) {
		val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(INIT_POLL_MS)
		}
		assertFalse("Application initialization timed out before tests could run", app.isApplicationInitializing)
	}

	/**
	 * Runs the given [block] synchronously on the main thread and returns its result,
	 * rethrowing any exception thrown by the block.
	 */
	fun <T> onMain(block: () -> T): T {
		var result: Result<T>? = null
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			result = runCatching(block)
		}
		return result!!.getOrThrow()
	}

	/** Float keys of the "previous route" backup that every new destination overwrites. */
	private val TARGET_POINTS_BACKUP_FLOAT_KEYS = listOf(
		OsmandSettings.POINT_NAVIGATE_LAT_BACKUP,
		OsmandSettings.POINT_NAVIGATE_LON_BACKUP,
		OsmandSettings.START_POINT_LAT_BACKUP,
		OsmandSettings.START_POINT_LON_BACKUP
	)

	/** String keys of the "previous route" backup that every new destination overwrites. */
	private val TARGET_POINTS_BACKUP_STRING_KEYS = listOf(
		OsmandSettings.POINT_NAVIGATE_DESCRIPTION_BACKUP,
		OsmandSettings.START_POINT_DESCRIPTION_BACKUP,
		OsmandSettings.INTERMEDIATE_POINTS_BACKUP,
		OsmandSettings.INTERMEDIATE_POINTS_DESCRIPTION_BACKUP
	)

	/** Saved "previous route" backup values; null means the key was absent. */
	class TargetPointsBackup(val floats: Map<String, Float?>, val strings: Map<String, String?>)

	/**
	 * Saves the "previous route" backup ([OsmandSettings.backupTargetPoints]), which a route preview
	 * overwrites with the test destination.
	 */
	fun saveTargetPointsBackup(app: OsmandApplication): TargetPointsBackup {
		val api = app.settings.settingsAPI
		val prefs = app.settings.globalPreferences
		val floats = TARGET_POINTS_BACKUP_FLOAT_KEYS.associateWith { key ->
			if (api.contains(prefs, key)) {
				api.getFloat(prefs, key, 0f)
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
		return TargetPointsBackup(floats, strings)
	}

	/** Restores the "previous route" backup saved by [saveTargetPointsBackup]. */
	fun restoreTargetPointsBackup(app: OsmandApplication, backup: TargetPointsBackup) {
		val editor = app.settings.settingsAPI.edit(app.settings.globalPreferences)
		for ((key, value) in backup.floats) {
			if (value != null) {
				editor.putFloat(key, value)
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

	/**
	 * Takes a snapshot of all navigation history entry serialized keys currently present in the app.
	 */
	fun navigationHistoryKeys(app: OsmandApplication): Set<String> {
		return app.searchHistoryHelper.getHistoryEntries(HistorySource.NAVIGATION, false, true)
			.map { entry ->
				PointDescription.serializeToString(entry.name)
			}
			.toSet()
	}

	/**
	 * Cleans up navigation history entries created during test execution.
	 *
	 * Destinations of a route preview may be named by reverse geocoding instead of using
	 * the test prefix. To clean them up safely without deleting foreign entries (e.g. user
	 * history), only entries absent from the pre-test snapshot whose [HistoryEntry.lastAccessTime]
	 * is >= [testStartTimeMs] and whose coordinates are within ~50 meters of one of the [testPoints]
	 * are removed. Entries whose names contain [testPrefix] are always removed.
	 */
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
				MapUtils.getDistance(entry.lat, entry.lon, point.latitude, point.longitude) <= 50.0
			}
			val isTestReverseGeocoded = absentFromSnapshot && entry.lastAccessTime >= testStartTimeMs && matchesTestLocation
			if (entryName.contains(testPrefix) || isTestReverseGeocoded) {
				val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
				val result = SearchHistoryAPI.createSearchResult(app, entry, phrase)
				app.searchHistoryHelper.remove(result)
			}
		}
	}

	/**
	 * Computes a timestamp strictly newer than all existing navigation history entries.
	 *
	 * Real history entries may carry timestamps from the future (e.g. written while the device clock
	 * was ahead), so a test entry stamped with "now" is not guaranteed to be the newest one.
	 */
	fun nextNewestTime(app: OsmandApplication): Long {
		val newestExisting = app.searchHistoryHelper.getHistoryEntries(HistorySource.NAVIGATION, false, true)
			.maxOfOrNull { entry ->
				entry.lastAccessTime
			} ?: 0L
		return maxOf(System.currentTimeMillis(), newestExisting + ENTRY_TIMESTAMP_INTERVAL_MS)
	}

	/**
	 * Adds a new navigation history entry with a timestamp strictly greater than all existing entries,
	 * optionally tracking it in [createdDescriptions] for subsequent cleanup.
	 */
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

	/**
	 * Test double for [SuggestionManager] that records all published suggestion lists.
	 */
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
