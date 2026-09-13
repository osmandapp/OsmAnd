package net.osmand.shared.util

import kotlin.jvm.JvmStatic
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * The wall clock reading that [OpeningHoursParser] evaluates rules against.
 *
 * Replaces `java.util.Calendar` from the Java original, exposing the same fields with the same
 * numbering so the rule logic ports across unchanged: [month] counts from zero and [dayOfWeek]
 * counts Sunday as 1, both as `Calendar` did.
 *
 * Mutable and cheap to [copy], because the parser walks forward a day at a time while looking for
 * the next opening.
 */
class OpeningHoursTime(var dateTime: LocalDateTime) {

	/** Four digit year. */
	val year: Int get() = dateTime.year

	/** Zero based, January is 0, like `Calendar.MONTH`. */
	val month: Int get() = dateTime.monthNumber - 1

	/** One based day of the month. */
	val dayOfMonth: Int get() = dateTime.dayOfMonth

	/** Sunday is 1 through Saturday is 7, like `Calendar.DAY_OF_WEEK`. */
	val dayOfWeek: Int get() = dateTime.dayOfWeek.isoDayNumber % 7 + 1

	/** Hour on a 24 hour clock. */
	val hourOfDay: Int get() = dateTime.hour

	val minute: Int get() = dateTime.minute

	/** Number of days in the current month, like `Calendar.getActualMaximum(DAY_OF_MONTH)`. */
	val daysInMonth: Int
		get() {
			val firstOfMonth = LocalDate(dateTime.year, dateTime.monthNumber, 1)
			return firstOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
		}

	/** Moves this reading by [days], keeping the time of day. */
	fun addDays(days: Int) {
		val shifted = dateTime.date.plus(days, DateTimeUnit.DAY)
		dateTime = LocalDateTime(
			shifted.year, shifted.monthNumber, shifted.dayOfMonth,
			dateTime.hour, dateTime.minute, dateTime.second, dateTime.nanosecond
		)
	}

	fun copy(): OpeningHoursTime = OpeningHoursTime(dateTime)

	fun isBefore(other: OpeningHoursTime): Boolean = dateTime < other.dateTime

	override fun toString(): String = dateTime.toString()

	companion object {

		/** The current reading in the device time zone. */
		@JvmStatic
		fun now(): OpeningHoursTime =
			OpeningHoursTime(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))

		/** [epochMillis] read in the device time zone, the equivalent of `Calendar.setTimeInMillis`. */
		@JvmStatic
		fun ofEpochMillis(epochMillis: Long): OpeningHoursTime = OpeningHoursTime(
			Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
		)

		/** Explicit wall clock reading, mostly useful in tests. */
		@JvmStatic
		fun of(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int): OpeningHoursTime =
			OpeningHoursTime(LocalDateTime(year, month + 1, dayOfMonth, hour, minute))
	}
}
