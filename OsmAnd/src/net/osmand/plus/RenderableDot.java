package net.osmand.plus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

public class RenderableDot extends RenderableSegment {

	float dotScale;

	RenderableDot(RenderType type, List<WptPt> pt, double param1, double param2) {
		super(type, pt, param1, param2);

		dotScale = (float) param2;
	}

	public void recalculateRenderScale(OsmandMapTileView view, double zoom) {
		if (culled == null && culler == null) {
			culler = new AsyncRamerDouglasPeucer(renderType, view, this, param1, param2);
			assert (culler != null);
			if (culler != null)
				culler.execute("");
		}
	}

	private String getLabel(double value) {
		String lab;
//		value /= 1000.;
//		int v2 = (int)((value+0.005)*100);

//		if (v2%100 == 0)
//			lab = String.format("%d km",(int)value);
//		else
			lab = String.format("%.2f km",value/1000.);
		return lab;
	}

	public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

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

			for (WptPt pt : culled) {

				float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
				float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

				if (isIn(x, y, w, h)) {

					px.setColor(0xFF000000);
					px.setStrokeWidth(ds + 2);
					canvas.drawPoint(x, y, px);
					px.setStrokeWidth(ds);
					px.setColor(0xFFFFFFFF);
					canvas.drawPoint(x, y, px);

					if (sw > 10) {
						px.setColor(Color.BLACK);
						px.setStrokeWidth(1);
						px.setTextSize(sw*2f);
						canvas.drawText(getLabel(pt.getCumulativeDistance()), x + ds / 2, y + ds / 2, px);
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
