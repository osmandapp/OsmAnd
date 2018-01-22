package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;

import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.WptPt;

import java.util.ArrayList;
import java.util.List;


public class Renderable {

    public static abstract class RenderableSegment {

        public List<WptPt> points = null;                           // Original list of points
        protected List<WptPt> culled = new ArrayList<>();           // Reduced/resampled list of points
        protected int pointSize;
        protected double segmentSize;

        protected QuadRect trackBounds;
        protected double zoom = -1;
        protected AsynchronousResampler culler = null;                        // The currently active resampler
        protected Paint paint = null;                               // MUST be set by 'updateLocalPaint' before use

        public RenderableSegment(List <WptPt> points, double segmentSize) {
            this.points = points;
            calculateBounds(points);
            this.segmentSize = segmentSize;
        }

        protected void updateLocalPaint(Paint p) {
            if (paint == null) {
                paint = new Paint(p);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStyle(Paint.Style.FILL);
            }
            paint.setColor(p.getColor());
            paint.setStrokeWidth(p.getStrokeWidth());
        }

        protected abstract void startCuller(double newZoom);

        protected void drawSingleSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {}

        public void drawSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            if (QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) { // is visible?
                startCuller(zoom);
                drawSingleSegment(zoom, p, canvas, tileBox);
            }
        }

        private void calculateBounds(List<WptPt> pts) {
            trackBounds = new QuadRect(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            updateBounds(pts, 0);
        }

        protected void updateBounds(List<WptPt> pts, int startIndex) {
            pointSize = pts.size();
            for (int i = startIndex; i < pointSize; i++) {
                WptPt pt = pts.get(i);
                trackBounds.right = Math.max(trackBounds.right, pt.lon);
                trackBounds.left = Math.min(trackBounds.left, pt.lon);
                trackBounds.top = Math.max(trackBounds.top, pt.lat);
                trackBounds.bottom = Math.min(trackBounds.bottom, pt.lat);
            }
        }

        public void setRDP(List<WptPt> cull) {
            culled = cull;
        }

        protected void draw(List<WptPt> pts, Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (pts.size() > 1) {

                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                QuadRect tileBounds = tileBox.getLatLonBounds();

                WptPt lastPt = pts.get(0);
                float lastx = 0;
                float lasty = 0;
                boolean reCalculateLastXY = true;

                int size = pts.size();
                for (int i = 1; i < size; i++) {
                    WptPt pt = pts.get(i);

                    if (Math.min(pt.lon, lastPt.lon) < tileBounds.right && Math.max(pt.lon, lastPt.lon) > tileBounds.left
                            && Math.min(pt.lat, lastPt.lat) < tileBounds.top && Math.max(pt.lat, lastPt.lat) > tileBounds.bottom) {

                        if (reCalculateLastXY) {
                            lastx = tileBox.getPixXFromLatLon(lastPt.lat, lastPt.lon);
                            lasty = tileBox.getPixYFromLatLon(lastPt.lat, lastPt.lon);
                            reCalculateLastXY = false;
                        }

                        float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                        float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                        canvas.drawLine(lastx, lasty, x, y, paint);

                        lastx = x;
                        lasty = y;

                    } else {
                        reCalculateLastXY = true;
                    }
                    lastPt = pt;
                }
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            }
        }
    }

    public static class StandardTrack extends RenderableSegment {

        public StandardTrack(List<WptPt> pt, double base) {
            super(pt, base);
        }

        @Override public void startCuller(double newZoom) {

            if (zoom != newZoom) {
                if (culler != null) {
                    culler.cancel(true);
                }
                if (zoom < newZoom) {            // if line would look worse (we're zooming in) then...
                    culled.clear();              // use full-resolution until re-cull complete
                }
                zoom = newZoom;

                double cullDistance = Math.pow(2.0, segmentSize - zoom);    // segmentSize == epsilon
                culler = new AsynchronousResampler.RamerDouglasPeucer(this, cullDistance);
                culler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            }
        }

        @Override public void drawSingleSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            draw(culled.isEmpty() ? points : culled, p, canvas, tileBox);
        }
    }

    public static class CurrentTrack extends RenderableSegment {

        public CurrentTrack(List<WptPt> pt) {
            super(pt, 0);
        }

        @Override public void drawSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            if (points.size() != pointSize) {
                updateBounds(points, pointSize);
            }
            drawSingleSegment(zoom, p, canvas, tileBox);
        }

        @Override protected void startCuller(double newZoom) {}

        @Override public void drawSingleSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            draw(points, p, canvas, tileBox);
        }
    }
}
