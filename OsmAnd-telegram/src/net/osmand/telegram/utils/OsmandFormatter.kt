package net.osmand.telegram.utils

import android.content.Context
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import java.text.DateFormatSymbols
import java.text.DecimalFormat
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.*

object OsmandFormatter {

	val METERS_IN_KILOMETER = 1000f
	val METERS_IN_ONE_MILE = 1609.344f // 1609.344
	val METERS_IN_ONE_NAUTICALMILE = 1852f // 1852

	val YARDS_IN_ONE_METER = 1.0936f
	val FEET_IN_ONE_METER = YARDS_IN_ONE_METER * 3f

	val FORMAT_METERS_KEY = "m"
	val FORMAT_FEET_KEY = "ft"
	val FORMAT_YARDS_KEY = "yd"
	val FORMAT_KILOMETERS_KEY = "km"
	val FORMAT_NAUTICALMILES_KEY = "nmi"
	val FORMAT_MILES_KEY = "mi"

	private val fixed2 = DecimalFormat("0.00")
	private val fixed1 = DecimalFormat("0.0")

	private const val SHORT_TIME_FORMAT = "%02d:%02d"
	private const val SIMPLE_TIME_OF_DAY_FORMAT = "HH:mm"
	private const val SIMPLE_DATE_FORMAT = "dd MMM HH:mm:ss"
	private const val SHORT_DATE_FORMAT = "dd MMM yyyy"

	private const val MIN_DURATION_FOR_DATE_FORMAT = 48 * 60 * 60

	private val dateFormatSymbols = DateFormatSymbols.getInstance()
	private val localDaysStr = getLettersStringArray(dateFormatSymbols.shortWeekdays, 2)

	init {
		fixed2.minimumFractionDigits = 2
		fixed1.minimumFractionDigits = 1
		fixed1.minimumIntegerDigits = 1
		fixed2.minimumIntegerDigits = 1
	}

	fun getFormattedDurationForWidget(seconds: Long): String {
		val hours = seconds / (60 * 60)
		val minutes = seconds / 60 % 60
		return when {
			hours > 9 -> String.format("%10d:%01d", hours, minutes)
			hours > 0 -> String.format("%1d:%01d", hours, minutes)
			minutes > 9 -> String.format("%11d", minutes)
			minutes > 0 -> String.format("%1d", minutes)
			else -> "1"
		}.trim()
	}

	fun getFormattedDuration(ctx: Context, seconds: Long, short: Boolean = false): String {
		val hours = seconds / (60 * 60)
		val minutes = seconds / 60 % 60
		val secs = seconds - minutes * 60
		if (short) {
			return String.format(SHORT_TIME_FORMAT, hours, minutes)
		}
		return when {
			hours > 0 -> {
				var res = "$hours ${ctx.getString(R.string.shared_string_hour_short)}"
				if (minutes > 0) {
					res += " $minutes ${ctx.getString(R.string.shared_string_minute_short)}"
				}
				res
			}
			minutes > 0 -> {
				var res = "$minutes ${ctx.getString(R.string.shared_string_minute_short)}"
				if (secs > 0) {
					res += " $secs ${ctx.getString(R.string.shared_string_second_short)}"
				}
				res
			}
			else -> "$seconds ${ctx.getString(R.string.shared_string_second_short)}"
		}
	}

	fun getFormattedTime(milliseconds: Long, useCurrentTime: Boolean = true, short: Boolean = false): String {
		val calendar = Calendar.getInstance()
		if (useCurrentTime) {
			calendar.timeInMillis = System.currentTimeMillis() + milliseconds
		} else {
			calendar.timeInMillis = milliseconds
		}
		return if (isSameDay(calendar, Calendar.getInstance()) || short) {
			SimpleDateFormat(SIMPLE_TIME_OF_DAY_FORMAT, Locale.getDefault()).format(calendar.time)
		} else {
			SimpleDateFormat(SIMPLE_TIME_OF_DAY_FORMAT, Locale.getDefault()).format(calendar.time) +
					" " + localDaysStr[calendar.get(Calendar.DAY_OF_WEEK)]
		}
	}

	fun getFormattedDate(seconds: Long, shortFormat: Boolean = false): String {
		return if (shortFormat) {
			SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.getDefault()).format(seconds * 1000L)
		} else {
			SimpleDateFormat(SHORT_DATE_FORMAT, Locale.getDefault()).format(seconds * 1000L)
		}
	}

	fun getListItemLiveTimeDescr(ctx: TelegramApplication, lastUpdated: Int, prefix: String = ""): String {
		return if (lastUpdated > 0) {
			val duration = System.currentTimeMillis() / 1000 - lastUpdated
			when {
				duration > MIN_DURATION_FOR_DATE_FORMAT -> prefix + getFormattedDate(lastUpdated.toLong())
				duration > 0 -> prefix + getFormattedDuration(ctx, duration) + " " + ctx.getString(R.string.time_ago)
				else -> ""
			}
		} else {
			""
		}
	}

	fun calculateRoundedDist(distInMeters: Double, ctx: TelegramApplication): Double {
		val mc = ctx.settings.metricsConstants
		var mainUnitInMeter = 1.0
		var metersInSecondUnit = METERS_IN_KILOMETER.toDouble()
		if (mc == MetricsConstants.MILES_AND_FEET) {
			mainUnitInMeter = FEET_IN_ONE_METER.toDouble()
			metersInSecondUnit = METERS_IN_ONE_MILE.toDouble()
		} else if (mc == MetricsConstants.MILES_AND_METERS) {
			mainUnitInMeter = 1.0
			metersInSecondUnit = METERS_IN_ONE_MILE.toDouble()
		} else if (mc == MetricsConstants.NAUTICAL_MILES) {
			mainUnitInMeter = 1.0
			metersInSecondUnit = METERS_IN_ONE_NAUTICALMILE.toDouble()
		} else if (mc == MetricsConstants.MILES_AND_YARDS) {
			mainUnitInMeter = YARDS_IN_ONE_METER.toDouble()
			metersInSecondUnit = METERS_IN_ONE_MILE.toDouble()
		}

		// 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000 ...
		var generator = 1
		var pointer: Byte = 1
		var point = mainUnitInMeter
		var roundDist = 1.0
		while (distInMeters * point > generator) {
			roundDist = generator / point
			if (pointer++ % 3 == 2) {
				generator = generator * 5 / 2
			} else {
				generator *= 2
			}

			if (point == mainUnitInMeter && metersInSecondUnit * mainUnitInMeter * 0.9 <= generator) {
				point = 1 / metersInSecondUnit
				generator = 1
				pointer = 1
			}
		}
		//Miles exceptions: 2000ft->0.5mi, 1000ft->0.25mi, 1000yd->0.5mi, 500yd->0.25mi, 1000m ->0.5mi, 500m -> 0.25mi
		if (mc == MetricsConstants.MILES_AND_METERS && roundDist == 1000.0) {
			roundDist = (0.5f * METERS_IN_ONE_MILE).toDouble()
		} else if (mc == MetricsConstants.MILES_AND_METERS && roundDist == 500.0) {
			roundDist = (0.25f * METERS_IN_ONE_MILE).toDouble()
		} else if (mc == MetricsConstants.MILES_AND_FEET && roundDist == 2000 / FEET_IN_ONE_METER.toDouble()) {
			roundDist = (0.5f * METERS_IN_ONE_MILE).toDouble()
		} else if (mc == MetricsConstants.MILES_AND_FEET && roundDist == 1000 / FEET_IN_ONE_METER.toDouble()) {
			roundDist = (0.25f * METERS_IN_ONE_MILE).toDouble()
		} else if (mc == MetricsConstants.MILES_AND_YARDS && roundDist == 1000 / YARDS_IN_ONE_METER.toDouble()) {
			roundDist = (0.5f * METERS_IN_ONE_MILE).toDouble()
		} else if (mc == MetricsConstants.MILES_AND_YARDS && roundDist == 500 / YARDS_IN_ONE_METER.toDouble()) {
			roundDist = (0.25f * METERS_IN_ONE_MILE).toDouble()
		}
		return roundDist
	}

	fun getFormattedRoundDistanceKm(meters: Float, digits: Int, ctx: TelegramApplication): String {
		val mainUnitStr = R.string.km
		val mainUnitInMeters = METERS_IN_KILOMETER
		return if (digits == 0) {
			(meters / mainUnitInMeters + 0.5).toInt().toString() + " " + ctx.getString(mainUnitStr) //$NON-NLS-1$
		} else if (digits == 1) {
			fixed1.format((meters / mainUnitInMeters).toDouble()) + " " + ctx.getString(mainUnitStr)
		} else {
			fixed2.format((meters / mainUnitInMeters).toDouble()) + " " + ctx.getString(mainUnitStr)
		}
	}

	@JvmOverloads
	fun getFormattedDistance(meters: Float, ctx: TelegramApplication, forceTrailingZeros: Boolean = true, useLocalizedString: Boolean = true): String {
		val format1 = if (forceTrailingZeros) "{0,number,0.0} " else "{0,number,0.#} "
		val format2 = if (forceTrailingZeros) "{0,number,0.00} " else "{0,number,0.##} "

		val mc = ctx.settings.metricsConstants
		val mainUnitStr: String
		val mainUnitInMeters: Float
		when (mc) {
			MetricsConstants.KILOMETERS_AND_METERS -> {
				mainUnitStr = if (useLocalizedString) ctx.getString(R.string.km) else FORMAT_KILOMETERS_KEY
				mainUnitInMeters = METERS_IN_KILOMETER
			}
			MetricsConstants.NAUTICAL_MILES -> {
				mainUnitStr = if (useLocalizedString) ctx.getString(R.string.nm) else FORMAT_NAUTICALMILES_KEY
				mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE
			}
			else -> {
				mainUnitStr = if (useLocalizedString) ctx.getString(R.string.mile) else FORMAT_MILES_KEY
				mainUnitInMeters = METERS_IN_ONE_MILE
			}
		}

		if (meters >= 100 * mainUnitInMeters) {
			return (meters / mainUnitInMeters + 0.5).toInt().toString() + " " + mainUnitStr //$NON-NLS-1$
		} else if (meters > 9.99f * mainUnitInMeters) {
			return MessageFormat.format(format1 + mainUnitStr, meters / mainUnitInMeters).replace('\n', ' ') //$NON-NLS-1$
		} else if (meters > 0.999f * mainUnitInMeters) {
			return MessageFormat.format(format2 + mainUnitStr, meters / mainUnitInMeters).replace('\n', ' ') //$NON-NLS-1$
		} else if (mc == MetricsConstants.MILES_AND_FEET && meters > 0.249f * mainUnitInMeters) {
			return MessageFormat.format(format2 + mainUnitStr, meters / mainUnitInMeters).replace('\n', ' ') //$NON-NLS-1$
		} else if (mc == MetricsConstants.MILES_AND_METERS && meters > 0.249f * mainUnitInMeters) {
			return MessageFormat.format(format2 + mainUnitStr, meters / mainUnitInMeters).replace('\n', ' ') //$NON-NLS-1$
		} else if (mc == MetricsConstants.MILES_AND_YARDS && meters > 0.249f * mainUnitInMeters) {
			return MessageFormat.format(format2 + mainUnitStr, meters / mainUnitInMeters).replace('\n', ' ') //$NON-NLS-1$
		} else if (mc == MetricsConstants.NAUTICAL_MILES && meters > 0.99f * mainUnitInMeters) {
			return MessageFormat.format(format2 + mainUnitStr, meters / mainUnitInMeters).replace('\n', ' ') //$NON-NLS-1$
		} else {
			if (mc == MetricsConstants.KILOMETERS_AND_METERS || mc == MetricsConstants.MILES_AND_METERS) {
				return (meters + 0.5).toInt().toString() + if (useLocalizedString) " " + ctx.getString(R.string.m) else " $FORMAT_METERS_KEY"  //$NON-NLS-1$
			} else if (mc == MetricsConstants.MILES_AND_FEET) {
				val feet = (meters * FEET_IN_ONE_METER + 0.5).toInt()
				return feet.toString() + if (useLocalizedString) " " + ctx.getString(R.string.foot) else " $FORMAT_FEET_KEY" //$NON-NLS-1$
			} else if (mc == MetricsConstants.MILES_AND_YARDS) {
				val yards = (meters * YARDS_IN_ONE_METER + 0.5).toInt()
				return yards.toString() + if (useLocalizedString) " " + ctx.getString(R.string.yard) else " $FORMAT_YARDS_KEY" //$NON-NLS-1$
			}
			return (meters + 0.5).toInt().toString() + if (useLocalizedString) " " + ctx.getString(R.string.m) else " $FORMAT_METERS_KEY" //$NON-NLS-1$
		}
	}

	fun getFormattedAlt(alt: Double, ctx: TelegramApplication, useLocalizedString: Boolean = true): String {
		val mc = ctx.settings.metricsConstants
		val useFeet = mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.MILES_AND_YARDS
		return if (!useFeet) {
			(alt + 0.5).toInt().toString() + if (useLocalizedString) " " + ctx.getString(R.string.m) else " $FORMAT_METERS_KEY"
		} else {
			(alt * FEET_IN_ONE_METER + 0.5).toInt().toString() + if (useLocalizedString) " " + ctx.getString(R.string.foot) else " $FORMAT_FEET_KEY"
		}
	}

	fun getFormattedSpeed(metersPerSeconds: Float, ctx: TelegramApplication, useLocalizedString: Boolean = true): String {
		val mc = ctx.settings.speedConstants
		val kmh = metersPerSeconds * 3.6f
		val convertedSpeed: Float = when (mc) {
			SpeedConstants.KILOMETERS_PER_HOUR -> kmh
			SpeedConstants.MILES_PER_HOUR -> kmh * METERS_IN_KILOMETER / METERS_IN_ONE_MILE
			SpeedConstants.NAUTICALMILES_PER_HOUR -> kmh * METERS_IN_KILOMETER / METERS_IN_ONE_NAUTICALMILE
			SpeedConstants.MINUTES_PER_KILOMETER -> {
				if (metersPerSeconds < 0.111111111) {
					return "-" + if (useLocalizedString) mc.toShortString(ctx) else mc.getDefaultString()
				}
				METERS_IN_KILOMETER / (metersPerSeconds * 60)
			}
			SpeedConstants.MINUTES_PER_MILE -> {
				if (metersPerSeconds < 0.111111111) {
					return "-" + if (useLocalizedString) mc.toShortString(ctx) else mc.getDefaultString()
				}
				METERS_IN_ONE_MILE / (metersPerSeconds * 60)
			}
			else -> metersPerSeconds /*if (mc == SpeedConstants.METERS_PER_SECOND) */
		}

		return if (convertedSpeed >= mc.speedThreshold) {
			// e.g. car case and for high-speeds: Display rounded to 1 km/h (5% precision at 20 km/h)
			"${Math.round(convertedSpeed)} " + if (useLocalizedString) mc.toShortString(ctx) else mc.getDefaultString()
		} else {
			// for smaller values display 1 decimal digit x.y km/h, (0.5% precision at 20 km/h)
			"${Math.round(convertedSpeed * 10f) / 10f} " + if (useLocalizedString) mc.toShortString(ctx) else mc.getDefaultString()
		}
	}

	private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
		return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
				cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
	}

	private fun getLettersStringArray(strings: Array<String>, letters: Int): Array<String?> {
		val newStrings = arrayOfNulls<String>(strings.size)
		for (i in strings.indices) {
			if (strings[i].length > letters) {
				newStrings[i] = (strings[i].substring(0, letters)).capitalize()
			} else {
				newStrings[i] = strings[i].capitalize()
			}
		}
		return newStrings
	}
	
	enum class MetricsConstants private constructor(private val key: Int) {
		KILOMETERS_AND_METERS(R.string.si_km_m),
		MILES_AND_FEET(R.string.si_mi_feet),
		MILES_AND_METERS(R.string.si_mi_meters),
		MILES_AND_YARDS(R.string.si_mi_yard),
		NAUTICAL_MILES(R.string.si_nm);

		fun toHumanString(ctx: Context): String {
			return ctx.getString(key)
		}
	}

	enum class SpeedConstants private constructor(
		private val key: String,
		private val title: Int,
		private val descr: Int,
		val speedThreshold: Int
	) {
		KILOMETERS_PER_HOUR("km/h", R.string.km_h, R.string.si_kmh, 20),
		MILES_PER_HOUR("mph", R.string.mile_per_hour, R.string.si_mph, 20),
		METERS_PER_SECOND("m/s", R.string.m_s, R.string.si_m_s, 10),
		MINUTES_PER_MILE("min/m", R.string.min_mile, R.string.si_min_m, 10),
		MINUTES_PER_KILOMETER("min/km", R.string.min_km, R.string.si_min_km, 10),
		NAUTICALMILES_PER_HOUR("nmi/h", R.string.nm_h, R.string.si_nm_h, 20);

		fun toHumanString(ctx: Context): String {
			return ctx.getString(descr)
		}

		fun toShortString(ctx: Context): String {
			return ctx.getString(title)
		}

		fun getDefaultString() = key
	}
}
