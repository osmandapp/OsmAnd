package net.osmand.shared.util

expect object Localization {
	fun getString(key: String): String
	fun getString(key: String, vararg args: Any): String
}