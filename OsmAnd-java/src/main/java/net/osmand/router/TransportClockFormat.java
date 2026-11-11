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

	public String formatArrival(TimedTransportRoute route) {
		return format(route.getArrivalTimeSec(), route.getTimeZone());
	}

	/** For example {@code 08:15 - 08:54}. */
	public String formatRange(TimedTransportRoute route) {
		return formatDeparture(route) + " – " + formatArrival(route);
	}
}
