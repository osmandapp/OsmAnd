package net.osmand.router.gtfs.guidance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.router.gtfs.DelayProvider;
import net.osmand.router.gtfs.GtfsItinerary;
import net.osmand.router.gtfs.GtfsLeg;

/**
 * Everything between the {@link GtfsGuidanceEngine} and the Android layer that does not need Android: the
 * engine's events become a {@link GuidanceStatus} to show, a list of events to speak and, when the traveller
 * asks for or accepts a replan, a {@link ReplanRequest}. Not thread safe; call from one thread.
 */
public final class GuidanceController {

	/** What the traveller tapped. */
	public enum Command {
		BOARDED, ALIGHTED, SKIP, REPLAN, CANCEL
	}

	/** The outcome of one input. */
	public static final class Update {
		/** Events that should be spoken, in order. */
		public final List<GuidanceEvent> spoken;
		/** Non-null when a new trip must be planned and this guidance is over. */
		public final ReplanRequest replan;
		/** True when the destination was reached in this update. */
		public final boolean arrived;
		/** True when this guidance is over (arrived, cancelled or replanning). */
		public final boolean finished;

		Update(List<GuidanceEvent> spoken, ReplanRequest replan, boolean arrived, boolean finished) {
			this.spoken = spoken;
			this.replan = replan;
			this.arrived = arrived;
			this.finished = finished;
		}
	}

	private final GtfsGuidanceEngine engine;
	private final GuidanceConfig cfg;
	private final GuidanceNames names;
	private final List<GtfsLeg> legs;

	private GuidanceEvent shown;
	private ReplanRequest offered;
	private boolean haveFix;
	private double fixLat, fixLon;
	private long fixSec;

	public GuidanceController(GtfsItinerary itinerary, GuidanceNames names) {
		this(itinerary, names, DelayProvider.NONE, GuidanceConfig.defaults());
	}

	public GuidanceController(GtfsItinerary itinerary, GuidanceNames names, DelayProvider delays, GuidanceConfig cfg) {
		this.engine = new GtfsGuidanceEngine(itinerary, names, delays, cfg);
		this.cfg = cfg;
		this.names = names;
		this.legs = itinerary.legs;
	}

	public GtfsGuidanceEngine getEngine() {
		return engine;
	}

	public boolean isFinished() {
		return engine.isFinished();
	}

	public Update start(long nowSec) {
		return handle(engine.start(nowSec), nowSec, null);
	}

	public Update onLocation(double lat, double lon, float accuracyM, long nowSec, boolean hasFix) {
		if (hasFix && accuracyM <= cfg.maxAccuracyM && !Double.isNaN(lat) && !Double.isNaN(lon)) {
			haveFix = true;
			fixLat = lat;
			fixLon = lon;
			fixSec = nowSec;
		}
		return handle(engine.onLocation(lat, lon, accuracyM, nowSec, hasFix), nowSec, null);
	}

	public Update onTick(long nowSec) {
		return handle(engine.onTick(nowSec), nowSec, null);
	}

	public Update onCommand(Command command, long nowSec) {
		switch (command) {
			case BOARDED:
				return handle(engine.onUserAction(UserAction.BOARDED, nowSec), nowSec, null);
			case ALIGHTED:
				return handle(engine.onUserAction(UserAction.ALIGHTED, nowSec), nowSec, null);
			case SKIP:
				return handle(engine.onUserAction(UserAction.SKIP_LEG, nowSec), nowSec, null);
			case CANCEL:
				return handle(engine.onUserAction(UserAction.CANCEL, nowSec), nowSec, null);
			case REPLAN:
				GuidanceState s = engine.getState();
				if (s == GuidanceState.OFF_ROUTE || s == GuidanceState.MISSED) {
					return handle(engine.onUserAction(UserAction.REPLAN_ACK, nowSec), nowSec, null);
				}
				if (engine.isFinished()) {
					return handle(Collections.<GuidanceEvent>emptyList(), nowSec, null);
				}
				// the engine only knows replans from OFF_ROUTE and MISSED: end it and plan from here
				ReplanRequest manual = manualReplan(nowSec);
				return handle(engine.onUserAction(UserAction.CANCEL, nowSec), nowSec, manual);
			default:
				throw new IllegalArgumentException(String.valueOf(command));
		}
	}

	/** The status to show at this time. Cheap; call as often as the display needs. */
	public GuidanceStatus status(long nowSec) {
		GuidanceState state = engine.getState();
		String key = null;
		List<Object> args = Collections.emptyList();
		GuidanceEvent e = shown;
		if (state == GuidanceState.OFF_ROUTE) {
			key = GuidanceEventType.OFF_ROUTE_OFFER.getTextKey();
		} else if (e != null) {
			key = e.getTextKey();
			args = e.getArgs();
		}
		return new GuidanceStatus(state, key, args, walkRemaining(state, nowSec));
	}

	// ------------------------------------------------------------------ internals

	private int walkRemaining(GuidanceState state, long nowSec) {
		if (!(state == GuidanceState.WALK_TO_BOARD || state == GuidanceState.TRANSFER_WALK
				|| state == GuidanceState.WALK_TO_DEST) || !haveFix || nowSec - fixSec > cfg.fixFreshSec) {
			return -1;
		}
		LatLon to = legs.get(engine.getLegIndex()).to;
		return to == null ? -1 : (int) Math.round(GeoMath.distance(fixLat, fixLon, to));
	}

	private Update handle(List<GuidanceEvent> events, long nowSec, ReplanRequest manual) {
		boolean cancelled = false, replanned = false, arrived = false;
		ReplanRequest request = manual;
		for (GuidanceEvent e : events) {
			switch (e.getType()) {
				case WALK_LEG_STARTED:
				case BOARD_PROMPT:
				case TRANSFER_INSTRUCTION:
				case RIDE_STARTED:
				case NEXT_STOP:
				case GET_OFF_NEXT:
				case MISSED:
				case ARRIVED:
					shown = e;
					break;
				case REPLAN_REQUESTED:
					offered = new ReplanRequest(e.getFrom(), e.getTo(), e.getTimeSec());
					break;
				case CANCELLED:
					cancelled = true;
					replanned = "replan".equals(e.getArgs().get(0));
					break;
				default:
					break;
			}
			arrived |= e.getType() == GuidanceEventType.ARRIVED;
		}
		if (replanned && request == null) {
			request = offered;
		}
		List<GuidanceEvent> spoken = new ArrayList<>();
		for (GuidanceEvent e : events) {
			if (isSpoken(e, cancelled)) {
				spoken.add(e);
			}
		}
		return new Update(spoken, request, arrived, engine.isFinished());
	}

	/** Spoken: every event with a text, except a replan that the traveller already accepted and a plain cancel. */
	private static boolean isSpoken(GuidanceEvent e, boolean batchCancelled) {
		if (e.getTextKey() == null) {
			return false;
		}
		switch (e.getType()) {
			case REPLAN_REQUESTED:
				return !batchCancelled;
			case CANCELLED:
				return "replan".equals(e.getArgs().get(0));
			default:
				return true;
		}
	}

	/** Plan from the last good fix, else from where the schedule says the traveller is; leave now. */
	private ReplanRequest manualReplan(long nowSec) {
		GtfsLeg leg = legs.get(engine.getLegIndex());
		LatLon from;
		if (haveFix && nowSec - fixSec <= cfg.fixFreshSec) {
			from = new LatLon(fixLat, fixLon);
		} else if (leg.type == GtfsLeg.Type.RIDE && engine.getState() == GuidanceState.RIDING) {
			int pos = Math.min(engine.getStopPosition(), leg.stops.length - 1);
			LatLon stop = names.stopLocation(leg.stops[pos]);
			from = stop != null ? stop : leg.from;
		} else {
			from = leg.from;
		}
		return new ReplanRequest(from, legs.get(legs.size() - 1).to, nowSec);
	}
}
