package net.osmand.shared.util

import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual object PlatformDateNames {

	private fun symbols(languageTag: String?): DateFormatSymbols =
		if (languageTag == null) DateFormatSymbols.getInstance()
		else DateFormatSymbols.getInstance(Locale.forLanguageTag(languageTag))

	private fun locale(languageTag: String?): Locale =
		if (languageTag == null) Locale.getDefault() else Locale.forLanguageTag(languageTag)

	actual fun shortMonths(languageTag: String?): Array<String> =
		symbols(languageTag).shortMonths.map { it ?: "" }.toTypedArray()

	actual fun shortWeekdays(languageTag: String?): Array<String> =
		symbols(languageTag).shortWeekdays.map { it ?: "" }.toTypedArray()

	actual fun formatTwelveHourTime(minutesOfDay: Int, languageTag: String?, withAmPm: Boolean): String {
		val locale = locale(languageTag)
		val format: DateFormat = if (withAmPm) {
			DateFormat.getTimeInstance(DateFormat.SHORT, locale)
		} else {
			SimpleDateFormat("h:mm", locale)
		}
		// the value is minutes of a day, not an instant, so read it back in UTC
		format.timeZone = TimeZone.getTimeZone("UTC")
		return format.format(Date(minutesOfDay * 60L * 1000L))
	}
}
