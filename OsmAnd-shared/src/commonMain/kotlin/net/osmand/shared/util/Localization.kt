package net.osmand.shared.util

expect object Localization {
	fun getStringId(key: String): Int
	fun getString(key: String): String
	fun getString(key: String, vararg args: Any): String
}