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
import kotlinx.cinterop.refTo
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.cinterop.wcstr
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.api.SQLiteAPI.SQLiteCursor
import net.osmand.shared.api.SQLiteAPI.SQLiteStatement
import net.osmand.shared.util.LoggerFactory
import platform.posix.usleep
import sqlite3.SQLITE_BUSY
import sqlite3.SQLITE_DONE
import sqlite3.SQLITE_LOCKED
import sqlite3.SQLITE_NULL
import sqlite3.SQLITE_OK
import sqlite3.SQLITE_OPEN_CREATE
import sqlite3.SQLITE_OPEN_FULLMUTEX
import sqlite3.SQLITE_OPEN_READONLY
import sqlite3.SQLITE_OPEN_READWRITE
import sqlite3.SQLITE_ROW
import sqlite3.SQLITE_TRANSIENT
import sqlite3.sqlite3_bind_blob
import sqlite3.sqlite3_bind_double
import sqlite3.sqlite3_bind_int64
import sqlite3.sqlite3_bind_null
import sqlite3.sqlite3_bind_text
import sqlite3.sqlite3_bind_zeroblob
import sqlite3.sqlite3_busy_timeout
import sqlite3.sqlite3_clear_bindings
import sqlite3.sqlite3_close_v2
import sqlite3.sqlite3_column_blob
import sqlite3.sqlite3_column_bytes
import sqlite3.sqlite3_column_count
import sqlite3.sqlite3_column_double
import sqlite3.sqlite3_column_int64
import sqlite3.sqlite3_column_name
import sqlite3.sqlite3_column_text
import sqlite3.sqlite3_column_type
import sqlite3.sqlite3_db_readonly
import sqlite3.sqlite3_errmsg
import sqlite3.sqlite3_exec
import sqlite3.sqlite3_finalize
import sqlite3.sqlite3_open_v2
import sqlite3.sqlite3_prepare16_v2
import sqlite3.sqlite3_reset
import sqlite3.sqlite3_step

@OptIn(ExperimentalForeignApi::class)
internal class SQLiteConnectionImpl private constructor(
	private val db: CPointer<sqlite3>,
	private val readOnly: Boolean
) : SQLiteConnection {

	private var closed = false
	private var transactionSuccessful = false

	override fun rawQuery(sql: String, selectionArgs: Array<String>?): SQLiteCursor {
		val stmt = prepare(sql)
		try {
			selectionArgs?.forEachIndexed { index, arg -> bind(stmt, index + 1, arg) }
		} catch (e: Exception) {
			sqlite3_finalize(stmt)
			throw e
		}
		return SQLiteCursorImpl(stmt)
	}

	override fun execSQL(query: String) {
		exec(query)
	}

	override fun execSQL(query: String, objects: Array<Any?>) {
		val stmt = prepare(query)
		try {
			objects.forEachIndexed { index, obj -> bind(stmt, index + 1, obj) }
			executeNonQuery(stmt)
		} catch (e: Exception) {
			LOG.error("Failed to execute $query with ${objects.joinToString { it?.toString() ?: "null" }}", e)
			throw e
		} finally {
			sqlite3_finalize(stmt)
		}
	}

	override fun compileStatement(query: String): SQLiteStatement {
		return SQLiteStatementImpl(prepare(query))
	}

	override fun setVersion(newVersion: Int) {
		val stmt = prepare("PRAGMA user_version = $newVersion")
		try {
			executeNonQuery(stmt)
		} finally {
			sqlite3_finalize(stmt)
		}
	}

	override fun getVersion(): Int = longForQuery("PRAGMA user_version").toInt()

	override fun isReadOnly(): Boolean = readOnly

	override fun isClosed(): Boolean = closed

	override fun close() {
		if (!closed) {
			closed = true
			sqlite3_close_v2(db)
		}
	}

	override fun beginTransaction() {
		transactionSuccessful = false
		exec("BEGIN IMMEDIATE TRANSACTION;")
	}

	override fun setTransactionSuccessful() {
		transactionSuccessful = true
	}

	override fun endTransaction() {
		try {
			exec(if (transactionSuccessful) "COMMIT;" else "ROLLBACK;")
		} finally {
			transactionSuccessful = false
		}
	}

	private fun exec(sql: String) {
		ensureOk(sqlite3_exec(db, sql, null, null, null), "exec: $sql")
	}

	private fun longForQuery(sql: String): Long {
		val stmt = prepare(sql)
		try {
			return if (step(stmt)) sqlite3_column_int64(stmt, 0) else 0L
		} finally {
			sqlite3_finalize(stmt)
		}
	}

	private fun stringForQuery(sql: String): String {
		val stmt = prepare(sql)
		try {
			return if (step(stmt)) columnString(stmt, 0) else ""
		} finally {
			sqlite3_finalize(stmt)
		}
	}

	private fun prepare(sql: String): CPointer<sqlite3_stmt> = memScoped {
		val stmtPtr = alloc<CPointerVar<sqlite3_stmt>>()
		val sql16 = sql.wcstr
		ensureOk(sqlite3_prepare16_v2(db, sql16.ptr, sql16.size, stmtPtr.ptr, null), "prepare: $sql")
		stmtPtr.value!!
	}

	private fun bind(stmt: CPointer<sqlite3_stmt>, index: Int, value: Any?) {
		val rc = when (value) {
			null -> sqlite3_bind_null(stmt, index)
			is String -> sqlite3_bind_text(stmt, index, value, -1, SQLITE_TRANSIENT)
			is Long -> sqlite3_bind_int64(stmt, index, value)
			is Int -> sqlite3_bind_int64(stmt, index, value.toLong())
			is Short -> sqlite3_bind_int64(stmt, index, value.toLong())
			is Byte -> sqlite3_bind_int64(stmt, index, value.toLong())
			is Boolean -> sqlite3_bind_int64(stmt, index, if (value) 1L else 0L)
			is Double -> sqlite3_bind_double(stmt, index, value)
			is Float -> sqlite3_bind_double(stmt, index, value.toDouble())
			is ByteArray -> if (value.isEmpty()) {
				sqlite3_bind_zeroblob(stmt, index, 0)
			} else {
				sqlite3_bind_blob(stmt, index, value.refTo(0), value.size, SQLITE_TRANSIENT)
			}
			else -> sqlite3_bind_text(stmt, index, value.toString(), -1, SQLITE_TRANSIENT)
		}
		ensureOk(rc, "bind")
	}

	private fun step(stmt: CPointer<sqlite3_stmt>): Boolean {
		repeat(STEP_RETRIES) {
			when (val rc = sqlite3_step(stmt)) {
				SQLITE_ROW -> return true
				SQLITE_DONE -> return false
				SQLITE_BUSY, SQLITE_LOCKED -> usleep(1000u)
				else -> throw sqliteError(rc, "step")
			}
		}
		throw IllegalStateException("SQLite step retry count exceeded")
	}

	private fun executeNonQuery(stmt: CPointer<sqlite3_stmt>) {
		when (val rc = sqlite3_step(stmt)) {
			SQLITE_DONE -> {}
			SQLITE_ROW -> throw IllegalStateException("A statement that returns rows needs rawQuery")
			else -> throw sqliteError(rc, "step")
		}
	}

	private fun columnString(stmt: CPointer<sqlite3_stmt>, index: Int): String =
		sqlite3_column_text(stmt, index)?.reinterpret<ByteVar>()?.toKString() ?: ""

	private fun ensureOk(rc: Int, op: String) {
		if (rc != SQLITE_OK) {
			throw sqliteError(rc, op)
		}
	}

	private fun sqliteError(rc: Int, op: String) =
		IllegalStateException("SQLite $op failed ($rc): ${sqlite3_errmsg(db)?.toKString()}")

	private inner class SQLiteCursorImpl(private val stmt: CPointer<sqlite3_stmt>) : SQLiteCursor {

		private val columnNames = Array(sqlite3_column_count(stmt)) { i ->
			sqlite3_column_name(stmt, i)?.toKString() ?: ""
		}
		private var finalized = false

		override fun getColumnNames(): Array<String> = columnNames

		override fun moveToFirst(): Boolean = step(stmt)

		override fun moveToNext(): Boolean = step(stmt)

		override fun getString(ind: Int): String = columnString(stmt, ind)

		override fun getDouble(ind: Int): Double = sqlite3_column_double(stmt, ind)

		override fun getLong(ind: Int): Long = sqlite3_column_int64(stmt, ind)

		override fun getInt(ind: Int): Int = getLong(ind).toInt()

		override fun getBlob(ind: Int): ByteArray {
			val blob = sqlite3_column_blob(stmt, ind)
			val size = sqlite3_column_bytes(stmt, ind)
			if (blob == null) {
				check(size == 0 && !isNull(ind)) { "Column $ind is NULL" }
				return ByteArray(0)
			}
			return blob.readBytes(size)
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
	}

	private inner class SQLiteStatementImpl(private val stmt: CPointer<sqlite3_stmt>) : SQLiteStatement {

		override fun bindString(i: Int, value: String) = bind(stmt, i, value)

		override fun bindNull(i: Int) = bind(stmt, i, null)

		override fun bindLong(i: Int, value: Long) = bind(stmt, i, value)

		override fun bindDouble(i: Int, value: Double) = bind(stmt, i, value)

		override fun bindBlob(i: Int, value: ByteArray) = bind(stmt, i, value)

		override fun execute() {
			try {
				executeNonQuery(stmt)
			} finally {
				sqlite3_reset(stmt)
				sqlite3_clear_bindings(stmt)
			}
		}

		override fun simpleQueryForLong(): Long {
			try {
				return if (step(stmt)) sqlite3_column_int64(stmt, 0) else 0L
			} finally {
				sqlite3_reset(stmt)
			}
		}

		override fun simpleQueryForString(): String {
			try {
				return if (step(stmt)) columnString(stmt, 0) else ""
			} finally {
				sqlite3_reset(stmt)
			}
		}

		override fun close() {
			sqlite3_finalize(stmt)
		}
	}

	companion object {

		private val LOG = LoggerFactory.getLogger("SQLiteConnectionImpl")
		private const val BUSY_TIMEOUT_MS = 5000
		private const val STEP_RETRIES = 50

		fun openReadOnly(path: String): SQLiteConnectionImpl =
			open(path, SQLITE_OPEN_READONLY, readOnly = true) {
				// the open is lazy: reading the header is what rejects a file that is not a database
				it.getVersion()
			}

		fun openReadWrite(path: String): SQLiteConnectionImpl =
			open(path, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE, readOnly = false) {
				check(sqlite3_db_readonly(it.db, "main") == 0) { "Could not open the database in read/write mode" }
				if (!it.stringForQuery("PRAGMA journal_mode").equals("wal", ignoreCase = true)) {
					it.stringForQuery("PRAGMA journal_mode=WAL")
				}
			}

		private fun open(
			path: String,
			flags: Int,
			readOnly: Boolean,
			configure: (SQLiteConnectionImpl) -> Unit
		): SQLiteConnectionImpl {
			val db = memScoped {
				val dbPtr = alloc<CPointerVar<sqlite3>>()
				val rc = sqlite3_open_v2(path, dbPtr.ptr, flags or SQLITE_OPEN_FULLMUTEX, null)
				val db = dbPtr.value
				if (rc != SQLITE_OK || db == null) {
					val message = db?.let { sqlite3_errmsg(it)?.toKString() }
					sqlite3_close_v2(db)
					throw IllegalStateException("SQLite open failed ($rc): $message")
				}
				db
			}
			sqlite3_busy_timeout(db, BUSY_TIMEOUT_MS)
			val connection = SQLiteConnectionImpl(db, readOnly)
			try {
				configure(connection)
			} catch (e: Exception) {
				connection.close()
				throw e
			}
			return connection
		}
	}
}
