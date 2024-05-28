package net.osmand.shared.util

import net.osmand.shared.io.CommonFile
import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.timeIntervalSince1970

actual object PlatformUtil {

	private var appDir: String? = null
	private var gpxDir: String? = null

	fun initialize(appDir: NSString, gpxDir: NSString) {
		this.appDir = appDir.toString()
		this.gpxDir = gpxDir.toString()
	}

	actual fun currentTimeMillis(): Long {
		return (NSDate().timeIntervalSince1970 * 1000).toLong()
	}

	actual fun getAppDir(): CommonFile {
		val dir = appDir
		if (dir == null) {
			throw IllegalStateException("App dir not initialized")
		} else {
			return CommonFile(dir)
		}
	}

	actual fun getGpxDir(): CommonFile {
		val dir = gpxDir
		if (dir == null) {
			throw IllegalStateException("Gpx dir not initialized")
		} else {
			return CommonFile(dir)
		}
	}
}