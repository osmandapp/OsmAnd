package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapAlgorithms;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import gnu.trove.list.array.TIntArrayList;


public class Renderable {

    // This class handles the actual drawing of segment 'layers'. A segment is a piece of track
    // (i.e., a list of WptPt) which has renders attached to it. There can be any number of renders
    // layered upon each other to give multiple effects.

    public enum RenderType {
        ORIGINAL,             // Auto-resizing using Ramer-Douglas-Peucer algorithm
        DISTANCE,             // markers at given distance
        CONVEYOR,             // arrows/direction movers
        ALTITUDE,             // colour-rainbow altitude band
        SPEED,                // colour-rainbow speed band
    }


    static private Timer t = null;                      // fires a repaint for animating segments
    static private int conveyor = 0;                    // single cycler for 'conveyor' style renders
    static private OsmandMapTileView view = null;       // for paint refresh


    // If any render wants to have animation, something needs to make a one-off call to 'startScreenRefresh'
    // to setup a timer to periodically force a screen refresh/redraw

    public static void startScreenRefresh(OsmandMapTileView v, double period) {
        view = v;
        if (t==null && v != null) {
            t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    conveyor = (conveyor+1)&31;             // mask/wrap to avoid boundary issues
                    view.refreshMap();
                }
            }, 0, (long) period);
        }
    }


    //----------------------------------------------------------------------------------------------

    public static class RenderableSegment {


        protected RenderType renderType;
        protected List<GPXUtilities.WptPt> points = null;           // Original list of points
        protected List<GPXUtilities.WptPt> culled = null;           // Reduced/resampled list of points

        double hash;
        double param1,param2;
        boolean shadow = false;         // TODO: fixup shadow support

        AsynchronousResampler culler = null;                        // The currently active resampler

        public List<GPXUtilities.WptPt> getPoints() { return points; }


        public RenderableSegment(RenderType type, List<GPXUtilities.WptPt> pt, double param1, double param2) {

            hash = 0;
            culled = null;
            renderType = type;
            this.param1 = param1;
            this.param2 = param2;
            points = pt;
        }


        // When the asynchronous task has finished, it calls this function to set the 'culled' list
        public void setRDP(List<GPXUtilities.WptPt> cull) {
            culled = cull;
        }


        // When there is a zoom change OR the list of points changes, then we want to trigger a
        // cull of the original point list (for example, Ramer-Douglas-Peucer algorithm or a
        // simple distance-based resampler.  The cull operates asynchronously and results will be
        // returned into 'culled' via 'setRDP' algorithm (above).

        // Notes:
        // 1. If a new cull is triggered, then the existing one is immediately discarded
        //    so that we don't 'zoom in' to low-resolution tracks and see poor visuals.
        // 2. Individual derived classes (altitude, speed, etc) can override this routine to
        //    ensure that the cull only ever happens once.

        public void recalculateRenderScale(OsmandMapTileView view) {

            // Here we create the 'shadow' resampled/culled points list, based on the asynchronous call.
            // The asynchronous callback will set the variable, and that is used for rendering

            double zoom = view.getZoom();

            if (points != null) {
                double hashCode = points.hashCode() + zoom;
                if (culled == null || hash != hashCode) {
                    if (culler != null)
                        culler.cancel(true);                // stop any still-running cull
                    hash = hashCode;
                    double cullDistance = Math.pow(2.0,param1-zoom);
                    culler = new AsynchronousResampler(renderType, view, this, cullDistance, param2);
                    culled = null;                // effectively use full-resolution until re-cull complete (see [Note 1] below)
                    culler.execute("");
                }
            }
        }


        public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            List<GPXUtilities.WptPt> pts = culled == null? points: culled;			// [Note 1]: use culled points preferentially

            final QuadRect latLonBounds = tileBox.getLatLonBounds();

            int startIndex = -1;
            int endIndex = -1;
            int prevCross = 0;
            double shift = 0;
            for (int i = 0; i < pts.size(); i++) {
                GPXUtilities.WptPt ls = pts.get(i);
                int cross = 0;
                cross |= (ls.lon < latLonBounds.left - shift ? 1 : 0);
                cross |= (ls.lon > latLonBounds.right + shift ? 2 : 0);
                cross |= (ls.lat > latLonBounds.top + shift ? 4 : 0);
                cross |= (ls.lat < latLonBounds.bottom - shift ? 8 : 0);
                if (i > 0) {
                    if ((prevCross & cross) == 0) {
                        if (endIndex == i - 1 && startIndex != -1) {
                            // continue previous line
                        } else {
                            // start new segment
                            if (startIndex >= 0) {
                                drawSegment(pts, p, canvas, tileBox, startIndex, endIndex);
                            }
                            startIndex = i - 1;
                        }
                        endIndex = i;
                    }
                }
                prevCross = cross;
            }
            if (startIndex != -1) {
                drawSegment(pts, p, canvas, tileBox, startIndex, endIndex);
            }
        }


        private void drawSegment(List<GPXUtilities.WptPt> pts, Paint paint, Canvas canvas, RotatedTileBox tb, int startIndex, int endIndex) {
            TIntArrayList tx = new TIntArrayList();
            TIntArrayList ty = new TIntArrayList();
            canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
            Path path = new Path();
            for (int i = startIndex; i <= endIndex; i++) {
                GPXUtilities.WptPt p = pts.get(i);
                tx.add((int)(tb.getPixXFromLatLon(p.lat, p.lon) + 0.5));
                ty.add((int)(tb.getPixYFromLatLon(p.lat, p.lon) + 0.5));
            }

//TODO: colour
            calculatePath(tb, tx, ty, path);

            if (shadow) {
                float sw = paint.getStrokeWidth();
                int col = paint.getColor();
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(sw + 4);
                canvas.drawPath(path, paint);
                paint.setStrokeWidth(sw);
                paint.setColor(col);
                canvas.drawPath(path, paint);
            } else
                canvas.drawPath(path, paint);


            canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
        }

        public int calculatePath(RotatedTileBox tb, TIntArrayList xs, TIntArrayList ys, Path path) {
            boolean start = false;
            int px = xs.get(0);
            int py = ys.get(0);
            int h = tb.getPixHeight();
            int w = tb.getPixWidth();
            int cnt = 0;
            boolean pin = isIn(px, py, w, h);
            for(int i = 1; i < xs.size(); i++) {
                int x = xs.get(i);
                int y = ys.get(i);
                boolean in = isIn(x, y, w, h);
                boolean draw = false;
                if(pin && in) {
                    draw = true;
                } else {
                    long intersection = MapAlgorithms.calculateIntersection(x, y,
                            px, py, 0, w, h, 0);
                    if (intersection != -1) {
                        px = (int) (intersection >> 32);
                        py = (int) (intersection & 0xffffffff);
                        draw = true;
                    }
                }
                if (draw) {
                    if (!start) {
                        cnt++;
                        path.moveTo(px, py);
                    }
                    path.lineTo(x, y);
                    start = true;
                } else{
                    start = false;
                }
                pin = in;
                px = x;
                py = y;
            }
            return cnt;
        }


        protected boolean isIn(float x, float y, float rx, float by) {
            return x >= 0f && x <= rx && y >= 0f && y <= by;
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class RenderableAltitude extends RenderableSegment {

        private Paint alphaPaint = null;
        private int alpha;
        protected float colorBandWidth;             // width of speed/altitude colour band

        public RenderableAltitude(RenderType type, List<GPXUtilities.WptPt> pt, double param1, double param2) {
            super(type, pt, param1, param2);

            alpha = (int)param2;
            alphaPaint = new Paint();
            alphaPaint.setStrokeCap(Paint.Cap.ROUND);

            colorBandWidth = 3.0f;
        }

        @Override public void recalculateRenderScale(OsmandMapTileView view) {
            if (culler == null && culled == null) {
                culler = new AsynchronousResampler(renderType, view, this, param1, param2);
                culler.execute("");
            }
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled != null && culled.size() > 0) {

                // Draws into a bitmap so that the lines can be drawn solid and the *ENTIRE* can be alpha-blended

                Bitmap newBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas2 = new Canvas(newBitmap);
                canvas2.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                alphaPaint.setAlpha(255);
                alphaPaint.setStrokeWidth(p.getStrokeWidth()*colorBandWidth);


                float lastx = Float.NEGATIVE_INFINITY;
                float lasty = 0;

                for (GPXUtilities.WptPt pt : culled) {
                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if (lasty != Float.NEGATIVE_INFINITY) {
                        alphaPaint.setColor(pt.colourARGB);
                        canvas2.drawLine(lastx, lasty, x, y, alphaPaint);
                    }

                    lastx = x;
                    lasty = y;
                }
                canvas2.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                alphaPaint.setAlpha(alpha);
                canvas.drawBitmap(newBitmap, 0, 0, alphaPaint);
            }
        }

    }

    //----------------------------------------------------------------------------------------------

    public static class RenderableSpeed extends RenderableAltitude {
        public RenderableSpeed(RenderType type, List<GPXUtilities.WptPt> pt, double param1, double param2) {
            super(type, pt, param1, param2);
            colorBandWidth = 3f;
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class RenderableConveyor extends RenderableSegment {

        private double zoom = 0;

        public RenderableConveyor(RenderType type, List<GPXUtilities.WptPt> pt, double param1, double param2) {
            super(type, pt, param1, param2);
        }

        @Override public void recalculateRenderScale(OsmandMapTileView view) {
            this.zoom = zoom;
            if (culled == null && culler == null) {       // i.e., do NOT resample when scaling - only allow a one-off generation
                culler = new AsynchronousResampler(renderType, view, this, param1, param2);
                culler.execute("");
            }
        }

        public int getComplementaryColor(int colorToInvert) {
            float[] hsv = new float[3];
            Color.RGBToHSV(Color.red(colorToInvert), Color.green(colorToInvert), Color.blue(colorToInvert), hsv);
            hsv[0] = (hsv[0] + 180) % 360;
            return Color.HSVToColor(hsv);
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            // This is a simple/experimental track subsegment 'conveyor' animator just to show how
            // effects of constant segment-size can be used for animation effects.  I've put an arrowhead
            // in just to show what can be done.  Very hacky, it's just a "hey look at this".

            if (culled == null)
                return;

            canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

            int pCol = p.getColor();
            float pSw = p.getStrokeWidth();

            //p.setStrokeWidth(pSw * 2f);                         // use a thicker line
            p.setColor(getComplementaryColor(p.getColor()));    // and a complementary colour

            float lastx = Float.NEGATIVE_INFINITY;
            float lasty = Float.NEGATIVE_INFINITY;
            Path path = new Path();

            int h = tileBox.getPixHeight();
            int w = tileBox.getPixWidth();
            boolean broken = true;
            int intp = conveyor;                                // the segment cycler
            for (GPXUtilities.WptPt pt : culled) {
                intp--;                                         // increment to go the other way!

                if ((intp & 7) < 3) {

                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if ((isIn(x, y, w, h) || isIn(lastx, lasty, w, h))) {
                        if (broken) {
                            path.moveTo(x, y);
                            broken = false;
                        } else
                            path.lineTo(x, y);
                        lastx = x;
                        lasty = y;
                    } else
                        broken = true;
                } else
                    broken = true;

            }

            canvas.drawPath(path, p);
            canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

            p.setStrokeWidth(pSw);
            p.setColor(pCol);
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class RenderableDot extends RenderableSegment {

        float dotScale;

        public RenderableDot(RenderType type, List<GPXUtilities.WptPt> pt, double param1, double param2) {
            super(type, pt, param1, param2);

            dotScale = (float) param2;
        }

        @Override public void recalculateRenderScale(OsmandMapTileView view) {
            if (culled == null && culler == null) {
                culler = new AsynchronousResampler(renderType, view, this, param1, param2);
                assert (culler != null);
                if (culler != null)
                    culler.execute("");
            }
        }

        private String getLabel(double value) {
            String lab;
            lab = String.format("%.2f km",value/1000.);
            return lab;
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            assert p != null;
            assert canvas != null;
            assert tileBox != null;

            try {

                if (culled == null)
                    return;

                Paint px = new Paint();
                assert (px != null);

                px.setStrokeCap(Paint.Cap.ROUND);

                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float sw = p.getStrokeWidth();
                float ds = sw * dotScale;

                int w = tileBox.getPixWidth();
                int h = tileBox.getPixHeight();

                for (GPXUtilities.WptPt pt : culled) {

                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if (isIn(x, y, w, h)) {

                        px.setColor(0xFF000000);
                        px.setStrokeWidth(ds + 2);
                        canvas.drawPoint(x, y, px);
                        px.setStrokeWidth(ds);
                        px.setColor(0xFFFFFFFF);
                        canvas.drawPoint(x, y, px);

                        //TODO: I do not know how to correctly handle screen density!
                        //TODO: modify the text size based on density calculations!!

                        if (sw > 6) {
                            px.setColor(Color.BLACK);
                            px.setStrokeWidth(1);
                            px.setTextSize(sw*2f); //<<< TODO fix
                            canvas.drawText(getLabel(pt.getDistance()), x + ds / 2, y + ds / 2, px);
                        }
                    }
                    canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                }
            } catch (Exception e) {
                String exception = e.getMessage();
                Throwable cause = e.getCause();

            }
        }
    }

}
