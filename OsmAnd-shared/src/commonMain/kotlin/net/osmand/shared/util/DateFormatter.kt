package net.osmand.shared.util

expect object DateFormatter {
	fun formatYear(timestamp: Long): String
	fun formatMonthAndYear(timestamp: Long): String
}