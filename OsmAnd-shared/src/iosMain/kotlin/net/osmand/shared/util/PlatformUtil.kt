package net.osmand.shared.util

import net.osmand.shared.db.SQLiteAPI
import net.osmand.shared.db.SQLiteAPIImpl
import net.osmand.shared.io.KFile
import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.timeIntervalSince1970

actual object PlatformUtil {

	private var appDir: String? = null
	private var gpxDir: String? = null

	private var sqliteApi: SQLiteAPI? = null

	fun initialize(appDir: NSString, gpxDir: NSString) {
		this.appDir = appDir.toString()
		this.gpxDir = gpxDir.toString()

		sqliteApi = SQLiteAPIImpl()
	}

	actual fun currentTimeMillis(): Long {
		return (NSDate().timeIntervalSince1970 * 1000).toLong()
	}

	actual fun getAppDir(): KFile {
		val dir = appDir
		if (dir == null) {
			throw IllegalStateException("App dir not initialized")
		} else {
			return KFile(dir)
		}
	}

	actual fun getGpxDir(): KFile {
		val dir = gpxDir
		if (dir == null) {
			throw IllegalStateException("Gpx dir not initialized")
		} else {
			return KFile(dir)
		}
	}

	actual fun getSQLiteAPI(): SQLiteAPI {
		val sqliteApi = sqliteApi
		if (sqliteApi == null) {
			throw IllegalStateException("SQLiteAPI not initialized")
		} else {
			return sqliteApi
		}
	}
}