package net.osmand.shared.util

import net.osmand.shared.KException
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl
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

	actual fun getOsmAndContext(): OsmAndContext {
		TODO("Not yet implemented")
	}
}