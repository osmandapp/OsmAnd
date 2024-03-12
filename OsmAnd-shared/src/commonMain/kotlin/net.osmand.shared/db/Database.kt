package net.osmand.shared.db

import com.squareup.sqldelight.db.SqlCursor

class Database @Throws(Exception::class) constructor(databaseDriverFactory: DatabaseDriverFactory) {
	private val driver = databaseDriverFactory.createDriver()

	@Throws(Exception::class)
	fun execSQL(sql: String) {
		try {
			driver.execute(null, sql, 0)
		} catch (e: Exception) {
			throw e
		}
	}

	@Throws(Exception::class)
	fun execSQL(sql: String, parameters: List<Any>) {
		try {
			driver.execute(null, sql, parameters.size) {
				for ((index, param) in parameters.withIndex()) {
					val i = index + 1
					when (param) {
						is Long -> bindLong(i, param)
						is String -> bindString(i, param)
						is Double -> bindDouble(i, param)
						is ByteArray -> bindBytes(i, param)
						else -> throw IllegalArgumentException("Non supported parameter: $param")
					}
				}
			}
		} catch (e: Exception) {
			throw e
		}
	}

	@Throws(Exception::class)
	fun execSQLQuery(sql: String): SqlCursor {
		try {
			return driver.executeQuery(null, sql, 0)
		} catch (e: Exception) {
			throw e
		}
	}

	@Throws(Exception::class)
	fun execSQLQuery(sql: String, parameters: List<Any>): SqlCursor {
		try {
			return driver.executeQuery(null, sql, parameters.size) {
				for ((index, param) in parameters.withIndex()) {
					val i = index + 1
					when (param) {
						is Long -> bindLong(i, param)
						is String -> bindString(i, param)
						is Double -> bindDouble(i, param)
						is ByteArray -> bindBytes(i, param)
						else -> throw IllegalArgumentException("Non supported parameter: $param")
					}
				}
			}
		} catch (e: Exception) {
			throw e
		}
	}

	@Throws(Exception::class)
	fun close() {
		try {
			driver.close()
		} catch (e: Exception) {
			throw e
		}
	}
}