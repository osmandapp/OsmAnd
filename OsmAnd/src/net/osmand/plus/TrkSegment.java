package net.osmand.plus;

import android.graphics.Canvas;
import android.graphics.Paint;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.ArrayList;
import java.util.List;

public class TrkSegment extends GPXExtensions {

	private OsmandMapTileView view;
	public List<WptPt> points = new ArrayList<WptPt>();
	public List<RenderableSegment> renders = new ArrayList<>();

	public void addRenderable(OsmandMapTileView view, RenderType type, double param1, double param2) {
		RenderableSegment rs = null;
		switch (type) {
			case SPEED:
				rs = new RenderableSpeed(type, points, param1, param2);
				break;
			case ALTITUDE:
				rs = new RenderableAltitude(type, points, param1, param2);
				break;
			case ORIGINAL:
				rs = new RenderableSegment(type, points, param1, param2);
				break;
			case DISTANCE:
				rs = new RenderableDot(type, points, param1, param2);
				break;
			case CONVEYOR:
				rs = new RenderableConveyor(type, points, param1, param2);
				rs.startScreenRefresh(view, (long) param2);						// start timer to refresh screen
				break;

			default:
				break;
		}
		if (rs != null)
			renders.add(rs);
	}


	public void recalculateRenderScales(OsmandMapTileView view, double zoom) {
		for (RenderableSegment rs : renders)
			rs.recalculateRenderScale(view, zoom);
	}

	public void drawRenderers(Paint p, Canvas c, RotatedTileBox tb) {
		for (RenderableSegment rs : renders)
			rs.drawSingleSegment(p, c, tb);
	}

}
