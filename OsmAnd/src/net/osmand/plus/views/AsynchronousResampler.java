package net.osmand.plus.views;

import android.graphics.Color;
import android.os.AsyncTask;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AsynchronousResampler extends AsyncTask<String,Integer,String> {

    protected Renderable rs;
    protected List<WptPt2> culled = null;
    protected double epsilon;

    AsynchronousResampler(Renderable rs, double epsilon) {
        assert rs != null;
        assert rs.points != null;
        this.rs = rs;
        this.epsilon = epsilon;
    }

    @Override protected void onPostExecute(String result) {
        if (!isCancelled()) {
            rs.setRDP(culled);
        }
    }

    @Override protected String doInBackground(String... params) { return null;}


    private WptPt2 createIntermediatePoint(WptPt lastPt, WptPt pt, double partial, double dist) {
        double angle = Math.atan2(lastPt.lat - pt.lat, lastPt.lon - pt.lon);    // kludge
        return new WptPt2(
                lastPt.lat + (pt.lat - lastPt.lat) * partial,
                lastPt.lon + (pt.lon - lastPt.lon) * partial,
                (long) (lastPt.time + (pt.time - lastPt.time) * partial),
                lastPt.ele + (pt.ele - lastPt.ele) * partial,
                lastPt.speed + (pt.speed - lastPt.speed) * partial,
                dist, angle);
    }

    public List<WptPt2> resampleTrack(List<WptPt> pts, double dist) {

        List<WptPt2> newPts = new ArrayList<>();

        int size = pts.size();
        if (size > 0) {
            WptPt lastPt = pts.get(0);

            double segSub = 0;
            double cumDist = 0;
            for (int i = 1; i < size && !isCancelled(); i++) {
                WptPt pt = pts.get(i);
                double segLength = MapUtils.getDistance(pt.lat, pt.lon, lastPt.lat, lastPt.lon);
                while (segSub < segLength) {
                    double partial = segSub / segLength;
                    newPts.add(createIntermediatePoint(lastPt, pt, partial, cumDist + segLength * partial));
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

    protected List<WptPt2> doRamerDouglasPeucerSimplification(List<WptPt> pts) {
        List<WptPt2> rdpTrack = null;
        int nsize = pts.size();
        if (nsize > 0) {
            boolean survivor[] = new boolean[nsize];
            cullRamerDouglasPeucer(survivor, 0, nsize - 1);
            if (!isCancelled()) {
                rdpTrack = new ArrayList<>();
                survivor[0] = true;
                for (int i = 0; i < nsize; i++) {
                    if (survivor[i]) {
                        rdpTrack.add(new WptPt2(pts.get(i)));
                    }
                }
            }
        }
        return rdpTrack;
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

    public static class Generic extends AsynchronousResampler {

        public Generic(Renderable rs, double epsilon) {
            super(rs, epsilon);
        }

        @Override protected String doInBackground(String... params) {
            culled = resampleTrack(rs.points, epsilon);
            return null;
        }
    }

    public static class RamerDouglasPeucer extends AsynchronousResampler {

        public RamerDouglasPeucer(Renderable rs, double epsilon) {
            super(rs, epsilon);
        }

        @Override protected String doInBackground(String... params) {
            culled = doRamerDouglasPeucerSimplification(rs.points);
            return null;
        }
    }

    public static class Altitude extends AsynchronousResampler {

        private double clampMin;
        private double clampMax;
        private boolean clamp;

        public Altitude(Renderable rs, double epsilon) {
            super(rs, epsilon);
            this.clamp = false;
        }

        public Altitude(Renderable rs, double epsilon, double clampMin, double clampMax) {
            super(rs, epsilon);
            this.clampMin = clampMin;
            this.clampMax = clampMax;
            this.clamp = true;
        }

        @Override protected String doInBackground(String... params) {
            culled = doRamerDouglasPeucerSimplification(rs.points); //resampleTrack(rs.points, epsilon);
            if (!isCancelled() && !culled.isEmpty()) {

                int halfC = Algorithms.getRainbowColor(0.5);
                for (WptPt2 pt : culled) {
                    pt.colourARGB = halfC;                  // default, in case there are no 'ele' in GPX
                }

                // If no clamping, auto-detect range
                if (!clamp) {
                    clampMin = Double.POSITIVE_INFINITY;
                    clampMax = Double.NEGATIVE_INFINITY;
                    for (WptPt2 pt : culled) {
                        clampMax = Math.max(clampMax, pt.ele);
                        clampMin = Math.min(clampMin, pt.ele);
                    }
                }

                Double elevationRange = clampMax - clampMin;
                if (elevationRange > 0)
                    for (WptPt2 pt : culled) {
                        double clamped = Math.min(clampMax, Math.max(pt.ele, clampMin));
                        pt.colourARGB = Algorithms.getRainbowColor((clamped - clampMin) / elevationRange);
                    }
            }

            return null;
        }
    }

    public static class Speed extends AsynchronousResampler {

        private double clampMin;
        private double clampMax;
        private boolean clamp;

        public Speed(Renderable rs, double epsilon) {
            super(rs, epsilon);
            this.clamp = false;
        }

        public Speed(Renderable rs, double epsilon, double clampMin, double clampMax) {
            super(rs, epsilon);
            this.clampMin = clampMin;
            this.clampMax = clampMax;
            this.clamp = true;
        }

        @Override protected String doInBackground(String... params) {

            // Resample track, then analyse speeds and set colours for each point

            culled = doRamerDouglasPeucerSimplification(rs.points); //resampleTrack(rs.points, epsilon);
            if (!isCancelled() && !culled.isEmpty()) {

                WptPt2 lastPt = culled.get(0);
                lastPt.speed = 0;

                int size = culled.size();
                for (int i = 1; i < size; i++) {
                    WptPt2 pt = culled.get(i);
                    double delta = pt.time - lastPt.time;
                    pt.speed = delta > 0 ? MapUtils.getDistance(pt.lat, pt.lon,
                            lastPt.lat, lastPt.lon) / delta * 3600 : 0;             // units:  km/h
                    lastPt = pt;
                }

                if (size > 1) {
                    culled.get(0).speed = culled.get(1).speed;      // fixup 1st speed
                }

                // If no manual clamping, auto-detect range
                if (!clamp) {
                    clampMin = Double.POSITIVE_INFINITY;
                    clampMax = Double.NEGATIVE_INFINITY;
                    for (WptPt2 pt : culled) {
                        clampMax = Math.max(clampMax, pt.speed);
                        clampMin = Math.min(clampMin, pt.speed);
                    }
                }

                int halfC = Algorithms.getRainbowColor(0.5);
                for (WptPt2 pt : culled) {
                    pt.colourARGB = halfC;                  // default, in case there are no 'time' in GPX
                }

                double speedRange = clampMax - clampMin;
                if (speedRange > 0) {
                    for (WptPt2 pt : culled) {
                        double clamped = Math.min(clampMax, Math.max(pt.speed, clampMin));
                        pt.colourARGB = Algorithms.getRainbowColor((clamped - clampMin) / speedRange);
                    }
                }
            }
            return null;
        }
    }


    public static class RouteMarker extends AsynchronousResampler {

        public RouteMarker(Renderable rs, double epsilon) {
            super(rs, epsilon);
        }

        private double calculatedAngle(int i) {
            double angle = 0;
            int size = culled.size();
            if (i > 0 && i < size) {
                angle += Math.abs(diff(culled.get(i - 1).angle, culled.get(i).angle));
            }
            return angle;
        }

        public static double diff(double theta1, double theta2) {
            double dif = ((theta2 - theta1)%(2*Math.PI)); // in range
            if (theta1>theta2) {dif += 2*Math.PI;}
            if (dif >= Math.PI) {dif = -(dif - 2*Math.PI);}
            return dif;
        }


        @Override protected String doInBackground(String... params) {

            culled = resampleTrack(rs.points, epsilon);

            int detectionLength = 2;
            double cumulativeAngle = 0;

            // Pre-calc sum of angles for the first part of the track
            int size = culled.size();
            for (int i = 0; i < size && i < detectionLength + 1; i++) {
                cumulativeAngle += calculatedAngle(i);
            }

            for (int i = 0; i < size; i++) {
                cumulativeAngle -= calculatedAngle(i - detectionLength);
                cumulativeAngle += calculatedAngle(i + detectionLength + 1);
                culled.get(i).speed = cumulativeAngle;          // re-use var
            }




            int run = 0;
            int gap = 0;

            WptPt2 last = null;

            double threshold = Math.PI/2f;


            for (int i = 0; i < size; i++) {
                WptPt2 pt = culled.get(i);
                boolean warn = pt.speed > threshold;

                if (run > 0 && run < 5) {
                    warn = true;
                }

                if (run > 8 || (gap > 0 && gap < 3)) {
                    warn = false;
                }

                if (warn) {
                    run++;
                    gap = 0;
                    pt.colourARGB = Color.YELLOW;
                    if (last != null && last.colourARGB == Color.BLACK) {
                        last.colourARGB = 0;
                    }

                } else {
                    gap++;
                    if (gap > 3) {
                        pt.colourARGB = Color.BLACK;
                        gap = 0;
                    }

                    run = 0;
                }
                last = pt;
            }

            return null;
        }
    }


}
