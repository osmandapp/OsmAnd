package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

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


        double zoom;
        double hashPoint;

        boolean shadow = false;         // TODO: fixup shadow support

        AsynchronousResampler culler = null;                        // The currently active resampler

        public RenderableSegment(List<GPXUtilities.WptPt> pt) {
            points = pt;
            hashPoint = points.hashCode();
            zoom = 0;
            culled = null;
        }

        public void recalculateRenderScale(double zoom) {}
        public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {}

        // When the asynchronous task has finished, it calls this function to set the 'culled' list
        public void setRDP(List<GPXUtilities.WptPt> cull) {

            assert cull!=null;
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

            //if (view != null) {
            //    view.refreshMap();          // force a redraw
            //}
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

        @Override public void recalculateRenderScale(double zoom) {

            // Here we create the 'shadow' resampled/culled points list, based on the asynchronous call.
            // The asynchronous callback will set the variable, and that is preferentially used for rendering

            double hashCode = points.hashCode() ;

            if (hashPoint != hashCode) {            // current track, changing?
                if (culler != null) {
                    culler.cancel(true);            // STOP culling a track with changing points
                    culled =  null;                 // and force use of original track
                }
            } else if (culler == null || this.zoom != zoom) {

                if (culler != null) {
                    culler.cancel(true);
                }

                double cullDistance = Math.pow(2.0, base - zoom);
                culler = new AsynchronousResampler.RamerDouglasPeucer(this, cullDistance);

                if (this.zoom < zoom) {            // if line would look worse (we're zooming in) then...
                    culled = null;                // use full-resolution until re-cull complete
                }

                this.zoom = zoom;

                culler.execute("");

                // The trackBounds may be slightly inaccurate (unlikely, but...) so let's reset it
                //trackBounds.left = trackBounds.bottom = Double.POSITIVE_INFINITY;
                //trackBounds.right = trackBounds.bottom = Double.NEGATIVE_INFINITY;
            }
        }

        @Override public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

            List<GPXUtilities.WptPt> pts = culled == null? points: culled;			// use culled points preferentially
            if (pts != null && !pts.isEmpty()
                    && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float stroke = p.getStrokeWidth()/2;

                float clipL = -stroke;
                float clipB = -stroke;
                float clipT = canvas.getHeight() + stroke;
                float clipR = canvas.getWidth() + stroke;

                GPXUtilities.WptPt pt = pts.get(0);
                float lastx = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float lasty = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                int size = pts.size();
                for (int i = 1; i < size; i++) {
                    pt = pts.get(i);

                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                            && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {
                        canvas.drawLine(lastx, lasty, x, y, p);
                    }
                    lastx = x;
                    lasty = y;
                }
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Altitude extends RenderableSegment {

        protected double segmentSize;

        public Altitude(List<GPXUtilities.WptPt> pt, double segmentSize) {
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

            if (culled != null && !culled.isEmpty()
                && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                Paint.Cap cap = p.getStrokeCap();
                float stroke = p.getStrokeWidth();
                int col = p.getColor();

                p.setStrokeCap(Paint.Cap.ROUND);

                float bandWidth = stroke * 3f;
                p.setStrokeWidth(bandWidth);

                float clipL = -bandWidth / 2f;
                float clipB = -bandWidth / 2f;
                float clipT = canvas.getHeight() + bandWidth / 2f;
                float clipR = canvas.getWidth() + bandWidth / 2f;

                GPXUtilities.WptPt pt = culled.get(0);
                float lastx = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float lasty = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                for (int i = 1; i < culled.size(); i++) {
                    pt = culled.get(i);

                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                            && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {
                        p.setColor(pt.colourARGB);
                        canvas.drawLine(lastx, lasty, x, y, p);
                    }
                    lastx = x;
                    lasty = y;
                }
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                p.setStrokeCap(cap);
                p.setStrokeWidth(stroke);
                p.setColor(col);

            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class SpeedColours extends Altitude {

        public SpeedColours(List<GPXUtilities.WptPt> pt, double segmentSize) {
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
                   /* && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)*/ ) {

                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                int pCol = p.getColor();
                float stroke = p.getStrokeWidth()/2f;
                p.setColor(getComplementaryColor(p.getColor()));

                float clipL = -stroke;
                float clipB = -stroke;
                float clipT = canvas.getHeight() + stroke;
                float clipR = canvas.getWidth() + stroke;

                GPXUtilities.WptPt pt = culled.get(0);
                float lastx = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float lasty = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                int intp = conveyor;
                for (int i = 1; i < culled.size(); i++, intp--) {

                    pt = culled.get(i);
                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    if ((intp & 7) < 3) {
                        if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                                && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {
                            canvas.drawLine(lastx, lasty, x, y, p);
                        }
                    }
                    lastx = x;
                    lasty = y;
                }
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                p.setStrokeWidth(stroke*2f);
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
            //this.view = view;
            this.dotScale = view.getScaleCoefficient();
            this.segmentSize = segmentSize;
        }

        @Override public void recalculateRenderScale(double zoom) {
            this.zoom = zoom;
            if (culled == null && culler == null) {
                culler = new AsynchronousResampler.GenericResampler(this, segmentSize);
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

                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float scale = 50;

                float stroke = p.getStrokeWidth();
                int col = p.getColor();
                float ts = p.getTextSize();
                Paint.Style st = p.getStyle();
                p.setStyle(Paint.Style.FILL);

                for (int i = culled.size()-1; --i >= 0;) {

                    GPXUtilities.WptPt pt = culled.get(i);
                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    p.setTextSize(scale);
                    p.setStrokeWidth(3);

                    Rect bounds = new Rect();
                    String lab = getKmLabel(pt.getDistance());
                    p.getTextBounds(lab, 0, lab.length(), bounds);

                    int rectH =  bounds.height();
                    int rectW =  bounds.width();

                    if (x < canvas.getWidth() + rectW/2 + scale && x > -rectW/2 + scale && y < canvas.getHeight() + rectH/2f && y > -rectH/2f) {

                        p.setStyle(Paint.Style.FILL);
                        p.setColor(Color.BLACK);
                        p.setStrokeWidth(stroke);
                        canvas.drawPoint(x, y, p);
                        p.setStrokeWidth(4);
                        p.setColor(Color.BLACK);
                        canvas.drawText(lab,x-rectW/2+40,y+rectH/2,p);
                    }
                    canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                }

                p.setStyle(st);
                p.setStrokeWidth(stroke);
                p.setColor(col);
                p.setTextSize(ts);
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public static class Arrows extends RenderableSegment {
    // EXPERIMENTAL! WORK IN PROGRESS...

        private double segmentSize;

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

            if (culled != null && !culled.isEmpty() && zoom > 14
                    && QuadRect.trivialOverlap(tileBox.getLatLonBounds(), trackBounds)) {

                // This is all very hacky and experimental code. Just showing how to do an animating segmented
                // line to draw arrows in the direction of movement.

                canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

                float stroke = p.getStrokeWidth();
                p.setStrokeWidth(stroke*1.5f);

                float sizer = (float) Math.pow(2.0,zoom-18) * 128;
                int pCol = p.getColor();

                p.setColor(getComplementaryColor(p.getColor()));    // and a complementary colour

                float lastx = Float.NEGATIVE_INFINITY;
                float lasty = Float.NEGATIVE_INFINITY;
                Path path = new Path();

                int h = tileBox.getPixHeight();
                int w = tileBox.getPixWidth();
                boolean broken = true;
                int intp = conveyor;                                // the segment cycler


                float clipL = -sizer;
                float clipB = -sizer;
                float clipT = canvas.getHeight() + sizer;
                float clipR = canvas.getWidth() + sizer;


                for (GPXUtilities.WptPt pt : culled) {
                    intp--;                                         // increment to go the other way!

                    float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                    float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                    boolean nextBroken = true;

                    if (Math.min(x, lastx) < clipR && Math.max(x, lastx) > clipL
                            && Math.min(y, lasty) < clipT && Math.max(y, lasty) > clipB) {

                        if ((intp & 15) < 6) {
                            p.setStrokeWidth(stroke * (1f + 1.25f* (5 - (intp & 7)) / 2f));

                            if (!broken) {
                                canvas.drawLine(lastx, lasty, x, y, p);
                            }

                            nextBroken = false;

                            if (zoom > 15 &&  (intp & 15) == 0) {
                                // arrowhead - trial and error trig till it looked OK :)
                                double angle = Math.atan2(lasty - y, lastx - x);
                                float newx1 = x + (float) Math.sin(angle - 0.4 + Math.PI / 2) * sizer;
                                float newy1 = y - (float) Math.cos(angle - 0.4 + Math.PI / 2) * sizer;
                                float newx2 = x + (float) Math.sin(angle + 0.4 + Math.PI / 2) * sizer;
                                float newy2 = y - (float) Math.cos(angle + 0.4 + Math.PI / 2) * sizer;

                                canvas.drawLine(newx1, newy1, x, y, p);
                                canvas.drawLine(newx2, newy2, x, y, p);
                            }
                        }
                    }

                    broken = nextBroken;
                    lastx = x;
                    lasty = y;
                }

//                canvas.drawPath(path, p);
                canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
                p.setColor(pCol);
                p.setStrokeWidth(stroke);
            }
        }
    }


}
