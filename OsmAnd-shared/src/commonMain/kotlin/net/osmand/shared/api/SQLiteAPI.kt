package net.osmand.shared.api

interface SQLiteAPI {

	interface SQLiteConnection {
		fun close()
		fun rawQuery(sql: String, selectionArgs: Array<String>?): SQLiteCursor?
		fun execSQL(query: String)
		fun execSQL(query: String, objects: Array<Any?>)
		fun compileStatement(query: String): SQLiteStatement
		fun setVersion(newVersion: Int)
		fun getVersion(): Int
		fun isReadOnly(): Boolean
		fun isClosed(): Boolean
	}

	interface SQLiteCursor {
		fun getColumnNames(): Array<String>
		fun moveToFirst(): Boolean
		fun moveToNext(): Boolean
		fun getString(ind: Int): String
		fun getDouble(ind: Int): Double
		fun getLong(ind: Int): Long
		fun getInt(ind: Int): Int
		fun getBlob(ind: Int): ByteArray
		fun isNull(ind: Int): Boolean
		fun getColumnIndex(columnName: String): Int
		fun close()
	}

	interface SQLiteStatement {
		fun bindString(i: Int, value: String)
		fun bindNull(i: Int)
		fun execute()
		fun close()
		fun simpleQueryForLong(): Long
		fun simpleQueryForString(): String
		fun bindLong(i: Int, value: Long)
		fun bindBlob(i: Int, value: ByteArray)
	}

	fun getOrCreateDatabase(name: String, readOnly: Boolean): SQLiteConnection?
	fun openByAbsolutePath(path: String, readOnly: Boolean): SQLiteConnection?
}
