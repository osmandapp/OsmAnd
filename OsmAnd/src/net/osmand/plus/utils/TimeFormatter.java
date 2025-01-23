package net.osmand.plus.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeFormatter {

	private final DateFormat simpleTimeFormat;
	private final DateFormat amPmTimeFormat;

	public TimeFormatter(@NonNull Locale locale, @NonNull String pattern,
			@NonNull String amPmPattern) {
		this(locale, pattern, amPmPattern, null);
	}

	public TimeFormatter(@NonNull Locale locale, @NonNull String pattern,
			@NonNull String amPmPattern, @Nullable TimeZone timeZone) {
		simpleTimeFormat = new SimpleDateFormat(pattern, locale);
		amPmTimeFormat = new SimpleDateFormat(amPmPattern, locale);

		if (timeZone != null) {
			simpleTimeFormat.getCalendar().setTimeZone(timeZone);
			amPmTimeFormat.getCalendar().setTimeZone(timeZone);
		}
	}

	public String format(@NonNull Date date, boolean twelveHoursFormat) {
		DateFormat timeFormat = twelveHoursFormat ? amPmTimeFormat : simpleTimeFormat;
		return timeFormat.format(date);
	}
}
