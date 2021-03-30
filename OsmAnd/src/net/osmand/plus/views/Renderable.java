package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.views.layers.geometry.GpxGeometryWay;
import net.osmand.util.Algorithms;

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

import androidx.annotation.NonNull;


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
        protected Paint borderPaint;
        protected GradientScaleType scaleType = null;

        protected GpxGeometryWay geometryWay;

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
                paint.setStyle(Paint.Style.STROKE);
            }
            paint.setColor(p.getColor());
            paint.setStrokeWidth(p.getStrokeWidth());
        }

        public void setBorderPaint(@NonNull Paint paint) {
            borderPaint = paint;
        }

        public void setGradientScaleType(GradientScaleType type) {
            this.scaleType = type;
        }

        public GpxGeometryWay getGeometryWay() {
            return geometryWay;
        }

        public void setGeometryWay(GpxGeometryWay geometryWay) {
            this.geometryWay = geometryWay;
        }

        protected abstract void startCuller(double newZoom);

        protected void drawSingleSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            if (points.size() < 2) {
                return;
            }

            updateLocalPaint(p);
            canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            if (scaleType != null) {
                drawGradient(getPointsForDrawing(), p, canvas, tileBox);
            } else {
                drawSolid(getPointsForDrawing(), p, canvas, tileBox);
            }
            canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
        }


        public void drawSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            if (QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) { // is visible?
                startCuller(zoom);
                drawSingleSegment(zoom, p, canvas, tileBox);
            }
        }

        public void setRDP(List<WptPt> cull) {
            culled = cull;
        }

        public List<WptPt> getPointsForDrawing() {
            return culled.isEmpty() ? points : culled;
        }

        public void drawGeometry(Canvas canvas, RotatedTileBox tileBox, QuadRect quadRect, int arrowColor, int trackColor, float trackWidth) {
            if (geometryWay != null) {
                List<WptPt> points = getPointsForDrawing();
                if (!Algorithms.isEmpty(points)) {
                    geometryWay.setTrackStyleParams(arrowColor, trackColor, trackWidth);
                    geometryWay.updatePoints(tileBox, points);
                    geometryWay.drawSegments(tileBox, canvas, quadRect.top, quadRect.left, quadRect.bottom, quadRect.right, null, 0);
                }
            }
        }

        protected void drawSolid(List<WptPt> pts, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            QuadRect tileBounds = tileBox.getLatLonBounds();
            WptPt lastPt = pts.get(0);
            boolean recalculateLastXY = true;
            Path path = new Path();
            for (int i = 1; i < pts.size(); i++) {
                WptPt pt = pts.get(i);
                if (arePointsInsideTile(pt, lastPt, tileBounds)) {
                    if (recalculateLastXY) {
                        recalculateLastXY = false;
                        float lastX = tileBox.getPixXFromLatLon(lastPt.lat, lastPt.lon);
                        float lastY = tileBox.getPixYFromLatLon(lastPt.lat, lastPt.lon);
                        if (!path.isEmpty()) {
                            canvas.drawPath(path, paint);
                        }
                        path.reset();
                        path.moveTo(lastX, lastY);
                    }
                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);
                    path.lineTo(x, y);
                } else {
                    recalculateLastXY = true;
                }
                lastPt = pt;
            }
            if (!path.isEmpty()) {
                canvas.drawPath(path, paint);
            }
        }

        protected void drawGradient(List<WptPt> pts, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            QuadRect tileBounds = tileBox.getLatLonBounds();
            Path currentPath = new Path();
            Path nextPath = new Path();
            Paint paint = new Paint(this.paint);

            WptPt prevWpt = pts.get(0);
            WptPt currWpt = pts.get(1);

            PointF prevXY = new PointF();
            PointF currXY = new PointF();
            PointF nextXY = new PointF();

            boolean currLineVisible = arePointsInsideTile(prevWpt, currWpt, tileBounds);
            boolean nextLineVisible;

            if (currLineVisible) {
                pixXYFromWptPt(tileBox, prevXY, prevWpt);
                pixXYFromWptPt(tileBox, currXY, currWpt);
                canvas.drawPath(pathFromStartEnd(currentPath, prevXY, currXY), borderPaint);
            }

            for (int i = 1; i < pts.size(); i++) {
                currWpt = pts.get(i);
                WptPt nextWpt = i + 1 == pts.size() ? null : pts.get(i + 1);

                nextLineVisible = arePointsInsideTile(currWpt, nextWpt, tileBounds);
                if (nextWpt != null && nextLineVisible) {
                    pixXYFromWptPt(tileBox, currXY, currWpt);
                    pixXYFromWptPt(tileBox, nextXY, nextWpt);
                    canvas.drawPath(pathFromStartEnd(nextPath, currXY, nextXY), borderPaint);
                }

                if (currLineVisible) {
                    int prevColor = prevWpt.getColor(scaleType.toColorizationType());
                    int currentColor = currWpt.getColor(scaleType.toColorizationType());
                    LinearGradient gradient = new LinearGradient(prevXY.x, prevXY.y, currXY.x, currXY.y,
                            prevColor, currentColor, Shader.TileMode.CLAMP);
                    paint.setShader(gradient);
                    canvas.drawPath(currentPath, paint);
                }

                prevWpt = currWpt;
                currentPath.set(nextPath);
                prevXY.set(currXY);
                currXY.set(nextXY);
                currLineVisible = nextLineVisible;
            }
        }

        protected boolean arePointsInsideTile(WptPt first, WptPt second, QuadRect tileBounds) {
            if (first == null || second == null) {
                return false;
            }
            return Math.min(first.lon, second.lon) < tileBounds.right && Math.max(first.lon, second.lon) > tileBounds.left
                    && Math.min(first.lat, second.lat) < tileBounds.top && Math.max(first.lat, second.lat) > tileBounds.bottom;
        }

        protected PointF pixXYFromWptPt(RotatedTileBox tileBox, PointF pointF, WptPt wptPt) {
            pointF.x = tileBox.getPixXFromLatLon(wptPt.lat, wptPt.lon);
            pointF.y = tileBox.getPixYFromLatLon(wptPt.lat, wptPt.lon);
            return pointF;
        }

        protected Path pathFromStartEnd(Path path, PointF start, PointF end) {
            path.reset();
            path.moveTo(start.x, start.y);
            path.lineTo(end.x, end.y);
            return path;
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
    }
}
