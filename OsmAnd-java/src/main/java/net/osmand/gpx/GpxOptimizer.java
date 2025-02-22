package net.osmand.gpx;

import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KMapUtils;

import java.util.*;

// Human-based version of OverlappedSegmentsMergerDS / OverlappedSegmentsMergerGPT

public class GpxOptimizer {
	private static final double EDGE_POINTS_MAX_ORTHOGONAL_DISTANCE = 10.0;
	private static final double PRECISION = KMapUtils.DEFAULT_LATLON_PRECISION;

	public static Track deduplicateAndJoinTrackSegments(Track track) {
		Set<String> duplicates = new HashSet<>();
		findDisplacedEdgePointsToDeduplicate(track, duplicates);

		List<TrkSegment> cleanedSegments = new ArrayList<>();
		deduplicatePointsToCleanedSegments(track, duplicates, cleanedSegments);

		List<TrkSegment> joinedSegments = new ArrayList<>();
		joinCleanedSegments(cleanedSegments, joinedSegments);

		Track joinedTrack = new Track();
		joinedTrack.setSegments(joinedSegments);
		return joinedTrack;
	}

	private static String llKey(WptPt edge) {
		return (int) (edge.getLatitude() / PRECISION) + "," + (int) (edge.getLongitude() / PRECISION); // String is fast
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
				if (points.size() > 1) {
					for (int i = 1; i < points.size(); i++) {
						WptPt p1 = points.get(i);
						WptPt p2 = points.get(i - 1);
						for (WptPt edge : edgePoints) {
							double d = KMapUtils.INSTANCE.getOrthogonalDistance(edge.getLatitude(), edge.getLongitude(),
									p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
							if (d > 0 && d < EDGE_POINTS_MAX_ORTHOGONAL_DISTANCE) {
								duplicates.add(llKey(edge));
							}
						}
					}
				}
			}
		}
	}

	private static void deduplicatePointsToCleanedSegments(Track track, Set<String> duplicates,
	                                                       List<TrkSegment> cleanedSegments) {
		for (TrkSegment seg : track.getSegments()) {
			TrkSegment clean = new TrkSegment();
			List<WptPt> points = seg.getPoints();
			if (!points.isEmpty()) {
				Set<String> nextDuplicates = new HashSet<>();
				for (int i = 0; i < points.size(); i++) {
					WptPt p = points.get(i);
					String key = llKey(p);
					if (!duplicates.contains(key)) {
						if (!isPossiblyDisplacedEdgePoint(i, points, duplicates)) {
							clean.getPoints().add(p);
						}
						if (i != 0 && i != points.size() - 1) {
							nextDuplicates.add(key);
						}
					}
				}
				duplicates.addAll(nextDuplicates);
			}
			if (!clean.getPoints().isEmpty()) {
				cleanedSegments.add(clean);
			}
		}
	}

	private static boolean isPossiblyDisplacedEdgePoint(int i, List<WptPt> points, Set<String> duplicates) {
		// fast fallback method if findDisplacedEdgePointsToDeduplicate has failed
		return (i == 0 && points.size() > 1 && duplicates.contains(llKey(points.get(i + 1))))
				|| (i == points.size() - 1 && points.size() > 1 && duplicates.contains(llKey(points.get(i - 1))));
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
		if (!result.isEmpty() && !points.isEmpty()) {
			List<WptPt> skipLeadingPoint = points.subList(insert ? 0 : 1, points.size() - (insert ? 1 : 0));
			result.addAll(insert ? 0 : result.size(), skipLeadingPoint); // avoid duplicate point at joints
		} else {
			result.addAll(insert ? 0 : result.size(), points); // first addition to the result
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

		if (equalWptPt(lastPoint, firstCandidate)) {
			addSegmentToResult(result, false, candidate, false); // nodes + Candidate
		} else if (equalWptPt(lastPoint, lastCandidate)) {
			addSegmentToResult(result, false, candidate, true); // nodes + etadidnaC
		} else if (equalWptPt(firstPoint, firstCandidate)) {
			addSegmentToResult(result, true, candidate, true); // etadidnaC + nodes
		} else if (equalWptPt(firstPoint, lastCandidate)) {
			addSegmentToResult(result, true, candidate, false); // Candidate + nodes
		} else {
			return false;
		}

		return true;
	}

	private static boolean equalWptPt(WptPt p1, WptPt p2) {
		return KMapUtils.INSTANCE.
				areLatLonEqual(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), PRECISION);
	}
}
