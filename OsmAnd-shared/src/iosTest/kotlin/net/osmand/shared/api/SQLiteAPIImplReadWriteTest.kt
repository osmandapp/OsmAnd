package net.osmand.shared.api

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.posix.chmod
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class SQLiteAPIImplReadWriteTest {

	private val api = SQLiteAPIImpl()
	private val fs = FileSystem.SYSTEM
	private val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sqlite-rw-test-${Random.nextLong().toULong()}"

	@BeforeTest
	fun setUp() {
		fs.createDirectories(dir)
	}

	@AfterTest
	fun tearDown() {
		fs.deleteRecursively(dir)
	}

	@Test
	fun readWriteOpenCreatesWalDatabase() {
		val path = dir / "new.db"
		val db = assertNotNull(api.openByAbsolutePath(path.toString(), false))
		try {
			assertFalse(db.isReadOnly())
			assertEquals(0, db.getVersion())
			db.execSQL("CREATE TABLE t (x INTEGER)")
		} finally {
			db.close()
		}
		assertTrue(db.isClosed())
		assertEquals(2, writeVersion(path))
	}

	@Test
	fun readWriteOpenSwitchesRollbackJournalToWal() {
		val path = dir / "delete.db"
		val db = assertNotNull(api.openByAbsolutePath(path.toString(), false))
		db.execSQL("PRAGMA journal_mode=DELETE")
		db.execSQL("CREATE TABLE t (x INTEGER)")
		db.close()
		assertEquals(1, writeVersion(path))

		assertNotNull(api.openByAbsolutePath(path.toString(), false)).close()
		assertEquals(2, writeVersion(path))
	}

	@Test
	fun transactionCommitsOnlyWhenMarkedSuccessful() {
		val db = openTable("tx.db", "x INTEGER")
		try {
			db.beginTransaction()
			db.execSQL("INSERT INTO t VALUES (1)")
			db.setTransactionSuccessful()
			db.endTransaction()

			db.beginTransaction()
			db.execSQL("INSERT INTO t VALUES (2)")
			db.endTransaction()

			assertEquals(listOf(1L), longs(db, "SELECT x FROM t"))
		} finally {
			db.close()
		}
	}

	@Test
	fun execSqlBindsEveryArgumentType() {
		val db = openTable("types.db", "a, b, c, d, e, f, g, h, i, j, k")
		try {
			db.execSQL(
				"INSERT INTO t VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				arrayOf("s", 5L, 6, 7.toShort(), 8.toByte(), true, 1.5, 2.5f, byteArrayOf(1, 2), null, StringBuilder("x"))
			)
			val cursor = assertNotNull(db.rawQuery("SELECT * FROM t", null))
			assertTrue(cursor.moveToFirst())
			assertEquals("s", cursor.getString(0))
			assertEquals(5L, cursor.getLong(1))
			assertEquals(6, cursor.getInt(2))
			assertEquals(7L, cursor.getLong(3))
			assertEquals(8L, cursor.getLong(4))
			assertEquals(1L, cursor.getLong(5))
			assertEquals(1.5, cursor.getDouble(6))
			assertEquals(2.5, cursor.getDouble(7))
			assertContentEquals(byteArrayOf(1, 2), cursor.getBlob(8))
			assertTrue(cursor.isNull(9))
			assertEquals("x", cursor.getString(10))
			assertEquals(setOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"), cursor.getColumnNames().toSet())
			assertEquals(10, cursor.getColumnIndex("k"))
			assertFalse(cursor.moveToNext())
			cursor.close()
		} finally {
			db.close()
		}
	}

	@Test
	fun compiledStatementClearsBindingsAfterExecute() {
		val db = openTable("statement.db", "x INTEGER")
		try {
			val insert = db.compileStatement("INSERT INTO t VALUES (?)")
			insert.bindLong(1, 42)
			insert.execute()
			insert.execute()
			insert.close()

			val cursor = assertNotNull(db.rawQuery("SELECT x FROM t ORDER BY rowid", null))
			assertTrue(cursor.moveToFirst())
			assertEquals(42L, cursor.getLong(0))
			assertTrue(cursor.moveToNext())
			assertTrue(cursor.isNull(0))
			cursor.close()
		} finally {
			db.close()
		}
	}

	@Test
	fun simpleQueriesReadTheFirstColumn() {
		val db = openTable("simple.db", "x INTEGER, name TEXT")
		try {
			db.execSQL("INSERT INTO t VALUES (?, ?)", arrayOf(3L, "three"))
			val count = db.compileStatement("SELECT count(*) FROM t")
			assertEquals(1L, count.simpleQueryForLong())
			assertEquals(1L, count.simpleQueryForLong())
			count.close()

			val name = db.compileStatement("SELECT name FROM t WHERE x = ?")
			name.bindLong(1, 3)
			assertEquals("three", name.simpleQueryForString())
			name.close()

			val none = db.compileStatement("SELECT x FROM t WHERE 0")
			assertEquals(0L, none.simpleQueryForLong())
			assertEquals("", none.simpleQueryForString())
			none.close()
		} finally {
			db.close()
		}
	}

	@Test
	fun versionSurvivesReopen() {
		val path = dir / "version.db"
		val db = assertNotNull(api.openByAbsolutePath(path.toString(), false))
		db.setVersion(9)
		assertEquals(9, db.getVersion())
		db.close()
		val reopened = assertNotNull(api.openByAbsolutePath(path.toString(), false))
		assertEquals(9, reopened.getVersion())
		reopened.close()
	}

	@Test
	fun readWriteOpenOfNonDatabaseFileReturnsNull() {
		val path = dir / "garbage.db"
		fs.write(path) { writeUtf8("not a database ".repeat(100)) }
		assertNull(api.openByAbsolutePath(path.toString(), false))
	}

	@Test
	fun readWriteOpenOfUnwritableFileReturnsNull() {
		val path = dir / "locked.db"
		openTable("locked.db", "x INTEGER").close()
		chmod(path.toString(), 0x124u) // 0444
		try {
			assertNull(api.openByAbsolutePath(path.toString(), false))
		} finally {
			chmod(path.toString(), 0x1a4u) // 0644
		}
	}

	@Test
	fun getOrCreateDatabaseKeepsFilesInApplicationSupportDatabases() {
		val name = "sqlite-api-test-${Random.nextLong().toULong()}"
		val appSupport = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)[0] as String
		val path = "$appSupport/databases/$name".toPath()
		try {
			val db = assertNotNull(api.getOrCreateDatabase(name, false))
			db.execSQL("CREATE TABLE t (x INTEGER)")
			db.close()
			assertTrue(fs.exists(path))
			assertEquals(2, writeVersion(path))
		} finally {
			fs.delete(path, mustExist = false)
			fs.delete("$path-wal".toPath(), mustExist = false)
			fs.delete("$path-shm".toPath(), mustExist = false)
		}
	}

	private fun openTable(name: String, columns: String): SQLiteAPI.SQLiteConnection {
		val db = assertNotNull(api.openByAbsolutePath((dir / name).toString(), false))
		db.execSQL("CREATE TABLE t ($columns)")
		return db
	}

	private fun longs(db: SQLiteAPI.SQLiteConnection, sql: String): List<Long> {
		val cursor = assertNotNull(db.rawQuery(sql, null))
		val result = mutableListOf<Long>()
		if (cursor.moveToFirst()) {
			do {
				result.add(cursor.getLong(0))
			} while (cursor.moveToNext())
		}
		cursor.close()
		return result
	}

	// Byte 18 of the SQLite header: 1 for a rollback journal, 2 for WAL.
	private fun writeVersion(path: Path): Int = fs.read(path) {
		skip(18)
		readByte().toInt()
	}
}
