package net.osmand.shared.api

import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * rawQuery of the JVM implementation, read the way the shared code reads a cursor on Android:
 * with selection arguments, columns indexed from 0 and moveToFirst() before the first row.
 */
class SQLiteAPIImplRawQueryTest {

	private lateinit var dir: File
	private lateinit var path: String
	private lateinit var db: SQLiteConnection

	@BeforeTest
	fun setUp() {
		dir = Files.createTempDirectory("sqlite-jvm-test").toFile()
		path = File(dir, "crs.db").absolutePath
		// Written with plain JDBC so that only rawQuery is under test
		DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
			connection.createStatement().use { statement ->
				statement.executeUpdate("CREATE TABLE crs (code INTEGER, name TEXT)")
				statement.executeUpdate(
					"INSERT INTO crs VALUES (2056, 'CH1903+ / LV95'), (4326, 'WGS 84'), (21781, 'CH1903 / LV03')"
				)
			}
		}
		db = assertNotNull(SQLiteAPIImpl().openByAbsolutePath(path, false))
	}

	@AfterTest
	fun tearDown() {
		db.close()
		dir.deleteRecursively()
	}

	@Test
	fun selectionArgumentsAreBoundInOrder() {
		val sql = "SELECT code, name FROM crs WHERE code = ? OR name = ? ORDER BY code"
		val cursor = assertNotNull(db.rawQuery(sql, arrayOf("2056", "WGS 84")))
		try {
			val rows = mutableListOf<String>()
			while (cursor.moveToNext()) {
				rows.add("${cursor.getLong(0)} ${cursor.getString(1)}")
			}
			assertEquals(listOf("2056 CH1903+ / LV95", "4326 WGS 84"), rows)
		} finally {
			cursor.close()
		}
	}

	@Test
	fun columnsAreIndexedFromZero() {
		val sql = "SELECT code, name, NULL AS area, 0.5 AS accuracy FROM crs WHERE code = 2056"
		val cursor = assertNotNull(db.rawQuery(sql, null))
		try {
			assertTrue(cursor.moveToNext())
			assertContentEquals(arrayOf("code", "name", "area", "accuracy"), cursor.getColumnNames())
			assertEquals(1, cursor.getColumnIndex("name"))
			assertEquals(2056L, cursor.getLong(0))
			assertEquals(2056, cursor.getInt(0))
			assertEquals("CH1903+ / LV95", cursor.getString(1))
			assertFalse(cursor.isNull(1))
			assertTrue(cursor.isNull(2))
			assertEquals(0.5, cursor.getDouble(3))
		} finally {
			cursor.close()
		}
	}

	@Test
	fun moveToFirstStartsOverAfterReading() {
		val cursor = assertNotNull(db.rawQuery("SELECT code FROM crs WHERE code <> ? ORDER BY code", arrayOf("4326")))
		try {
			assertTrue(cursor.moveToFirst())
			assertEquals(2056L, cursor.getLong(0))
			assertTrue(cursor.moveToNext())
			assertEquals(21781L, cursor.getLong(0))
			assertFalse(cursor.moveToNext())

			assertTrue(cursor.moveToFirst())
			assertEquals(2056L, cursor.getLong(0))
		} finally {
			cursor.close()
		}
	}

	@Test
	fun moveToFirstIsFalseWhenNothingMatches() {
		val cursor = assertNotNull(db.rawQuery("SELECT code FROM crs WHERE code = ?", arrayOf("1")))
		try {
			assertFalse(cursor.moveToFirst())
		} finally {
			cursor.close()
		}
	}

	@Test
	fun closeLetsAnotherConnectionWrite() {
		val cursor = assertNotNull(db.rawQuery("SELECT code FROM crs", null))
		assertTrue(cursor.moveToNext())
		cursor.close()
		// The file uses a rollback journal: a read left unfinished would keep this write from committing
		DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
			connection.createStatement().use { it.executeUpdate("DELETE FROM crs") }
		}
	}
}
