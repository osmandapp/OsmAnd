package net.osmand.shared.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.timeZoneWithAbbreviation

actual object PlatformDateNames {

	private fun locale(languageTag: String?): NSLocale =
		if (languageTag == null) NSLocale.currentLocale
		// NSLocale identifiers use an underscore, BCP 47 tags a hyphen
		else NSLocale.localeWithLocaleIdentifier(languageTag.replace('-', '_'))

	private fun formatter(languageTag: String?): NSDateFormatter {
		val formatter = NSDateFormatter()
		formatter.locale = locale(languageTag)
		return formatter
	}

	actual fun shortMonths(languageTag: String?): Array<String> {
		val symbols = formatter(languageTag).shortMonthSymbols
		return Array(12) { index -> symbols.getOrNull(index)?.toString() ?: "" }
	}

	actual fun shortWeekdays(languageTag: String?): Array<String> {
		// NSDateFormatter starts at Sunday with index 0, java.util.Calendar leaves index 0 unused
		// and starts at Sunday with index 1, and the parser indexes the Calendar way
		val symbols = formatter(languageTag).shortWeekdaySymbols
		return Array(8) { index ->
			if (index == 0) "" else symbols.getOrNull(index - 1)?.toString() ?: ""
		}
	}

	actual fun formatTwelveHourTime(minutesOfDay: Int, languageTag: String?, withAmPm: Boolean): String {
		val formatter = formatter(languageTag)
		if (withAmPm) {
			formatter.timeStyle = NSDateFormatterShortStyle
		} else {
			formatter.dateFormat = "h:mm"
		}
		// the value is minutes of a day, not an instant, so read it back in UTC
		NSTimeZone.timeZoneWithAbbreviation("UTC")?.let { formatter.timeZone = it }
		return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(minutesOfDay * 60.0))
	}
}
