package net.osmand.shared.api

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.NO_VERSION_CHECK
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.getColumnIndexOrThrow
import co.touchlab.sqliter.getVersion
import co.touchlab.sqliter.longForQuery
import co.touchlab.sqliter.setVersion
import co.touchlab.sqliter.stringForQuery
import co.touchlab.sqliter.withStatement
import net.osmand.shared.api.SQLiteAPI.*
import okio.Path.Companion.toPath

class SQLiteAPIImpl : SQLiteAPI {

	private lateinit var databaseManager: DatabaseManager

	override fun getOrCreateDatabase(name: String, readOnly: Boolean): SQLiteConnection {
		val configuration = DatabaseConfiguration(name = name, version = NO_VERSION_CHECK, create = { db ->
			// No-op: example creation logic
		}, upgrade = { db, oldVersion, newVersion ->
			// No-op: example upgrade logic
		})
		databaseManager = createDatabaseManager(configuration)
		val ds = databaseManager.createMultiThreadedConnection()
		return SQLiteDatabaseWrapper(ds)
	}

	override fun openByAbsolutePath(path: String, readOnly: Boolean): SQLiteConnection {
		val p = path.toPath()
		val configuration = DatabaseConfiguration(name = p.name, version = 1, create = { db ->
			// No-op: example creation logic
		}, upgrade = { db, oldVersion, newVersion ->
			// No-op: example upgrade logic
		}, extendedConfig = DatabaseConfiguration.Extended(basePath = p.parent.toString()))
		databaseManager = createDatabaseManager(configuration)
		val ds = databaseManager.createMultiThreadedConnection()
		return SQLiteDatabaseWrapper(ds)
	}

	class SQLiteDatabaseWrapper(private val ds: DatabaseConnection) : SQLiteConnection {
		override fun close() {
			ds.close()
		}

		override fun rawQuery(sql: String, selectionArgs: Array<String>?): SQLiteCursor {
			val statement = ds.createStatement(sql)
			selectionArgs?.forEachIndexed { index, s -> statement.bindString(index + 1, s) }
			return SQLiteCursorImpl(statement.query(), statement)
		}

		override fun execSQL(query: String) {
			ds.rawExecSql(query)
		}

		override fun execSQL(query: String, objects: Array<Any?>) {
			ds.withStatement(query) {
				objects.forEachIndexed { index, obj ->
					when (obj) {
						is String -> bindString(index + 1, obj)
						is Long -> bindLong(index + 1, obj)
						is Double -> bindDouble(index + 1, obj)
						is ByteArray -> bindBlob(index + 1, obj)
						null -> bindNull(index + 1)
					}
				}
				execute()
			}
		}

		override fun compileStatement(query: String): SQLiteStatement {
			val statement = ds.createStatement(query)
			return SQLiteStatementImpl(statement)
		}

		override fun setVersion(newVersion: Int) {
			ds.setVersion(newVersion)
		}

		override fun getVersion(): Int {
			return ds.getVersion()
		}

		override fun isReadOnly(): Boolean {
			return false
		}

		override fun isClosed(): Boolean {
			return ds.closed
		}
	}

	class SQLiteCursorImpl(
		private val cursor: Cursor,
		private val statement: Statement?
	) : SQLiteCursor {
		override fun getColumnNames(): Array<String> {
			return cursor.columnNames.keys.toTypedArray()
		}

		override fun moveToFirst(): Boolean {
			return cursor.next()
		}

		override fun moveToNext(): Boolean {
			return cursor.next()
		}

		override fun getString(ind: Int): String {
			return cursor.getString(ind)
		}

		override fun getDouble(ind: Int): Double {
			return cursor.getDouble(ind)
		}

		override fun getLong(ind: Int): Long {
			return cursor.getLong(ind)
		}

		override fun getInt(ind: Int): Int {
			return cursor.getLong(ind).toInt()
		}

		override fun getBlob(ind: Int): ByteArray {
			return cursor.getBytes(ind)
		}

		override fun isNull(ind: Int): Boolean {
			return cursor.isNull(ind)
		}

		override fun getColumnIndex(columnName: String): Int {
			return cursor.getColumnIndexOrThrow(columnName)
		}

		override fun close() {
			statement?.finalizeStatement()
		}
	}

	class SQLiteStatementImpl(private val statement: Statement) :
		SQLiteStatement {
		override fun bindString(i: Int, value: String) {
			statement.bindString(i, value)
		}

		override fun bindNull(i: Int) {
			statement.bindNull(i)
		}

		override fun execute() {
			statement.execute()
		}

		override fun close() {
			statement.finalizeStatement()
		}

		override fun simpleQueryForLong(): Long {
			return statement.longForQuery()
		}

		override fun simpleQueryForString(): String {
			return statement.stringForQuery()
		}

		override fun bindLong(i: Int, value: Long) {
			statement.bindLong(i, value)
		}

		override fun bindBlob(i: Int, value: ByteArray) {
			statement.bindBlob(i, value)
		}
	}
}
