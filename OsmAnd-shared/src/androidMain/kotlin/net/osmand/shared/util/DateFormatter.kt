package net.osmand.shared.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual object DateFormatter {

	actual fun formatYear(timestamp: Long): String {
		return format(timestamp, "yyyy")
	}

	actual fun formatMonthAndYear(timestamp: Long): String {
		return format(timestamp, "MMMM yyyy")
	}

	private fun format(timestamp: Long, pattern: String): String {
		if (timestamp == 0L) return ""
		val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
		dateFormat.timeZone = TimeZone.getDefault()
		return dateFormat.format(Date(timestamp))
	}
}