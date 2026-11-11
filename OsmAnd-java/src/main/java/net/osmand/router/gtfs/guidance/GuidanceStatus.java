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

	GuidanceStatus(GuidanceState state, String textKey, List<Object> args, int walkRemainingM) {
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
