package net.osmand.util;

import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KMapUtils;

import java.util.*;

public class AdvancedTrackMerger {

    private static final double PRECISION = KMapUtils.HIGH_LATLON_PRECISION;

    public static Track mergeSegmentsWithOverlapHandling(Track originalTrack) {
        List<TrkSegment> originalSegments = new ArrayList<>(originalTrack.getSegments());
        List<TrkSegment> mergedSegments = mergeSegments(originalSegments);

        Track resultTrack = new Track();
        resultTrack.getSegments().addAll(mergedSegments);
        return resultTrack;
    }

    private static List<TrkSegment> mergeSegments(List<TrkSegment> segments) {
        List<TrkSegment> workList = new ArrayList<>(segments);
        boolean changed;

        do {
            changed = false;
            for (int i = 0; i < workList.size(); i++) {
                TrkSegment current = workList.get(i);
                if (current == null || current.getPoints().size() < 2) continue;

                for (int j = 0; j < workList.size(); j++) {
                    if (i == j) continue;
                    TrkSegment other = workList.get(j);
                    if (other == null || other.getPoints().size() < 2) continue;

                    TrkSegment merged = tryMerge(current, other);
                    if (merged != null && isValidSegment(merged)) {
                        workList.set(i, merged);
                        workList.set(j, null);
                        changed = true;
                        break;
                    }
                }
            }
            workList.removeIf(Objects::isNull);
        } while (changed);

        return workList;
    }

    private static TrkSegment tryMerge(TrkSegment a, TrkSegment b) {
        for (ConnectionType type : ConnectionType.values()) {
            TrkSegment merged = attemptMerge(a, b, type);
            if (merged != null && isValidSegment(merged)) return merged;
        }
        return null;
    }

    private static TrkSegment attemptMerge(TrkSegment a, TrkSegment b, ConnectionType type) {
        List<WptPt> aPoints = type.reverseA ? reverse(a.getPoints()) : a.getPoints();
        List<WptPt> bPoints = type.reverseB ? reverse(b.getPoints()) : b.getPoints();

        int overlap = findValidOverlap(aPoints, bPoints);
        if (overlap > 0) {
            List<WptPt> merged = new ArrayList<>(aPoints.subList(0, aPoints.size() - overlap));
            merged.addAll(bPoints);
            return createSegment(merged);
        }
        return null;
    }

    private static int findValidOverlap(List<WptPt> a, List<WptPt> b) {
        for (int overlap = Math.min(a.size(), b.size()); overlap > 0; overlap--) {
            if (isEdgeOverlap(a, b, overlap) && !createsLoop(a, b, overlap)) {
                return overlap;
            }
        }
        return 0;
    }

    private static boolean isEdgeOverlap(List<WptPt> a, List<WptPt> b, int overlap) {
        List<WptPt> aPart = a.subList(a.size() - overlap, a.size());
        List<WptPt> bPart = b.subList(0, overlap);
        return isOverlap(aPart, bPart);
    }

    private static boolean createsLoop(List<WptPt> a, List<WptPt> b, int overlap) {
        // Проверка, что объединение не создаёт петлю
        WptPt firstAfterMerge = a.get(a.size() - overlap - 1);
        WptPt lastAfterMerge = b.get(overlap);
        return equals(firstAfterMerge, lastAfterMerge);
    }

    private static boolean isOverlap(List<WptPt> aPart, List<WptPt> bPart) {
        for (int i = 0; i < aPart.size(); i++) {
            if (!equals(aPart.get(i), bPart.get(i))) return false;
        }
        return true;
    }

    private static boolean isValidSegment(TrkSegment segment) {
        List<WptPt> points = segment.getPoints();
        if (points.size() < 2) return false;

        // Проверка на петлю внутри сегмента
        for (int i = 1; i < points.size(); i++) {
            if (equals(points.get(i-1), points.get(i))) return false;
        }
        return true;
    }

    private static boolean equals(WptPt p1, WptPt p2) {
        return KMapUtils.INSTANCE.areLatLonEqual(
                p1.getLatitude(), p1.getLongitude(),
                p2.getLatitude(), p2.getLongitude(),
                PRECISION
        );
    }

    private static List<WptPt> reverse(List<WptPt> list) {
        List<WptPt> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        return reversed;
    }

    private static TrkSegment createSegment(List<WptPt> points) {
        TrkSegment segment = new TrkSegment();
        segment.getPoints().addAll(points);
        return segment;
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