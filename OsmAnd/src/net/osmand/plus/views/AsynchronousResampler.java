package net.osmand.plus.views;

import android.os.AsyncTask;

import net.osmand.plus.GPXUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AsynchronousResampler extends AsyncTask<String,Integer,String> {

    protected Renderable.RenderableSegment rs;
    protected List<GPXUtilities.WptPt> culled;

    AsynchronousResampler(Renderable.RenderableSegment rs) {
        this.rs = rs;
        culled = new ArrayList<>();     // so we NEVER return a null list
    }

    @Override protected void onPostExecute(String result) {
        // executes on the UI thread so it's OK to change its variables
        if (rs != null && result.equals("OK") && !isCancelled()) {
            rs.setRDP(culled);
        }
    }

    // Resample a list of points into a new list of points.
    // The new list is evenly-spaced (dist) and contains the first and last point from the original list.
    // The purpose is to allow tracks to be displayed with colours/shades/animation with even spacing
    // This routine essentially 'walks' along the path, dropping sample points along the trail where necessary. It is
    // Typically, pass a point list to this, and set dist (in metres) to something that's relative to screen zoom
    // The new point list has resampled times, elevations, speed and hdop too!

    protected List<GPXUtilities.WptPt> resampleTrack(List<GPXUtilities.WptPt> pts, double dist) {

        List<GPXUtilities.WptPt> newPts = new ArrayList<>();

        int ptCt = pts.size();
        if (ptCt > 1) {

            GPXUtilities.WptPt lastPt = pts.get(0);
            double segSub = 0;
            double cumDist = 0;
            for (int i = 1; i < ptCt; i++) {

                if (isCancelled()) {
                    return null;
                }

                GPXUtilities.WptPt pt = pts.get(i);
                double segLength = MapUtils.getDistance(pt.getLatitude(), pt.getLongitude(), lastPt.getLatitude(), lastPt.getLongitude());

                // March along the segment, calculating the interpolated point values as we go
                while (segSub < segLength) {
                    double partial = segSub / segLength;
                    GPXUtilities.WptPt newPoint = new GPXUtilities.WptPt(
                            lastPt.getLatitude() + partial * (pt.getLatitude() - lastPt.getLatitude()),
                            lastPt.getLongitude() + partial * (pt.getLongitude() - lastPt.getLongitude()),
                            (long) (lastPt.time + partial * (pt.time - lastPt.time)),
                            lastPt.ele + partial * (pt.ele - lastPt.ele),
                            lastPt.speed + partial * (pt.speed - lastPt.speed),
                            lastPt.hdop + partial * (pt.hdop - lastPt.hdop)
                    );
                    newPoint.setDistance(cumDist + segLength * partial);
                    newPts.add(newPts.size(), newPoint);
                    segSub += dist;
                }
                segSub -= segLength;                // leftover
                cumDist += segLength;
                lastPt = pt;
            }

            // Add in the last point as a terminator (with total distance recorded)
            GPXUtilities.WptPt newPoint = new GPXUtilities.WptPt( lastPt.getLatitude(), lastPt.getLongitude(), lastPt.time, lastPt.ele, lastPt.speed, lastPt. hdop);
            newPoint.setDistance(cumDist);
            newPts.add(newPts.size(), newPoint);

        } else { // 0 and 1 point lines are just copied
            for (GPXUtilities.WptPt pt : pts) {
                newPts.add(new GPXUtilities.WptPt(pt));
            }
        }
        return newPts;
    }



    //----------------------------------------------------------------------------------------------

    public static class ResampleAltitude extends AsynchronousResampler {

        private double segmentSize;

        ResampleAltitude(Renderable.RenderableSegment rs, double segmentSize) {
            super(rs);
            this.segmentSize = segmentSize;
        }

        @Override protected String doInBackground(String... params) {

            // Resample track, then analyse altitudes and set colours for each point

            culled = resampleTrack(rs.getPoints(), segmentSize);
            if (!isCancelled() && !culled.isEmpty()) {

                int halfC = Algorithms.getRainbowColor(0.5);

                // Calculate the absolutes of the altitude variations
                Double max = Double.NEGATIVE_INFINITY;
                Double min = Double.POSITIVE_INFINITY;
                for (GPXUtilities.WptPt pt : culled) {
                    max = Math.max(max, pt.ele);
                    min = Math.min(min, pt.ele);
                    pt.colourARGB = halfC;                  // default, in case there are no 'ele' in GPX
                }

                Double elevationRange = max - min;
                if (elevationRange > 0)
                    for (GPXUtilities.WptPt pt : culled)
                        pt.colourARGB = Algorithms.getRainbowColor((pt.ele - min) / elevationRange);
            }

            return isCancelled() ? "" : "OK";
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class ResampleSpeed extends AsynchronousResampler {

        private double segmentSize;

        ResampleSpeed(Renderable.RenderableSegment rs, double segmentSize) {
            super(rs);
            this.segmentSize = segmentSize;
        }

        @Override protected String doInBackground(String... params) {

            // Resample track, then analyse speeds and set colours for each point

            List<GPXUtilities.WptPt> points = rs.getPoints();
            culled = resampleTrack(points, segmentSize);
            if (!isCancelled() && !culled.isEmpty()) {

                GPXUtilities.WptPt lastPt = points.get(0);
                lastPt.speed = 0;

                // calculate speeds using time:distance for each segment
                for (int i = 1; i < points.size(); i++) {
                    GPXUtilities.WptPt pt = points.get(i);
                    double delta = pt.time - lastPt.time;
                    if (delta > 0) {
                        pt.speed = MapUtils.getDistance(pt.getLatitude(), pt.getLongitude(),
                                lastPt.getLatitude(), lastPt.getLongitude()) / delta;
                    } else {
                        pt.speed = 0;        // GPX doesn't have time - this is OK, colour will be mid-range for whole track
                    }
                    lastPt = pt;
                }

                int halfC = Algorithms.getRainbowColor(0.5);

                // Calculate the absolutes of the speed variations
                Double max = Double.NEGATIVE_INFINITY;
                Double min = Double.POSITIVE_INFINITY;
                for (GPXUtilities.WptPt pt : points) {
                    max = Math.max(max, pt.speed);
                    min = Math.min(min, pt.speed);
                    pt.colourARGB = halfC;                  // default, in case there are no 'time' in GPX
                }
                Double speedRange = max - min;
                if (speedRange > 0) {
                    for (GPXUtilities.WptPt pt : points)
                        pt.colourARGB = Algorithms.getRainbowColor((pt.speed - min) / speedRange);
                }
            }
            return isCancelled() ? "" : "OK";
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class GenericResampler extends AsynchronousResampler {

        private double segmentSize;

        public GenericResampler(Renderable.RenderableSegment rs, double segmentSize) {
            super(rs);
            this.segmentSize = segmentSize;
        }

        @Override protected String doInBackground(String... params) {
            culled = resampleTrack(rs.getPoints(), segmentSize);
            return isCancelled() ? "" : "OK";
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class RamerDouglasPeucer extends AsynchronousResampler {

        private double epsilon;
        private boolean survivor[];
        private List<GPXUtilities.WptPt> points;

        public RamerDouglasPeucer(Renderable.RenderableSegment rs, double epsilon) {
            super(rs);
            this.epsilon = epsilon;
        }

        @Override protected String doInBackground(String... params) {

            // Reduce the point-count of the GPX track using Ramer-Douglas-Peucker algorithm.

            points = rs.getPoints();
            int nsize = points.size();
            if (nsize > 2) {
                culled = new ArrayList<>();
                survivor = new boolean[nsize];
                cullRamerDouglasPeucer(0, nsize - 1);
                if (!isCancelled()) {
                    survivor[0] = true;
                    for (int i = 0; i < survivor.length; i++)
                        if (survivor[i]) {
                            culled.add(points.get(i));
                        }
                }
            } else { // make a copy of 0-1-2 point arrays
                culled = new ArrayList<>();
                for (GPXUtilities.WptPt pt : points) {
                    culled.add(new GPXUtilities.WptPt(pt));
                }
            }
            return isCancelled() ? "" : "OK";
        }

        private void cullRamerDouglasPeucer(int start, int end) {

            double dmax = -1;
            int index = -1;
            for (int i = start + 1; i < end; i++) {

                if (isCancelled()) {
                    return;
                }

                double d = MapUtils.getOrthogonalDistance(
                        points.get(i).getLatitude(), points.get(i).getLongitude(),
                        points.get(start).getLatitude(), points.get(start).getLongitude(),
                        points.get(end).getLatitude(), points.get(end).getLongitude());
                if (d > dmax) {
                    dmax = d;
                    index = i;
                }
            }
            if (dmax >= epsilon) {
                cullRamerDouglasPeucer(start, index);
                cullRamerDouglasPeucer(index, end);
            } else {
                survivor[end] = true;
            }
        }

    }



}
