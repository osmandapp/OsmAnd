package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.AsyncTask;

import net.osmand.Location;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public abstract class Renderable {

    public enum Priority {

        CURRENT(1000),
        ALTITUDE(1),
        SPEED(0),
        DISTANCE(500),
        STANDARD(300),
        ARROWS(600);

        int priority;
        Priority(int priority) {
            this.priority = priority;
        }
    }


    static private Timer t = null;                      // fires a repaint for animating segments
    static private int conveyor = 0;                    // single cycler for 'conveyor' style renders
    static private OsmandMapTileView view = null;       // for paint refresh

    // If any render wants to have animation, something needs to make a one-off call to 'startScreenRefresh'
    // to setup a timer to periodically force a screen refresh/redraw

    public static void startScreenRefresh(OsmandMapTileView v, long period) {
        view = v;
        if (t==null && v != null) {
            t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    conveyor++;
                    view.refreshMap();
                }
            }, 0, period);
        }
    }


    public List<GPXUtilities.WptPt> points = null;               // Original list of points
    protected List<WptPt2> culled;                               // Reduced/resampled list of points
    protected int pointSize;
    protected double epsilon;

    protected QuadRect trackBounds;
    protected double zoom = -1;
    protected AsynchronousResampler culler = null;              // The currently active resampler
    protected Paint paint = null;                               // MUST be set by 'updateLocalPaint' before use
    protected int priority;

    public Renderable(Priority priority, OsmandMapTileView view, List<GPXUtilities.WptPt> points, double epsilon) {

        assert view != null;

        this.priority = priority.priority;
        this.points = points;
        this.epsilon = epsilon;
        this.view = view;

        culled = new ArrayList<>();

        trackBounds = new QuadRect(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        updateBounds(points, 0);
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

    protected void seedCulledTrack() {
        for (GPXUtilities.WptPt pt : points) {
            culled.add(new WptPt2(pt));
        }
    }

    protected AsynchronousResampler Factory() {
        return null;
    };

    protected void startCuller(double newZoom) {
        if (zoom != newZoom) {
            if (culler != null) {
                culler.cancel(true);
            }
            zoom = newZoom;
            culler = Factory();
            culler.execute("");
        }
    }

    protected void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {}

    public void drawSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {
        if (QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) { // is visible?
            startCuller(zoom);
            drawSingleSegment(p, canvas, tileBox);
        }
    }

    public int getPriority() {
        return priority;
    }

    protected void updateBounds(List<GPXUtilities.WptPt> pts, int startIndex) {
        for (int i = startIndex; i < pts.size(); i++) {
            GPXUtilities.WptPt pt = pts.get(i);
            trackBounds.right = Math.max(trackBounds.right, pt.lon);
            trackBounds.left = Math.min(trackBounds.left, pt.lon);
            trackBounds.top = Math.max(trackBounds.top, pt.lat);
            trackBounds.bottom = Math.min(trackBounds.bottom, pt.lat);
        }
        pointSize = pts.size();
    }

    public void setRDP(List<WptPt2> cull) {
        culled = cull;
        view.refreshMap();
    }

    protected void basicDraw (Canvas canvas, RotatedTileBox tileBox) {

        canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
        QuadRect tileBounds = tileBox.getLatLonBounds();

        Path path = new Path();
        WptPt2 lastPt = culled.get(0);
        boolean reCalculateLastXY = true;

        int size = culled.size();
        for (int i = 1; i < size; i++) {
            WptPt2 pt = culled.get(i);

            if (Math.min(pt.lon, lastPt.lon) < tileBounds.right && Math.max(pt.lon, lastPt.lon) > tileBounds.left
                    && Math.min(pt.lat, lastPt.lat) < tileBounds.top && Math.max(pt.lat, lastPt.lat) > tileBounds.bottom) {

                if (reCalculateLastXY) {
                    reCalculateLastXY = false;
                    path.moveTo(tileBox.getPixXFromLatLon(lastPt.lat, lastPt.lon), tileBox.getPixYFromLatLon(lastPt.lat, lastPt.lon));
                }
                path.lineTo(tileBox.getPixXFromLatLon(pt.lat, pt.lon), tileBox.getPixYFromLatLon(pt.lat, pt.lon));
            } else {
                reCalculateLastXY = true;
            }
            lastPt = pt;
        }

        canvas.drawPath(path, paint);
        canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
    }

    //----------------------------------------------------------------------------------------------

    public static class StandardTrack extends Renderable {

        public StandardTrack(OsmandMapTileView view, List<GPXUtilities.WptPt> pt, double epsilon) {
            super(Priority.STANDARD, view, pt, epsilon);
            seedCulledTrack();
        }

        @Override protected AsynchronousResampler Factory() {
            return new AsynchronousResampler.RamerDouglasPeucer(this, Math.pow(2.0, epsilon - zoom));
        }

        @Override protected void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {
            if (culled.size() > 1) {
                updateLocalPaint(p);
                basicDraw(canvas, tileBox);
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class CurrentTrack extends Renderable {

        private boolean forceCull = false;

        public CurrentTrack(OsmandMapTileView view, List<GPXUtilities.WptPt> pt, double base) {
            super(Priority.CURRENT, view, pt, base);
        }

        @Override protected void startCuller(double newZoom) {

            if ((forceCull && culler != null && culler.getStatus() == AsyncTask.Status.FINISHED)
                    || zoom != newZoom) {
                forceCull = false;
                zoom = newZoom;
                culler = new AsynchronousResampler.RamerDouglasPeucer(this, Math.pow(2.0, epsilon - zoom));
                culler.execute("");
            }
        }

        @Override public void drawSegment(double zoom, Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (pointSize != points.size()) {
                for (int i = pointSize; i < points.size(); i++) {
                    culled.add(new WptPt2(points.get(i)));
                }
                updateBounds(points, pointSize);
                forceCull = true;
            }

            if (QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) { // is visible?
                startCuller(zoom);
                if (culled.size() > 1 ) {
                    updateLocalPaint(p);
                    basicDraw(canvas, tileBox);
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    private abstract static class ColourBand extends Renderable {

        protected double widthZoom;

        public ColourBand(Priority priority, OsmandMapTileView view, List<GPXUtilities.WptPt> pt, double epsilon, double widthZoom) {
            super(priority, view, pt, epsilon);
            this.widthZoom = widthZoom;
        }

        @Override protected void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled.size() > 1) {
                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float bandWidth = paint.getStrokeWidth() * (float)widthZoom;
                paint.setStrokeWidth(bandWidth);

                float clipL = -bandWidth / 2;
                float clipB = -bandWidth / 2;
                float clipT = canvas.getHeight() + bandWidth / 2;
                float clipR = canvas.getWidth() + bandWidth / 2;

                WptPt2 pt = culled.get(0);
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

    public static class Speed extends ColourBand {

        public Speed(OsmandMapTileView view, List<GPXUtilities.WptPt> pt, double epsilon, double widthZoom) {
            super(Priority.SPEED, view, pt, epsilon, widthZoom);
        }

        @Override protected AsynchronousResampler Factory() {
            return new AsynchronousResampler.Speed(this, Math.pow(2.0, epsilon - zoom));
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Altitude extends ColourBand {

        public Altitude(OsmandMapTileView view, List<GPXUtilities.WptPt> pt, double epsilon, double widthZoom) {
            super(Priority.ALTITUDE, view, pt, epsilon, widthZoom);
        }

        @Override protected AsynchronousResampler Factory() {
            return new AsynchronousResampler.Altitude(this, Math.pow(2.0, epsilon - zoom));
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Distance extends Renderable {

        public enum unit {
            METRES (1, "m", 21),
            METERS (1, "m", 21),
            KILOMETRES (1000, "km", 12),
            KILOMETERS (1000, "km", 12),
            MILES (1609.344, "mi.", 11),
            YARDS (0.9144, "yd.", 21),
            FEET (0.3048, "ft.", 21);

            double value;
            String abbreviation;
            int visible;

            unit(double value, String abbreviation, int visible) {
                this.value = value;
                this.abbreviation = abbreviation;
                this.visible = visible;
            }
        }

        private unit option;

        public Distance(OsmandMapTileView view, List<GPXUtilities.WptPt> pt, unit segment) {
            super(Priority.DISTANCE, view, pt, segment.value);
            this.option = segment;
        }

        @Override protected void startCuller(double zoom) {
            this.zoom = zoom;
            if (culler == null) {
                culler = new AsynchronousResampler.Generic(this, option.value);       // once
                culler.execute("");
            }
        }

        @Override protected void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (!culled.isEmpty() && zoom > option.visible) {

                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float scale = 11 * tileBox.getDensity();
                paint.setTextSize(scale);

                float stroke = paint.getStrokeWidth();

                for (int i = culled.size()-1; --i >= 0;) {

                    WptPt2 pt = culled.get(i);
                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    Rect bounds = new Rect();
                    String lab = String.format("  %d %s", (int) ((pt.distance / option.value) + 0.5), option.abbreviation);
                    paint.getTextBounds(lab, 0, lab.length(), bounds);

                    int rectH =  bounds.height();
                    int rectW =  bounds.width();

                    if (x < canvas.getWidth() + rectW && x > -rectW
                            && y < canvas.getHeight() + rectH/2f && y > -rectH/2f) {

                        //paint.setStrokeWidth(2f * tileBox.getDensity());
                        //paint.setStyle(Paint.Style.STROKE);
                        //paint.setColor(Color.WHITE);
                        //canvas.drawText(lab, x, y + rectH / 2, paint);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setStrokeWidth(1.5f * tileBox.getDensity());
                        paint.setColor(Color.BLACK);
                        canvas.drawText(lab, x, y + rectH / 2, paint);

                        paint.setStrokeWidth(stroke * 2);
                        canvas.drawPoint(x, y, paint);
                    }
                    canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                }
            }
        }
    }


    //----------------------------------------------------------------------------------------------

    public static class Arrows extends Renderable {

        private int cachedC;

        public Arrows(OsmandMapTileView view, List<GPXUtilities.WptPt> pt, double epsilon, long refreshRate) {
            super(Priority.ARROWS, view, pt, epsilon);
            Renderable.startScreenRefresh(view, refreshRate);
        }

        @Override protected AsynchronousResampler Factory() {
            return new AsynchronousResampler.Generic(this, 10 * Math.pow(2.0, epsilon - zoom));
        }

        protected void startCuller(double newZoom) {
            if (zoom != newZoom) {
                if (culler != null) {
                    culler.cancel(true);
                }
                zoom = newZoom;
                culled.clear();
                culler = Factory();
                culler.execute("");
            }
        }


        private void drawArrows(Canvas canvas, RotatedTileBox tileBox) {

            Path path = new Path();

            double zoomlimit = zoom > 15 ? 15f : zoom;
            float arrowSize = (float) Math.pow(2.0, zoomlimit - 18) * 500; //800;
            boolean broken = true;
            int intp = cachedC;                                // the segment cycler

            float clipL = -arrowSize;
            float clipB = -arrowSize;
            float clipT = canvas.getHeight() + arrowSize;
            float clipR = canvas.getWidth() + arrowSize;

            WptPt2 pt = culled.get(0);
            float lastx = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
            float lasty = tileBox.getPixYFromLatLon(pt.lat, pt.lon);


            int size = culled.size();
            for (int i = 1; i < size; i++) {

                pt = culled.get(i);
                intp--;                                         // increment to go the other way!

                float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                boolean nextBroken = true;

                if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                        && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {

                    float segment = intp & 15;
                    if (segment < 3) {

                        if (broken) {
                            path.moveTo(lastx, lasty);
                        }
                        path.lineTo(x,y);

                        nextBroken = false;
                        // arrowhead...
                        if (segment == 0 && lasty != Float.NEGATIVE_INFINITY) {
                            double angle = Math.atan2(lasty - y, lastx - x);

                            float extendx = x - (float) Math.cos(angle) * arrowSize / 2;
                            float extendy = y - (float) Math.sin(angle) * arrowSize / 2;
                            float newx1 = extendx + (float) Math.cos(angle - 0.4) * arrowSize;
                            float newy1 = extendy + (float) Math.sin(angle - 0.4) * arrowSize;
                            float newx2 = extendx + (float) Math.cos(angle + 0.4) * arrowSize;
                            float newy2 = extendy + (float) Math.sin(angle + 0.4) * arrowSize;

                            path.lineTo(extendx, extendy);
                            path.moveTo(newx1, newy1);
                            path.lineTo(extendx, extendy);
                            path.lineTo(newx2, newy2);
                        }
                    }
                }
                broken = nextBroken;
                lastx = x;
                lasty = y;
            }


            float stroke = paint.getStrokeWidth();
            paint.setStrokeWidth(40f);
            paint.setColor(0xFFFF00FF);
            canvas.drawPath(path, paint);

            paint.setStrokeWidth(32f);
            paint.setColor(0xFF200020);
            canvas.drawPath(path, paint);
        }

        @Override
        public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (!culled.isEmpty() && zoom > 8) {
                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                cachedC = conveyor;
                drawArrows(canvas, tileBox);
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            }
        }
    }


    //----------------------------------------------------------------------------------------------

    public static class RouteMarker extends Renderable {

        public RouteMarker(OsmandMapTileView view, List<GPXUtilities.WptPt> pt, double epsilon, long refreshRate) {
            super(Priority.ARROWS, view, pt, epsilon);
            Renderable.startScreenRefresh(view, refreshRate);
        }

        @Override protected AsynchronousResampler Factory() {
            return new AsynchronousResampler.RouteMarker(this, 20 * Math.pow(2.0, epsilon - zoom));
        }

        protected void startCuller(double newZoom) {
            if (zoom != newZoom) {
                if (culler != null) {
                    culler.cancel(true);
                }
                zoom = newZoom;
                culled.clear();
                culler = Factory();
                culler.execute("");
            }
        }

        private void drawArrows(Canvas canvas, RotatedTileBox tileBox, boolean actionPoints) {

            paint.setStrokeJoin(Paint.Join.MITER);

            List<Path> paths = new ArrayList<>();
            Path path = new Path();
            paths.add(path);


            float stroke = paint.getStrokeWidth();
            float arrowSize = 64f;                       //(float) Math.pow(2.0, zoomlimit - 18) * 500; //800;

            float clipL = -arrowSize;
            float clipB = -arrowSize;
            float clipT = canvas.getHeight() + arrowSize;
            float clipR = canvas.getWidth() + arrowSize;

            float lastx = 0;
            float lasty = Float.NEGATIVE_INFINITY;

            boolean broken = true;

            int size = culled.size();
            for (int i = 0; i < size; i++) {

                WptPt2 pt = culled.get(i);

                float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                        && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {

                    double angle = Math.atan2(lasty - y, lastx - x);
                    float extendx = x - (float) Math.cos(angle) * arrowSize / 4;
                    float extendy = y - (float) Math.sin(angle) * arrowSize / 4;
                    float newx1 = extendx + (float) Math.cos(angle - 0.6) * arrowSize;
                    float newy1 = extendy + (float) Math.sin(angle - 0.6) * arrowSize;
                    float newx2 = extendx + (float) Math.cos(angle + 0.6) * arrowSize;
                    float newy2 = extendy + (float) Math.sin(angle + 0.6) * arrowSize;

                    int clr = pt.colourARGB;
                    if (actionPoints && clr == Color.YELLOW) {

                        if (broken) {
                            path.moveTo(lastx, lasty);
                            broken = false;
                        }
                        path.lineTo(x, y);

                        // look for end of line (requiring an arrow)...
                        if ((i == size-1 || i < size - 1 && culled.get(i + 1).colourARGB != Color.YELLOW)) {
                            path.lineTo(extendx, extendy);
                            path.moveTo(newx2, newy2);
                            path.lineTo(extendx, extendy);
                            path.lineTo(newx1, newy1);

                            path = new Path();
                            paths.add(path);
                        }
                    } else if (!actionPoints && clr == Color.BLACK) {
                        path.moveTo(newx2, newy2);
                        path.lineTo(extendx, extendy);
                        path.lineTo(newx1, newy1);
                    } else {
                        broken = true;
                    }
                } else {
                    broken = true;
                }
                lastx = x;
                lasty = y;
            }

            if (actionPoints) {
                for (Path path2 : paths) {
                    paint.setStrokeWidth(3f * stroke);
                    paint.setColor(Color.BLACK);
                    canvas.drawPath(path2, paint);
                    paint.setColor(0xFFFFE000);
                    paint.setStrokeWidth(2f * stroke);
                    canvas.drawPath(path2, paint);
                }

            } else {
                for (Path path2 : paths) {
                    paint.setStrokeWidth(2f * stroke);
                    paint.setColor(Color.BLACK);
                    canvas.drawPath(path2, paint);
                    paint.setStrokeWidth(1.2f * stroke);
                    paint.setColor(0x80FF00FF);
                    canvas.drawPath(path2, paint);
                }
            }

            paint.setStrokeWidth(stroke);
        }

        @Override
        public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (!culled.isEmpty() && zoom > 10) {
                updateLocalPaint(p);
                paint.setStrokeWidth(12.0f);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                drawArrows(canvas, tileBox, false);
                drawArrows(canvas, tileBox, true);
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            }
        }
    }
}
