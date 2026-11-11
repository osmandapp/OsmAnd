package net.osmand.router.gtfs.guidance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.router.gtfs.DelayProvider;
import net.osmand.router.gtfs.GtfsItinerary;
import net.osmand.router.gtfs.GtfsLeg;

/**
 * Turn-by-turn guidance state machine for a {@link GtfsItinerary} (design.md section 9). It is a pure
 * function of the events fed in and the clock they carry: no threads, no timers, no Android, no routing.
 * The host calls {@link #onLocation}, {@link #onTick} (at least every few seconds, also without any fix, as the
 * subway has none) and {@link #onUserAction}, and turns the returned {@link GuidanceEvent}s into speech,
 * notifications and UI. Not thread safe; call from one thread.
 *
 * <p>Principles: location is used when there is a fresh good fix, the schedule otherwise; manual taps always
 * work; a state never moves backwards except into OFF_ROUTE or MISSED, which resume forwards; every prompt
 * fires once; nothing replans by itself.
 */
public final class GtfsGuidanceEngine {

	private final GtfsItinerary itinerary;
	private final List<GtfsLeg> legs;
	private final GuidanceNames names;
	private final DelayProvider delays;
	private final GuidanceConfig cfg;
	private final EventLog log = new EventLog();

	private GuidanceState state = GuidanceState.PLANNED;
	private GuidanceState resumeState;
	private int legIdx;
	/** Position within the current ride's stop list of the stop the vehicle last left or is at. */
	private int pos;
	private boolean alightByLocation;
	/** When the current walk leg was entered; a late start (delays) pushes the walk's end later. */
	private long walkStartSec;
	private int offStreak, offEpisode;
	private long clock = Long.MIN_VALUE;
	private boolean haveFix;
	private double fixLat, fixLon;
	private long fixSec;

	/** Engine with no delays and default thresholds. */
	public GtfsGuidanceEngine(GtfsItinerary itinerary, GuidanceNames names) {
		this(itinerary, names, DelayProvider.NONE, GuidanceConfig.defaults());
	}

	public GtfsGuidanceEngine(GtfsItinerary itinerary, GuidanceNames names, DelayProvider delays, GuidanceConfig cfg) {
		if (itinerary.legs.isEmpty()) {
			throw new IllegalArgumentException("itinerary has no legs");
		}
		this.itinerary = itinerary;
		this.legs = itinerary.legs;
		this.names = names;
		this.delays = delays == null ? DelayProvider.NONE : delays;
		this.cfg = cfg;
	}

	// ------------------------------------------------------------------ public API

	public GuidanceState getState() {
		return state;
	}

	/** Index of the current leg (the last leg once arrived). */
	public int getLegIndex() {
		return legIdx;
	}

	/** While riding: position in the ride's stop list of the last stop reached (0 = boarding stop). */
	public int getStopPosition() {
		return pos;
	}

	public GtfsItinerary getItinerary() {
		return itinerary;
	}

	/** True once ARRIVED or CANCELLED: all further input is ignored. */
	public boolean isFinished() {
		return state.isTerminal();
	}

	/** Starts guidance at this time (optional: the first input starts it too). */
	public List<GuidanceEvent> start(long epochSec) {
		return run(epochSec, null, false);
	}

	/**
	 * A location update. With {@code hasFix} false (or a fix less accurate than the configured limit) it only
	 * advances the clock, like a tick. Epoch seconds earlier than the engine clock are treated as the clock.
	 */
	public List<GuidanceEvent> onLocation(double lat, double lon, float accuracyM, long epochSec, boolean hasFix) {
		long now = Math.max(clock, epochSec);
		boolean usable = hasFix && accuracyM <= cfg.maxAccuracyM && !Double.isNaN(lat) && !Double.isNaN(lon);
		if (usable && !state.isTerminal()) {
			haveFix = true;
			fixLat = lat;
			fixLon = lon;
			fixSec = now;
		}
		return run(now, null, usable);
	}

	/** Advances the clock. */
	public List<GuidanceEvent> onTick(long epochSec) {
		return run(epochSec, null, false);
	}

	/** A manual action; ones that make no sense in the current state are ignored. */
	public List<GuidanceEvent> onUserAction(UserAction action, long epochSec) {
		return run(epochSec, action, false);
	}

	// ------------------------------------------------------------------ driver

	private List<GuidanceEvent> run(long epochSec, UserAction action, boolean locationUsable) {
		if (state.isTerminal()) {
			return log.take();
		}
		long now = Math.max(clock, epochSec);
		clock = now;
		if (state == GuidanceState.PLANNED) {
			if (action == UserAction.CANCEL) {
				cancel(now, "cancel");
				return log.take();
			}
			enterLeg(0, now, false);
		}
		if (action != null) {
			apply(action, now);
		}
		settle(now);
		if (locationUsable && !state.isTerminal()) {
			checkOffRoute(now);
			settle(now);
		}
		return log.take();
	}

	private void settle(long now) {
		for (int guard = 0; guard < 200 && !state.isTerminal() && step(now); guard++) {
			// each step changes something; the cap only protects against a logic error
		}
	}

	private boolean step(long now) {
		switch (state) {
			case WALK_TO_BOARD:
			case TRANSFER_WALK:
			case WALK_TO_DEST:
				return stepWalk(now);
			case WAIT_FOR_VEHICLE:
				if (inferBoarded(now)) {
					board(now);
					return true;
				}
				if (isMissed(legIdx, now)) {
					miss(legIdx, now);
					return true;
				}
				return false;
			case MISSED:
				if (inferBoarded(now)) {
					board(now);
					return true;
				}
				return false;
			case RIDING:
				return stepRide(now);
			default:
				return false;
		}
	}

	private boolean stepWalk(long now) {
		int ride = nextRide(legIdx);
		if (ride >= 0 && isMissed(ride, now)) {
			miss(ride, now);
			return true;
		}
		GtfsLeg w = legs.get(legIdx);
		boolean done;
		if (isFresh(now) && w.to != null) {
			done = GeoMath.distance(fixLat, fixLon, w.to) <= cfg.arriveRadiusM;
		} else {
			long end = Math.max(w.endSec, walkStartSec + (w.endSec - w.startSec));
			done = !isFresh(now) && now >= end + cfg.walkSlackSec;
		}
		if (done) {
			endWalk(now);
			return true;
		}
		return false;
	}

	private boolean stepRide(long now) {
		GtfsLeg r = legs.get(legIdx);
		int n = r.stops.length;
		boolean moved = updateProgress(r, now);
		if (pos == n - 1 && (alightByLocation
				|| (!useLocation(r, now) && now >= expectedArrival(r, n - 1) + cfg.alightSlackSec))) {
			alight(now);
			return true;
		}
		return moved;
	}

	// ------------------------------------------------------------------ user actions

	private void apply(UserAction action, long now) {
		if (state == GuidanceState.OFF_ROUTE && action != UserAction.CANCEL && action != UserAction.REPLAN_ACK) {
			setState(resumeState, now);
		}
		switch (action) {
			case CANCEL:
				cancel(now, "cancel");
				break;
			case REPLAN_ACK:
				if (state == GuidanceState.OFF_ROUTE) {
					requestReplan(legIdx, now, offEpisode);
					cancel(now, "replan");
				} else if (state == GuidanceState.MISSED) {
					cancel(now, "replan");
				}
				break;
			case BOARDED:
				onBoardedTap(now);
				break;
			case ALIGHTED:
				if (state == GuidanceState.RIDING || state == GuidanceState.WAIT_FOR_VEHICLE
						|| state == GuidanceState.MISSED) {
					alight(now);
				}
				break;
			case SKIP_LEG:
				if (state == GuidanceState.RIDING) {
					alight(now);
				} else if (!state.isTerminal() && state != GuidanceState.PLANNED) {
					log.event(GuidanceEventType.LEG_SKIPPED, now, state).leg(legIdx).emit();
					enterLeg(legIdx + 1, now, false);
				}
				break;
			default:
				break;
		}
	}

	private void onBoardedTap(long now) {
		switch (state) {
			case WALK_TO_BOARD:
			case TRANSFER_WALK:
				int ride = nextRide(legIdx);
				if (ride >= 0) {
					log.event(GuidanceEventType.WALK_LEG_ENDED, now, state).leg(legIdx)
							.points(legs.get(legIdx).from, legs.get(legIdx).to).emit();
					enterLeg(ride, now, true);
					board(now);
				}
				break;
			case WAIT_FOR_VEHICLE:
			case MISSED:
				board(now);
				break;
			default:
				break;
		}
	}

	// ------------------------------------------------------------------ transitions

	private void setState(GuidanceState next, long now) {
		GuidanceState prev = state;
		state = next;
		log.event(GuidanceEventType.STATE_CHANGED, now, next).previous(prev).leg(legIdx).emit();
	}

	/** Makes leg {@code idx} current, or finishes if there is none. {@code quiet} skips the board prompt. */
	private void enterLeg(int idx, long now, boolean quiet) {
		if (idx >= legs.size()) {
			legIdx = legs.size() - 1;
			GtfsLeg last = legs.get(legIdx);
			setState(GuidanceState.ARRIVED, now);
			log.event(GuidanceEventType.ARRIVED, now, state).leg(legIdx).stop(last.toStop, -1).time(now)
					.points(last.from, last.to).emit();
			return;
		}
		legIdx = idx;
		pos = 0;
		alightByLocation = false;
		offStreak = 0;
		GtfsLeg leg = legs.get(idx);
		if (leg.type == GtfsLeg.Type.WALK) {
			walkStartSec = now;
			GuidanceState s = idx == legs.size() - 1 ? GuidanceState.WALK_TO_DEST
					: idx > 0 ? GuidanceState.TRANSFER_WALK : GuidanceState.WALK_TO_BOARD;
			log.event(GuidanceEventType.LEG_CHANGED, now, state).leg(idx).emit();
			setState(s, now);
			log.event(GuidanceEventType.WALK_LEG_STARTED, now, s).leg(idx).stop(leg.toStop, -1).time(leg.endSec)
					.points(leg.from, leg.to).args(leg.toStop >= 0 ? stopName(leg.toStop) : null).emit();
		} else {
			log.event(GuidanceEventType.LEG_CHANGED, now, state).leg(idx).emit();
			setState(GuidanceState.WAIT_FOR_VEHICLE, now);
			if (!quiet) {
				long dep = expectedDeparture(leg, 0);
				log.event(GuidanceEventType.BOARD_PROMPT, now, state).leg(idx).stop(leg.fromStop, 0).time(dep)
						.args(names.routeName(leg), names.headsign(leg), stopName(leg.fromStop), dep).emit();
			}
		}
	}

	private void endWalk(long now) {
		GtfsLeg w = legs.get(legIdx);
		log.event(GuidanceEventType.WALK_LEG_ENDED, now, state).leg(legIdx).points(w.from, w.to).emit();
		enterLeg(legIdx + 1, now, false);
	}

	private void board(long now) {
		GtfsLeg r = legs.get(legIdx);
		int n = r.stops.length;
		pos = 0;
		alightByLocation = false;
		setState(GuidanceState.RIDING, now);
		long arr = expectedArrival(r, n - 1);
		log.event(GuidanceEventType.RIDE_STARTED, now, state).leg(legIdx).stop(r.toStop, n - 1).remaining(n - 1)
				.time(arr).args(stopName(r.toStop), n - 1, arr).emit();
		updateProgress(r, now);
	}

	private void alight(long now) {
		GtfsLeg r = legs.get(legIdx);
		log.event(GuidanceEventType.RIDE_ENDED, now, state).leg(legIdx).stop(r.toStop, r.stops.length - 1).emit();
		int next = nextRide(legIdx + 1);
		if (next >= 0) {
			GtfsLeg nr = legs.get(next);
			long dep = expectedDeparture(nr, 0);
			log.event(GuidanceEventType.TRANSFER_INSTRUCTION, now, state).leg(next).stop(nr.fromStop, 0).time(dep)
					.args(names.routeName(nr), names.headsign(nr), stopName(nr.fromStop), dep).emit();
		}
		enterLeg(legIdx + 1, now, false);
	}

	private void miss(int rideIdx, long now) {
		if (rideIdx != legIdx) {
			log.event(GuidanceEventType.WALK_LEG_ENDED, now, state).leg(legIdx).points(legs.get(legIdx).from,
					legs.get(legIdx).to).emit();
			legIdx = rideIdx;
			pos = 0;
			log.event(GuidanceEventType.LEG_CHANGED, now, state).leg(legIdx).emit();
		}
		GtfsLeg r = legs.get(rideIdx);
		long dep = expectedDeparture(r, 0);
		setState(GuidanceState.MISSED, now);
		log.event(GuidanceEventType.MISSED, now, state).leg(rideIdx).stop(r.fromStop, 0).time(dep)
				.args(names.routeName(r), stopName(r.fromStop), dep).emit();
		requestReplan(rideIdx, now, 0);
	}

	private void requestReplan(int leg, long now, int episode) {
		LatLon origin = haveFix ? new LatLon(fixLat, fixLon) : legs.get(leg).from;
		log.event(GuidanceEventType.REPLAN_REQUESTED, now, state).leg(leg).time(now).episode(episode)
				.points(origin, legs.get(legs.size() - 1).to).emit();
	}

	private void cancel(long now, String reason) {
		setState(GuidanceState.CANCELLED, now);
		log.event(GuidanceEventType.CANCELLED, now, state).leg(legIdx).args(reason).emit();
	}

	// ------------------------------------------------------------------ riding progress

	/** Moves {@code pos} forward (never back) from location or schedule and announces the new stop. */
	private boolean updateProgress(GtfsLeg r, long now) {
		int n = r.stops.length;
		int target = pos;
		if (useLocation(r, now)) {
			for (int k = n - 1; k > pos; k--) {
				if (GeoMath.distance(fixLat, fixLon, stopLocation(r, k)) <= cfg.stopRadiusM) {
					target = k;
					alightByLocation = k == n - 1;
					break;
				}
			}
		} else {
			target = Math.max(pos, scheduledPosition(r, now));
		}
		if (target <= pos) {
			return false;
		}
		pos = target;
		if (pos == n - 2) {
			log.event(GuidanceEventType.GET_OFF_NEXT, now, state).leg(legIdx).stop(r.toStop, n - 1).remaining(1)
					.args(stopName(r.toStop)).emit();
		} else if (pos < n - 2) {
			int next = r.stops[pos + 1];
			log.event(GuidanceEventType.NEXT_STOP, now, state).leg(legIdx).stop(next, pos + 1).remaining(n - 1 - pos)
					.time(expectedArrival(r, pos + 1)).args(stopName(next), n - 1 - pos).emit();
		}
		return true;
	}

	/** Position of the stop the vehicle should have last left (or reached, for the final stop) by the schedule. */
	private int scheduledPosition(GtfsLeg r, long now) {
		int n = r.stops.length;
		if (expectedArrival(r, n - 1) <= now) {
			return n - 1;
		}
		for (int k = n - 2; k > 0; k--) {
			if (expectedDeparture(r, k) <= now) {
				return k;
			}
		}
		return 0;
	}

	/** True if the traveller seems to be at a stop after the boarding one, so they must have boarded. */
	private boolean inferBoarded(long now) {
		GtfsLeg r = legs.get(legIdx);
		if (!useLocation(r, now)) {
			return false;
		}
		double atBoard = GeoMath.distance(fixLat, fixLon, stopLocation(r, 0));
		for (int k = 1; k < r.stops.length; k++) {
			double d = GeoMath.distance(fixLat, fixLon, stopLocation(r, k));
			if (d <= cfg.stopRadiusM && d < atBoard) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ off route

	private void checkOffRoute(long now) {
		if (state == GuidanceState.OFF_ROUTE) {
			List<LatLon> path = pathFor(resumeState);
			if (path != null && GeoMath.toPath(fixLat, fixLon, path) <= cfg.offRouteResumeM) {
				offStreak = 0;
				setState(resumeState, now);
				log.event(GuidanceEventType.ROUTE_RESUMED, now, state).leg(legIdx).episode(offEpisode).emit();
			}
			return;
		}
		List<LatLon> path = pathFor(state);
		if (path == null) {
			offStreak = 0;
			return;
		}
		offStreak = GeoMath.toPath(fixLat, fixLon, path) > cfg.offRouteM ? offStreak + 1 : 0;
		if (offStreak >= cfg.offRouteFixes) {
			offStreak = 0;
			resumeState = state;
			offEpisode++;
			setState(GuidanceState.OFF_ROUTE, now);
			log.event(GuidanceEventType.OFF_ROUTE_OFFER, now, state).leg(legIdx).episode(offEpisode)
					.points(new LatLon(fixLat, fixLon), null).emit();
		}
	}

	/** The planned path for a state, or null if there is nothing reliable to compare against. */
	private List<LatLon> pathFor(GuidanceState s) {
		GtfsLeg leg = legs.get(legIdx);
		switch (s) {
			case WALK_TO_BOARD:
			case TRANSFER_WALK:
			case WALK_TO_DEST:
				return leg.from == null || leg.to == null ? null : java.util.Arrays.asList(leg.from, leg.to);
			case WAIT_FOR_VEHICLE:
				return leg.from == null ? null : Collections.singletonList(leg.from);
			case RIDING:
				if (!hasAllStopLocations(leg)) {
					return null;
				}
				List<LatLon> path = new ArrayList<>();
				for (int k = 0; k < leg.stops.length; k++) {
					path.add(stopLocation(leg, k));
				}
				return path;
			default:
				return null;
		}
	}

	// ------------------------------------------------------------------ helpers

	private boolean isFresh(long now) {
		return haveFix && now - fixSec <= cfg.fixFreshSec;
	}

	private boolean useLocation(GtfsLeg r, long now) {
		return isFresh(now) && hasAllStopLocations(r);
	}

	private boolean hasAllStopLocations(GtfsLeg r) {
		for (int k = 0; k < r.stops.length; k++) {
			if (stopLocation(r, k) == null) {
				return false;
			}
		}
		return true;
	}

	private LatLon stopLocation(GtfsLeg r, int k) {
		if (k == 0 && r.from != null) {
			return r.from;
		}
		if (k == r.stops.length - 1 && r.to != null) {
			return r.to;
		}
		return names.stopLocation(r.stops[k]);
	}

	private boolean isMissed(int rideIdx, long now) {
		return now > expectedDeparture(legs.get(rideIdx), 0) + cfg.graceSec;
	}

	/** Index of the first ride at or after {@code from}, or -1. */
	private int nextRide(int from) {
		for (int i = from; i < legs.size(); i++) {
			if (legs.get(i).type == GtfsLeg.Type.RIDE) {
				return i;
			}
		}
		return -1;
	}

	private long expectedDeparture(GtfsLeg r, int k) {
		return r.departureSec[k] + delays.departureDelaySec(r.tripIdx, r.boardSeq + k);
	}

	private long expectedArrival(GtfsLeg r, int k) {
		return r.arrivalSec[k] + delays.arrivalDelaySec(r.tripIdx, r.boardSeq + k);
	}

	private String stopName(int stopIdx) {
		String name = stopIdx < 0 ? null : names.stopName(stopIdx);
		return name != null ? name : "#" + stopIdx;
	}
}
