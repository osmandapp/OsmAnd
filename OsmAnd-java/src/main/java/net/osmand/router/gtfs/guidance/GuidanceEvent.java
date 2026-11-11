package net.osmand.router.gtfs.guidance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.data.LatLon;

/**
 * One thing the engine wants the app to do or say. Immutable. The text is not in the event: the app
 * localises {@link #getTextKey()} with {@link #getArgs()} (see {@link EnglishGuidanceFormatter}), and can
 * use the structured fields (leg, stop indexes, times, coordinates) for UI.
 */
public final class GuidanceEvent {

	private final long seq;
	private final GuidanceEventType type;
	private final long epochSec;
	private final GuidanceState state, previousState;
	private final int legIndex, stopIdx, stopPosition, stopsRemaining;
	private final long timeSec;
	private final LatLon from, to;
	private final List<Object> args;

	GuidanceEvent(long seq, GuidanceEventType type, long epochSec, GuidanceState state, GuidanceState previousState,
	              int legIndex, int stopIdx, int stopPosition, int stopsRemaining, long timeSec, LatLon from,
	              LatLon to, List<Object> args) {
		this.seq = seq;
		this.type = type;
		this.epochSec = epochSec;
		this.state = state;
		this.previousState = previousState;
		this.legIndex = legIndex;
		this.stopIdx = stopIdx;
		this.stopPosition = stopPosition;
		this.stopsRemaining = stopsRemaining;
		this.timeSec = timeSec;
		this.from = from;
		this.to = to;
		this.args = Collections.unmodifiableList(new ArrayList<>(args));
	}

	/** Per-engine sequence number, 1-based and gapless: the total order of events. */
	public long getSeq() {
		return seq;
	}

	public GuidanceEventType getType() {
		return type;
	}

	/** The engine clock when the event was produced. */
	public long getEpochSec() {
		return epochSec;
	}

	/** State after the event. */
	public GuidanceState getState() {
		return state;
	}

	/** Previous state, only set on STATE_CHANGED. */
	public GuidanceState getPreviousState() {
		return previousState;
	}

	public int getLegIndex() {
		return legIndex;
	}

	/** GTFS stop index the event is about, or -1. */
	public int getStopIdx() {
		return stopIdx;
	}

	/** Position of that stop within the ride leg's stop list (0 = boarding stop), or -1. */
	public int getStopPosition() {
		return stopPosition;
	}

	/** Stops left until the alight stop, or -1. */
	public int getStopsRemaining() {
		return stopsRemaining;
	}

	/** The relevant expected time in epoch seconds (departure, arrival, suggested replan departure), or -1. */
	public long getTimeSec() {
		return timeSec;
	}

	/** Walk start, or replan origin; may be null. */
	public LatLon getFrom() {
		return from;
	}

	/** Walk end, or replan destination; may be null. */
	public LatLon getTo() {
		return to;
	}

	/** Localisation key, or null if the event is silent. */
	public String getTextKey() {
		return type.getTextKey();
	}

	/** Arguments of the text: Strings, Integers, and Longs for epoch-second times. May contain nulls. */
	public List<Object> getArgs() {
		return args;
	}

	@Override
	public String toString() {
		return "#" + seq + " " + type + " leg=" + legIndex + " state=" + state + (stopIdx >= 0 ? " stop=" + stopIdx : "")
				+ (timeSec >= 0 ? " t=" + timeSec : "") + " " + args;
	}
}
