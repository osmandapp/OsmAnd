package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.Path;

import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay.GeometryMultiProfileWayStyle;

import net.osmand.plus.views.OsmandMapLayer.RenderingLineAttributes;

public class MultiProfileGeometryWayDrawer extends GeometryWayDrawer<MultiProfileGeometryWayContext> {

	public MultiProfileGeometryWayDrawer(MultiProfileGeometryWayContext context) {
		super(context);
	}

	@Override
	public void drawPath(Canvas canvas, Path path, GeometryWayStyle<?> style) {
		if (style instanceof GeometryMultiProfileWayStyle) {
			RenderingLineAttributes attrs = getContext().getAttrs();
			attrs.paint2.setColor(((GeometryMultiProfileWayStyle) style).getLineColor());
			canvas.drawPath(path, attrs.paint2);
		}
	}

	public void drawPathBorder(Canvas canvas, Path path, GeometryWayStyle<?> style) {
		if (style instanceof GeometryMultiProfileWayStyle) {
			RenderingLineAttributes attrs = getContext().getAttrs();
			attrs.paint.setColor(((GeometryMultiProfileWayStyle) style).getBorderColor());
			canvas.drawPath(path, attrs.paint);
		}
	}
}