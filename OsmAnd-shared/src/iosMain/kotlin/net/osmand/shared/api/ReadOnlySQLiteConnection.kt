package net.osmand.shared.api

import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.cinterop.wcstr
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.api.SQLiteAPI.SQLiteCursor
import net.osmand.shared.api.SQLiteAPI.SQLiteStatement
import net.osmand.shared.util.LoggerFactory
import sqlite3.SQLITE_DONE
import sqlite3.SQLITE_NULL
import sqlite3.SQLITE_OK
import sqlite3.SQLITE_OPEN_FULLMUTEX
import sqlite3.SQLITE_OPEN_READONLY
import sqlite3.SQLITE_ROW
import sqlite3.SQLITE_TRANSIENT
import sqlite3.sqlite3_bind_text
import sqlite3.sqlite3_busy_timeout
import sqlite3.sqlite3_close_v2
import sqlite3.sqlite3_column_blob
import sqlite3.sqlite3_column_bytes
import sqlite3.sqlite3_column_count
import sqlite3.sqlite3_column_double
import sqlite3.sqlite3_column_int64
import sqlite3.sqlite3_column_name
import sqlite3.sqlite3_column_text
import sqlite3.sqlite3_column_type
import sqlite3.sqlite3_errmsg
import sqlite3.sqlite3_finalize
import sqlite3.sqlite3_open_v2
import sqlite3.sqlite3_prepare16_v2
import sqlite3.sqlite3_step

@OptIn(ExperimentalForeignApi::class)
class ReadOnlySQLiteConnection private constructor(private val db: CPointer<sqlite3>) : SQLiteConnection {

	private var closed = false

	override fun rawQuery(sql: String, selectionArgs: Array<String>?): SQLiteCursor {
		val stmt = prepare(sql)
		try {
			selectionArgs?.forEachIndexed { index, arg ->
				ensureOk(sqlite3_bind_text(stmt, index + 1, arg, -1, SQLITE_TRANSIENT), "bind")
			}
		} catch (e: Exception) {
			sqlite3_finalize(stmt)
			throw e
		}
		return ReadOnlyCursor(stmt)
	}

	override fun getVersion(): Int {
		val cursor = rawQuery("PRAGMA user_version", null)
		try {
			return if (cursor.moveToFirst()) cursor.getInt(0) else 0
		} finally {
			cursor.close()
		}
	}

	override fun isReadOnly(): Boolean = true

	override fun isClosed(): Boolean = closed

	override fun close() {
		if (!closed) {
			closed = true
			sqlite3_close_v2(db)
		}
	}

	override fun execSQL(query: String): Unit = readOnly()

	override fun execSQL(query: String, objects: Array<Any?>): Unit = readOnly()

	override fun compileStatement(query: String): SQLiteStatement = readOnly()

	override fun setVersion(newVersion: Int): Unit = readOnly()

	override fun beginTransaction(): Unit = readOnly()

	override fun setTransactionSuccessful(): Unit = readOnly()

	override fun endTransaction(): Unit = readOnly()

	private fun readOnly(): Nothing = throw UnsupportedOperationException("Database is opened read-only")

	private fun prepare(sql: String): CPointer<sqlite3_stmt> = memScoped {
		val stmtPtr = alloc<CPointerVar<sqlite3_stmt>>()
		val sql16 = sql.wcstr
		ensureOk(sqlite3_prepare16_v2(db, sql16.ptr, sql16.size, stmtPtr.ptr, null), "prepare: $sql")
		stmtPtr.value!!
	}

	private fun ensureOk(rc: Int, op: String) {
		if (rc != SQLITE_OK) {
			throw IllegalStateException("SQLite $op failed ($rc): ${sqlite3_errmsg(db)?.toKString()}")
		}
	}

	private inner class ReadOnlyCursor(private val stmt: CPointer<sqlite3_stmt>) : SQLiteCursor {

		private val columnNames = Array(sqlite3_column_count(stmt)) { i ->
			sqlite3_column_name(stmt, i)?.toKString() ?: ""
		}
		private var finalized = false

		override fun getColumnNames(): Array<String> = columnNames

		override fun moveToFirst(): Boolean = step()

		override fun moveToNext(): Boolean = step()

		override fun getString(ind: Int): String =
			sqlite3_column_text(stmt, ind)?.reinterpret<ByteVar>()?.toKString() ?: ""

		override fun getDouble(ind: Int): Double = sqlite3_column_double(stmt, ind)

		override fun getLong(ind: Int): Long = sqlite3_column_int64(stmt, ind)

		override fun getInt(ind: Int): Int = getLong(ind).toInt()

		override fun getBlob(ind: Int): ByteArray {
			val blob = sqlite3_column_blob(stmt, ind)
			val size = sqlite3_column_bytes(stmt, ind)
			return if (blob == null || size <= 0) ByteArray(0) else blob.readBytes(size)
		}

		override fun isNull(ind: Int): Boolean = sqlite3_column_type(stmt, ind) == SQLITE_NULL

		override fun getColumnIndex(columnName: String): Int {
			val index = columnNames.indexOf(columnName)
			require(index >= 0) { "Col for $columnName not found" }
			return index
		}

		override fun close() {
			if (!finalized) {
				finalized = true
				sqlite3_finalize(stmt)
			}
		}

		private fun step(): Boolean = when (val rc = sqlite3_step(stmt)) {
			SQLITE_ROW -> true
			SQLITE_DONE -> false
			else -> throw IllegalStateException("SQLite step failed ($rc): ${sqlite3_errmsg(db)?.toKString()}")
		}
	}

	companion object {

		private val LOG = LoggerFactory.getLogger("ReadOnlySQLiteConnection")
		private const val BUSY_TIMEOUT_MS = 5000

		fun open(path: String): ReadOnlySQLiteConnection? {
			val db = memScoped {
				val dbPtr = alloc<CPointerVar<sqlite3>>()
				val rc = sqlite3_open_v2(path, dbPtr.ptr, SQLITE_OPEN_READONLY or SQLITE_OPEN_FULLMUTEX, null)
				val db = dbPtr.value
				if (rc != SQLITE_OK || db == null) {
					LOG.error("Failed to open database read-only: $path, ${db?.let { sqlite3_errmsg(it)?.toKString() }}")
					sqlite3_close_v2(db)
					return null
				}
				db
			}
			sqlite3_busy_timeout(db, BUSY_TIMEOUT_MS)
			val connection = ReadOnlySQLiteConnection(db)
			return try {
				// the open is lazy: reading the header is what rejects a file that is not a database
				connection.getVersion()
				connection
			} catch (e: Exception) {
				LOG.error("Failed to open database read-only: $path", e)
				connection.close()
				null
			}
		}
	}
}
