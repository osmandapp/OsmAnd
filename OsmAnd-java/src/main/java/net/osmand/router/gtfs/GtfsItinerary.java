package net.osmand.router.gtfs;

import java.util.Collections;
import java.util.List;

/** A planned trip: walks and rides in order, ending at the destination. Times are epoch seconds. */
public final class GtfsItinerary {

	public final List<GtfsLeg> legs;
	public final long departSec, arriveSec;
	/** Vehicle changes: rides minus one. */
	public final int transfers;

	GtfsItinerary(List<GtfsLeg> legs, long departSec, long arriveSec, int transfers) {
		this.legs = Collections.unmodifiableList(legs);
		this.departSec = departSec;
		this.arriveSec = arriveSec;
		this.transfers = transfers;
	}
}
