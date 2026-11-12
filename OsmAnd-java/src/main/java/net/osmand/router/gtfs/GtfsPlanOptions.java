package net.osmand.router.gtfs;

/** Tuning knobs of {@link GtfsRoutePlanner#plan}; the defaults suit walking and city transit. */
public class GtfsPlanOptions {

	/** Longest walk from the start to a first stop and from a last stop to the destination, metres in a straight line. */
	public double maxWalkMeters = 800;
	/** Extra weight of walking time when ranking itineraries (1.0 = walking counts double). */
	public double walkWeight = 0;
	/** Rank penalty per transfer, seconds. */
	public int transferRankSec = 0;
	/** Longest walking transfer between two stops (the index holds footpaths up to 400 m). */
	public double maxTransferWalkMeters = 400;
	/** Time needed to change vehicles at a stop when the feed does not say otherwise. */
	public int transferBufferSec = 120;
	public double walkSpeedMps = 1.25;
	/** Straight-line distance times this factor approximates the walked distance. */
	public double walkDetourFactor = 1.3;
	/** Most itineraries returned, the fastest first. */
	public int maxItineraries = 3;
	/** How far past the departure time connections are scanned. */
	public int horizonSec = 4 * 3600;
	/** Extra seconds charged per transfer when looking for an alternative with fewer transfers. */
	public int transferPenaltySec = 600;
	/** Alternatives arriving later than the best itinerary by more than this are dropped. */
	public int maxAlternativeDelaySec = 3600;
	/** Not consulted by the planner yet (S4). */
	public DelayProvider delayProvider = DelayProvider.NONE;
}
