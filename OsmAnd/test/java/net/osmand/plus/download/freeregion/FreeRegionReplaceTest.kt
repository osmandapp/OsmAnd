package net.osmand.plus.download.freeregion

import android.content.Context
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.IndexConstants
import net.osmand.map.WorldRegion
import net.osmand.plus.OsmandApplication
import net.osmand.plus.download.DownloadActivityType
import net.osmand.plus.download.IndexItem
import net.osmand.plus.download.local.LocalItem
import net.osmand.plus.download.local.LocalItemType
import net.osmand.plus.utils.FileUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression tests for osmandapp/OsmAnd-Issues#3394: "Delete and download" deleted the maps of the old
 * free region before the new map was downloaded, so a failed or cancelled download left no maps.
 *
 * The replace now deactivates the old maps and hands them to the download, which deletes them only
 * once a new map is installed. Activating deactivated maps of another region makes it the free region
 * without a server call. Needs the free version: run on a nightlyFree build (net.osmand.dev).
 *
 * The replace deactivates every map of other regions on the device; the test moves them back and
 * restores the free region afterwards.
 */
@RunWith(AndroidJUnit4::class)
class FreeRegionReplaceTest {

	companion object {
		private const val PREFERENCES_NAME = "net.osmand.free_region"
		private const val TIMEOUT_SECONDS = 30L
		// the active maps of a run that did not reach tearDown, to put back on the next run
		private const val ACTIVE_MAPS_FILE = "free_region_replace_test_maps.txt"
	}

	private lateinit var app: OsmandApplication
	private lateinit var freeMaps: FreeVersionMapDownloads
	private lateinit var mapsDir: File
	private lateinit var roadsDir: File
	private lateinit var backupDir: File

	private lateinit var savedPreferences: Map<String, *>
	private lateinit var activeMapsBefore: List<File>
	private val createdFiles = mutableListOf<File>()

	private lateinit var malta: WorldRegion
	private lateinit var andorra: WorldRegion

	@Before
	fun setUp() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		freeMaps = app.downloadThread.freeVersionMapDownloads
		assumeTrue("Needs the free version (nightlyFree build)", freeMaps.isFreeVersion)
		waitFor("regions loaded") { app.regions.isInitialized }

		malta = getRegion("malta_europe")
		andorra = getRegion("andorra_europe")

		mapsDir = app.getAppPath(IndexConstants.MAPS_PATH)
		roadsDir = app.getAppPath(IndexConstants.ROADS_INDEX_DIR)
		backupDir = app.getAppPath(IndexConstants.BACKUP_INDEX_DIR)
		restoreActiveMaps(getActiveMapsFile().takeIf { it.exists() }?.readLines().orEmpty().map { File(it) })
		activeMapsBefore = listOf(mapsDir, roadsDir).flatMap { it.listFiles()?.toList().orEmpty() }
		// a deactivation would overwrite the deactivated copy
		assumeFalse("A map is both active and deactivated on this device",
			activeMapsBefore.any { File(backupDir, it.name).exists() })
		savedPreferences = getPreferences().all.toMap()
		getActiveMapsFile().writeText(activeMapsBefore.joinToString("\n") { it.absolutePath })
	}

	@After
	fun tearDown() {
		if (!::savedPreferences.isInitialized) {
			return
		}
		restoreActiveMaps(activeMapsBefore)
		getActiveMapsFile().delete()
		for (file in createdFiles) {
			file.delete()
			File(backupDir, file.name).delete()
		}
		val editor = getPreferences().edit().clear()
		for ((key, value) in savedPreferences) {
			when (value) {
				is String -> editor.putString(key, value)
				is Int -> editor.putInt(key, value)
				is Boolean -> editor.putBoolean(key, value)
			}
		}
		editor.commit()
	}

	@Test
	fun replaceDeactivatesOldMapsAndCountsTheReplace() {
		val maltaMap = createMap(mapsDir, "Malta_europe.obf")
		val maltaRoads = createMap(roadsDir, "Malta_europe.road.obf")
		val andorraMap = createMap(mapsDir, "Andorra_europe.obf")
		val worldMap = createMap(mapsDir, "World_basemap.obf")
		setFreeRegion("Malta_europe_2.obf.zip")
		val replaces = freeMaps.replacesCount

		val filesToDelete = replace(andorra)

		assertEquals(andorra.regionId, freeMaps.freeRegion?.regionId)
		assertEquals(replaces + 1, freeMaps.replacesCount)
		// deactivated, not deleted
		assertFalse(maltaMap.exists())
		assertFalse(maltaRoads.exists())
		assertTrue(File(backupDir, maltaMap.name).exists())
		assertTrue(File(backupDir, maltaRoads.name).exists())
		// the new region and the world map stay active
		assertTrue(andorraMap.exists())
		assertTrue(worldMap.exists())
		// the download deletes the deactivated copies once a new map is installed
		assertTrue(filesToDelete.contains(File(backupDir, maltaMap.name)))
		assertTrue(filesToDelete.contains(File(backupDir, maltaRoads.name)))
		assertFalse(filesToDelete.any { it.name == andorraMap.name || it.name == worldMap.name })
	}

	@Test
	fun downloadNotInstalledLeavesOldMapsDeactivated() {
		val maltaMap = createMap(mapsDir, "Malta_europe.obf")
		// the test deletes the deactivated copy: never a map of the device itself
		assumeTrue("Malta_europe.obf is on this device", maltaMap in createdFiles)
		val deactivatedAndorraRoads = createMap(backupDir, "Andorra_europe.road.obf")
		setFreeRegion("Malta_europe_2.obf.zip")
		val newMap = createIndexItem("Andorra_europe_2.obf.zip")

		val filesToDelete = replace(andorra)
		val deactivatedMalta = File(backupDir, maltaMap.name)
		assertTrue(filesToDelete.contains(deactivatedMalta))

		// failed, cancelled, no space: nothing deletes the deactivated maps, the user is told
		val reported = freeMaps.onMapsNotReplaced(newMap, filesToDelete)
		assertTrue(reported.any { it.regionId == malta.regionId })
		assertTrue(deactivatedMalta.exists())

		// once the new map is installed the download has deleted them: nothing to tell
		deactivatedMalta.delete()
		assertEquals(emptyList<WorldRegion>(), freeMaps.onMapsNotReplaced(newMap, listOf(deactivatedMalta)))
		// the duplicate map warning hands over a map of the downloaded region: not a replace
		assertEquals(emptyList<WorldRegion>(), freeMaps.onMapsNotReplaced(newMap, listOf(deactivatedAndorraRoads)))
	}

	@Test
	fun activatingMapsOfAnotherRegionMakesItFreeWithoutReplace() {
		val andorraMap = createMap(mapsDir, "Andorra_europe.obf")
		setFreeRegion("Andorra_europe_2.obf.zip")
		val replaces = freeMaps.replacesCount
		val deactivatedMalta = LocalItem(File(backupDir, "Malta_europe.obf"), LocalItemType.MAP_DATA)
		val worldMap = LocalItem(File(backupDir, "World_basemap.obf"), LocalItemType.MAP_DATA)
		val andorraWiki = LocalItem(File(backupDir, "Andorra_europe.wiki.obf"), LocalItemType.WIKI_AND_TRAVEL_MAPS)

		// world and plugin maps never ask
		assertNull(freeMaps.getActivatedRegion(listOf(worldMap, andorraWiki)))
		assertEquals(malta.regionId, freeMaps.getActivatedRegion(listOf(deactivatedMalta))?.regionId)

		val mapsToDeactivate = freeMaps.activateFreeRegion(malta)

		assertEquals(malta.regionId, freeMaps.freeRegion?.regionId)
		assertEquals(replaces, freeMaps.replacesCount)
		assertTrue(mapsToDeactivate.any { it.file == andorraMap })
		// maps of the free region activate without a prompt
		assertNull(freeMaps.getActivatedRegion(listOf(deactivatedMalta)))
	}

	/** Maps a replace deactivated go back to where they were. */
	private fun restoreActiveMaps(files: List<File>) {
		for (file in files) {
			val deactivated = File(backupDir, file.name)
			if (!file.exists() && deactivated.exists()) {
				FileUtils.move(deactivated, file)
			}
		}
	}

	private fun getActiveMapsFile() = File(app.cacheDir, ACTIVE_MAPS_FILE)

	private fun replace(region: WorldRegion): List<File> {
		val deactivated = CountDownLatch(1)
		var filesToDelete: List<File> = emptyList()
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			freeMaps.replaceFreeRegion(region) { files ->
				filesToDelete = files
				deactivated.countDown()
			}
		}
		assertTrue("Maps were not deactivated", deactivated.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
		return filesToDelete
	}

	/** A map file of a real region; one already on the device is used as it is. */
	private fun createMap(dir: File, name: String): File {
		val file = File(dir, name)
		assumeFalse("$name is deactivated on this device", File(backupDir, name).exists())
		if (!file.exists()) {
			dir.mkdirs()
			assertTrue(file.createNewFile())
			createdFiles.add(file)
		}
		return file
	}

	private fun setFreeRegion(fileName: String) {
		freeMaps.chooseFreeRegion(createIndexItem(fileName))
		assertNotNull(freeMaps.freeRegion)
	}

	private fun createIndexItem(fileName: String): IndexItem {
		return IndexItem(fileName, null, 0, null, 0, 0, DownloadActivityType.NORMAL_FILE, false, null)
	}

	private fun getRegion(downloadName: String): WorldRegion {
		val region = app.regions.getRegionDataByDownloadName(downloadName)
		assertNotNull(downloadName, region)
		return region
	}

	private fun getPreferences() = app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

	private fun waitFor(what: String, condition: () -> Boolean) {
		val end = SystemClock.uptimeMillis() + TIMEOUT_SECONDS * 1000
		while (!condition()) {
			assertTrue("Timed out: $what", SystemClock.uptimeMillis() < end)
			Thread.sleep(100)
		}
	}
}
