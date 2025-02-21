package net.osmand.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KMapUtils;

public class OverlappedSegmentsMergerGPT {

    /**
     * Processes the input Track as follows:
     * 1. Extracts polylines (lists of points) from the original segments.
     * 2. Attempts to merge pairs of polylines if there is an overlap at their boundaries.
     *    It determines the maximum number of consecutive points (overlap) that match at the junction
     *    and merges the segments so that the overlapping part appears only once.
     *    If necessary, one of the polylines is reversed to allow a proper connection.
     * 3. After merging, each resulting polyline is cleaned from self-intersections (loops),
     *    meaning that if a point appears again, the looped section is removed and only the first occurrence is kept.
     * 4. The final polylines are converted into segments and assembled into a new Track.
     *
     * In the final result, overlapping segments retain exactly one instance of the shared points,
     * while duplicate points are removed.
     *
     * @param track the input Track containing one or more segments
     * @return a new Track consisting of segments without duplicate overlaps and loops
     */
    private static final double PRECISION = KMapUtils.DEFAULT_LATLON_PRECISION;

    public static Track mergeSegmentsWithOverlapHandling(Track track) {
        // Step 1. Extract polylines from segments
        List<List<WptPt>> polylines = new ArrayList<>();
        for (TrkSegment seg : track.getSegments()) {
            List<WptPt> pts = new ArrayList<>(seg.getPoints());
            if (pts.size() >= 2) {
                polylines.add(pts);
            }
        }

        // Step 2. Merge polylines if there is an overlap at their boundaries.
        // Perform iterative merging until at least one pair is successfully merged.
        boolean mergedSomething = true;
        while (mergedSomething) {
            mergedSomething = false;
            outer:
            for (int i = 0; i < polylines.size(); i++) {
                List<WptPt> poly1 = polylines.get(i);
                for (int j = i + 1; j < polylines.size(); j++) {
                    List<WptPt> poly2 = polylines.get(j);
                    List<WptPt> merged = mergePolylines(poly1, poly2);
                    if (merged != null) {
                        polylines.set(i, merged);
                        polylines.remove(j);
                        mergedSomething = true;
                        break outer;
                    }
                }
            }
        }

        // Step 3. Remove self-intersections (loops) in each resulting polyline.
        List<List<WptPt>> finalPolylines = new ArrayList<>();
        for (List<WptPt> poly : polylines) {
            List<WptPt> cleaned = removeSelfLoops(poly);
            if (cleaned.size() >= 2) {
                finalPolylines.add(cleaned);
            }
        }

        // Step 4. Build a new Track from the resulting polylines.
        Track resultTrack = new Track();
        for (List<WptPt> poly : finalPolylines) {
            TrkSegment seg = new TrkSegment();
            seg.getPoints().clear();
            seg.getPoints().addAll(poly);
            seg.setGeneralSegment(false);
            resultTrack.getSegments().add(seg);
        }
        return resultTrack;
    }

    /**
     * Attempts to merge two polylines if their boundaries overlap.
     * The method considers the following cases:
     * - Consecutive matching of poly1's end with poly2's start.
     * - Consecutive matching of poly2's end with poly1's start.
     * - If needed, reversing one of the polylines to obtain a proper match.
     * If an overlap (of length >= 1) is found, the merged polyline is returned; otherwise, null is returned.
     */
    private static List<WptPt> mergePolylines(List<WptPt> poly1, List<WptPt> poly2) {
        // Option 1: merge if the end of poly1 equals the start of poly2
        int overlap = getMaxOverlap(poly1, poly2);
        if (overlap >= 1) {
            return mergeBySuffixPrefix(poly1, poly2, overlap);
        }
        // Option 2: if the end of poly2 equals the start of poly1
        overlap = getMaxOverlap(poly2, poly1);
        if (overlap >= 1) {
            return mergeBySuffixPrefix(poly2, poly1, overlap);
        }
        // Option 3: if the start of poly1 equals the start of poly2, try reversing poly1
        List<WptPt> rev1 = new ArrayList<>(poly1);
        Collections.reverse(rev1);
        overlap = getMaxOverlap(rev1, poly2);
        if (overlap >= 1) {
            return mergeBySuffixPrefix(rev1, poly2, overlap);
        }
        // Option 4: if the end of poly1 equals the end of poly2, try reversing poly2
        List<WptPt> rev2 = new ArrayList<>(poly2);
        Collections.reverse(rev2);
        overlap = getMaxOverlap(poly1, rev2);
        if (overlap >= 1) {
            return mergeBySuffixPrefix(poly1, rev2, overlap);
        }
        return null;
    }

    /**
     * Computes the maximum number of points (L) such that the last L points of list A match
     * the first L points of list B.
     */
    private static int getMaxOverlap(List<WptPt> A, List<WptPt> B) {
        int maxPossible = Math.min(A.size(), B.size());
        for (int L = maxPossible; L >= 1; L--) {
            boolean match = true;
            for (int i = 0; i < L; i++) {
                if (!pointsEqual(A.get(A.size() - L + i), B.get(i))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return L;
            }
        }
        return 0;
    }

    /**
     * Merges two polylines, assuming that the last L points of the first polyline match the first L points
     * of the second polyline. The result is the first polyline with the second polyline appended starting at index L,
     * thus avoiding duplication of the overlapping segment.
     */
    private static List<WptPt> mergeBySuffixPrefix(List<WptPt> first, List<WptPt> second, int overlap) {
        List<WptPt> merged = new ArrayList<>(first);
        // Append points from the second polyline starting at index 'overlap'
        for (int i = overlap; i < second.size(); i++) {
            merged.add(second.get(i));
        }
        return merged;
    }

    /**
     * Removes self-intersections (loops) from a polyline.
     * If a point appears again (already present in the polyline), the intermediate section forming the loop is removed,
     * leaving only the first occurrence.
     *
     * For example, if poly = [A, B, C, D, B, E],
     * the result will be [A, B, E] (the loop from B through C, D back to B is removed).
     */
    private static List<WptPt> removeSelfLoops(List<WptPt> poly) {
        List<WptPt> result = new ArrayList<>();
        for (WptPt pt : poly) {
            int index = indexOfPoint(result, pt);
            if (index != -1) {
                // Remove all points after the first occurrence, keeping that occurrence
                result = new ArrayList<>(result.subList(0, index + 1));
            } else {
                result.add(pt);
            }
        }
        return result;
    }

    /**
     * Returns the index of the first point in the list that is equal to pt (using areLatLonEqual),
     * or -1 if no such point is found.
     */
    private static int indexOfPoint(List<WptPt> list, WptPt pt) {
        for (int i = 0; i < list.size(); i++) {
            if (pointsEqual(list.get(i), pt)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Compares two WptPt points using the library method areLatLonEqual.
     */
    private static boolean pointsEqual(WptPt a, WptPt b) {
        KLatLon p1 = new KLatLon(a.getLatitude(), a.getLongitude());
        KLatLon p2 = new KLatLon(b.getLatitude(), b.getLongitude());
        return KMapUtils.INSTANCE.areLatLonEqual(p1, p2, PRECISION);
    }
}
