package net.osmand.plus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

public class RenderableDot extends RenderableSegment {

	float dotScale;

	RenderableDot(OsmandMapTileView view, RenderType type, List<WptPt> pt, double param1, double param2) {
		super(view, type, pt, param1, param2);

		dotScale = (float) param2;
	}

	public void recalculateRenderScale(OsmandMapTileView view, double zoom) {
		if (culled==null) {
			culler = new AsyncRamerDouglasPeucer(renderType, view, this, param1, param2);
			culler.execute("");
		}
	}

	private String getLabel(double value) {
		return String.valueOf((int)((value+0.5)/1000))+" km";
	}

	public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

		if (culled != null) {
			canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			float sw = p.getStrokeWidth();

			float ds = sw*dotScale;

			Paint.Style ps = p.getStyle();
			int col = p.getColor();
			int light = lighter(col,0.5f);

			p.setStyle(Paint.Style.FILL_AND_STROKE);
			p.setTextSize(sw*1.25f);

			int w = tileBox.getPixWidth();
			int h = tileBox.getPixHeight();

			for (WptPt pt : culled) {

				float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
				float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

				if (isIn(x, y, w, h)) {

					p.setColor(0xFF000000);
					p.setStrokeWidth(ds + 2);
					canvas.drawPoint(x, y, p);
					p.setStrokeWidth(ds);
					p.setColor(0xFFFFFFFF);
					canvas.drawPoint(x, y, p);

					if (sw>16) {
						p.setColor(Color.BLACK);
						p.setStrokeWidth(1);
						canvas.drawText(getLabel(pt.getCumulativeDistance()), x + ds/2, y+ds/2, p);
					}
				}
			}

			canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			p.setStrokeWidth(sw);
			p.setStyle(ps);
			p.setColor(col);
		}
	}


}
