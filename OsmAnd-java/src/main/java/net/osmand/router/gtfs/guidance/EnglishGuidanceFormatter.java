package net.osmand.router.gtfs.guidance;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Default English text for {@link GuidanceEvent}s. Other languages replace this class and map the same
 * text keys and arguments. Times are shown as 24-hour HH:mm in the given zone (the feed's time zone).
 */
public final class EnglishGuidanceFormatter {

	private final DateTimeFormatter clock;

	public EnglishGuidanceFormatter(ZoneId zone) {
		this.clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(zone);
	}

	/** The sentence for an event, or null if the event is silent. */
	public String format(GuidanceEvent e) {
		String key = e.getTextKey();
		if (key == null) {
			return null;
		}
		List<Object> a = e.getArgs();
		switch (key) {
			case "guidance.walk":
				return a.get(0) == null ? "Walk to your destination" : "Walk to " + a.get(0);
			case "guidance.board":
				return "Board the " + a.get(0) + (isBlank(a.get(1)) ? "" : " toward " + a.get(1)) + " at " + a.get(2)
						+ ", departs " + time(a.get(3));
			case "guidance.transfer":
				return "Transfer to the " + a.get(0) + (isBlank(a.get(1)) ? "" : " toward " + a.get(1)) + " at "
						+ a.get(2) + ", departs " + time(a.get(3));
			case "guidance.rideStarted":
				return "Get off at " + a.get(0) + ", " + stops(a.get(1));
			case "guidance.nextStop":
				return "Next stop: " + a.get(0) + ". " + stops(a.get(1)) + " remaining";
			case "guidance.getOffNext":
				return "Next stop: get off at " + a.get(0);
			case "guidance.missed":
				return "You missed the " + a.get(0) + " at " + a.get(1) + ", which left at " + time(a.get(2));
			case "guidance.replan":
				return "Replan from now?";
			case "guidance.offRoute":
				return "You are off route. Replan from here?";
			case "guidance.routeResumed":
				return "Back on route";
			case "guidance.arrived":
				return "You have arrived";
			case "guidance.cancelled":
				return "replan".equals(a.get(0)) ? "Planning a new trip" : "Guidance cancelled";
			default:
				return key + " " + a;
		}
	}

	private static boolean isBlank(Object o) {
		return o == null || o.toString().isEmpty();
	}

	private static String stops(Object n) {
		int k = ((Number) n).intValue();
		return k + (k == 1 ? " stop" : " stops");
	}

	private String time(Object epochSec) {
		return clock.format(Instant.ofEpochSecond(((Number) epochSec).longValue()));
	}
}
