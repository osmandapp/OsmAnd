package net.osmand.plus.views;

import android.os.AsyncTask;

import net.osmand.plus.GPXUtilities;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AsynchronousResampler extends AsyncTask<String,Integer,String> {

    protected Renderable.RenderableSegment rs;
    protected List<GPXUtilities.WptPt> culled;

    AsynchronousResampler(Renderable.RenderableSegment rs) {
        this.rs = rs;
        culled = null;
    }

    @Override protected void onPostExecute(String result) {
        // executes on the UI thread so it's OK to change its variables
        if (rs != null && result.equals("OK") && !isCancelled())
            rs.setRDP(culled);
    }

    protected int getRainbowColour(double percent) {

        // Given an input percentage (0.0-1.0) this will produce a colour from a "wide rainbow"
        // from purple (low) to red(high).  This is useful for producing value-based colourations (e.g., altitude)

        double a = (1. - percent) * 5.;
        int X = (int)Math.floor(a);
        int Y = (int)(Math.floor(255 * (a - X)));
        switch (X) {
            case 0: return 0xFFFF0000 + (Y<<8);
            case 1: return 0xFF00FF00 + ((255-Y)<<16);
            case 2: return 0xFF00FF00 + Y;
            case 3: return 0xFF0000FF + ((255-Y)<<8);
            case 4: return 0xFF0000FF + (Y << 16);
        }
        return 0xFFFF00FF;
    }


    // Resample a list of points into a new list of points.
    // The new list is evenly-spaced (dist) and contains the first and last point from the original list.
    // The purpose is to allow tracks to be displayed with colours/shades/animation with even spacing
    // This routine essentially 'walks' along the path, dropping sample points along the trail where necessary. It is
    // Typically, pass a point list to this, and set dist (in metres) to something that's relative to screen zoom
    // The new point list has resampled times, elevations, speed and hdop too!

    protected List<GPXUtilities.WptPt> resampleTrack(List<GPXUtilities.WptPt> pts, double dist) {

        ArrayList<GPXUtilities.WptPt> newPts = new ArrayList<GPXUtilities.WptPt>();

        int ptCt = pts.size();
        if (pts != null && ptCt > 0) {

            GPXUtilities.WptPt lastPt = pts.get(0);
            double segSub = 0;
            double cumDist = 0;
            for (int i = 1; i < ptCt; i++) {

                if (isCancelled())
                    return null;

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
            if (!isCancelled()) {

                int halfC = getRainbowColour(0.5);                             // default coloration if no elevations found

                // Calculate the absolutes of the altitude variations
                Double max = Double.NEGATIVE_INFINITY;
                Double min = Double.POSITIVE_INFINITY;
                for (GPXUtilities.WptPt pt : culled) {
                    max = Math.max(max, pt.ele);
                    min = Math.min(min, pt.ele);
                    pt.colourARGB = halfC;
                }

                Double elevationRange = max - min;
                if (elevationRange > 0)
                    for (GPXUtilities.WptPt pt : culled)
                        pt.colourARGB = getRainbowColour((pt.ele - min) / elevationRange);
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
            if (!isCancelled()) {

                GPXUtilities.WptPt lastPt = points.get(0);
                lastPt.speed = 0;

                // calculate speeds using time:distance for each segment
                for (int i = 1; i < points.size(); i++) {
                    GPXUtilities.WptPt pt = points.get(i);
                    double delta = pt.time - lastPt.time;
                    if (delta > 0)
                        pt.speed = MapUtils.getDistance(pt.getLatitude(), pt.getLongitude(),
                                lastPt.getLatitude(), lastPt.getLongitude()) / delta;
                    else
                        pt.speed = 0;        // GPX doesn't have time - this is OK, colour will be mid-range for whole track
                    lastPt = pt;
                }

                // Calculate the absolutes of the speed variations
                Double max = Double.NEGATIVE_INFINITY;
                Double min = Double.POSITIVE_INFINITY;
                for (GPXUtilities.WptPt pt : points) {
                    max = Math.max(max, pt.speed);
                    min = Math.min(min, pt.speed);
                    pt.colourARGB = getRainbowColour(0.5);
                }
                Double range = max - min;
                if (range > 0)
                    for (GPXUtilities.WptPt pt : points)
                        pt.colourARGB = getRainbowColour((pt.speed - min) / range);
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

            // Reduce the point-count of the GPX track. The concept is that at arbitrary scales, some points are superfluous.
            // This is handled using the well-known 'Ramer-Douglas-Peucker' algorithm. This code is modified from the similar code elsewhere
            // but optimised for this specific usage.

            points = rs.getPoints();
            culled = new ArrayList<>();

            int nsize = points.size();
            if (nsize > 0) {
                survivor = new boolean[nsize];
                cullRamerDouglasPeucer(0, nsize - 1);
                if (!isCancelled()) {
                    survivor[0] = true;
                    for (int i = 0; i < survivor.length; i++)
                        if (survivor[i])
                            culled.add(points.get(i));
                }
            }
            return isCancelled() ? "" : "OK";
        }

        private void cullRamerDouglasPeucer(int start, int end) {

            double dmax = -1;
            int index = -1;
            for (int i = start + 1; i < end; i++) {

                if (isCancelled())
                    return;

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
