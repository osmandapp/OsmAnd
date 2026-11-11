package net.osmand.router.gtfs.guidance;

/** Where the traveller is in an itinerary. See design.md section 9 for the transition table. */
public enum GuidanceState {
	/** Created, no input seen yet. */
	PLANNED,
	/** Walking to the first boarding stop. */
	WALK_TO_BOARD,
	/** At the boarding stop, waiting for the scheduled vehicle. */
	WAIT_FOR_VEHICLE,
	/** On a vehicle. */
	RIDING,
	/** Walking between two rides. */
	TRANSFER_WALK,
	/** Walking from the last stop to the destination. */
	WALK_TO_DEST,
	/** Terminal: the destination was reached. */
	ARRIVED,
	/** Far from the planned path (only judged with a good fix); a replan is offered, never automatic. */
	OFF_ROUTE,
	/** The vehicle left (plus the grace period) without the traveller boarding; a replan is requested. */
	MISSED,
	/** Terminal: the user cancelled, or accepted a replan, so this guidance is over. */
	CANCELLED;

	/** True for {@link #ARRIVED} and {@link #CANCELLED}. */
	public boolean isTerminal() {
		return this == ARRIVED || this == CANCELLED;
	}
}
