package net.osmand.router;

import net.osmand.Location;

import java.util.List;

/**
 * Picks the track point to start following a track from ("Point of the track to navigate = Nearest point").
 * <p>
 * A plain argmin over the whole track breaks round trips: near the start of a loop the last point is often
 * the nearest one, and nothing of the track is left to follow (#25428, #13851, #11717). Here the track is
 * split into passes near the position: the earliest pass wins, or, when the position is moving, the earliest
 * pass going the same way (#15013), so a loop, an out-and-back or a figure-eight starts from its beginning
 * instead of its end.
 */
public class TrackStartPointFinder {

	// same as the distance under which no connecting route segment is inserted
	public static final float SAME_PLACE_DISTANCE = 60;
	private static final float MIN_SPEED_FOR_BEARING = 1;
	private static final double MAX_BEARING_DIFF = 90;

	/**
	 * @param maxDistanceToFinish only points whose distance along the track to its end is at most this are
	 *                            considered (on recalculation: what was left of the previous route), or
	 *                            a negative value for no limit
	 * @return index of the point to start from, 0 for an empty track or no position
	 */
	public static int findStartIndex(List<Location> track, Location position, float maxDistanceToFinish) {
		int size = track.size();
		if (position == null || size == 0) {
			return 0;
		}
		int first = 0;
		if (maxDistanceToFinish >= 0) {
			float toFinish = 0;
			first = size - 1;
			while (first > 0) {
				toFinish += track.get(first - 1).distanceTo(track.get(first));
				if (toFinish > maxDistanceToFinish) {
					break;
				}
				first--;
			}
		}
		float[] dist = new float[size];
		float minDist = Float.MAX_VALUE;
		for (int i = first; i < size; i++) {
			dist[i] = track.get(i).distanceTo(position);
			minDist = Math.min(minDist, dist[i]);
		}
		boolean moving = position.hasBearing() && (!position.hasSpeed() || position.getSpeed() >= MIN_SPEED_FOR_BEARING);
		// standing at the start of the track means following the whole track, even a loop that never leaves the area
		boolean atTrackStart = first == 0 && dist[0] <= SAME_PLACE_DISTANCE && (!moving || sameDirection(track, 0, position));
		float nearDist = minDist + SAME_PLACE_DISTANCE;
		float leaveDist = nearDist + SAME_PLACE_DISTANCE;
		int earliest = -1;
		int passStart = first;
		int passNearest = -1;
		for (int i = first; i <= size; i++) {
			if (i == size || dist[i] > leaveDist) {
				// the track left the area (or ended): the pass so far is done
				if (passNearest != -1) {
					int candidate = passStart == 0 && atTrackStart ? 0 : passNearest;
					if (dist[candidate] <= nearDist) {
						if (!moving || sameDirection(track, candidate, position)) {
							return candidate;
						}
						if (earliest == -1) {
							earliest = candidate;
						}
					}
				}
				passNearest = -1;
			} else {
				if (passNearest == -1) {
					passStart = i;
				}
				if (passNearest == -1 || dist[i] < dist[passNearest]) {
					passNearest = i;
				}
			}
		}
		// no pass goes the way the position moves
		return earliest;
	}

	private static boolean sameDirection(List<Location> track, int index, Location position) {
		int from = index < track.size() - 1 ? index : index - 1;
		if (from < 0) {
			return true;
		}
		Location a = track.get(from);
		Location b = track.get(from + 1);
		double diff = Math.abs(a.bearingTo(b) - position.getBearing()) % 360;
		return Math.min(diff, 360 - diff) <= MAX_BEARING_DIFF;
	}
}
