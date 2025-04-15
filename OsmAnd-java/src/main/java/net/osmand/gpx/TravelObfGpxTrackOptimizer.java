package net.osmand.gpx;

import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KMapUtils;

import java.util.*;

// Human-based version of OverlappedSegmentsMergerDS / OverlappedSegmentsMergerGPT (commit 17701568cb)

public class TravelObfGpxTrackOptimizer {
	private static final int MAX_JUMPS_OVER_UNIQUE_POINTS = 5;
	private static final double EDGE_POINTS_MAX_ORTHOGONAL_DISTANCE = 10.0;
	private static final double PRECISION_DUPES = KMapUtils.DEFAULT_LATLON_PRECISION;
	private static final double PRECISION_EQUAL = KMapUtils.DEFAULT_LATLON_PRECISION; // ~1 meter
	private static final double PRECISION_CLOSE = KMapUtils.DEFAULT_LATLON_PRECISION * 50; // ~50 meters

	public static Track mergeOverlappedSegmentsAtEdges(Track track) {
		Set<String> duplicates = new HashSet<>();
		findDisplacedEdgePointsToDeduplicate(track, duplicates);

		List<TrkSegment> cleanedSegments = new ArrayList<>();
		deduplicatePointsFromEdges(track, duplicates, cleanedSegments);

		List<TrkSegment> joinedSegments = new ArrayList<>();
		joinCleanedSegments(cleanedSegments, joinedSegments);

		Track joinedTrack = new Track();
		joinedTrack.setSegments(joinedSegments);
		return joinedTrack;
	}

	private static void findDisplacedEdgePointsToDeduplicate(Track track, Set<String> duplicates) {
		Set<WptPt> edgePoints = new HashSet<>();
		for (TrkSegment seg : track.getSegments()) {
			List<WptPt> points = seg.getPoints();
			if (!points.isEmpty()) {
				edgePoints.add(points.get(0));
				edgePoints.add(points.get(points.size() - 1));
			}
		}
		if (!edgePoints.isEmpty()) {
			for (TrkSegment seg : track.getSegments()) {
				List<WptPt> points = seg.getPoints();
				for (int i = 1; i < points.size(); i++) {
					WptPt p1 = points.get(i);
					WptPt p2 = points.get(i - 1);
					searchEdgePointsDuplicates(duplicates, edgePoints, p1, p2);
				}
			}
		}
	}

	private static void searchEdgePointsDuplicates(Set<String> duplicates, Set<WptPt> edgePoints, WptPt p1, WptPt p2) {
		for (WptPt edge : edgePoints) {
			double coeff = KMapUtils.INSTANCE.
					getProjectionCoeff(edge.getLatitude(), edge.getLongitude(),
							p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
			if (coeff > 0.0 && coeff < 1.0) {
				double dist = KMapUtils.INSTANCE.
						getOrthogonalDistance(edge.getLatitude(), edge.getLongitude(),
								p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
				if (dist > 0 && dist < EDGE_POINTS_MAX_ORTHOGONAL_DISTANCE) {
					duplicates.add(llKey(edge));
				}
			}
		}
	}

	private static void deduplicatePointsFromEdges(Track track, Set<String> duplicates,
	                                               List<TrkSegment> cleanedSegments) {
		for (TrkSegment seg : track.getSegments()) {
			TrkSegment clean = new TrkSegment();
			List<WptPt> points = seg.getPoints();
			if (!points.isEmpty()) {
				int fromIndex = 0, toIndex = points.size() - 1; // inclusive indexes

				markDanglingEdgePointsToDeduplicate(points, duplicates);

				fromIndex += countDuplicates(true, fromIndex, toIndex, points, duplicates);
				toIndex -= countDuplicates(false, fromIndex, toIndex, points, duplicates);

				if (fromIndex < toIndex) {
					clean.getPoints().addAll(points.subList(fromIndex, toIndex + 1));
				}

				// edges [0, -1] must not be considered as future duplicates
				for (int i = 1; i < points.size() - 1; i++) {
					String key = llKey(points.get(i));
					duplicates.add(key);
				}
			}
			if (!clean.getPoints().isEmpty()) {
				cleanedSegments.add(clean);
			}
		}
	}

	private static int countDuplicates(boolean forward, int fromIndex, int toIndex,
	                                   List<WptPt> points, Set<String> duplicates) {
		int dupes = 0, uniques = 0;
		int a = forward ? fromIndex : toIndex;
		int b = forward ? toIndex : fromIndex;
		for (int i = a; i != b; i += forward ? +1 : -1) {
			if (duplicates.contains(llKey(points.get(i)))) {
				dupes += uniques + 1; // jumped over
				uniques = 0;
			} else {
				if (++uniques > MAX_JUMPS_OVER_UNIQUE_POINTS) {
					break;
				}
			}
		}

		return dupes > 1 ? dupes : 0; // keep solitary duplicate at the edge
	}

	private static void markDanglingEdgePointsToDeduplicate(List<WptPt> points, Set<String> duplicates) {
		if (points.size() > 1) {
			if (duplicates.contains(llKey(points.get(1)))) {
				duplicates.add(llKey(points.get(0)));
			}
			if (duplicates.contains(llKey(points.get(points.size() - 2)))) {
				duplicates.add(llKey(points.get(points.size() - 1)));
			}
		}
	}

	private static void joinCleanedSegments(List<TrkSegment> segmentsToJoin, List<TrkSegment> joinedSegments) {
		boolean[] done = new boolean[segmentsToJoin.size()];
		while (true) {
			List<WptPt> result = new ArrayList<>();
			for (int i = 0; i < segmentsToJoin.size(); i++) {
				if (!done[i]) {
					done[i] = true;
					if (!segmentsToJoin.get(i).getPoints().isEmpty()) {
						addSegmentToResult(result, false, segmentsToJoin.get(i), false); // "head" segment
						while (true) {
							boolean stop = true;
							for (int j = 0; j < segmentsToJoin.size(); j++) {
								if (!done[j] && considerSegmentToJoin(result, segmentsToJoin.get(j))) {
									done[j] = true;
									stop = false;
								}
							}
							if (stop) {
								break; // nothing joined
							}
						}
						break; // segment is done
					}
				}
			}
			if (result.isEmpty()) {
				break; // all done
			}
			TrkSegment joined = new TrkSegment();
			joined.getPoints().addAll(result);
			joinedSegments.add(joined);
		}
	}

	private static void addSegmentToResult(List<WptPt> result, boolean insert, TrkSegment segment, boolean reverse) {
		List<WptPt> points = new ArrayList<>();
		for (WptPt wpt : segment.getPoints()) {
			points.add(new WptPt(wpt.getLatitude(), wpt.getLongitude()));
		}
		if (reverse) {
			Collections.reverse(points);
		}
		if (insert) {
			boolean skipTrailingPoint = !result.isEmpty() && !points.isEmpty()
					&& equalWptPts(points.get(points.size() - 1), result.get(0));
			result.addAll(0, points.subList(0, points.size() - (skipTrailingPoint ? 1 : 0))); // insert
		} else {
			boolean skipLeadingPoint = !result.isEmpty() && !points.isEmpty()
					&& equalWptPts(points.get(0), result.get(result.size() - 1));
			result.addAll(result.size(), points.subList(skipLeadingPoint ? 1 : 0, points.size())); // append
		}
	}

	private static boolean considerSegmentToJoin(List<WptPt> result, TrkSegment candidate) {
		if (result.isEmpty()) {
			return false;
		}

		if (candidate.getPoints().isEmpty()) {
			return true;
		}

		WptPt firstPoint = result.get(0);
		WptPt lastPoint = result.get(result.size() - 1);
		WptPt firstCandidate = candidate.getPoints().get(0);
		WptPt lastCandidate = candidate.getPoints().get(candidate.getPoints().size() - 1);

		boolean avoidClosedLoop = (result.size() > 1 && equalWptPts(firstPoint, lastPoint))
				|| (candidate.getPoints().size() > 1 && equalWptPts(firstCandidate, lastCandidate));

		if (avoidClosedLoop) {
			return false;
		} else if (closeWptPts(lastPoint, firstCandidate)) {
			addSegmentToResult(result, false, candidate, false); // result + Candidate
		} else if (closeWptPts(lastPoint, lastCandidate)) {
			addSegmentToResult(result, false, candidate, true); // result + etadidnaC
		} else if (closeWptPts(firstPoint, firstCandidate)) {
			addSegmentToResult(result, true, candidate, true); // etadidnaC + result
		} else if (closeWptPts(firstPoint, lastCandidate)) {
			addSegmentToResult(result, true, candidate, false); // Candidate + result
		} else {
			return false;
		}

		return true;
	}

	private static boolean equalWptPts(WptPt p1, WptPt p2) {
		return KMapUtils.INSTANCE.areLatLonEqual(
				p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), PRECISION_EQUAL);
	}

	private static boolean closeWptPts(WptPt p1, WptPt p2) {
		return KMapUtils.INSTANCE.areLatLonEqual(
				p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), PRECISION_CLOSE);
	}

	private static String llKey(WptPt edge) {
		return (int) (edge.getLatitude() / PRECISION_DUPES) + "," + (int) (edge.getLongitude() / PRECISION_DUPES);
	}
}
