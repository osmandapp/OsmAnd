package net.osmand.shared.api

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import net.osmand.shared.api.SQLiteAPI.*
import net.osmand.shared.util.LoggerFactory

class SQLiteAPIImpl(private val context: Context) : SQLiteAPI {

	companion object {
		private val log = LoggerFactory.getLogger("SQLiteAPIImpl")
	}

	override fun getOrCreateDatabase(name: String, readOnly: Boolean): SQLiteConnection? {
		val db = try {
			context.openOrCreateDatabase(name, Context.MODE_PRIVATE or (if (readOnly) 0 else Context.MODE_ENABLE_WRITE_AHEAD_LOGGING), null)
		} catch (e: RuntimeException) {
			log.error("Failed to get or create database", e)
			null
		}
		return db?.let { SQLiteDatabaseWrapper(it) }
	}

	inner class SQLiteDatabaseWrapper(private val ds: SQLiteDatabase) : SQLiteConnection {

		override fun getVersion(): Int = ds.version

		override fun close() = ds.close()

		override fun rawQuery(sql: String, selectionArgs: Array<String>?): SQLiteCursor? {
			val c = ds.rawQuery(sql, selectionArgs)
			return c?.let { cursor ->
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
				}
			}
		}

		override fun setVersion(newVersion: Int) {
			ds.version = newVersion
		}

		override fun isReadOnly(): Boolean = ds.isReadOnly

		override fun isClosed(): Boolean = !ds.isOpen
	}

	override fun openByAbsolutePath(path: String, readOnly: Boolean): SQLiteConnection? {
		val db = SQLiteDatabase.openDatabase(path, null, if (readOnly) SQLiteDatabase.OPEN_READONLY else (SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING))
		return db?.let { SQLiteDatabaseWrapper(it) }
	}
}