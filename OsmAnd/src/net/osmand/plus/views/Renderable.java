package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.views.layers.geometry.GpxGeometryWay;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;


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
            Path path = new Path();
            Paint paint = new Paint(this.paint);
            WptPt prevPt = pts.get(0);
            for (int i = 1; i < pts.size(); i++) {
                WptPt currentPt = pts.get(i);
                if (arePointsInsideTile(currentPt, prevPt, tileBounds)) {
                    float startX = tileBox.getPixXFromLatLon(prevPt.lat, prevPt.lon);
                    float startY = tileBox.getPixYFromLatLon(prevPt.lat, prevPt.lon);
                    float endX = tileBox.getPixXFromLatLon(currentPt.lat, currentPt.lon);
                    float endY = tileBox.getPixYFromLatLon(currentPt.lat, currentPt.lon);
                    int prevColor = prevPt.getColor(scaleType.toColorizationType());
                    int currentColor = currentPt.getColor(scaleType.toColorizationType());
                    LinearGradient gradient = new LinearGradient(startX, startY, endX, endY, prevColor, currentColor, Shader.TileMode.CLAMP);
                    paint.setShader(gradient);
                    path.reset();
                    path.moveTo(startX, startY);
                    path.lineTo(endX, endY);
                    canvas.drawPath(path, paint);
                }
                prevPt = currentPt;
            }
        }

        protected boolean arePointsInsideTile(WptPt first, WptPt second, QuadRect tileBounds) {
            return Math.min(first.lon, second.lon) < tileBounds.right && Math.max(first.lon, second.lon) > tileBounds.left
                    && Math.min(first.lat, second.lat) < tileBounds.top && Math.max(first.lat, second.lat) > tileBounds.bottom;
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

    public static class MultiProfileTrack extends StandardTrack {

        private final Map<String, List<PointF>> profileIconsPositions = new HashMap<>();
        private final Map<String, Pair<Integer, Bitmap>> profileValues;

        private final DisplayMetrics displayMetrics;

        private Paint circlePaint;
        private Paint borderPaint;

        private final float circleSize;
        private final float iconOffset;

        private int userPtIdx = 0;
        private int leftPtIdx = 0;
        private int rightPtIdx = 0;

        public MultiProfileTrack(List<WptPt> routePoints, Map<String, Pair<Integer, Bitmap>> profileValues,
                                 DisplayMetrics displayMetrics, double base) {
            super(routePoints, base);
            this.profileValues = profileValues;
            this.displayMetrics = displayMetrics;
            updateCirclePaint();
            circleSize = dpToPx(18);
            iconOffset = dpToPx(30) + circleSize / 2;
        }

        @Override
        protected void drawSolid(List<WptPt> points, Paint linePaint, Canvas canvas, RotatedTileBox tileBox) {
            linePaint = new Paint(paint);

            QuadRect tileBounds = tileBox.getLatLonBounds();
            PathMeasure pathMeasure = new PathMeasure();
            Path path = new Path();

            WptPt prevPt = points.get(0);
            String currentProfile = getProfile(points);
            linePaint.setColor(profileValues.get(currentProfile).first);
            updateBorderPaint(linePaint.getColor(), linePaint.getStrokeWidth() + dpToPx(4));

            float lengthRemaining = getRemainingLength(tileBox, points);

            for (int i = 1; i < points.size(); i++) {
                WptPt currentPt = points.get(i);
                PointF start = getPointFromWpt(tileBox, prevPt);
                PointF end = getPointFromWpt(tileBox, currentPt);

                path.reset();
                path.moveTo(start.x, start.y);
                path.lineTo(end.x, end.y);
                if (arePointsInsideTile(currentPt, prevPt, tileBounds)) {
                    canvas.drawPath(path, borderPaint);
                    canvas.drawPath(path, linePaint);
                }

                if (lengthRemaining >= 0) {
                    pathMeasure.setPath(path, false);
                    float pathLength = pathMeasure.getLength();
                    if (lengthRemaining - pathLength <= 0) {
                        addIconPosition(start, end, pathLength, lengthRemaining, currentProfile);
                    }
                    lengthRemaining -= pathLength;
                }

                if (currentPt.hasProfile()) {
                    currentProfile = getProfile(points);
                    linePaint.setColor(profileValues.get(currentProfile).first);
                    updateBorderPaint(linePaint.getColor(), linePaint.getStrokeWidth() + dpToPx(2));
                    lengthRemaining = getRemainingLength(tileBox, points);
                }
                prevPt = currentPt;
            }
        }

        private void drawProfileIcons(Canvas canvas, RotatedTileBox tileBox) {
            for (String profile : profileIconsPositions.keySet()) {
                List<PointF> positions = profileIconsPositions.get(profile);
                if (Algorithms.isEmpty(positions)) {
                    continue;
                }
                for (PointF center : positions) {
                    if (tileBox.containsPoint(center.x, center.y, dpToPx(20)))
                    drawProfileCircle(canvas, center, profile);
                }

            }
        }

        private void drawProfileCircle(Canvas canvas, PointF center, String profile) {
            canvas.drawCircle(center.x, center.y, circleSize, circlePaint);

            Path ring = new Path();
            ring.addCircle(center.x, center.y, circleSize, Path.Direction.CW);
            updateBorderPaint(profileValues.get(profile).first, dpToPx(3));
            canvas.drawPath(ring, borderPaint);

            Bitmap icon = profileValues.get(profile).second;
            int iconX = icon.getWidth() / 2;
            int iconY = icon.getHeight() / 2;
            canvas.drawBitmap(icon, center.x - iconX, center.y - iconY, null);
        }

        @Override
        protected void updateLocalPaint(Paint p) {
            super.updateLocalPaint(p);
            paint.setPathEffect(null);
            paint.setStrokeWidth(dpToPx(8));
        }

        private void updateCirclePaint() {
            if (circlePaint == null) {
                circlePaint = new Paint();
                circlePaint.setColor(Color.WHITE);
                circlePaint.setStyle(Paint.Style.FILL);
            }
        }

        private void updateBorderPaint(int color, float pixWidth) {
            if (borderPaint == null) {
                borderPaint = new Paint();
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeCap(Paint.Cap.BUTT);
            }
            borderPaint.setColor(ColorUtils.blendARGB(color, Color.BLACK, 0.2f));
            borderPaint.setStrokeWidth(pixWidth);
        }

        private void addIconPosition(PointF start, PointF end, float pathLength, float lengthRemaining, String currentProfile) {
            PointF iconPosition = getPointOnSection(start, end, pathLength, lengthRemaining);
            List<PointF> positions = profileIconsPositions.get(currentProfile);
            if (positions == null) {
                positions = new ArrayList<>();
                profileIconsPositions.put(currentProfile, positions);
            }
            positions.add(iconPosition);
        }

        private PointF getPointOnSection(PointF start, PointF end, float sectionLength, float remainingLength) {
            float ratio = remainingLength / sectionLength;
            float diffX = ratio * (end.x - start.x);
            float diffY = ratio * (end.y - start.y);
            return new PointF(start.x + diffX, start.y + diffY);
        }

        private float getRemainingLength(RotatedTileBox tileBox, List<WptPt> points) {
            Path path = new Path();
            PointF start = getPointFromWpt(tileBox, points.get(leftPtIdx));
            path.moveTo(start.x, start.y);
            for (int i = leftPtIdx + 1; i <= rightPtIdx; i++) {
                PointF end = getPointFromWpt(tileBox, points.get(i));
                path.lineTo(end.x, end.y);
            }
            float routeHalfLength = new PathMeasure(path, false).getLength() / 2;
            return routeHalfLength < iconOffset ? -1 : routeHalfLength;
        }

        private String getProfile(List<WptPt> points) {
            int idx = updateLeftIdx();
            WptPt userWpt = idx == -1 ? null : points.get(leftPtIdx);
            updateRightIdx();
            return userWpt.getProfileType();
        }

        private int updateLeftIdx() {
            leftPtIdx = updateUserPtIdx();
            return leftPtIdx;
        }

        private int updateRightIdx() {
            userPtIdx++;
            rightPtIdx = updateUserPtIdx();
            return rightPtIdx;
        }

        private int updateUserPtIdx() {
            for (int i = userPtIdx; i < points.size(); i++) {
                if (points.get(i).hasProfile()) {
                    userPtIdx = i;
                    return userPtIdx;
                }
            }
            userPtIdx = -1;
            return userPtIdx;
        }

        private PointF getPointFromWpt(RotatedTileBox tileBox, WptPt pt) {
            float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
            float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);
            return new PointF(x, y);
        }

        private float dpToPx(float dp) {
            return (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, displayMetrics);
        }

        @Override
        public void drawSingleSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {
            super.drawSingleSegment(zoom, p, canvas, tileBox);
            drawProfileIcons(canvas, tileBox);
        }

        @Override
        public void startCuller(double newZoom) {
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
