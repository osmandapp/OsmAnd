package net.osmand.plus.routing;

// A recalculated route that starts against the movement direction is not announced,
// the next recalculation is expected to give a forward route.
// Engines unaware of the movement direction (e.g. BRouter) may keep returning such routes,
// announce such a recalculation once per deviation instead of never (#25544)
class SuppressedRecalculationPrompt {

	static final long MAX_SUPPRESSED_TIME = 15000;
	static final long DEVIATION_END_INTERVAL = 4 * MAX_SUPPRESSED_TIME;

	private long firstSuppressedTime;
	private long lastSuppressedTime;
	private boolean announced;

	synchronized void reset() {
		firstSuppressedTime = 0;
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
