package net.osmand.shared.util

import android.content.Context
import net.osmand.shared.db.SQLiteAPI
import net.osmand.shared.db.SQLiteAPIImpl
import net.osmand.shared.io.CommonFile
import java.io.File
import java.lang.ref.WeakReference

actual object PlatformUtil {

	private var context: WeakReference<Context>? = null
	private var appDir: File? = null
	private var gpxDir: File? = null

	private var sqliteApi: SQLiteAPI? = null

	fun initialize(context: Context, appDir: File, gpxDir: File) {
		this.context = WeakReference(context)
		this.appDir = appDir
		this.gpxDir = gpxDir

		sqliteApi = SQLiteAPIImpl(context)
	}

	actual fun currentTimeMillis(): Long {
		return System.currentTimeMillis()
	}

	actual fun getAppDir(): CommonFile {
		val dir = appDir
		if (dir == null) {
			throw IllegalStateException("App dir not initialized")
		} else {
			return CommonFile(dir.absolutePath)
		}
	}

	actual fun getGpxDir(): CommonFile {
		val dir = gpxDir
		if (dir == null) {
			throw IllegalStateException("Gpx dir not initialized")
		} else {
			return CommonFile(dir.absolutePath)
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

	fun getCommonFile(file: File): CommonFile {
		return CommonFile(file.absolutePath)
	}
}