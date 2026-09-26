package net.osmand.router;

import net.osmand.Location;

import java.util.List;

/**
 * Picks the track point to start following a track from ("Point of the track to navigate = Nearest point").
 * <p>
 * A plain argmin over the whole track breaks round trips: near the start of a loop the last point is often
 * the nearest one, and nothing of the track is left to follow (#25428, #13851, #11717). Here the track is
 * split into passes near the position: at the track start the whole track wins, elsewhere the earliest pass,
 * or, when the position is moving, the earliest pass going the same way (#15013), so a loop, an out-and-back
 * or a figure-eight starts from its beginning instead of its end.
 */
public class TrackStartPointFinder {

	// same as the distance under which no connecting route segment is inserted
	private static final float SAME_PLACE_DISTANCE = 60;
	// on recalculation the start may be this far behind where the previous route was
	private static final float SEARCH_BACK_DISTANCE = 300;
	private static final float MIN_SPEED_FOR_BEARING = 1;
	private static final double MAX_BEARING_DIFF = 90;
	// neighbouring points of a recorded track jitter by a few metres
	private static final float MIN_BEARING_DISTANCE = 20;

	private final List<Location> track;
	// route points are joined by routes calculated later, the bearing between two of them is not the way to go
	private final boolean routePoints;

	public TrackStartPointFinder(List<Location> track) {
		this(track, false);
	}

	public TrackStartPointFinder(List<Location> track, boolean routePoints) {
		this.track = track;
		this.routePoints = routePoints;
	}

	/**
	 * @param previousDistanceToFinish on recalculation what was left of the previous route: passes that get that
	 *                                 far are preferred, nothing more than SEARCH_BACK_DISTANCE behind it is
	 *                                 considered; a negative value when there is no previous route
	 * @return index of the point to start from, 0 for an empty track or no position
	 */
	public int findStartIndex(Location position, float previousDistanceToFinish) {
		int size = track.size();
		if (position == null || size == 0) {
			return 0;
		}
		boolean recalculation = previousDistanceToFinish >= 0;
		float[] toFinish = recalculation ? new float[size] : null;
		int first = 0;
		if (recalculation) {
			first = size - 1;
			while (first > 0) {
				float d = toFinish[first] + track.get(first - 1).distanceTo(track.get(first));
				if (d > previousDistanceToFinish + SEARCH_BACK_DISTANCE) {
					break;
				}
				toFinish[--first] = d;
			}
		}
		float[] dist = new float[size];
		float minDist = Float.MAX_VALUE;
		for (int i = first; i < size; i++) {
			dist[i] = track.get(i).distanceTo(position);
			minDist = Math.min(minDist, dist[i]);
		}
		// the tail of a loop or an out-and-back passes the start too: whichever way the position moves, start from
		// the beginning, even of a loop that never leaves the area, unless the recalculation window excludes it
		if (first == 0 && dist[0] <= SAME_PLACE_DISTANCE) {
			return 0;
		}
		boolean moving = !routePoints && position.hasBearing()
				&& (!position.hasSpeed() || position.getSpeed() >= MIN_SPEED_FOR_BEARING);
		float nearDist = minDist + SAME_PLACE_DISTANCE;
		float leaveDist = nearDist + SAME_PLACE_DISTANCE;
		int best = -1;
		int bestRank = -1;
		int passNearest = -1;
		for (int i = first; i <= size; i++) {
			if (i == size || dist[i] > leaveDist) {
				// the track left the area (or ended): the pass so far is done
				if (passNearest != -1 && dist[passNearest] <= nearDist) {
					boolean reachesPrevious = !recalculation || toFinish[i - 1] <= previousDistanceToFinish;
					boolean sameWay = !moving || sameDirection(passNearest, position);
					int rank = (reachesPrevious ? 2 : 0) + (sameWay ? 1 : 0);
					if (rank == 3) {
						return passNearest;
					}
					if (rank > bestRank) {
						best = passNearest;
						bestRank = rank;
					}
				}
				passNearest = -1;
			} else if (passNearest == -1 || dist[i] < dist[passNearest]) {
				passNearest = i;
			}
		}
		// no pass both gets as far as the previous route and goes the way the position moves
		return best;
	}

	private boolean sameDirection(int index, Location position) {
		int from = index;
		int to = index;
		while (to < track.size() - 1 && track.get(from).distanceTo(track.get(to)) < MIN_BEARING_DISTANCE) {
			to++;
		}
		while (from > 0 && track.get(from).distanceTo(track.get(to)) < MIN_BEARING_DISTANCE) {
			from--;
		}
		if (from == to) {
			return true;
		}
		double diff = Math.abs(track.get(from).bearingTo(track.get(to)) - position.getBearing()) % 360;
		return Math.min(diff, 360 - diff) <= MAX_BEARING_DIFF;
	}
}
