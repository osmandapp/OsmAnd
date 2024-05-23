package net.osmand.shared.db

import android.annotation.SuppressLint
import android.content.Context
import android.util.SparseArray
import com.squareup.sqldelight.db.SqlCursor
import net.osmand.shared.api.SQLiteAPI


class KtSQLiteAPIImpl(private val app: Context) : SQLiteAPI {
	@SuppressLint("InlinedApi")
	override fun getOrCreateDatabase(
		name: String,
		readOnly: Boolean): SQLiteAPI.SQLiteConnection {

//		val dbFileName = File(cacheDirPath, name).absolutePath
//		val callback = object: Callback(1) {
//			override fun onCreate(db: SupportSQLiteDatabase) {
//			}
//
//			override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
//			}
//		}

		val dbFactory = DatabaseDriverFactory(app, name, 1)
		val db = Database(dbFactory)
//		println("CREATE - START")
//		try {
//			db.execSQL("CREATE TABLE IF NOT EXISTS test(id, name, value)")
//		} catch (e: Exception) {
//			println(e.message)
//		}

//		var db: SQLiteDatabase? = null
//		try {
//			db = app.openOrCreateDatabase(
//				name, Context.MODE_PRIVATE
//						or if (readOnly) 0 else Context.MODE_ENABLE_WRITE_AHEAD_LOGGING, null)
//		} catch (e: RuntimeException) {
//			LOG.error(e.message, e)
//		}
//		return if (db == null) {
//			null
//		} else SQLiteDatabaseWrapper(db)
		return SQLiteDatabaseWrapper(db)
	}

	inner class SQLiteDatabaseWrapper(db: Database) : SQLiteAPI.SQLiteConnection {
		var db: Database

		init {
			this.db = db
		}

		override var version: Int
			get() {
				val cursor = db.execSQLQuery("PRAGMA user_version;")
				return if (cursor.next()) {
					cursor.getLong(0)!!.toInt()
				} else {
					0
				}
			}
			set(newVersion) {
				db.execSQLQuery("PRAGMA user_version = $newVersion")
			}
		override val isReadOnly: Boolean
			get() = false
		override val isClosed: Boolean
			get() = db.isClosed()

		override fun close() {
			db.close()
		}

		override fun rawQuery(sql: String, selectionArgs: List<String>): SQLiteAPI.SQLiteCursor {
			val lowCaseSql = sql.lowercase()
			val endOfSelect = lowCaseSql.indexOf("select") + "select".length
			val startOfFrom = lowCaseSql.indexOf("from")
			val columnNamesStr = sql.substring(endOfSelect, startOfFrom)
			val columnNamesSplit = columnNamesStr.trim().split(",")
			val columnNames = ArrayList<String>()
			for (name in columnNamesSplit) {
				columnNames.add(name.trim())
			}
			val c: SqlCursor = db.execSQLQuery(sql, selectionArgs)
			return object : SQLiteAPI.SQLiteCursor {
				override fun moveToNext(): Boolean {
					return c.next()
				}

				override val columnNames: List<String>
					get() = columnNames

				override fun getString(index: Int): String? {
					return c.getString(index)
				}

				override fun close() {
					c.close()
				}

				override fun isNull(ind: Int): Boolean {
					val stringValue = c.getString(ind)
					return stringValue == null
				}

				override fun getColumnIndex(columnName: String): Int {
					return columnNames.indexOf(columnName)
				}

				override fun getDouble(ind: Int): Double? {
					return c.getDouble(ind)
				}

				override fun getLong(ind: Int): Long? {
					return c.getLong(ind)
				}

				override fun getInt(ind: Int): Int? {
					return c.getLong(ind)?.toInt()
				}

				override fun getBlob(ind: Int): ByteArray? {
					return c.getBytes(ind)
				}
			}
		}

		override fun execSQL(query: String) {
			db.execSQL(query)
		}

		override fun execSQL(query: String, objects: List<Any>) {
			db.execSQL(query, objects)
		}

		override fun compileStatement(query: String): SQLiteAPI.SQLiteStatement {
			val statement = object : SQLiteAPI.SQLiteStatement {
				var db: Database? = null
				val params = SparseArray<Any>()
				lateinit var queryString: String
				override fun execute() {
					db?.execSQL(queryString, createParams(params))
				}

				override fun close() {
					db?.close()
				}

				override fun bindString(index: Int, value: String) {
					params.put(index, value)
				}

				override fun bindNull(index: Int) {
					params.put(index, "NULL")
				}

				override fun bindLong(index: Int, value: Long) {
					params.put(index, value)
				}

				override fun bindBlob(index: Int, value: ByteArray) {
					params.put(index, value)
				}
			}
			statement.db = db
			statement.queryString = query
			return statement
		}

		private fun createParams(source: SparseArray<Any>): List<String> {
			val params = ArrayList<String>(source.size())
			for (i in 0 until source.size()) {
				val value = source.valueAt(i)
				params.add(value?.toString() ?: "NULL")
			}
			return params
		}

//		override val isReadOnly: Boolean
//			get() = db.isReadOnly()
//		override val isClosed: Boolean
//			get() = !db.isOpen()
	}

	override fun openByAbsolutePath(path: String, readOnly: Boolean): SQLiteAPI.SQLiteConnection {
		// fix http://stackoverflow.com/questions/26937152/workaround-for-nexus-9-sqlite-file-write-operations-on-external-dirs
		val dbFactory = DatabaseDriverFactory(app, path, 1)
		val db = Database(dbFactory)
//
//		val db: SQLiteDatabase = SQLiteDatabase.openDatabase(
//			path, null,
//			if (readOnly) SQLiteDatabase.OPEN_READONLY else SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING)
//			?: return null
		return SQLiteDatabaseWrapper(db)
	}
}
