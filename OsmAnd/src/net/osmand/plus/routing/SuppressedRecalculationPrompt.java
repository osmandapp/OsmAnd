package net.osmand.plus.routing;

// A recalculated route that starts against the movement direction is not announced,
// the next recalculation is expected to give a forward route.
// Engines unaware of the movement direction (e.g. BRouter) may keep returning such routes,
// announce such a recalculation once per deviation instead of never (#25544)
class SuppressedRecalculationPrompt {

	static final long MAX_SUPPRESSED_TIME = 15000;
	static final long DEVIATION_END_INTERVAL = 4 * MAX_SUPPRESSED_TIME;
	static final long DEVIATION_END_FOLLOW_TIME = 15000;

	private long firstSuppressedTime;
	private long lastSuppressedTime;
	private boolean announced;

	private long followStartTime;

	synchronized void reset() {
		firstSuppressedTime = 0;
	}

	// the rider has followed the current route continuously for a while, so the deviation is over;
	// a rider beside a route whose start goes back does not follow it, so a long run of backward
	// routes is still announced after MAX_SUPPRESSED_TIME
	synchronized void onRouteFollowed(long now) {
		if (followStartTime == 0) {
			followStartTime = now;
		}
		if (now - followStartTime >= DEVIATION_END_FOLLOW_TIME) {
			reset();
		}
	}

	synchronized void onRouteNotFollowed() {
		followStartTime = 0;
	}

	synchronized boolean shouldAnnounce(long now) {
		if (firstSuppressedTime == 0 || now - lastSuppressedTime > DEVIATION_END_INTERVAL) {
			firstSuppressedTime = now;
			announced = false;
		}
		lastSuppressedTime = now;
		if (!announced && now - firstSuppressedTime > MAX_SUPPRESSED_TIME) {
			announced = true;
			return true;
		}
		return false;
	}
}
