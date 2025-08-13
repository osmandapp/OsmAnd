package net.osmand.shared.util

expect object LoggerFactory {
	fun getLogger(tag: String): Logger
}