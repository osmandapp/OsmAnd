package net.osmand.shared.api

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SQLiteAPIImplReadOnlyTest {

	private val api = SQLiteAPIImpl()
	private val fs = FileSystem.SYSTEM
	private val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sqlite-ro-test-${Random.nextLong().toULong()}"

	@BeforeTest
	fun setUp() {
		fs.createDirectories(dir)
	}

	@AfterTest
	fun tearDown() {
		fs.deleteRecursively(dir)
	}

	@Test
	fun readOnlyOpenLeavesRollbackJournalFileUntouched() {
		val path = createDatabase("delete.db", "DELETE")
		assertEquals(1, writeVersion(path))
		val before = fs.read(path) { readByteString() }

		val db = assertNotNull(api.openByAbsolutePath(path.toString(), true))
		val cursor = assertNotNull(db.rawQuery("SELECT code, name FROM crs WHERE code = ?", arrayOf("2056")))
		assertTrue(cursor.moveToFirst())
		assertEquals(2056L, cursor.getLong(0))
		assertEquals("CH1903+ / LV95", cursor.getString(1))
		cursor.close()
		db.close()

		assertEquals(before, fs.read(path) { readByteString() })
		assertFalse(fs.exists("$path-wal".toPath()))
		assertFalse(fs.exists("$path-shm".toPath()))
	}

	@Test
	fun readOnlyOpenOfMissingFileReturnsNull() {
		val path = dir / "missing.db"
		assertNull(api.openByAbsolutePath(path.toString(), true))
		assertFalse(fs.exists(path))
	}

	@Test
	fun readOnlyOpenOfNonDatabaseFileReturnsNull() {
		val path = dir / "garbage.db"
		fs.write(path) { writeUtf8("not a database ".repeat(100)) }
		assertNull(api.openByAbsolutePath(path.toString(), true))
	}

	@Test
	fun readOnlyConnectionRejectsWrites() {
		val path = createDatabase("writes.db", "DELETE")
		val db = assertNotNull(api.openByAbsolutePath(path.toString(), true))
		try {
			assertTrue(db.isReadOnly())
			assertFailsWith<UnsupportedOperationException> { db.execSQL("DELETE FROM crs") }
			assertFailsWith<UnsupportedOperationException> { db.setVersion(8) }
		} finally {
			db.close()
		}
		assertTrue(db.isClosed())
	}

	@Test
	fun readOnlyOpenReadsWalDatabase() {
		val path = createDatabase("wal.db", "WAL")
		assertEquals(2, writeVersion(path))

		val db = assertNotNull(api.openByAbsolutePath(path.toString(), true))
		try {
			assertEquals(7, db.getVersion())
			val cursor = assertNotNull(db.rawQuery("SELECT code, name, area, data FROM crs ORDER BY code", null))
			assertContentEquals(arrayOf("code", "name", "area", "data"), cursor.getColumnNames())
			assertEquals(1, cursor.getColumnIndex("name"))

			assertTrue(cursor.moveToFirst())
			assertEquals(2056, cursor.getInt(0))
			assertEquals(1.5, cursor.getDouble(2))
			assertContentEquals(byteArrayOf(1, 2, 3), cursor.getBlob(3))
			assertFalse(cursor.isNull(1))

			assertTrue(cursor.moveToNext())
			assertEquals(3857L, cursor.getLong(0))
			assertTrue(cursor.isNull(1))
			assertTrue(cursor.isNull(3))

			assertFalse(cursor.moveToNext())
			cursor.close()
		} finally {
			db.close()
		}
	}

	private fun createDatabase(name: String, journalMode: String): Path {
		val path = dir / name
		val db = assertNotNull(api.openByAbsolutePath(path.toString(), false))
		try {
			db.execSQL("PRAGMA journal_mode=$journalMode")
			db.execSQL("CREATE TABLE crs (code INTEGER, name TEXT, area REAL, data BLOB)")
			db.execSQL("INSERT INTO crs VALUES (?, ?, ?, ?)", arrayOf(2056L, "CH1903+ / LV95", 1.5, byteArrayOf(1, 2, 3)))
			db.execSQL("INSERT INTO crs VALUES (?, ?, ?, ?)", arrayOf(3857L, null, null, null))
			db.setVersion(7)
		} finally {
			db.close()
		}
		return path
	}

	// Byte 18 of the SQLite header: 1 for a rollback journal, 2 for WAL.
	private fun writeVersion(path: Path): Int = fs.read(path) {
		skip(18)
		readByte().toInt()
	}
}
