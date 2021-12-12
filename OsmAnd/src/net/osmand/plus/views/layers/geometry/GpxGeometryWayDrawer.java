package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;

public class GpxGeometryWayDrawer extends MultiColoringGeometryWayDrawer<GpxGeometryWayContext> {

	public GpxGeometryWayDrawer(GpxGeometryWayContext context) {
		super(context);
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		if (coloringType.isRouteInfoAttribute()) {
			drawCustomSolid(canvas, pathData);
		}
	}
}