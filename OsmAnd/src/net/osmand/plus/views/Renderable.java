package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities;
import net.osmand.util.MapAlgorithms;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import gnu.trove.list.array.TIntArrayList;


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
                    conveyor = (conveyor+1)&31;             // mask/wrap to avoid boundary issues
                    view.refreshMap();
                }
            }, 0, period);
        }
    }


    //----------------------------------------------------------------------------------------------

    public static abstract class RenderableSegment {

        protected List<GPXUtilities.WptPt> points = null;           // Original list of points
        protected List<GPXUtilities.WptPt> culled = null;           // Reduced/resampled list of points

        protected QuadRect trackBounds = new QuadRect(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);


        double hashZoom;
        double hashPoint;

        boolean shadow = false;         // TODO: fixup shadow support

        AsynchronousResampler culler = null;                        // The currently active resampler

        public RenderableSegment(List<GPXUtilities.WptPt> pt) {
            points = pt;
            hashPoint = points.hashCode();
            hashZoom = 0;
            culled = null;
        }

        public void recalculateRenderScale(double zoom) {}
        public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {}

        // When the asynchronous task has finished, it calls this function to set the 'culled' list
        public void setRDP(List<GPXUtilities.WptPt> cull) {

            culled = cull;

            // Find the segment's bounding box, to allow quick draw rejection later

            trackBounds.left = trackBounds.bottom = Double.POSITIVE_INFINITY;
            trackBounds.right = trackBounds.top = Double.NEGATIVE_INFINITY;

            for (GPXUtilities.WptPt pt : culled) {
                trackBounds.right = Math.max(trackBounds.right, pt.lon);
                trackBounds.left = Math.min(trackBounds.left, pt.lon);
                trackBounds.top = Math.max(trackBounds.top, pt.lat);
                trackBounds.bottom = Math.min(trackBounds.bottom, pt.lat);
            }

            if (view != null) {
                view.refreshMap();          // force a redraw
            }
        }

        public List<GPXUtilities.WptPt> getPoints() {
            return points;
        }

        protected boolean isIn(float x, float y, float rx, float by) {
            return x >= 0f && x <= rx && y >= 0f && y <= by;
        }

    }
    //----------------------------------------------------------------------------------------------

    public static class StandardTrack extends RenderableSegment {

        double base;     // parameter for calculating Ramer-Douglas-Peucer distance

        public StandardTrack(List<GPXUtilities.WptPt> pt, double base) {
            super(pt);
            this.base = base;
        }


        // When there is a zoom change, then we want to trigger a
        // cull of the original point list (for example, Ramer-Douglas-Peucer algorithm or a
        // simple distance-based resampler.  The cull operates asynchronously and results will be
        // returned into 'culled' via 'setRDP' algorithm (above).

        // Notes:
        // 1. If a new cull is triggered, then the existing one is immediately discarded
        //    so that we don't 'zoom in' to low-resolution tracks and see poor visuals.
        // 2. Individual derived classes (altitude, speed, etc) can override this routine to
        //    ensure that the cull only ever happens once.

        @Override public void recalculateRenderScale(double zoom) {

            // Here we create the 'shadow' resampled/culled points list, based on the asynchronous call.
            // The asynchronous callback will set the variable, and that is preferentially used for rendering

            double hashCode = points.hashCode() ;

            if (hashPoint != hashCode) {            // current track, changing?
                if (culler != null) {
                    culler.cancel(true);            // STOP culling a track with changing points
                    culled =  null;                 // and force use of original track
                }
            } else if (culler == null || hashZoom != zoom) {

                if (culler != null) {
                    culler.cancel(true);
                }

                double cullDistance = Math.pow(2.0, base - zoom);
                culler = new AsynchronousResampler.RamerDouglasPeucer(this, cullDistance);

                if (hashZoom < zoom) {            // if line would look worse (we're zooming in) then...
                    culled = null;                // use full-resolution until re-cull complete
                }

                hashZoom = zoom;

                culler.execute("");

                // The trackBounds may be slightly inaccurate (unlikely, but...) so let's reset it
                //trackBounds.left = trackBounds.bottom = Double.POSITIVE_INFINITY;
                //trackBounds.right = trackBounds.bottom = Double.NEGATIVE_INFINITY;
            }
        }


        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            List<GPXUtilities.WptPt> pts = culled == null? points: culled;			// [Note 1]: use culled points preferentially

            QuadRect latLonBounds = tileBox.getLatLonBounds();
            if (!QuadRect.trivialOverlap(latLonBounds, trackBounds)) {
                return; // Not visible
            }

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
                        if (endIndex != i - 1 || startIndex == -1) {
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

            calculatePath(tb, tx, ty, path);

            if (shadow) {                               // needs work, but let's leave it like this for now
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
                        py = (int) (intersection & 0xffffffff);     //TODO: Surely this is just intersection...!
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
    }

    //----------------------------------------------------------------------------------------------

    public static class Altitude extends RenderableSegment {

        private Paint alphaPaint = null;
        private int alpha;
        protected float colorBandWidth;             // width of speed/altitude colour band
        protected double segmentSize;

        public Altitude(List<GPXUtilities.WptPt> pt, double segmentSize, int alpha) {
            super(pt);

            this.segmentSize = segmentSize;
            this.alpha = alpha;
            alphaPaint = new Paint();
            alphaPaint.setStrokeCap(Paint.Cap.ROUND);
            colorBandWidth = 16f;
        }

        @Override public void recalculateRenderScale(double zoom) {
            if (culled == null && culler == null) {
                culler = new AsynchronousResampler.ResampleAltitude(this, segmentSize);     // once only!
                culler.execute("");
            }
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled != null && !culled.isEmpty()
                && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                // Draws into a bitmap so that the lines can be drawn solid and the *ENTIRE* can be alpha-blended

                Bitmap newBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas2 = new Canvas(newBitmap);
                canvas2.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                alphaPaint.setAlpha(255);
                alphaPaint.setStrokeWidth(p.getStrokeWidth()*4.0f);  // colorBandWidth


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

    public static class SpeedColours extends Altitude {

        public SpeedColours(List<GPXUtilities.WptPt> pt, double segmentSize, int alpha) {
            super(pt, segmentSize, alpha);
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

        public Conveyor(List<GPXUtilities.WptPt> pt, OsmandMapTileView view, double segmentSize, long refreshRate) {
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

            // This is a simple/experimental track subsegment 'conveyor' animator just to show how
            // effects of constant segment-size can be used for animation effects.  I've put an arrowhead
            // in just to show what can be done.  Very hacky, it's just a "hey look at this".

            if (culled != null && !culled.isEmpty()
                   /* && !QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)*/ ) {

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
                            } else {
                                path.lineTo(x, y);
                            }
                            lastx = x;
                            lasty = y;
                        } else {
                            broken = true;
                        }
                    } else {
                        broken = true;
                    }
                }

                canvas.drawPath(path, p);
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                p.setStrokeWidth(pSw);
                p.setColor(pCol);
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class DistanceMarker extends RenderableSegment {
    // EXPERIMENTAL!

        private float dotScale;
        private double segmentSize;
        private OsmandMapTileView view;

        public DistanceMarker(List<GPXUtilities.WptPt> pt, OsmandMapTileView view, double segmentSize) {
            super(pt);
            this.view = view;
            this.dotScale = view.getScaleCoefficient();
            this.segmentSize = segmentSize;
        }

        @Override public void recalculateRenderScale(double zoom) {
            if (culled == null && culler == null) {
                culler = new AsynchronousResampler.GenericResampler(this, segmentSize);
                culler.execute("");
            }
        }

        private String getLabel(double value) {
            String lab;

            int dig2 = (int)(value / 10);
            if ((dig2%10)==0)
                lab = String.format("%d km",value/100);
            else
                lab = String.format("%.2f km", value/1000.);
            return lab;
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled != null && !culled.isEmpty()
                    && !QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                Paint px = new Paint();
                px.setStrokeCap(Paint.Cap.ROUND);

                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                // rubbish... trying to understand screen scaling/density...

                float viewScale = view.getScaleCoefficient()/4.0f;       // now "1" for emulator sizing
                float density = view.getDensity();
                float ds = 160 / viewScale;      // "10pt"
                float sw = p.getStrokeWidth();


                int w = tileBox.getPixWidth();
                int h = tileBox.getPixHeight();

                for (GPXUtilities.WptPt pt : culled) {

                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if (isIn(x, y, w, h)) {

                        px.setColor(0xFF000000);
                        px.setStrokeWidth(sw + 4);
                        canvas.drawPoint(x, y, px);
                        px.setStrokeWidth(sw+2);
                        px.setColor(0xFFFFFFFF);
                        canvas.drawPoint(x, y, px);

                        if (view.getZoom()>11) {
                            px.setColor(Color.BLACK);
                            px.setStrokeWidth(1);
                            px.setTextSize(ds);
                            canvas.drawText(getLabel(pt.getDistance()), x + ds / 2, y + ds / 2, px);
                        }
                    }
                    canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Arrows extends RenderableSegment {
    // EXPERIMENTAL!

        private double segmentSize;
        private double zoom;

        public Arrows(List<GPXUtilities.WptPt> pt, OsmandMapTileView view, double segmentSize, long refreshRate) {
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

        public int getComplementaryColor(int colorToInvert) {
            float[] hsv = new float[3];
            Color.RGBToHSV(Color.red(colorToInvert), Color.green(colorToInvert), Color.blue(colorToInvert), hsv);
            hsv[0] = (hsv[0] + 180) % 360;
            return Color.HSVToColor(hsv);
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            if (culled != null && !culled.isEmpty()
                    /*&& !QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)*/) {

                // This is all very hacky and experimental code. Just showing how to do an animating segmented
                // line to draw arrows in the direction of movement.

                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float sizer = (float) Math.pow(2.0,zoom-18) * 512;
                int pCol = p.getColor();

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

                    if ((intp & 15) < 8) {

                        float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                        float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);


                        if ((intp&15) == 0) {
                            // arrowhead - trial and error trig till it looked OK :)
                            double angle = Math.atan2(lasty-y,lastx-x);
                            float newx1 = x + (float)Math.sin(angle-0.4+Math.PI/2)*sizer;
                            float newy1 = y - (float)Math.cos(angle-0.4+Math.PI/2)*sizer;
                            float newx2 = x + (float)Math.sin(angle+0.4+Math.PI/2)*sizer;
                            float newy2 = y - (float)Math.cos(angle+0.4+Math.PI/2)*sizer;

                            if (broken) {
                                path.moveTo(x, y);
                            }

                            path.lineTo(x,y);
                            path.moveTo(newx1, newy1);
                            path.lineTo(x, y);
                            path.lineTo(newx2, newy2);
                            path.moveTo(x,y);
                            broken = false;
                        }



                        if ((isIn(x, y, w, h) || isIn(lastx, lasty, w, h))) {
                            if (broken) {
                                path.moveTo(x, y);
                                broken = false;
                            } else {
                                path.lineTo(x, y);
                            }
                            lastx = x;
                            lasty = y;
                        } else
                            broken = true;
                    } else
                        broken = true;

                }

                canvas.drawPath(path, p);
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                p.setColor(pCol);
            }
        }
    }

}
