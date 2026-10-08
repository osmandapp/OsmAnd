package net.osmand.shared.api

import android.content.Context
import android.content.Context.MODE_ENABLE_WRITE_AHEAD_LOGGING
import android.content.Context.MODE_PRIVATE
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CREATE_IF_NECESSARY
import android.database.sqlite.SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.api.SQLiteAPI.SQLiteCursor
import net.osmand.shared.api.SQLiteAPI.SQLiteStatement
import net.osmand.shared.util.LoggerFactory
import java.io.File

class SQLiteAPIImpl(private val context: Context) : SQLiteAPI {

	companion object {
		private val log = LoggerFactory.getLogger("SQLiteAPIImpl")
	}

	override fun getOrCreateDatabase(name: String, readOnly: Boolean): SQLiteConnection? {
		val db = try {
			val file = context.getDatabasePath(name)
			if (readOnly && canOpenReadOnly(file)) {
				SQLiteDatabase.openDatabase(file.path, null, OPEN_READONLY)
			} else {
				// openOrCreateDatabase() always opens read-write. Keep write-ahead logging on for every
				// such open: a connection opened without it switches the file to a rollback journal,
				// the next one switches it back, each time under an exclusive lock that starves the
				// other connections until the busy timeout (OsmAnd-Issues #3331)
				context.openOrCreateDatabase(name, MODE_PRIVATE or MODE_ENABLE_WRITE_AHEAD_LOGGING, null)
			}
		} catch (e: RuntimeException) {
			log.error("Failed to get or create database", e)
			null
		}
		return db?.let { SQLiteDatabaseWrapper(it) }
	}

	/**
	 * A read-only connection can neither create the database nor roll back a hot journal left by
	 * an interrupted rollback-journal transaction; both need a writable open first.
	 */
	private fun canOpenReadOnly(file: File): Boolean =
		file.exists() && File(file.path + "-journal").length() == 0L

	inner class SQLiteDatabaseWrapper(private val ds: SQLiteDatabase) : SQLiteConnection {

		override fun getVersion(): Int = ds.version

		override fun close() = ds.close()

		override fun rawQuery(sql: String, selectionArgs: Array<String>?): SQLiteCursor? {
			val cursor = ds.rawQuery(sql, selectionArgs)
			return cursor.let { cursor ->
				object : SQLiteCursor {
					override fun moveToNext(): Boolean = cursor.moveToNext()
					override fun getColumnNames(): Array<String> = cursor.columnNames
					override fun moveToFirst(): Boolean = cursor.moveToFirst()
					override fun getString(ind: Int): String = cursor.getString(ind)
					override fun close() = cursor.close()
					override fun isNull(ind: Int): Boolean = cursor.isNull(ind)
					override fun getColumnIndex(columnName: String): Int = cursor.getColumnIndex(columnName)
					override fun getDouble(ind: Int): Double = cursor.getDouble(ind)
					override fun getLong(ind: Int): Long = cursor.getLong(ind)
					override fun getInt(ind: Int): Int = cursor.getInt(ind)
					override fun getBlob(ind: Int): ByteArray = cursor.getBlob(ind)
				}
			}
		}

		override fun execSQL(query: String) = ds.execSQL(query)

		override fun execSQL(query: String, objects: Array<Any?>) = ds.execSQL(query, objects)

		override fun compileStatement(query: String): SQLiteStatement {
			val st = ds.compileStatement(query)
			return st.let { statement ->
				object : SQLiteStatement {
					override fun execute() = statement.execute()
					override fun close() = statement.close()
					override fun bindString(i: Int, value: String) = statement.bindString(i, value)
					override fun bindNull(i: Int) = statement.bindNull(i)
					override fun simpleQueryForLong(): Long = statement.simpleQueryForLong()
					override fun simpleQueryForString(): String = statement.simpleQueryForString()
					override fun bindLong(i: Int, value: Long) = statement.bindLong(i, value)
					override fun bindBlob(i: Int, value: ByteArray) = statement.bindBlob(i, value)
					override fun bindDouble(i: Int, value: Double) = statement.bindDouble(i, value)
				}
			}
		}

		override fun setVersion(newVersion: Int) {
			ds.version = newVersion
		}

		override fun isReadOnly(): Boolean = ds.isReadOnly

		override fun isClosed(): Boolean = !ds.isOpen

		override fun beginTransaction() {
			ds.beginTransaction()
		}

		override fun setTransactionSuccessful() {
			ds.setTransactionSuccessful()
		}

		override fun endTransaction() {
			ds.endTransaction()
		}
	}

	override fun openByAbsolutePath(path: String, readOnly: Boolean): SQLiteConnection? {
		return try {
			val flags = if (readOnly) OPEN_READONLY else (OPEN_READWRITE or ENABLE_WRITE_AHEAD_LOGGING or CREATE_IF_NECESSARY)
			return SQLiteDatabaseWrapper(SQLiteDatabase.openDatabase(path, null, flags))
		} catch (e: Exception) {
			log.error("Failed to open database by path: $path readOnly=$readOnly", e)
			null
		}
	}
}