package net.osmand.shared.util

import android.util.Log

actual object LoggerFactory {
	actual fun getLogger(tag: String): Logger = AndroidLogger(tag)
}

class AndroidLogger(private val tag: String) : Logger {
	override fun debug(message: String) {
		Log.d(tag, message)
	}

	override fun info(message: String) {
		Log.i(tag, message)
	}

	override fun warn(message: String) {
		Log.w(tag, message)
	}

	override fun error(message: String?, throwable: Throwable?) {
		Log.e(tag, message, throwable)
	}
}