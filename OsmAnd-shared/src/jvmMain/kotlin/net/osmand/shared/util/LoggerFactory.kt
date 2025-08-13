package net.osmand.shared.util

import org.apache.commons.logging.LogFactory

actual object LoggerFactory {
	actual fun getLogger(tag: String): Logger = JvmLogger(tag)
}

class JvmLogger(private val tag: String) : Logger {
	private val log = LogFactory.getLog(tag)

	override fun debug(message: String) {
		log.debug(message)
	}

	override fun info(message: String) {
		log.info(message)
	}

	override fun warn(message: String) {
		log.warn(message)
	}

	override fun error(message: String?, throwable: Throwable?) {
		log.error(message, throwable)
	}
}