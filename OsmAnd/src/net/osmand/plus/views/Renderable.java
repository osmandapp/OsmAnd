package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class Renderable {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "Renderable #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<>(128);
    public static final Executor THREAD_POOL_EXECUTOR;

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    public static abstract class RenderableSegment {

        protected static final int MIN_CULLER_ZOOM = 16;

        public List<WptPt> points = null;                           // Original list of points
        protected List<WptPt> culled = new ArrayList<>();           // Reduced/resampled list of points
        protected int pointSize;
        protected double segmentSize;

        protected QuadRect trackBounds;
        protected double zoom = -1;
        protected AsynchronousResampler culler = null;                        // The currently active resampler
        protected Paint paint = null;                               // MUST be set by 'updateLocalPaint' before use

        public RenderableSegment(List<WptPt> points, double segmentSize) {
            this.points = points;
            this.segmentSize = segmentSize;
            trackBounds = GPXUtilities.calculateBounds(points);
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
                if (newZoom >= MIN_CULLER_ZOOM) {
                    return;
                }

                double cullDistance = Math.pow(2.0, segmentSize - zoom);    // segmentSize == epsilon
                culler = new AsynchronousResampler.RamerDouglasPeucer(this, cullDistance);
                try {
                    culler.executeOnExecutor(THREAD_POOL_EXECUTOR, "");
                } catch (RejectedExecutionException e) {
                    culler = null;
                }
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
                int prevSize = pointSize;
                pointSize = points.size();
                GPXUtilities.updateBounds(trackBounds, points, prevSize);
            }
            drawSingleSegment(zoom, p, canvas, tileBox);
        }

        @Override protected void startCuller(double newZoom) {}

        @Override public void drawSingleSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            draw(points, p, canvas, tileBox);
        }
    }
}
