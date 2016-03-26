package net.osmand.plus;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapAlgorithms;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import gnu.trove.list.array.TIntArrayList;



public class RenderableAltitude extends RenderableSegment {

    private double zoom = 0;

    RenderableAltitude(final OsmandMapTileView view, RenderType type, List<WptPt> pt, double param1, double param2) {
        super(view, type, pt, param1, param2);
    }

    public void recalculateRenderScale(OsmandMapTileView view, double zoom) {
        this.zoom = zoom;
        if (culled == null) {       // i.e., do NOT resample when scaling - only allow a one-off generation
            if (culler != null)
                culler.cancel(true);
            culler = new AsyncRamerDouglasPeucer(renderType, view, this, param1, param2);
            culler.execute("");
        }
    }

    public int getColor(double percent) {

        int r = (int)(255 * percent);
        int g = (int)(255 * (1.0 - percent));
        int b = 0;

        return 0xFF000000 + (r<<16) + (g<<8) + b;
    }

    public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {


        assert (p != null);
        assert (canvas != null);

        if (culled != null) {

            // Draws into a bitmap so that the lines can be drawn solid and the *ENTIRE* can be alpha-blended
            // back into the original canvas

            Bitmap newBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas2 = new Canvas(newBitmap);
            Paint alphaPaint = new Paint();
            alphaPaint.setAlpha(160);

            canvas2.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            float sw = p.getStrokeWidth();
            int col = p.getColor();

            //p.setStrokeWidth(sw*3f);

            float lastx = 0;
            float lasty = 0;
            boolean first = true;

            int h = tileBox.getPixHeight();
            int w = tileBox.getPixWidth();

            for (WptPt pt : culled) {

                float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                if (!first && (isIn(x,y,w,h) || isIn(lastx,lasty,w,h))) {
                    p.setColor(getColor(pt.fractionElevation));
                    canvas2.drawLine(lastx, lasty, x, y, p);
                }

                first = false;
                lastx = x;
                lasty = y;
            }

            canvas2.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            canvas.drawBitmap(newBitmap, 0, 0, alphaPaint);
            p.setStrokeWidth(sw);
            p.setColor(col);
        }



    }




}
