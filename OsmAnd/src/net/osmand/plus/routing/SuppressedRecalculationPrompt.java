package net.osmand.plus.routing;

import androidx.annotation.NonNull;

import net.osmand.Location;

import java.util.List;

public class SuppressedRecalculationPrompt {

	static final int DIRECTION_CHECK_DISTANCE = 100;
	static final long MAX_SUPPRESSED_TIME = 15000;
	static final long DEVIATION_END_INTERVAL = 4 * MAX_SUPPRESSED_TIME;
	static final long DEVIATION_END_FOLLOW_TIME = 15000;
	static final long FIRST_PROMPT_INTERVAL = 5000;
	static final long MAX_PROMPT_INTERVAL = 5 * 60 * 1000;
	static final float PROMPT_INTERVAL_FACTOR = 2.5f;

	private long firstSuppressedTime;
	private long lastSuppressedTime;
	private long lastPromptTime;
	private long promptInterval;

	private long followStartTime;
	private volatile boolean deviationEnded;

	void reset() {
		deviationEnded = false;
		firstSuppressedTime = 0;
		promptInterval = 0;
	}

	boolean shouldAnnounce(long now, boolean againstMovement) {
		if (deviationEnded) {
			reset();
		}
		if (againstMovement) {
			if (firstSuppressedTime == 0 || now - lastSuppressedTime > DEVIATION_END_INTERVAL) {
				firstSuppressedTime = now;
			}
			lastSuppressedTime = now;
			if (now - firstSuppressedTime <= MAX_SUPPRESSED_TIME) {
				return false;
			}
		} else {
			firstSuppressedTime = 0;
		}
		if (promptInterval > 0 && now - lastPromptTime < promptInterval) {
			return false;
		}
		lastPromptTime = now;
		promptInterval = promptInterval == 0
				? FIRST_PROMPT_INTERVAL
				: Math.min(MAX_PROMPT_INTERVAL, (long) (promptInterval * PROMPT_INTERVAL_FACTOR));
		return true;
	}

	void onRouteFollowed(long now) {
		if (followStartTime == 0) {
			followStartTime = now;
		}
		if (now - followStartTime >= DEVIATION_END_FOLLOW_TIME) {
			deviationEnded = true;
		}
	}

	void onRouteNotFollowed() {
		followStartTime = 0;
	}

	public static boolean isRouteAgainstMovement(@NonNull Location start, @NonNull RouteCalculationResult route) {
		List<Location> routeNodes = route.getImmutableAllLocations();
		int current = RoutingHelperUtils.lookAheadFindMinOrthogonalDistance(start, routeNodes, route.currentRoute, 15);
		if (current + 1 >= routeNodes.size()) {
			return false;
		}
		Location prev = route.getRouteLocationByDistance(-15);
		Location ahead = route.getRouteLocationByDistance(DIRECTION_CHECK_DISTANCE);
		if (ahead == null) {
			ahead = routeNodes.get(routeNodes.size() - 1);
		}
		return RoutingHelperUtils.checkWrongMovementDirection(start, prev, routeNodes.get(current + 1))
				&& RoutingHelperUtils.checkWrongMovementDirection(start, prev, ahead);
	}
}
