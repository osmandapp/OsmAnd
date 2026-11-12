package net.osmand.router.gtfs;

import java.util.Collections;
import java.util.List;

/** A planned trip: walks and rides in order, ending at the destination. Times are epoch seconds. */
public final class GtfsItinerary {

	public final List<GtfsLeg> legs;
	/** Start of the first leg. The first walk is shifted as late as possible, so this can be after the request. */
	public final long departSec;
	public final long arriveSec;
	/** Vehicle changes: rides minus one. */
	public final int transfers;
	/** The time the trip was requested for (equal to {@link #departSec} when unknown). */
	public final long requestedDepartSec;
	/** Seconds spent in vehicles: the sum of the ride legs. */
	public final int inVehicleSec;
	/** Total walk, straight line, and with the detour factor applied (what the walking times are based on). */
	public final int walkMeters, walkMetersAdjusted;

	GtfsItinerary(List<GtfsLeg> legs, long departSec, long arriveSec, int transfers) {
		this(legs, departSec, arriveSec, transfers, departSec);
	}

	GtfsItinerary(List<GtfsLeg> legs, long departSec, long arriveSec, int transfers, long requestedDepartSec) {
		this.legs = Collections.unmodifiableList(legs);
		this.departSec = departSec;
		this.arriveSec = arriveSec;
		this.transfers = transfers;
		this.requestedDepartSec = requestedDepartSec;
		int ride = 0, straight = 0, adjusted = 0;
		for (GtfsLeg l : legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				ride += (int) (l.endSec - l.startSec);
			} else {
				straight += l.walkMeters;
				adjusted += l.walkMetersAdjusted;
			}
		}
		this.inVehicleSec = ride;
		this.walkMeters = straight;
		this.walkMetersAdjusted = adjusted;
	}

	/** From the shifted departure to the arrival: what the legs add up to, without the wait at home. */
	public long durationSec() {
		return arriveSec - departSec;
	}

	/** From the requested time to the arrival, the wait before the shifted departure included. */
	public long durationFromRequestSec() {
		return arriveSec - requestedDepartSec;
	}
}
