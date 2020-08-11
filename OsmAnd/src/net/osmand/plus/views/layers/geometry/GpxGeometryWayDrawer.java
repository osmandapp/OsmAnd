package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.Paint;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer.PathPoint;
import net.osmand.plus.views.layers.geometry.GpxGeometryWay.GeometryArrowsStyle;

import java.util.ArrayList;
import java.util.List;

public class GpxGeometryWayDrawer extends GeometryWayDrawer<GpxGeometryWayContext> {

	private static final float DIRECTION_ARROW_CIRCLE_MULTIPLIER = 1.5f;

	public GpxGeometryWayDrawer(GpxGeometryWayContext context) {
		super(context);
	}

	@Override
	protected PathPoint getArrowPathPoint(float iconx, float icony, GeometryWayStyle<?> style, double angle) {
		return new ArrowPathPoint(iconx, icony, angle, style);
	}

	private static class ArrowPathPoint extends PathPoint {

		ArrowPathPoint(float x, float y, double angle, GeometryWayStyle<?> style) {
			super(x, y, angle, style);
		}

		@Override
		void draw(Canvas canvas, GeometryWayContext context) {
			if (style instanceof GeometryArrowsStyle) {
				GeometryArrowsStyle arrowsWayStyle = (GeometryArrowsStyle) style;

				float arrowWidth = style.getPointBitmap().getWidth();
				if (arrowWidth > arrowsWayStyle.getTrackWidth()) {
					Paint paint = context.getPaintIcon();
					paint.setColor(arrowsWayStyle.getTrackColor());
					paint.setStrokeWidth(arrowWidth * DIRECTION_ARROW_CIRCLE_MULTIPLIER);
					canvas.drawPoint(x, y, paint);
				}
			}
			super.draw(canvas, context);
		}
	}
}
