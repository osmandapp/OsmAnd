package net.osmand.router.gtfs.guidance;

/** A manual confirmation or choice from the traveller. First-class: the subway has no GPS. */
public enum UserAction {
	/** "I'm on board." */
	BOARDED,
	/** "I got off." */
	ALIGHTED,
	/** Give up the current leg and go on to the next one. */
	SKIP_LEG,
	/** "Not on this train": undo an auto-detected or assumed boarding. */
	NOT_ON_TRAIN,
	/** Stop guidance. */
	CANCEL,
	/** The traveller accepted the offered replan (after MISSED or OFF_ROUTE); the host plans a new trip. */
	REPLAN_ACK
}
