package net.osmand.plus;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;


public class RenderableAltitude extends RenderableSegment {

    private Paint alphaPaint = null;
    private int alpha;
    protected float colorBandWidth;

    RenderableAltitude(RenderType type, List<WptPt> pt, double param1, double param2) {
        super(type, pt, param1, param2);

        alpha = (int)param2;
        alphaPaint = new Paint();
        //alphaPaint.setStrokeWidth(100);
        alphaPaint.setStrokeCap(Paint.Cap.ROUND);

        colorBandWidth = 3.0f;
    }

    public void recalculateRenderScale(OsmandMapTileView view, double zoom) {
        if (culler == null && culled == null) {
            culler = new AsyncRamerDouglasPeucer(renderType, view, this, param1, param2);
            culler.execute("");
        }
    }

    public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

        if (culled != null && culled.size() > 0) {

            // Draws into a bitmap so that the lines can be drawn solid and the *ENTIRE* can be alpha-blended

            Bitmap newBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas2 = new Canvas(newBitmap);
            canvas2.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

            alphaPaint.setAlpha(255);
            alphaPaint.setStrokeWidth(p.getStrokeWidth()*colorBandWidth);


            float lastx = Float.NEGATIVE_INFINITY;
            float lasty = 0;

            for (WptPt pt : culled) {
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
