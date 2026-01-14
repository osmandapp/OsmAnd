package net.osmand.shared.util

expect object UrlEncoder {
	fun encode(s: String): String
	fun decode(s: String): String
}