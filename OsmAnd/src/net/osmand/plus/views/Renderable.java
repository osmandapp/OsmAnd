package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class Renderable {

    // This class handles the actual drawing of segment 'layers'. A segment is a piece of track
    // (i.e., a list of WptPt) which has renders attached to it. There can be any number of renders
    // layered upon each other to give multiple effects.

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


    //----------------------------------------------------------------------------------------------

    public static abstract class RenderableSegment {

        public List<WptPt> points = null;              // Original list of points
        protected List<WptPt> culled = null;           // Reduced/resampled list of points

        protected QuadRect trackBounds;
        double zoom = -1;
        AsynchronousResampler culler = null;                        // The currently active resampler
        protected Paint paint = null;                   // MUST be set by 'updateLocalPaint' before use

        public RenderableSegment(List<WptPt> pt) {
            points = pt;
            calculateBounds(points);
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

        public void recalculateRenderScale(double zoom) {}
        public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {}

        private void calculateBounds(List<WptPt> pts) {
            trackBounds = new QuadRect(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            updateBounds(pts, 0);
        }

        public void updateBounds(List<WptPt> pts, int startIndex) {
            int size = pts.size();
            for (int i = startIndex; i < size; i++) {
                WptPt pt = pts.get(i);
                trackBounds.right = Math.max(trackBounds.right, pt.lon);
                trackBounds.left = Math.min(trackBounds.left, pt.lon);
                trackBounds.top = Math.max(trackBounds.top, pt.lat);
                trackBounds.bottom = Math.min(trackBounds.bottom, pt.lat);
            }
        }

        // When the asynchronous task has finished, it calls this function to set the 'culled' list
        public void setRDP(List<WptPt> cull) {
            culled = cull;
            //calculateBounds(culled);
        }

        protected void draw(List<WptPt> pts, Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (pts.size() > 1 && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                
                float stroke = paint.getStrokeWidth() / 2;

                float clipL = -stroke;
                float clipB = -stroke;
                float clipT = canvas.getHeight() + stroke;
                float clipR = canvas.getWidth() + stroke;

                WptPt pt = pts.get(0);
                float lastx = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float lasty = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                int size = pts.size();
                for (int i = 1; i < size; i++) {
                    pt = pts.get(i);

                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                            && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {
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

    public static class StandardTrack extends RenderableSegment {

        double base;     // parameter for calculating Ramer-Douglas-Peucer distance

        public StandardTrack(List<WptPt> pt, double base) {
            super(pt);
            this.base = base;
        }

        @Override public void recalculateRenderScale(double newZoom) {

            // Here we create the 'shadow' resampled/culled points list, based on the asynchronous call.
            // The asynchronous callback will set the variable 'culled', and that is preferentially used for rendering
            // The current track does not undergo this process, as it is potentially constantly changing.

            if (zoom != newZoom) {

                if (culler != null) {
                    culler.cancel(true);
                }

                if (zoom < newZoom) {            // if line would look worse (we're zooming in) then...
                    culled = null;                 // use full-resolution until re-cull complete
                }
                zoom = newZoom;

                double cullDistance = Math.pow(2.0, base - zoom);
                culler = new AsynchronousResampler.RamerDouglasPeucer(this, cullDistance);
                culler.execute("");
            }
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {
            draw(culled == null ? points : culled, p, canvas, tileBox);
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Altitude extends RenderableSegment {

        protected double segmentSize;

        public Altitude(List<WptPt> pt, double segmentSize) {
            super(pt);
            this.segmentSize = segmentSize;
        }

        @Override public void recalculateRenderScale(double zoom) {
            if (culled == null && culler == null) {
                culler = new AsynchronousResampler.ResampleAltitude(this, segmentSize);     // once only!
                culler.execute("");
            }
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled != null && culled.size() > 1
                && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float bandWidth = paint.getStrokeWidth() * 3;
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

        public Speed(List<WptPt> pt, double segmentSize) {
            super(pt, segmentSize);
        }

        @Override public void recalculateRenderScale(double zoom) {
            if (culled == null && culler == null) {
                culler = new AsynchronousResampler.ResampleSpeed(this, segmentSize);        // once only!
                culler.execute("");
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Conveyor extends RenderableSegment {

        private double segmentSize;

        public Conveyor(List<WptPt> pt, OsmandMapTileView view, double segmentSize, long refreshRate) {
            super(pt);
            this.segmentSize = segmentSize;

            Renderable.startScreenRefresh(view, refreshRate);
        }

        @Override public void recalculateRenderScale(double zoom) {
            if (culled == null && culler == null) {
                culler = new AsynchronousResampler.GenericResampler(this, segmentSize);     // once only!
                culler.execute("");
            }
        }

        private int getComplementaryColor(int colorToInvert) {
            float[] hsv = new float[3];
            Color.RGBToHSV(Color.red(colorToInvert), Color.green(colorToInvert), Color.blue(colorToInvert), hsv);
            hsv[0] = (hsv[0] + 180) % 360;
            return Color.HSVToColor(hsv);
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled != null && culled.size() > 1
                   && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                paint.setColor(getComplementaryColor(p.getColor()));

                float strokeRadius = paint.getStrokeWidth() / 2;

                float clipL = -strokeRadius;
                float clipB = -strokeRadius;
                float clipT = canvas.getHeight() + strokeRadius;
                float clipR = canvas.getWidth() + strokeRadius;

                WptPt pt = culled.get(0);
                float lastx = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float lasty = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                int intp = conveyor;

                int size = culled.size();
                for (int i = 1; i < size; i++, intp--) {
                    pt = culled.get(i);

                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if ((intp & 7) < 3) {
                        if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                                && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {
                            canvas.drawLine(lastx, lasty, x, y, paint);
                        }
                    }
                    lastx = x;
                    lasty = y;
                }
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class DistanceMarker extends RenderableSegment {

        private double segmentSize;

        public DistanceMarker(List<WptPt> pt, double segmentSize) {
            super(pt);
            this.segmentSize = segmentSize;
        }

        @Override public void recalculateRenderScale(double zoom) {
            this.zoom = zoom;
            if (culled == null && culler == null) {
                culler = new AsynchronousResampler.GenericResampler(this, segmentSize);     // once only
                culler.execute("");
            }
        }

        private String getKmLabel(double value) {
            String lab;
            lab = String.format("%d",(int)((value+0.5)/1000));
            return lab;
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled != null && !culled.isEmpty() && zoom > 12
                    && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float scale = 50;
                float stroke = paint.getStrokeWidth();

                for (int i = culled.size()-1; --i >= 0;) {

                    WptPt pt = culled.get(i);
                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    paint.setTextSize(scale);
                    paint.setStrokeWidth(3);

                    Rect bounds = new Rect();
                    String lab = getKmLabel(pt.getDistance());
                    paint.getTextBounds(lab, 0, lab.length(), bounds);

                    int rectH =  bounds.height();
                    int rectW =  bounds.width();

                    if (x < canvas.getWidth() + rectW/2 + scale && x > -rectW/2 + scale
                            && y < canvas.getHeight() + rectH/2f && y > -rectH/2f) {

                        paint.setColor(Color.BLACK);
                        paint.setStrokeWidth(stroke);
                        canvas.drawPoint(x, y, paint);
                        paint.setStrokeWidth(4);
                        canvas.drawText(lab,x-rectW/2+40,y+rectH/2,paint);
                    }
                    canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Arrows extends RenderableSegment {

        private double segmentSize;
        private int cachedC;

        public Arrows(List<WptPt> pt, OsmandMapTileView view, double segmentSize, long refreshRate) {
            super(pt);
            this.segmentSize = segmentSize;

            Renderable.startScreenRefresh(view, refreshRate);
        }

        @Override public void recalculateRenderScale(double zoom) {
            this.zoom = zoom;
            if (culled == null && culler == null) {
                culler = new AsynchronousResampler.GenericResampler(this, segmentSize);     // once
                culler.execute("");
            }
        }

        private void drawArrows(Canvas canvas, RotatedTileBox tileBox, boolean internal) {

            float scale = internal ? 0.8f : 1.0f;

            float stroke = paint.getStrokeWidth();
            double zoomlimit = zoom > 15 ? 15f : zoom;
            float arrowSize = (float) Math.pow(2.0,zoomlimit-18) * 800;
            boolean broken = true;
            int intp = cachedC;                                // the segment cycler

            float clipL = -arrowSize;
            float clipB = -arrowSize;
            float clipT = canvas.getHeight() + arrowSize;
            float clipR = canvas.getWidth() + arrowSize;

            float lastx = 0;
            float lasty = Float.NEGATIVE_INFINITY;

            int size = culled.size();
            for (int i = 0; i < size; i++) {
                WptPt pt = culled.get(i);

                intp--;                                         // increment to go the other way!

                float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                boolean nextBroken = true;

                if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                        && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {

                    float segment = intp & 15;
                    if (segment < 6) {

                        paint.setColor(internal ? Algorithms.getRainbowColor(((double) (i)) / ((double) size)) : Color.BLACK);

                        float segpiece = 6 - segment;
                        if (segpiece > 4)
                            segpiece = 4;

                        if (!broken) {
                            float sw = stroke * segpiece * scale;
                            paint.setStrokeWidth(sw);
                            canvas.drawLine(lastx, lasty, x, y, paint);
                        }
                        nextBroken = false;
                        // arrowhead...
                        if (segment == 0 && lasty != -Float.NEGATIVE_INFINITY) {
                            float sw = stroke * (6 - segment) / 2f * scale;
                            paint.setStrokeWidth(sw);
                            double angle = Math.atan2(lasty - y, lastx - x);

                            float extendx = x - (float) Math.cos(angle) * arrowSize / 2;
                            float extendy = y - (float) Math.sin(angle) * arrowSize / 2;
                            float newx1 = extendx + (float) Math.cos(angle - 0.4) * arrowSize;
                            float newy1 = extendy + (float) Math.sin(angle - 0.4) * arrowSize;
                            float newx2 = extendx + (float) Math.cos(angle + 0.4) * arrowSize;
                            float newy2 = extendy + (float) Math.sin(angle + 0.4) * arrowSize;

                            canvas.drawLine(extendx, extendy, x, y, paint);
                            canvas.drawLine(newx1, newy1, extendx, extendy, paint);
                            canvas.drawLine(newx2, newy2, extendx, extendy, paint);
                        }
                    }
                }
                broken = nextBroken;
                lastx = x;
                lasty = y;
            }
            paint.setStrokeWidth(stroke);
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled != null && !culled.isEmpty() && zoom > 13
                    && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                updateLocalPaint(p);
                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                cachedC = conveyor;
                drawArrows(canvas, tileBox, false);
                drawArrows(canvas, tileBox, true);
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class CurrentTrack extends RenderableSegment {

        private int size;

        public CurrentTrack(List<WptPt> pt) {
            super(pt);
            size = pt.size();
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {
            if (points.size() != size) {
                updateBounds(points, size);        // use newly added points to recalculate bounding box
                size = points.size();
            }
            draw(points, p, canvas, tileBox);
        }
    }

}
