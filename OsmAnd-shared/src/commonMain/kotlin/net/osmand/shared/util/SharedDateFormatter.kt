package net.osmand.shared.util

expect object SharedDateFormatter {
	fun formatYear(timestamp: Long): String
	fun formatMonthAndYear(timestamp: Long): String
}