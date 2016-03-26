package net.osmand.plus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

public class RenderableDot extends RenderableSegment {

	float dotSize;

	RenderableDot(OsmandMapTileView view, RenderType type, List<WptPt> pt, double param1, double param2) {
		super(view, type, pt, param1, param2);

		dotSize = (float) param2;
	}

	public void recalculateRenderScale(OsmandMapTileView view, int zoom) {
		if (culled==null) {
			culler = new AsyncRamerDouglasPeucer(renderType, view, this, param1, param2);
			culler.execute("");
		}
	}

	private String getLabel(double value) {
		return String.valueOf((int)(value+0.5));
	}

	public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {

		if (culled != null) {
			canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			float sw = p.getStrokeWidth();
			Paint.Style ps = p.getStyle();
			int col = p.getColor();
			p.setStyle(Paint.Style.FILL_AND_STROKE);
			p.setTextSize(32);

			for (WptPt pt : culled) {		//TODO: probls with not in tilebox...?
				float x = tileBox.getPixXFromLatLon(pt.lat, pt.lon);
				float y = tileBox.getPixYFromLatLon(pt.lat, pt.lon);

				p.setColor(0xFF000000);
				p.setStrokeWidth(dotSize + 4);
				canvas.drawPoint(x, y, p);
				p.setStrokeWidth(dotSize);
				p.setColor(0xFFFFFFFF);
				canvas.drawPoint(x, y, p);

				p.setColor(Color.BLACK);
				p.setStrokeWidth(1);
				canvas.drawText(getLabel(pt.getCumulativeDistance()), x+25, y+25, p);
			}

			canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			p.setStrokeWidth(sw);
			p.setStyle(ps);
			p.setColor(col);
		}
	}


}
