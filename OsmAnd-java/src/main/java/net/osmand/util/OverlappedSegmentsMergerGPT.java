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

    private static final double PRECISION = KMapUtils.DEFAULT_LATLON_PRECISION;
    private static final double MAX_OVERLAP_DISTANCE = 10.0;

    /**
     * Merges segments with overlap handling.
     * <p>
     * The method extracts polylines (lists of points) from the original segments,
     * then iteratively merges polylines that overlap at their boundaries.
     * The maximum overlap (i.e. the maximum number of consecutive points that match at the junction)
     * is determined and used so that the overlapping part appears only once.
     * If needed, one of the polylines is reversed to obtain a proper match.
     * After merging, self-intersections (loops) are removed from each polyline.
     * Finally, the resulting polylines are converted back to segments and assembled into a new Track.
     *
     * @param track the input Track containing one or more segments
     * @return a new Track consisting of segments merged with proper overlap handling
     */
    public static Track mergeSegmentsWithOverlapHandling(Track track) {
        // Step 1. Extract polylines from segments.
        List<List<WptPt>> polylines = new ArrayList<>();
        for (TrkSegment seg : track.getSegments()) {
            List<WptPt> pts = new ArrayList<>(seg.getPoints());
            if (pts.size() >= 2) {
                polylines.add(pts);
            }
        }

        // Step 2. Iteratively merge polylines if their boundaries overlap.
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

        // Step 3. Remove self-intersections (loops) from each polyline.
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
     * The method considers these cases:
     * - The end of poly1 matches the start of poly2.
     * - The end of poly2 matches the start of poly1.
     * - If needed, one of the polylines is reversed to obtain a proper match.
     * If an overlap (of length >= 1) is found, the merged polyline is returned; otherwise, null is returned.
     */
    private static List<WptPt> mergePolylines(List<WptPt> poly1, List<WptPt> poly2) {
        // Option 1: merge if the end of poly1 equals the start of poly2
        int overlap = getMaxOverlap(poly1, poly2);
        if (overlap >= 1) {
            return mergeBySuffixPrefix(poly1, poly2, overlap);
        }
        // Option 2: merge if the end of poly2 equals the start of poly1
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
     * For the first and last points of the overlapping region, if the points do not exactly match,
     * an additional fuzzy check using orthogonal distance (max MAX_OVERLAP_DISTANCE meters) is applied.
     */
    private static int getMaxOverlap(List<WptPt> A, List<WptPt> B) {
        int maxPossible = Math.min(A.size(), B.size());
        for (int L = maxPossible; L >= 1; L--) {
            boolean match = true;
            for (int i = 0; i < L; i++) {
                WptPt a = A.get(A.size() - L + i);
                WptPt b = B.get(i);
                KLatLon p1 = new KLatLon(a.getLatitude(), a.getLongitude());
                KLatLon p2 = new KLatLon(b.getLatitude(), b.getLongitude());
                if (i == 0) {
                    if (!KMapUtils.INSTANCE.areLatLonEqual(p1, p2, PRECISION)) {
                        if (B.size() >= 2) {
                            WptPt b1 = B.get(0);
                            WptPt b2 = B.get(1);
                            double d = KMapUtils.INSTANCE.getOrthogonalDistance(
                                    a.getLatitude(), a.getLongitude(),
                                    b1.getLatitude(), b1.getLongitude(),
                                    b2.getLatitude(), b2.getLongitude());
                            if (d > MAX_OVERLAP_DISTANCE) {
                                match = false;
                                break;
                            }
                        } else {
                            match = false;
                            break;
                        }
                    }
                } else if (i == L - 1) {
                    if (!KMapUtils.INSTANCE.areLatLonEqual(p1, p2, PRECISION)) {
                        if (A.size() >= 2) {
                            WptPt aPrev = A.get(A.size() - 2);
                            double d = KMapUtils.INSTANCE.getOrthogonalDistance(
                                    b.getLatitude(), b.getLongitude(),
                                    aPrev.getLatitude(), aPrev.getLongitude(),
                                    a.getLatitude(), a.getLongitude());
                            if (d > MAX_OVERLAP_DISTANCE) {
                                match = false;
                                break;
                            }
                        } else {
                            match = false;
                            break;
                        }
                    }
                } else {
                    if (!KMapUtils.INSTANCE.areLatLonEqual(p1, p2, PRECISION)) {
                        match = false;
                        break;
                    }
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
     * of the second polyline. The result is the first polyline appended with the second polyline starting from index L,
     * thus avoiding duplication of the overlapping segment.
     */
    private static List<WptPt> mergeBySuffixPrefix(List<WptPt> first, List<WptPt> second, int overlap) {
        List<WptPt> merged = new ArrayList<>(first);
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
                result = new ArrayList<>(result.subList(0, index + 1));
            } else {
                result.add(pt);
            }
        }
        return result;
    }

    /**
     * Returns the index of the first point in the list that is equal to pt (using pointsEqual),
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
     * Compares two WptPt points using the library method areLatLonEqual with precision.
     * Two points are considered equal if the library method returns true for the given precision.
     */
    private static boolean pointsEqual(WptPt a, WptPt b) {
        KLatLon p1 = new KLatLon(a.getLatitude(), a.getLongitude());
        KLatLon p2 = new KLatLon(b.getLatitude(), b.getLongitude());
        return KMapUtils.INSTANCE.areLatLonEqual(p1, p2, PRECISION);
    }
}
