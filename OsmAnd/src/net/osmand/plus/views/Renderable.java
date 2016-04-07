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
        protected int priority;

        public RenderableSegment(int priority, List <WptPt> points, double segmentSize) {
            this.priority = priority;
            this.points = points;
            this.segmentSize = segmentSize;

            trackBounds = new QuadRect(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            updateBounds(points, 0);
        }

        protected void updateLocalPaint(Paint p) {
            if (paint == null) {
                paint = new Paint(p);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStyle(Paint.Style.STROKE); //.Style.FILL);
            }
            paint.setColor(p.getColor()|0xFF000000);        // no transparency
            paint.setStrokeWidth(p.getStrokeWidth());
        }

        protected abstract void startCuller(double newZoom);

        protected void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {}

        public void drawSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (pointSize != points.size()) {
                updateBounds(points, pointSize);
            }

            if (QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) { // is visible?
                startCuller(zoom);
                drawSingleSegment(p, canvas, tileBox);
            }
        }

        public int getPriority() {
            return priority;
        }

        protected void updateBounds(List<WptPt> pts, int startIndex) {
            for (int i = startIndex; i < pts.size(); i++) {
                WptPt pt = pts.get(i);
                trackBounds.right = Math.max(trackBounds.right, pt.lon);
                trackBounds.left = Math.min(trackBounds.left, pt.lon);
                trackBounds.top = Math.max(trackBounds.top, pt.lat);
                trackBounds.bottom = Math.min(trackBounds.bottom, pt.lat);
            }
            pointSize = pts.size();
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

        public StandardTrack(int priority, List<WptPt> pt, double base) {
            super(priority, pt, base);
        }

        @Override public void startCuller(double newZoom) {

            if ((pointSize != points.size() && culler != null && culler.getStatus() == AsyncTask.Status.FINISHED)
                    || zoom != newZoom) {

                if (culler != null) {
                    culler.cancel(true);
                }
                if (zoom < newZoom) {            // if line would look worse (we're zooming in) then...
                    culled.clear();              // use full-resolution until re-cull complete
                }
                zoom = newZoom;

                double cullDistance = Math.pow(2.0, segmentSize - zoom);    // segmentSize == epsilon
                culler = new AsynchronousResampler.RamerDouglasPeucer(this, cullDistance);
                culler.execute("");
            }
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {
            draw(culled.isEmpty() ? points : culled, p, canvas, tileBox);
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Altitude extends RenderableSegment {

        double widthZoom;

        public Altitude(int priority, List<WptPt> pt, double epsilon, double widthZoom) {
            super(priority, pt, epsilon);
            this.widthZoom = widthZoom;
        }

        @Override public void startCuller(double newZoom) {
            if (zoom != newZoom) {
                if (culler != null) {
                    culler.cancel(true);
                }
                //if (zoom < newZoom) {            // if line would look worse (we're zooming in) then...
                    culled.clear();              // use NOTHING until re-cull complete
                //}
                zoom = newZoom;

                double epsilon = Math.pow(2.0, 16 - zoom);
                culler = new AsynchronousResampler.ResampleAltitude(this, epsilon);     // once only!
                culler.execute("");
            }
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled.size() > 1
                    && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float bandWidth = paint.getStrokeWidth() * (float)widthZoom;
                paint.setStrokeWidth(bandWidth);

                float clipL = -bandWidth / 2;
                float clipB = -bandWidth / 2;
                float clipT = canvas.getHeight() + bandWidth / 2;
                float clipR = canvas.getWidth() + bandWidth / 2;

                WptPt pt = culled.get(0);
                float lastx = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float lasty = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                int size = culled.size();
                for (int i = 1; i < size; i++) {
                    pt = culled.get(i);

                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                            && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {
                        paint.setColor(pt.colourARGB);
                        canvas.drawLine(lastx, lasty, x, y, paint);
                    }
                    lastx = x;
                    lasty = y;
                }
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Speed extends Altitude {

        public Speed(int priority, List<WptPt> pt, double segmentSize, double widthZoom) {
            super(priority, pt, segmentSize, widthZoom);
        }

        @Override public void startCuller(double newZoom) {
            if (zoom != newZoom) {
                if (culler != null) {
                    culler.cancel(true);
                }
                //if (zoom < newZoom) {            // if line would look worse (we're zooming in) then...
                    culled.clear();              // use nothing until cull finished
                //}
                zoom = newZoom;

                double epsilon = Math.pow(2.0, 16 - zoom);
                culler = new AsynchronousResampler.ResampleSpeed(this, epsilon);        // TODO: convert to same as altitude once only!
                culler.execute("");
            }
        }
    }

}
