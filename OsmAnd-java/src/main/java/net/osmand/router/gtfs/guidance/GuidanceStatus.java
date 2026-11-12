package net.osmand.router.gtfs.guidance;

import java.util.Collections;
import java.util.List;

/**
 * What a guidance panel or notification shows right now. The text is a key of {@link GuidanceEventType} with
 * its arguments (the app localises it); the booleans say which buttons make sense. Immutable.
 */
public final class GuidanceStatus {

	public final GuidanceState state;
	/** Key of the instruction to show (for example "guidance.board"), or null before guidance has produced one. */
	public final String textKey;
	public final List<Object> args;
	/** While walking with a fresh fix: metres in a straight line to the end of the walk, else -1. */
	public final int walkRemainingM;
	/** "I'm on board": makes sense while walking to, or waiting for, a vehicle. */
	public final boolean canBoard;
	/** "I got off": makes sense while riding. */
	public final boolean canAlight;
	/** "Skip": give up a walk or a wait and go on to the next leg. */
	public final boolean canSkip;
	/** "Replan": plan a new trip from here and now. */
	public final boolean canReplan;

	/** How the ride/alight state was reached. */
	public final GtfsGuidanceEngine.Confidence confidence;
	/** True when the state is assumed (no evidence) and the app may show a quiet "correct me" prompt. */
	public final boolean needsConfirmation;
	/** "Not on this train": makes sense while riding on an assumed or detected boarding. */
	public final boolean canNotOnTrain;
	/** While riding or waiting: the route and headsign of the current ride (headsign may be null). */
	public final String rideRoute, rideHeadsign;

	GuidanceStatus(GuidanceState state, String textKey, List<Object> args, int walkRemainingM) {
		this(state, textKey, args, walkRemainingM, GtfsGuidanceEngine.Confidence.MANUAL);
	}

	GuidanceStatus(GuidanceState state, String textKey, List<Object> args, int walkRemainingM,
	               GtfsGuidanceEngine.Confidence confidence) {
		this(state, textKey, args, walkRemainingM, confidence, null, null);
	}

	GuidanceStatus(GuidanceState state, String textKey, List<Object> args, int walkRemainingM,
	               GtfsGuidanceEngine.Confidence confidence, String rideRoute, String rideHeadsign) {
		this.rideRoute = rideRoute;
		this.rideHeadsign = rideHeadsign;
		this.confidence = confidence;
		this.needsConfirmation = confidence == GtfsGuidanceEngine.Confidence.ASSUMED && (state == GuidanceState.RIDING
				|| state == GuidanceState.WALK_TO_DEST || state == GuidanceState.TRANSFER_WALK);
		this.canNotOnTrain = state == GuidanceState.RIDING && confidence != GtfsGuidanceEngine.Confidence.MANUAL;
		this.state = state;
		this.textKey = textKey;
		this.args = Collections.unmodifiableList(args);
		this.walkRemainingM = walkRemainingM;
		boolean walking = state == GuidanceState.WALK_TO_BOARD || state == GuidanceState.TRANSFER_WALK;
		this.canBoard = walking || state == GuidanceState.WAIT_FOR_VEHICLE || state == GuidanceState.MISSED;
		this.canAlight = state == GuidanceState.RIDING;
		this.canSkip = walking || state == GuidanceState.WALK_TO_DEST || state == GuidanceState.WAIT_FOR_VEHICLE
				|| state == GuidanceState.MISSED;
		this.canReplan = !state.isTerminal();
	}

	@Override
	public String toString() {
		return state + " " + textKey + " " + args + (walkRemainingM >= 0 ? " walk=" + walkRemainingM + "m" : "");
	}
}
