package net.osmand.shared.util

import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl
import net.osmand.shared.api.XmlFactoryAPI
import net.osmand.shared.api.XmlPullParserAPI
import net.osmand.shared.api.XmlSerializerAPI
import net.osmand.shared.io.KFile
import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.timeIntervalSince1970

actual object PlatformUtil {

	private var appDir: String? = null
	private var gpxDir: String? = null

	private var sqliteApi: SQLiteAPI? = null
	private var xmlFactoryApi: XmlFactoryAPI? = null

	fun initialize(appDir: NSString, gpxDir: NSString, xmlFactoryApi: XmlFactoryAPI) {
		this.appDir = appDir.toString()
		this.gpxDir = gpxDir.toString()

		this.sqliteApi = SQLiteAPIImpl()
		this.xmlFactoryApi = xmlFactoryApi
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

	fun getXmlPullParserApi(): XmlPullParserAPI {
		val xmlFactoryApi = xmlFactoryApi
		if (xmlFactoryApi == null) {
			throw IllegalStateException("XmlPullParserAPI not initialized")
		} else {
			return xmlFactoryApi.createXmlPullParserApi()
		}
	}

	fun getXmlSerializerApi(): XmlSerializerAPI {
		val xmlFactoryApi = xmlFactoryApi
		if (xmlFactoryApi == null) {
			throw IllegalStateException("XmlSerializerAPI not initialized")
		} else {
			return xmlFactoryApi.createXmlSerializerApi()
		}
	}
}