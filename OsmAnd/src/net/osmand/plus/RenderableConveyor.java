package net.osmand.plus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class RenderableConveyor extends RenderableSegment {

    private double zoom = 0;

    RenderableConveyor(RenderType type, List<WptPt> pt, double param1, double param2) {
        super(type, pt, param1, param2);
    }

    public void recalculateRenderScale(OsmandMapTileView view, double zoom) {
        this.zoom = zoom;
        if (culled == null && culler == null) {       // i.e., do NOT resample when scaling - only allow a one-off generation
            culler = new AsyncRamerDouglasPeucer(renderType, view, this, param1, param2);
            culler.execute("");
        }
    }

    public static int getComplementaryColor(int colorToInvert) {
        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(colorToInvert), Color.green(colorToInvert), Color.blue(colorToInvert), hsv);
        hsv[0] = (hsv[0] + 180) % 360;
        return Color.HSVToColor(hsv);
    }

    public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

        if (culled == null)
            return;

        canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

        int pCol = p.getColor();
        float pSw = p.getStrokeWidth();

        p.setStrokeWidth(pSw * 2f);
        p.setColor(getComplementaryColor(p.getColor()));

        float lastx = Float.NEGATIVE_INFINITY;
        float lasty = Float.NEGATIVE_INFINITY;
        Path path = new Path();

        int h = tileBox.getPixHeight();
        int w = tileBox.getPixWidth();
        boolean broken = true;
        int intp = conveyor;
        for (WptPt pt : culled) {
            intp--;

            if ((intp & 15) < 7) {

                float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                if ((isIn(x,y,w,h) || isIn(lastx,lasty,w,h))) {

                    if ((intp & 15 )== 0) {
                        // arrowhead

                        double slope = Math.atan2(lasty-y,lastx-x);
                        float x2 = x - (float)Math.sin(slope+4.2)*70f;
                        float y2 = y + (float)Math.cos(slope+4.2)*70f;

                        float x3 = x + (float)Math.sin(slope-4.2)*70f;
                        float y3 = y - (float)Math.cos(slope-4.2)*70f;

                        if (!broken)
                            path.lineTo(x,y);
                        path.moveTo(x3,y3);
                        path.lineTo(x,y);
                        path.lineTo(x2,y2);
                        broken = true;
                    }

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
