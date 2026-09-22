package net.osmand.shared.api

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.sqliter.NO_VERSION_CHECK
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.getColumnIndexOrThrow
import co.touchlab.sqliter.getVersion
import co.touchlab.sqliter.longForQuery
import co.touchlab.sqliter.setVersion
import co.touchlab.sqliter.stringForQuery
import co.touchlab.sqliter.withStatement
import co.touchlab.sqliter.interop.SQLiteException
import co.touchlab.sqliter.interop.SQLiteExceptionErrorCode
import co.touchlab.sqliter.interop.SqliteErrorType
import net.osmand.shared.api.SQLiteAPI.*
import net.osmand.shared.util.LoggerFactory
import okio.Path.Companion.toPath

class SQLiteAPIImpl : SQLiteAPI {

	companion object {
		private val log = LoggerFactory.getLogger("SQLiteAPIImpl")
	}

	override fun getOrCreateDatabase(name: String, readOnly: Boolean): SQLiteConnection? {
		return try {
			open(name, null)
		} catch (e: SQLiteException) {
			if (isDamaged(e)) {
				recreateDatabase(name, e)
			} else {
				log.error("Failed to get or create database $name", e)
				null
			}
		}
	}

	override fun openByAbsolutePath(path: String, readOnly: Boolean): SQLiteConnection? {
		val p = path.toPath()
		return try {
			open(p.name, p.parent.toString())
		} catch (e: SQLiteException) {
			log.error("Failed to open database by path: $path readOnly=$readOnly", e)
			null
		}
	}

	private fun open(name: String, basePath: String?): SQLiteConnection {
		val configuration = DatabaseConfiguration(
			name = name,
			version = NO_VERSION_CHECK,
			create = { _ -> },
			upgrade = { _, _, _ -> },
			extendedConfig = DatabaseConfiguration.Extended(basePath = basePath)
		)
		val ds = createDatabaseManager(configuration).createMultiThreadedConnection()
		return SQLiteDatabaseWrapper(ds)
	}

	// The file never opens again, so null here would fail every later read and write too
	private fun recreateDatabase(name: String, cause: SQLiteException): SQLiteConnection? {
		log.error("Database $name is damaged, recreating it", cause)
		DatabaseFileContext.deleteDatabase(name)
		return try {
			open(name, null)
		} catch (e: SQLiteException) {
			log.error("Failed to recreate database $name", e)
			null
		}
	}

	private fun isDamaged(e: SQLiteException): Boolean {
		// errorType throws when sqlite reports a code it does not know
		val errorType = (e as? SQLiteExceptionErrorCode)?.let { runCatching { it.errorType }.getOrNull() }
		return errorType == SqliteErrorType.SQLITE_NOTADB || errorType == SqliteErrorType.SQLITE_CORRUPT
	}

	class SQLiteDatabaseWrapper(private val ds: DatabaseConnection) : SQLiteConnection {

		private var transactionSuccessful = false

		override fun close() {
			ds.close()
		}

		override fun beginTransaction() {
			transactionSuccessful = false
			ds.rawExecSql("BEGIN IMMEDIATE TRANSACTION;")
		}

		override fun setTransactionSuccessful() {
			transactionSuccessful = true
		}

		override fun endTransaction() {
			try {
				if (transactionSuccessful) {
					ds.rawExecSql("COMMIT;")
				} else {
					ds.rawExecSql("ROLLBACK;")
				}
			} finally {
				transactionSuccessful = false
			}
		}

		override fun rawQuery(sql: String, selectionArgs: Array<String>?): SQLiteCursor {
			val statement = ds.createStatement(sql)
			selectionArgs?.forEachIndexed { index, s ->
				statement.bindString(index + 1, s)
			}
			return SQLiteCursorImpl(statement.query(), statement)
		}

		override fun execSQL(query: String) {
			ds.rawExecSql(query)
		}

		override fun execSQL(query: String, objects: Array<Any?>) {
			ds.withStatement(query) {
				objects.forEachIndexed { index, obj ->
					val bindIndex = index + 1
					when (obj) {
						is String -> bindString(bindIndex, obj)
						is Long -> bindLong(bindIndex, obj)
						is Int -> bindLong(bindIndex, obj.toLong())
						is Short -> bindLong(bindIndex, obj.toLong())
						is Byte -> bindLong(bindIndex, obj.toLong())
						is Boolean -> bindLong(bindIndex, if (obj) 1L else 0L)
						is Double -> bindDouble(bindIndex, obj)
						is Float -> bindDouble(bindIndex, obj.toDouble())
						is ByteArray -> bindBlob(bindIndex, obj)
						null -> bindNull(bindIndex)
						else -> bindString(bindIndex, obj.toString())
					}
				}
				
				try {
					execute()
				} catch (e: SQLiteException) {
					println("SQLiteException during execSQL:")
					println("Query: $query")
					println("Params: ${objects.joinToString { it?.toString() ?: "null" }}")
					println("Error: ${e.message}")
					throw e
				}
			}
		}

		override fun compileStatement(query: String): SQLiteStatement {
			return SQLiteStatementImpl(ds.createStatement(query))
		}

		override fun setVersion(newVersion: Int) {
			ds.setVersion(newVersion)
		}

		override fun getVersion(): Int {
			return ds.getVersion()
		}

		override fun isReadOnly(): Boolean = false

		override fun isClosed(): Boolean = ds.closed
	}

	class SQLiteCursorImpl(
			private val cursor: Cursor,
			private val statement: Statement?
	) : SQLiteCursor {

		override fun getColumnNames(): Array<String> {
			return cursor.columnNames.keys.toTypedArray()
		}

		override fun moveToFirst(): Boolean = cursor.next()

		override fun moveToNext(): Boolean = cursor.next()

		override fun getString(ind: Int): String = cursor.getString(ind)

		override fun getDouble(ind: Int): Double = cursor.getDouble(ind)

		override fun getLong(ind: Int): Long = cursor.getLong(ind)

		override fun getInt(ind: Int): Int = cursor.getLong(ind).toInt()

		override fun getBlob(ind: Int): ByteArray = cursor.getBytes(ind)

		override fun isNull(ind: Int): Boolean = cursor.isNull(ind)

		override fun getColumnIndex(columnName: String): Int = cursor.getColumnIndexOrThrow(columnName)

		override fun close() {
			statement?.finalizeStatement()
		}
	}

	class SQLiteStatementImpl(private val statement: Statement) : SQLiteStatement {
		override fun bindString(i: Int, value: String) = statement.bindString(i, value)
		override fun bindLong(i: Int, value: Long) = statement.bindLong(i, value)
		override fun bindDouble(i: Int, value: Double) = statement.bindDouble(i, value)
		override fun bindBlob(i: Int, value: ByteArray) = statement.bindBlob(i, value)
		override fun bindNull(i: Int) = statement.bindNull(i)

		override fun execute() = statement.execute()

		override fun simpleQueryForLong(): Long = statement.longForQuery()
		override fun simpleQueryForString(): String = statement.stringForQuery()

		override fun close() {
			statement.finalizeStatement()
		}
	}
}
