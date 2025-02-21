package net.osmand.gpx;

import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KMapUtils;

import java.util.*;

// Human-based version of OverlappedSegmentsMergerDS / OverlappedSegmentsMergerGPT

public class GpxOptimizer {
	private static final double MAX_ORTHOGONAL_DISTANCE = 10.0;
	private static final double PRECISION = KMapUtils.DEFAULT_LATLON_PRECISION;

	public static Track deduplicateAndSpliceTrackSegments(Track track) {
		Set<String> duplicates = new HashSet<>();
		findDisplacedEdgePointsToDeduplicate(track, duplicates);

		List<TrkSegment> cleanSegments = new ArrayList<>();
		deduplicatePointsToCleanSegments(track, duplicates, cleanSegments);

		Track cleanTrack = new Track();
		cleanTrack.setSegments(cleanSegments);
		return cleanTrack;
	}

	private static void deduplicatePointsToCleanSegments(Track track, Set<String> duplicates,
	                                                     List<TrkSegment> cleanSegments) {
		for (TrkSegment seg : track.getSegments()) {
			TrkSegment cleanSeg = new TrkSegment();
			List<WptPt> points = seg.getPoints();
			if (!points.isEmpty()) {
				Set<String> prepareDuplicates = new HashSet<>();
				for (int i = 0; i < points.size(); i++) {
					WptPt p = points.get(i);
					String key = llKey(p);
					if (!duplicates.contains(key)) {
						cleanSeg.getPoints().add(p);
						if (i != 0 && i != points.size() - 1) {
							prepareDuplicates.add(key); // don't deduplicate itself
						}
					}
				}
				duplicates.addAll(prepareDuplicates);
			}
			if (!cleanSeg.getPoints().isEmpty()) {
				cleanSegments.add(cleanSeg);
			}
		}
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
							if (d > 0 && d < MAX_ORTHOGONAL_DISTANCE) {
								duplicates.add(llKey(edge));
							}
						}
					}
				}
			}
		}
	}

	private static String llKey(WptPt edge) {
		return (int) (edge.getLatitude() / PRECISION) + "," + (int) (edge.getLongitude() / PRECISION);
	}

}