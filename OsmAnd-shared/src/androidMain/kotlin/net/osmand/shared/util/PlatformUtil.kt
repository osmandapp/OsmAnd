package net.osmand.shared.util

import android.content.Context
import net.osmand.shared.io.CommonFile
import java.io.File
import java.lang.ref.WeakReference

actual object PlatformUtil {

	private var context: WeakReference<Context>? = null
	private var appDir: File? = null
	private var gpxDir: File? = null

	fun initialize(context: Context, appDir: File, gpxDir: File) {
		this.context = WeakReference(context)
		this.appDir = appDir
		this.gpxDir = gpxDir
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
}