package net.osmand.shared.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

actual object DateFormatter {

	actual fun formatYear(timestamp: Long): String {
		return format(timestamp, "yyyy")
	}

	actual fun formatMonthAndYear(timestamp: Long): String {
		return format(timestamp, "MMMM yyyy")
	}

	private fun format(timestamp: Long, pattern: String): String {
		if (timestamp == 0L) return ""

		val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
		return Instant.ofEpochMilli(timestamp)
			.atZone(ZoneId.systemDefault())
			.format(formatter)
	}
}