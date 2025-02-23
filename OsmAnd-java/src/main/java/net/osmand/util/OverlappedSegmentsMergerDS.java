package net.osmand.util;

import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KMapUtils;

import java.util.*;

public class OverlappedSegmentsMergerDS {

    private static final double PRECISION = KMapUtils.DEFAULT_LATLON_PRECISION;
    private static final double MAX_DISTANCE_METERS = 10.0;

    public static Track mergeSegmentsWithOverlapHandling(Track originalTrack) {
        List<TrkSegment> segments = new ArrayList<>(originalTrack.getSegments());
        List<TrkSegment> mergedSegments = new ArrayList<>();

        while (!segments.isEmpty()) {
            TrkSegment current = segments.remove(0);
            boolean merged = false;

            for (int i = 0; i < segments.size(); i++) {
                TrkSegment other = segments.get(i);
                TrkSegment mergedSegment = mergeSegments(current, other);

                if (mergedSegment != null) {
                    segments.set(i, mergedSegment);
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                mergedSegments.add(current);
            }
        }

        Track result = new Track();
        result.getSegments().addAll(mergedSegments);
        return result;
    }

    private static TrkSegment mergeSegments(TrkSegment a, TrkSegment b) {
        for (ConnectionType type : ConnectionType.values()) {
            TrkSegment merged = attemptMerge(a, b, type);
            if (merged != null) {
                return merged;
            }
        }
        return null;
    }

    private static TrkSegment attemptMerge(TrkSegment a, TrkSegment b, ConnectionType type) {
        List<WptPt> aPoints = type.reverseA ? reverse(a.getPoints()) : a.getPoints();
        List<WptPt> bPoints = type.reverseB ? reverse(b.getPoints()) : b.getPoints();

        // Проверка перекрытия конца A и начала B
        int overlap = findMaxOverlap(aPoints, bPoints);
        if (overlap > 0) {
            return buildMergedSegment(aPoints, bPoints, overlap);
        }
        return null;
    }

    private static int findMaxOverlap(List<WptPt> a, List<WptPt> b) {
        int maxOverlap = Math.min(a.size(), b.size());
        for (int overlap = maxOverlap; overlap >= 1; overlap--) {
            if (isValidOverlap(a, b, overlap)) {
                return overlap;
            }
        }
        return 0;
    }

    private static boolean isValidOverlap(List<WptPt> a, List<WptPt> b, int overlap) {
        List<WptPt> aPart = a.subList(a.size() - overlap, a.size());
        List<WptPt> bPart = b.subList(0, overlap);

        for (int i = 0; i < overlap; i++) {
            WptPt aPt = aPart.get(i);
            WptPt bPt = bPart.get(i);
            if (KMapUtils.INSTANCE.getDistance(
                    aPt.getLatitude(), aPt.getLongitude(),
                    bPt.getLatitude(), bPt.getLongitude()
            ) > MAX_DISTANCE_METERS) {
                return false;
            }
        }
        return true;
    }

    private static TrkSegment buildMergedSegment(List<WptPt> a, List<WptPt> b, int overlap) {
        List<WptPt> merged = new ArrayList<>(a.subList(0, a.size() - overlap));
        merged.addAll(b);
        return createSegment(merged);
    }

    private static TrkSegment createSegment(List<WptPt> points) {
        TrkSegment segment = new TrkSegment();
        segment.getPoints().addAll(points);
        return segment;
    }

    private static List<WptPt> reverse(List<WptPt> list) {
        List<WptPt> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        return reversed;
    }

    private enum ConnectionType {
        END_TO_START(false, false),
        END_TO_END(false, true),
        START_TO_END(true, false),
        START_TO_START(true, true);

        final boolean reverseA;
        final boolean reverseB;

        ConnectionType(boolean reverseA, boolean reverseB) {
            this.reverseA = reverseA;
            this.reverseB = reverseB;
        }
    }
}
