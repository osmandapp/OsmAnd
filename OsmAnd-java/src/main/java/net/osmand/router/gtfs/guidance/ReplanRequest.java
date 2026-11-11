package net.osmand.router.gtfs.guidance;

import net.osmand.data.LatLon;

/** What the host needs to plan a new trip: where from, where to, and when to leave (epoch seconds). */
public final class ReplanRequest {

	public final LatLon from, to;
	public final long departSec;

	public ReplanRequest(LatLon from, LatLon to, long departSec) {
		this.from = from;
		this.to = to;
		this.departSec = departSec;
	}

	@Override
	public String toString() {
		return "replan " + from + " -> " + to + " at " + departSec;
	}
}
