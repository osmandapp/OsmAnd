package net.osmand.router.gtfs.guidance;

/** What a {@link GuidanceEvent} announces. Each type has at most one text key; null means silent. */
public enum GuidanceEventType {
	/** The state changed; see {@link GuidanceEvent#getPreviousState()}. Silent. */
	STATE_CHANGED(null),
	/** A new leg became current. Silent. */
	LEG_CHANGED(null),
	/** A walk leg began; the host should start pedestrian guidance from {@code from} to {@code to}. */
	WALK_LEG_STARTED("guidance.walk"),
	/** A walk leg is over. Silent. */
	WALK_LEG_ENDED(null),
	/** The user skipped the leg. Silent. */
	LEG_SKIPPED(null),
	/** Board a vehicle: args route, headsign (nullable), stop name, departure epoch seconds. */
	BOARD_PROMPT("guidance.board"),
	/** After getting off, the next line to take: args route, headsign (nullable), stop name, departure time. */
	TRANSFER_INSTRUCTION("guidance.transfer"),
	/** Riding started: args alight stop name, stops to go, arrival epoch seconds. */
	RIDE_STARTED("guidance.rideStarted"),
	/** Args next stop name, stops remaining until the alight stop. */
	NEXT_STOP("guidance.nextStop"),
	/** One stop before the alight stop: args alight stop name. */
	GET_OFF_NEXT("guidance.getOffNext"),
	/** The ride is over (tapped, located or assumed from the schedule). Silent. */
	RIDE_ENDED(null),
	/** The vehicle was missed: args route, stop name, scheduled departure. */
	MISSED("guidance.missed"),
	/** Host should plan a new trip from {@code from} with departure at {@link GuidanceEvent#getTimeSec()}. */
	REPLAN_REQUESTED("guidance.replan"),
	/** Far from the path: offer to replan from here. */
	OFF_ROUTE_OFFER("guidance.offRoute"),
	/** Back near the path after an off-route offer. */
	ROUTE_RESUMED("guidance.routeResumed"),
	/** The destination was reached. */
	ARRIVED("guidance.arrived"),
	/** Guidance ended without arriving: arg reason "cancel" or "replan". */
	CANCELLED("guidance.cancelled");

	private final String textKey;

	GuidanceEventType(String textKey) {
		this.textKey = textKey;
	}

	/** Key for localisation, or null when the event is not spoken or shown. */
	public String getTextKey() {
		return textKey;
	}
}
