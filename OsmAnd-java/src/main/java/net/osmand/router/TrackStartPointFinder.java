package net.osmand.router;

import net.osmand.Location;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks the track point to start following a track from ("Point of the track to navigate = Nearest point").
 * <p>
 * A plain argmin over the whole track breaks round trips: near the start of a loop the last point is often
 * the nearest one, and nothing of the track is left to follow (#25428, #13851, #11717). Here the track is
 * split into passes near the position: at the track start the whole track wins, elsewhere the earliest pass,
 * or, when the position is moving, the pass on the road the position is on going the way it moves (#15013),
 * so a loop, an out-and-back or a figure-eight starts from its beginning instead of its end.
 */
public class TrackStartPointFinder {

	// same as the distance under which no connecting route segment is inserted
	private static final float SAME_PLACE_DISTANCE = 60;
	// on recalculation the start may be this far behind where the previous route was
	private static final float SEARCH_BACK_DISTANCE = 300;
	private static final float MIN_SPEED_FOR_BEARING = 1;
	private static final double MAX_BEARING_DIFF = 90;
	// a later pass beats an earlier one going the same way only when it fits the heading this much better,
	// so laps of a loop or a road driven twice the same way keep the earliest pass
	private static final double CLEARLY_BETTER_FIT = MAX_BEARING_DIFF / 2;
	// GPS error: a segment this much farther than the nearest one may still be the road the position is on
	private static final double SAME_ROAD_DISTANCE = 10;
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
		// passes as {first point, last point, nearest point}
		List<int[]> passes = new ArrayList<>();
		int passFirst = -1;
		int passNearest = -1;
		for (int i = first; i <= size; i++) {
			if (i == size || dist[i] > leaveDist) {
				// the track left the area (or ended): the pass so far is done
				if (passNearest != -1 && dist[passNearest] <= nearDist) {
					passes.add(new int[] {passFirst, i - 1, passNearest});
				}
				passFirst = -1;
				passNearest = -1;
			} else {
				if (passFirst == -1) {
					passFirst = i;
				}
				if (passNearest == -1 || dist[i] < dist[passNearest]) {
					passNearest = i;
				}
			}
		}
		// the road the position is on: the segments next to the nearest one, of any pass. A route has points only
		// at turns, so the segments to the points out of the area count too
		double roadDist = Double.MAX_VALUE;
		for (int[] pass : passes) {
			for (int s = Math.max(pass[0] - 1, 0); moving && s <= pass[1] && s < size - 1; s++) {
				roadDist = Math.min(roadDist, segmentDistance(s, position));
			}
		}
		roadDist += SAME_ROAD_DISTANCE;
		int best = -1;
		int bestRank = -1;
		double bestBearingDiff = 0;
		for (int[] pass : passes) {
			boolean reachesPrevious = !recalculation || toFinish[pass[1]] <= previousDistanceToFinish;
			// the segment of the pass on that road fitting the heading best; a pass not on that road does not go
			// the way the position moves. Only the visit of the road nearest to the position counts: a pass may come
			// by twice, e.g. both legs of a hairpin, and the bend between them leaves the road
			double bearingDiff = moving ? 180 : 0;
			int nearSeg = -1;
			double nearSegDist = Double.MAX_VALUE;
			for (int s = Math.max(pass[0] - 1, 0); moving && s <= pass[1] && s < size - 1; s++) {
				double d = segmentDistance(s, position);
				if (d < nearSegDist) {
					nearSegDist = d;
					nearSeg = s;
				}
			}
			int lo = nearSeg, hi = nearSeg;
			while (moving && lo > Math.max(pass[0] - 1, 0) && segmentDistance(lo - 1, position) <= roadDist) {
				lo--;
			}
			while (moving && hi < Math.min(pass[1], size - 2) && segmentDistance(hi + 1, position) <= roadDist) {
				hi++;
			}
			for (int s = lo; moving && s <= hi; s++) {
				// a repeated point has no direction of its own
				if (track.get(s).distanceTo(track.get(s + 1)) > 0 && segmentDistance(s, position) <= roadDist) {
					bearingDiff = Math.min(bearingDiff, bearingDiff(s, position));
				}
			}
			boolean sameWay = bearingDiff <= MAX_BEARING_DIFF;
			int rank = (reachesPrevious ? 2 : 0) + (sameWay ? 1 : 0);
			// the earliest pass of the best rank, unless a later one going the same way clearly fits better: next to
			// a turn the passes share it, but only one goes on the way the position moves
			if (rank > bestRank || (sameWay && rank == bestRank
					&& bearingDiff < bestBearingDiff - CLEARLY_BETTER_FIT)) {
				best = pass[2];
				bestRank = rank;
				bestBearingDiff = bearingDiff;
			}
		}
		return best;
	}

	private double segmentDistance(int segment, Location position) {
		Location a = track.get(segment);
		Location b = track.get(segment + 1);
		return MapUtils.getOrthogonalDistance(position.getLatitude(), position.getLongitude(),
				a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
	}

	// difference of the heading from the way the track goes on over MIN_BEARING_DISTANCE from where the position
	// projects onto the segment: a jittery recorded track is smoothed, and a pass turning away right there does
	// not go the way of a position moving on
	private double bearingDiff(int segment, Location position) {
		Location a = track.get(segment);
		Location b = track.get(segment + 1);
		double at = a.distanceTo(b) * MapUtils.getProjectionCoeff(position.getLatitude(), position.getLongitude(),
				a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
		Location from = along(segment, at);
		Location to = along(segment, at + MIN_BEARING_DISTANCE);
		// at the track end nothing is ahead: the segment itself is the way the track goes there
		double bearing = from.distanceTo(to) > 0 ? from.bearingTo(to) : a.bearingTo(b);
		double diff = Math.abs(bearing - position.getBearing()) % 360;
		return Math.min(diff, 360 - diff);
	}

	// the point of the track this far along it from the start of the segment, clamped to the track end
	private Location along(int segment, double offset) {
		while (segment < track.size() - 1 && offset >= track.get(segment).distanceTo(track.get(segment + 1))) {
			offset -= track.get(segment).distanceTo(track.get(segment + 1));
			segment++;
		}
		if (segment >= track.size() - 1) {
			return track.get(segment);
		}
		Location a = track.get(segment);
		Location b = track.get(segment + 1);
		double f = offset / a.distanceTo(b);
		return new Location("", a.getLatitude() + f * (b.getLatitude() - a.getLatitude()),
				a.getLongitude() + f * (b.getLongitude() - a.getLongitude()));
	}
}
