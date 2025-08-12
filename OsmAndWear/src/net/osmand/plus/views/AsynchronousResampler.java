package net.osmand.plus.views;

import android.os.AsyncTask;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AsynchronousResampler extends AsyncTask<String,Integer,String> {

    protected Renderable.RenderableSegment rs;
    protected List<WptPt> culled;

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

    public static class RamerDouglasPeucer extends AsynchronousResampler {

        private final double epsilon;

        public RamerDouglasPeucer(Renderable.RenderableSegment rs, double epsilon) {
            super(rs);
            this.epsilon = epsilon;
        }

        @Override protected String doInBackground(String... params) {

            int nsize = rs.points.size();
            if (nsize > 0) {
                boolean[] survivor = new boolean[nsize];
                cullRamerDouglasPeucer(survivor, 0, nsize - 1);
                if (!isCancelled()) {
                    culled = new ArrayList<>();
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

        private void cullRamerDouglasPeucer(boolean[] survivor, int start, int end) {

            double dmax = Double.NEGATIVE_INFINITY;
            int index = -1;

            WptPt startPt = rs.points.get(start);
            WptPt endPt = rs.points.get(end);

            for (int i = start + 1; i < end && !isCancelled(); i++) {
                WptPt pt = rs.points.get(i);
                double d = MapUtils.getOrthogonalDistance(pt.getLat(), pt.getLon(),
                        startPt.getLat(), startPt.getLon(), endPt.getLat(), endPt.getLon());
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
