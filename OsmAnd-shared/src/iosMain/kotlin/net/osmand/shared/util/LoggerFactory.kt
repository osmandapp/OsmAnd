package net.osmand.shared.util

import platform.Foundation.NSLog

actual object LoggerFactory {
	actual fun getLogger(tag: String): Logger = IosLogger(tag)
}

class IosLogger(private val tag: String) : Logger {
	override fun debug(message: String) {
		NSLog("DEBUG: [$tag] $message")
	}

	override fun info(message: String) {
		NSLog("INFO: [$tag] $message")
	}

	override fun warn(message: String) {
		NSLog("WARN: [$tag] $message")
	}

	override fun error(message: String?, throwable: Throwable?) {
		NSLog("ERROR: [$tag] $message")
		throwable?.let {
			NSLog("ERROR: [$tag] ${it.message}")
		}
	}
}