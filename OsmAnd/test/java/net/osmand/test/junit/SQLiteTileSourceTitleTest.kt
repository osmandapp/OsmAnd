package net.osmand.test.junit

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.IndexConstants
import net.osmand.plus.OsmandApplication
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * #26761: `OsmandSettings.getTileSourceTitle()` creates a new `SQLiteTileSource` to read the title
 * of a `.sqlitedb` map source. Reading it opened the database and nothing closed it, so lists of
 * map sources and quick action buttons leaked open files until "Too many open files".
 */
@RunWith(AndroidJUnit4::class)
class SQLiteTileSourceTitleTest {

	private lateinit var app: OsmandApplication
	private lateinit var file: File

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
	}

	@Test
	fun titleLookupClosesDatabase() {
		SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
			db.execSQL("CREATE TABLE tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x, y, z, s))")
			db.execSQL("CREATE TABLE info (title TEXT)")
			db.execSQL("INSERT INTO info VALUES ('Custom title')")
		}

		assertEquals("Custom title", app.settings.getTileSourceTitle(file.name))
		assertEquals("open files of ${file.name}", 0, openFiles())
	}

	private fun openFiles(): Int = File("/proc/self/fd").listFiles()!!.count { fd ->
		runCatching { fd.canonicalPath.contains(file.name) }.getOrDefault(false)
	}
}
