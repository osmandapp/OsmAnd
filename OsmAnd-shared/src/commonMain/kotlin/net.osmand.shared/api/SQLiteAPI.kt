package net.osmand.shared.api

interface SQLiteAPI {
	interface SQLiteConnection {
		fun close()
		fun rawQuery(sql: String, selectionArgs: List<String>): SQLiteCursor?
		fun execSQL(query: String)
		fun execSQL(query: String, objects: List<Any>)
		fun compileStatement(query: String): SQLiteStatement
		var version: Int
		val isReadOnly: Boolean
		val isClosed: Boolean
	}

	interface SQLiteCursor {
		val columnNames: List<String>
//		fun moveToFirst(): Boolean
		fun moveToNext(): Boolean

		/**
		 * Takes parameter value (zero based)
		 */
		fun getString(index: Int): String?
		fun getDouble(ind: Int): Double?
		fun getLong(ind: Int): Long?
		fun getInt(ind: Int): Int?
		fun getBlob(ind: Int): ByteArray?
		fun isNull(ind: Int): Boolean
		fun getColumnIndex(columnName: String): Int
		fun close()
	}

	interface SQLiteStatement {
		fun bindString(index: Int, value: String)
		fun bindNull(index: Int)
		fun execute()
		fun close()
//		fun simpleQueryForLong(): Long
//		fun simpleQueryForString(): String?
		fun bindLong(index: Int, value: Long)
		fun bindBlob(index: Int, value: ByteArray)
	}

	fun getOrCreateDatabase(name: String, readOnly: Boolean): SQLiteConnection

	fun openByAbsolutePath(path: String, readOnly: Boolean): SQLiteConnection
}
