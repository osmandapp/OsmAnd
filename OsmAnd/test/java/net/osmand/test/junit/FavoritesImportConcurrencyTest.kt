package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.myplaces.favorites.FavouritesHelper
import net.osmand.plus.settings.backend.backup.items.FavoritesSettingsItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Regression test for issue #3335 - Cloud sync dropped downloaded favourite folders and then
 * uploaded them as deletions, which removed them from every other device.
 *
 * `BackupImporter.importItems()` downloads one file per settings item on a four thread pool and
 * calls `SettingsItem.apply()` from the worker thread. For favourites that means several threads
 * run `FavoritesSettingsItem.apply()` at once against the one shared [FavouritesHelper], whose
 * `favoriteGroups` and `flatGroups` are updated by copy-on-write: each thread reads the current
 * collection, copies it, adds its own group and assigns the copy back. Two threads that read the
 * same base both write a copy holding only their own addition, so one group is lost.
 *
 * Losing the group also loses its file. `apply()` finishes with a full save, and
 * `SaveFavoritesTask.cleanupOrphanedGroupFiles()` deletes every `favorites-*.gpx` whose group is
 * absent from the saved snapshot - so a thread saving a snapshot that never had another thread's
 * group deletes that group's file even when it was already written correctly.
 *
 * The damage the issue reports comes one sync later. `BackupImporter` records the download as
 * complete either way, so the next `GenerateBackupInfoTask` finds a remote file, an upload record
 * and no local file, reads that as a local deletion and uploads `filesize = -1`.
 *
 * The test drives `apply()` directly on a pool of the same size as the importer's, because the
 * race is between the applies and needs neither the network nor an account. [GROUPS] groups are
 * enough that some pair of threads interleaves on nearly every run; before the fix a handful of
 * them are missing afterwards, and the assertions name them.
 *
 * Both the in-memory group and its file are checked. They fail for different reasons - the lost
 * update leaves no group to save, the orphan cleanup removes a file that was written - and
 * `apply()` reloads from disk at the end, so either failure ends with both missing.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FavoritesImportConcurrencyTest {

	companion object {
		/** Marks the groups this test creates, so cleanup never touches the user's own. */
		private const val GROUP_PREFIX = "test-3335-"

		/** Groups applied concurrently. See the class comment for why one is not enough. */
		private const val GROUPS = 30

		/** `BackupImporter.createExecutor()` uses `ThreadPoolTaskExecutor`'s default pool size. */
		private const val THREADS = 4

		private const val POINTS_PER_GROUP = 3
		private const val APPLY_TIMEOUT_SEC = 120L
		private const val APP_INIT_TIMEOUT_SEC = 120L
	}

	private lateinit var app: OsmandApplication
	private lateinit var favoritesHelper: FavouritesHelper

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext
			.applicationContext as OsmandApplication
		val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(APP_INIT_TIMEOUT_SEC)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(200)
		}
		favoritesHelper = app.favoritesHelper
		removeTestGroups()
	}

	@Test
	fun concurrentApplyKeepsEveryGroup() {
		val names = (0 until GROUPS).map { GROUP_PREFIX + it }
		val items = names.map { name ->
			FavoritesSettingsItem(app, mutableListOf(group(name)))
		}

		val executor = Executors.newFixedThreadPool(THREADS)
		try {
			// The importer applies each downloaded item on its own worker thread.
			val futures = items.map { item ->
				executor.submit {
					item.processDuplicateItems()
					item.apply()
				}
			}
			executor.shutdown()
			assertTrue("the applies did not finish in time",
				executor.awaitTermination(APPLY_TIMEOUT_SEC, TimeUnit.SECONDS))
			futures.forEach { it.get() }
		} finally {
			executor.shutdownNow()
		}

		val missingGroups = names.filter { favoritesHelper.getGroup(it) == null }
		val missingFiles = names.filter { name ->
			val group = FavoriteGroup(name, ArrayList(), 0, true, false)
			!favoritesHelper.fileHelper.getExternalFile(group).exists()
		}

		assertEquals("groups dropped by concurrent apply()", emptyList<String>(), missingGroups)
		assertEquals("group files missing after concurrent apply()", emptyList<String>(), missingFiles)
	}

	@After
	fun cleanup() {
		removeTestGroups()
	}

	private fun removeTestGroups() {
		val groups = favoritesHelper.favoriteGroups.filter { it.name.startsWith(GROUP_PREFIX) }
		if (groups.isEmpty()) {
			return
		}
		groups.forEach { favoritesHelper.deleteGroup(it, false) }
		favoritesHelper.saveCurrentPointsIntoFile(false)
		favoritesHelper.loadFavorites()
	}

	/** The altitude is set so that `addFavourite()` does not start an altitude lookup for it. */
	private fun group(name: String): FavoriteGroup {
		val points = ArrayList<FavouritePoint>()
		for (i in 0 until POINTS_PER_GROUP) {
			points.add(FavouritePoint(50.0 + i / 1000.0, 4.0 + i / 1000.0, "$name-$i", name,
				10.0, (i + 1) * 1_000L))
		}
		return FavoriteGroup(name, points, 0, true, false)
	}
}
