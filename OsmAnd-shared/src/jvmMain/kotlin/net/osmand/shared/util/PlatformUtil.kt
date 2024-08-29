package net.osmand.shared.util

import net.osmand.shared.KException
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl
import net.osmand.shared.io.KFile
import java.io.File

actual object PlatformUtil {

	private var appDir: File? = null
	private var gpxDir: File? = null

	private var sqliteApi: SQLiteAPI? = null

	fun initialize(appDir: File, gpxDir: File) {
		this.appDir = appDir
		this.gpxDir = gpxDir

		sqliteApi = SQLiteAPIImpl()
		Localization.initialize()
	}

	actual fun currentTimeMillis(): Long {
		return System.currentTimeMillis()
	}

	actual fun getAppDir(): KFile {
		val dir = appDir
		if (dir == null) {
			throw IllegalStateException("App dir not initialized")
		} else {
			return KFile(dir.absolutePath)
		}
	}

	actual fun getGpxDir(): KFile {
		val dir = gpxDir
		if (dir == null) {
			throw IllegalStateException("Gpx dir not initialized")
		} else {
			return KFile(dir.absolutePath)
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

	fun getKotlinException(e: java.lang.Exception): KException {
		return KException(e.message, e)
	}

	fun getJavaException(e: KException): java.lang.Exception {
		return Exception(e.message, e)
	}
}