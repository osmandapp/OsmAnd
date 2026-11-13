package net.osmand.router.gtfs;

import net.osmand.data.LatLon;

/** One walk or one ride of a {@link GtfsItinerary}. Times are epoch seconds. */
public final class GtfsLeg {

	public enum Type {
		WALK, RIDE
	}

	public final Type type;
	public final long startSec, endSec;
	/** Stop indices; -1 for the requested start (from) or destination (to) of a walk. */
	public final int fromStop, toStop;
	public final LatLon from, to;
	/** Straight-line length of a walk; the real route is for the pedestrian router to find. */
	public final int walkMeters;
	/**
	 * Length of a walk with the detour factor applied (the distance the timing is based on); 0 for a ride.
	 * {@link #walkMeters} stays the straight-line value.
	 */
	public final int walkMetersAdjusted;

	// RIDE only, otherwise -1 / null
	public final int tripIdx, routeIdx;
	/** "feed:trip_id" and "feed:route_id" of a ride, null for a walk. */
	public final String tripId, routeId;
	/** All stops from boarding to alighting, with scheduled times; positions index the trip's stop list. */
	public final int boardSeq, alightSeq;
	public final int[] stops;
	public final long[] arrivalSec, departureSec;

	private GtfsLeg(Type type, long startSec, long endSec, int fromStop, int toStop, LatLon from, LatLon to,
	                int walkMeters, int walkMetersAdjusted, int tripIdx, int routeIdx, int boardSeq, int alightSeq,
	                int[] stops, long[] arrivalSec, long[] departureSec, String tripId, String routeId) {
		this.type = type;
		this.startSec = startSec;
		this.endSec = endSec;
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.from = from;
		this.to = to;
		this.walkMeters = walkMeters;
		this.walkMetersAdjusted = walkMetersAdjusted;
		this.tripIdx = tripIdx;
		this.routeIdx = routeIdx;
		this.tripId = tripId;
		this.routeId = routeId;
		this.boardSeq = boardSeq;
		this.alightSeq = alightSeq;
		this.stops = stops;
		this.arrivalSec = arrivalSec;
		this.departureSec = departureSec;
	}

	/** A walk whose adjusted length uses the default detour factor. */
	static GtfsLeg walk(long startSec, long endSec, int fromStop, int toStop, LatLon from, LatLon to, int meters) {
		return walk(startSec, endSec, fromStop, toStop, from, to, meters, new GtfsPlanOptions().walkDetourFactor);
	}

	static GtfsLeg walk(long startSec, long endSec, int fromStop, int toStop, LatLon from, LatLon to, int meters,
	                    double detourFactor) {
		return new GtfsLeg(Type.WALK, startSec, endSec, fromStop, toStop, from, to, meters,
				(int) Math.round(meters * detourFactor), -1, -1, -1, -1, null, null, null, null, null);
	}

	static GtfsLeg ride(int tripIdx, int routeIdx, int boardSeq, int alightSeq, int[] stops, long[] arrivalSec,
	                    long[] departureSec, LatLon from, LatLon to, String tripId, String routeId) {
		int n = stops.length;
		return new GtfsLeg(Type.RIDE, departureSec[0], arrivalSec[n - 1], stops[0], stops[n - 1], from, to, 0, 0,
				tripIdx, routeIdx, boardSeq, alightSeq, stops, arrivalSec, departureSec, tripId, routeId);
	}

	GtfsLeg withTimes(long start, long end) {
		return new GtfsLeg(type, start, end, fromStop, toStop, from, to, walkMeters, walkMetersAdjusted, tripIdx, routeIdx,
				boardSeq, alightSeq, stops, arrivalSec, departureSec, tripId, routeId);
	}
}
