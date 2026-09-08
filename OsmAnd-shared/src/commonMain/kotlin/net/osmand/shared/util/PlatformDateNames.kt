package net.osmand.shared.util

/**
 * Locale dependent date names and time formatting, for the parts of [OpeningHoursParser] that render
 * text for a person to read.
 *
 * Only the localized output goes through here. The canonical OSM spelling of days and months is
 * fixed vocabulary, not locale data, so it lives in [OpeningHoursParser] as constants.
 */
expect object PlatformDateNames {

	/**
	 * Short month names for [languageTag] (null means the device language), index 0 = January.
	 */
	fun shortMonths(languageTag: String?): Array<String>

	/**
	 * Short weekday names for [languageTag] (null means the device language), laid out like
	 * `java.util.Calendar`: index 1 = Sunday through index 7 = Saturday, index 0 unused. The rest of
	 * the parser indexes weekdays that way, so the layout is part of the contract.
	 */
	fun shortWeekdays(languageTag: String?): Array<String>

	/**
	 * [minutesOfDay] rendered on a 12 hour clock, with the am/pm marker only when [withAmPm].
	 */
	fun formatTwelveHourTime(minutesOfDay: Int, languageTag: String?, withAmPm: Boolean): String
}
