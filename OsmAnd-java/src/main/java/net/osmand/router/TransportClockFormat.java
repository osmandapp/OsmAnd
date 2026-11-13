package net.osmand.router;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formats clock times of {@link TimedTransportRoute} results, 24 or 12 hour as the device is set.
 * Seconds are cut off, as in a printed timetable.
 */
public class TransportClockFormat {

	private final DateTimeFormatter formatter;

	public TransportClockFormat(boolean is24Hour, Locale locale) {
		formatter = DateTimeFormatter.ofPattern(is24Hour ? "HH:mm" : "h:mm a", locale);
	}

	public String format(long epochSec, ZoneId zone) {
		return formatter.format(Instant.ofEpochSecond(epochSec).atZone(zone));
	}

	public String formatDeparture(TimedTransportRoute route) {
		return format(route.getDepartureTimeSec(), route.getTimeZone());
	}

	/** The arrival clock time, with " +1" (or +2, ...) when it falls on a later local day than the departure. */
	public String formatArrival(TimedTransportRoute route) {
		return formatWithDayOffset(route.getArrivalTimeSec(), route.getDepartureTimeSec(), route.getTimeZone());
	}

	/** Clock time of {@code epochSec}, marked +N when its local date is N days after that of {@code referenceSec}. */
	public String formatWithDayOffset(long epochSec, long referenceSec, ZoneId zone) {
		long days = java.time.temporal.ChronoUnit.DAYS.between(Instant.ofEpochSecond(referenceSec).atZone(zone).toLocalDate(),
				Instant.ofEpochSecond(epochSec).atZone(zone).toLocalDate());
		String clock = format(epochSec, zone);
		return days > 0 ? clock + " +" + days : clock;
	}

	/** For example {@code 08:15 - 08:54}. */
	public String formatRange(TimedTransportRoute route) {
		return formatDeparture(route) + " – " + formatArrival(route);
	}
}
