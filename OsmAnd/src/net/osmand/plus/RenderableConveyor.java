package net.osmand.plus;

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

public class RenderableConveyor extends RenderableSegment {

    private int conveyor = 0;
    private int zoom = 0;

    RenderableConveyor(final OsmandMapTileView view, RenderType type, List<WptPt> pt, double param1, double param2) {
        super(view, type, pt, param1, param2);

        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                view.refreshMap();         //TODO: incorrect!  << we want to force a redraw here instead
            }
        }, 0, (int)param2);

    }

    public void recalculateRenderScale(OsmandMapTileView view, int zoom) {
        this.zoom = zoom;
        if (culled==null) {
            culler = new AsyncRamerDouglasPeucer(renderType, view, this, param1, param2);
            culler.execute("");
        }
    }


    public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

        assert (p != null);
        assert (canvas != null);

        if (culled != null) {

            conveyor++;

            canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            float sw = p.getStrokeWidth();
            int col = p.getColor();

            p.setStrokeWidth((float)(Math.pow(2.0,(zoom-17))*100));
            p.setColor(0xFFFF8080);

            float lastx = 0;
            float lasty = 0;
            boolean first = true;
            Path path = new Path();

            int intp = 0;
            for (WptPt pt : culled) {
                intp--;

                float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
                float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

                if (!first) {
                    if ((conveyor+intp) % 7 == 0){
                        path.moveTo(lastx, lasty);
                        path.lineTo(x, y);
                    }
                }

                first = false;
                lastx = x;
                lasty = y;
            }
            canvas.drawPath(path, p);

            canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            p.setStrokeWidth(sw);
            p.setColor(col);
        }
    }


}
