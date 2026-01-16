package net.osmand.shared.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

actual object SharedDateFormatter {

	actual fun formatYear(timestamp: Long): String {
		return format(timestamp, "yyyy")
	}

	actual fun formatMonthAndYear(timestamp: Long): String {
		return format(timestamp, "MMMM yyyy")
	}

	private fun format(timestamp: Long, pattern: String): String {
		if (timestamp == 0L) return ""

		val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)

		val formatter = NSDateFormatter()
		formatter.dateFormat = pattern
		formatter.locale = NSLocale.currentLocale

		return formatter.stringFromDate(date)
	}
}