package net.osmand.test.junit

import android.database.sqlite.SQLiteDatabase
import android.os.FileObserver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.IndexConstants
import net.osmand.map.ITileSource
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.backup.exporttype.ExportType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * #26761: `OsmandSettings.getTileSourceTitle()` creates a new `SQLiteTileSource` to read the title
 * of a `.sqlitedb` map source. Reading it opened the database and nothing closed it, so lists of
 * map sources and quick action buttons leaked open files until "Too many open files".
 */
@RunWith(AndroidJUnit4::class)
class SQLiteTileSourceTitleTest {

	private lateinit var app: OsmandApplication
	private lateinit var file: File
	private val sources = mutableListOf<File>()

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation()
			.targetContext.applicationContext as OsmandApplication
		file = File(app.getAppPath(IndexConstants.TILES_INDEX_DIR), "test_26761.sqlitedb")
		file.parentFile?.mkdirs()
		SQLiteDatabase.deleteDatabase(file)
	}

	@After
	fun tearDown() {
		SQLiteDatabase.deleteDatabase(file)
		sources.forEach { SQLiteDatabase.deleteDatabase(it) }
	}

	@Test
	fun titleLookupClosesDatabase() {
		createSource(file, "Custom title")

		assertEquals("Custom title", app.settings.getTileSourceTitle(file.name))
		assertEquals("open files of ${file.name}", 0, openFiles())
	}

	/**
	 * The map export list is sorted by the names shown to the user. The comparator looked up the
	 * title of a `.sqlitedb` source on every comparison, opening the database each time on the
	 * main thread (an ANR in Play Console).
	 */
	@Test
	fun mapExportReadsEachTitleOnce() {
		val dir = file.parentFile!!
		for (i in 1..SOURCES) {
			// File names run opposite to the titles, so the sort has to read the titles
			val source = File(dir, "test_export_%02d.sqlitedb".format(i))
			SQLiteDatabase.deleteDatabase(source)
			createSource(source, "Source %02d".format(SOURCES + 1 - i))
			sources.add(source)
		}
		val names = sources.map { it.name }.toSet()
		val sentinel = File(dir, "test_export_sentinel").apply { writeText("") }
		val opens = AtomicInteger()
		val drained = CountDownLatch(1)
		@Suppress("DEPRECATION")
		val observer = object : FileObserver(dir.path, OPEN) {
			override fun onEvent(event: Int, path: String?) {
				when (path) {
					sentinel.name -> drained.countDown()
					in names -> opens.incrementAndGet()
				}
			}
		}
		val exported: List<*>
		observer.startWatching()
		try {
			exported = ExportType.STANDARD_MAPS.fetchExportData(app, false)
			// Events arrive in order: once the sentinel's is seen, all the export's opens are counted
			sentinel.readText()
			assertTrue("sentinel open event", drained.await(10, TimeUnit.SECONDS))
		} finally {
			observer.stopWatching()
			sentinel.delete()
		}

		val exportedSources = exported.map { (it as File).name }.filter { it in names }
		assertEquals("sorted by title", sources.map { it.name }.reversed(), exportedSources)
		assertTrue("opens of $SOURCES map sources: ${opens.get()}", opens.get() <= 2 * SOURCES)
	}

	/**
	 * The export of map sources (export screen, OsmAnd Cloud backup) creates a `SQLiteTileSource`
	 * for every `.sqlitedb` and reads its URL template from the database. Nothing closed it.
	 */
	@Test
	fun mapSourcesExportClosesDatabases() {
		val online = File(file.parentFile, "test_export_online.sqlitedb")
		SQLiteDatabase.deleteDatabase(online)
		sources.add(online)
		createSource(online, "Online title", URL)
		createSource(file, "Offline title")

		val exported = ExportType.MAP_SOURCES.fetchExportData(app, false)
		val openFiles = openFiles(online) + openFiles(file)

		// The exported sources are still referenced here, so GC cannot have closed their databases
		val source = exported.map { it as ITileSource }.firstOrNull { it.name == "test_export_online" }
		assertNotNull("exported", source)
		assertEquals("open files", 0, openFiles)
		// The export writes the info kept in fields, reading it does not open the database again
		assertEquals(URL, source!!.urlTemplate)
		assertEquals("open files after reading the info", 0, openFiles(online))
	}

	private fun createSource(source: File, title: String, url: String? = null) {
		SQLiteDatabase.openOrCreateDatabase(source, null).use { db ->
			db.execSQL("CREATE TABLE tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x, y, z, s))")
			db.execSQL("CREATE TABLE info (title TEXT, url TEXT)")
			db.execSQL("INSERT INTO info VALUES (?, ?)", arrayOf(title, url))
		}
	}

	private fun openFiles(source: File = file): Int = File("/proc/self/fd").listFiles()!!.count { fd ->
		runCatching { fd.canonicalPath.contains(source.name) }.getOrDefault(false)
	}

	companion object {
		private const val SOURCES = 20
		private const val URL = "https://tile.example.com/{0}/{1}/{2}.png"
	}
}
