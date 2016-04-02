package net.osmand.plus.views;

import android.os.AsyncTask;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AsynchronousResampler extends AsyncTask<String,Integer,String> {

    protected Renderable.RenderableSegment rs;
    protected List<WptPt> culled = null;

    AsynchronousResampler(Renderable.RenderableSegment rs) {
        assert rs != null;
        assert rs.points != null;
        this.rs = rs;
    }

    @Override protected void onPostExecute(String result) {
        if (!isCancelled()) {
            rs.setRDP(culled);
        }
    }

    private WptPt createIntermediatePoint(WptPt lastPt, WptPt pt, double partial, double dist) {
        WptPt newPt = new WptPt(
                lastPt.getLatitude() + partial * (pt.getLatitude() - lastPt.getLatitude()),
                lastPt.getLongitude() + partial * (pt.getLongitude() - lastPt.getLongitude()),
                (long) (lastPt.time + partial * (pt.time - lastPt.time)),
                lastPt.ele + partial * (pt.ele - lastPt.ele),
                lastPt.speed + partial * (pt.speed - lastPt.speed),
                lastPt.hdop + partial * (pt.hdop - lastPt.hdop));
        newPt.setDistance(dist);
        return newPt;
    }
    
    protected List<WptPt> resampleTrack(List<WptPt> pts, double dist) {

        List<WptPt> newPts = new ArrayList<>();

        int size = pts.size();
        if (size > 0) {
            WptPt lastPt = pts.get(0);

            double segSub = 0;
            double cumDist = 0;
            for (int i = 1; i < size && !isCancelled(); i++) {
                WptPt pt = pts.get(i);
                double segLength = MapUtils.getDistance(pt.getLatitude(), pt.getLongitude(), lastPt.getLatitude(), lastPt.getLongitude());
                while (segSub < segLength) {
                    double partial = segSub / segLength;
                    newPts.add(createIntermediatePoint(lastPt, pt, segSub/segLength, cumDist + segLength * partial));
                    segSub += dist;
                }
                segSub -= segLength;
                cumDist += segLength;
                lastPt = pt;
            }
            newPts.add(createIntermediatePoint(lastPt, lastPt, 0, cumDist));
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

            culled = resampleTrack(rs.points, segmentSize);
            if (!isCancelled() && !culled.isEmpty()) {

                int halfC = Algorithms.getRainbowColor(0.5);

                // Calculate the absolutes of the altitude variations
                Double max = culled.get(0).ele;
                Double min = max;
                for (WptPt pt : culled) {
                    max = Math.max(max, pt.ele);
                    min = Math.min(min, pt.ele);
                    pt.colourARGB = halfC;                  // default, in case there are no 'ele' in GPX
                }

                Double elevationRange = max - min;
                if (elevationRange > 0)
                    for (WptPt pt : culled)
                        pt.colourARGB = Algorithms.getRainbowColor((pt.ele - min) / elevationRange);
            }

            return null;
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

            culled = resampleTrack(rs.points, segmentSize);
            if (!isCancelled() && !culled.isEmpty()) {

                WptPt lastPt = culled.get(0);
                lastPt.speed = 0;

                int size = culled.size();
                for (int i = 1; i < size; i++) {
                    WptPt pt = culled.get(i);
                    double delta = pt.time - lastPt.time;
                    pt.speed = delta > 0 ? MapUtils.getDistance(pt.getLatitude(), pt.getLongitude(),
                                lastPt.getLatitude(), lastPt.getLongitude()) / delta : 0;
                    lastPt = pt;
                }

                if (size > 1) {
                    culled.get(0).speed = culled.get(1).speed;      // fixup 1st speed
                }

                double max = lastPt.speed;
                double min = max;

                int halfC = Algorithms.getRainbowColor(0.5);

                for (WptPt pt : culled) {
                    max = Math.max(max, pt.speed);
                    min = Math.min(min, pt.speed);
                    pt.colourARGB = halfC;                  // default, in case there are no 'time' in GPX
                }
                double speedRange = max - min;
                if (speedRange > 0) {
                    for (WptPt pt : culled)
                        pt.colourARGB = Algorithms.getRainbowColor((pt.speed - min) / speedRange);
                }
            }
            return null;
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
            culled = resampleTrack(rs.points, segmentSize);
            return null;
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class RamerDouglasPeucer extends AsynchronousResampler {

        private double epsilon;

        public RamerDouglasPeucer(Renderable.RenderableSegment rs, double epsilon) {
            super(rs);
            this.epsilon = epsilon;
        }

        @Override protected String doInBackground(String... params) {

            int nsize = rs.points.size();
            if (nsize > 0) {
                boolean survivor[] = new boolean[nsize];
                cullRamerDouglasPeucer(survivor, 0, nsize - 1);
                if (!isCancelled()) {
                    culled = new ArrayList();
                    survivor[0] = true;
                    for (int i = 0; i < nsize; i++) {
                        if (survivor[i]) {
                            culled.add(rs.points.get(i));
                        }
                    }
                }
            }
            return null;
        }

        private void cullRamerDouglasPeucer(boolean survivor[], int start, int end) {

            double dmax = Double.NEGATIVE_INFINITY;
            int index = -1;

            WptPt startPt = rs.points.get(start);
            WptPt endPt = rs.points.get(end);

            for (int i = start + 1; i < end && !isCancelled(); i++) {
                WptPt pt = rs.points.get(i);
                double d = MapUtils.getOrthogonalDistance(pt.lat, pt.lon, startPt.lat, startPt.lon, endPt.lat, endPt.lon);
                if (d > dmax) {
                    dmax = d;
                    index = i;
                }
            }
            if (dmax > epsilon) {
                cullRamerDouglasPeucer(survivor, start, index);
                cullRamerDouglasPeucer(survivor, index, end);
            } else {
                survivor[end] = true;
            }
        }

    }



}
