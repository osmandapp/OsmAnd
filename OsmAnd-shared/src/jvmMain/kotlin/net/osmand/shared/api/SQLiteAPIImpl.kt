package net.osmand.shared.api

import net.osmand.shared.api.SQLiteAPI.*
import net.osmand.shared.util.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager

class SQLiteAPIImpl : SQLiteAPI {

	companion object {
		private val log = LoggerFactory.getLogger("SQLiteAPIImpl")
	}

	override fun getOrCreateDatabase(name: String, readOnly: Boolean): SQLiteConnection? {
		val readOnlyParam = if (readOnly) "?mode=ro" else ""
		val db = try {
			DriverManager.getConnection("jdbc:sqlite:$name$readOnlyParam")
		} catch (e: RuntimeException) {
			log.error("Failed to get or create database", e)
			null
		}
		return db?.let { SQLiteDatabaseWrapper(it) }
	}

	inner class SQLiteDatabaseWrapper(private val ds: Connection) : SQLiteConnection {

		override fun getVersion(): Int {
			val stmt = ds.createStatement()
			stmt.use {
				val resultSet = it.executeQuery("PRAGMA user_version")
				if (resultSet.next()) {
					return resultSet.getInt(1)
				}
			}
			return 0
		}

		override fun close() = ds.close()

		override fun rawQuery(sql: String, selectionArgs: Array<String>?): SQLiteCursor? {
			ds.prepareStatement(sql, selectionArgs).use { statement ->
				statement.executeQuery().use { cursor ->
					return object : SQLiteCursor {
						var colNames: Array<String>? = null

						override fun moveToNext(): Boolean = cursor.next()
						override fun getColumnNames(): Array<String> {
							if (colNames != null) {
								return colNames!!
							}
							val metaData = cursor.metaData
							val columnCount = metaData.columnCount
							val res = Array(columnCount) { "" }
							for (i in 1..columnCount) {
								res[i - 1] = metaData.getColumnName(i)
							}
							colNames = res
							return res
						}

						override fun moveToFirst(): Boolean = cursor.first()
						override fun getString(ind: Int): String = cursor.getString(ind)
						override fun close() = cursor.close()
						override fun isNull(ind: Int): Boolean {
							cursor.getObject(ind)
							return cursor.wasNull()
						}

						override fun getColumnIndex(columnName: String): Int {
							val names = getColumnNames()
							return names.indexOf(columnName)
						}

						override fun getDouble(ind: Int): Double = cursor.getDouble(ind)
						override fun getLong(ind: Int): Long = cursor.getLong(ind)
						override fun getInt(ind: Int): Int = cursor.getInt(ind)
						override fun getBlob(ind: Int): ByteArray {
							val blob = cursor.getBlob(ind)
							val res = blob.getBytes(1, blob.length().toInt())
							blob.free()
							return res;
						}
					}
				}
			}
		}

		override fun execSQL(query: String) {
			ds.createStatement().use { stmt -> stmt.executeQuery(query) }
		}

		override fun execSQL(query: String, objects: Array<Any?>) {
			val preparedStatement = ds.prepareStatement(query)
			preparedStatement.use { stmt ->
				objects.forEachIndexed { index, obj ->
					stmt.setObject(index + 1, obj)
				}
				stmt.executeUpdate()
			}
		}

		override fun compileStatement(query: String): SQLiteStatement {
			ds.prepareStatement(query).use { stmt ->
				return object : SQLiteStatement {
					override fun execute() {
						stmt.execute()
					}

					override fun close() = stmt.close()
					override fun bindString(i: Int, value: String) = stmt.setString(i, value)
					override fun bindNull(i: Int) = stmt.setNull(i, java.sql.Types.NULL)
					override fun simpleQueryForLong(): Long {
						stmt.executeQuery().use { resultSet ->
							return if (resultSet.next()) {
								resultSet.getLong(1)
							} else {
								throw java.sql.SQLException("Query did not return any result")
							}
						}
					}

					override fun simpleQueryForString(): String {
						stmt.executeQuery().use { resultSet ->
							return if (resultSet.next()) {
								resultSet.getString(1)
							} else {
								throw java.sql.SQLException("Query did not return any result")
							}
						}
					}

					override fun bindLong(i: Int, value: Long) = stmt.setLong(i, value)
					override fun bindBlob(i: Int, value: ByteArray) = stmt.setBytes(i, value)
				}
			}
		}

		override fun setVersion(newVersion: Int) {
			val stmt = ds.createStatement()
			stmt.use {
				it.execute("PRAGMA user_version = $newVersion")
			}
		}

		override fun isReadOnly(): Boolean = ds.isReadOnly

		override fun isClosed(): Boolean = ds.isClosed
	}

	override fun openByAbsolutePath(path: String, readOnly: Boolean): SQLiteConnection? {
		return getOrCreateDatabase(path, readOnly)
	}
}