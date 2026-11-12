package net.osmand.router.gtfs;

/** Tuning knobs of {@link GtfsRoutePlanner#plan}; the defaults suit walking and city transit. */
public class GtfsPlanOptions {

	/** Longest walk from the start to a first stop and from a last stop to the destination, metres in a straight line. */
	public double maxWalkMeters = 800;
	/** Longest walking transfer between two stops (the index holds footpaths up to 400 m). */
	public double maxTransferWalkMeters = 400;
	/** Time needed to change vehicles at a stop when the feed does not say otherwise. */
	public int transferBufferSec = 120;
	/**
	 * Walking speed, metres per second. 1.25 stays the default: on the plan-matrix rows 1.35 changes the best
	 * arrival by 0 to 30 s (mean 11 s) and never the lines (GtfsWalkSpeedEvalTest), because the arrival is bound
	 * by the vehicles; it only shortens the walks.
	 */
	public double walkSpeedMps = 1.25;
	/** Straight-line distance times this factor approximates the walked distance. */
	public double walkDetourFactor = 1.3;
	/** Most itineraries returned, the fastest first. */
	public int maxItineraries = 3;
	/** How far past the departure time connections are scanned. */
	public int horizonSec = 4 * 3600;
	/**
	 * Seconds each ride counts for when labels are compared in the passes that look for an alternative with
	 * fewer transfers (run at once and at three times this value); the arrival times reported stay real.
	 */
	public int transferPenaltySec = 600;
	/** Alternatives are ranked by arrival plus this many seconds per transfer (the best itinerary stays first). */
	public int fewerTransfersToleranceSec = 300;
	/** Most routes of the best itinerary tried, one at a time, as banned in the search for alternatives. */
	public int maxRouteBans = 4;
	/** Alternatives arriving later than the best itinerary by more than this are dropped. */
	public int maxAlternativeDelaySec = 3600;
	/** Not consulted by the planner yet (S4). */
	public DelayProvider delayProvider = DelayProvider.NONE;
}
