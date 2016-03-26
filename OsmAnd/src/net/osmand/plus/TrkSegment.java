package net.osmand.plus;

import android.graphics.Canvas;
import android.graphics.Paint;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class TrkSegment extends GPXExtensions {

	private OsmandMapTileView view;
	public List<WptPt> points = new ArrayList<WptPt>();
	public List<RenderableSegment> renders = new ArrayList<>();

	public void addRenderable(OsmandMapTileView view, RenderType type, double param1, double param2) {
		RenderableSegment rs = null;
		switch (type) {
			case ORIGINAL:
				rs = new RenderableSegment(view, type, points, param1, param2);
				break;
			case RESAMPLE:
				rs = new RenderableDot(view, type, points, param1, param2);
				break;
			case CONVEYOR:
				rs = new RenderableConveyor(view, type, points, param1, param2);
			default:
				break;
		}
		if (rs != null)
			renders.add(rs);
	}


	public void recalculateRenderScales(OsmandMapTileView view, int zoom) {
		for (RenderableSegment rs : renders)
			rs.recalculateRenderScale(view, zoom);
	}

	public void drawRenderers(Paint p, Canvas c, RotatedTileBox tb) {
		for (RenderableSegment rs : renders)
			rs.drawSingleSegment(p, c, tb);
	}

}
