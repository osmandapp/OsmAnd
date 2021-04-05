package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.Path;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapLayer.RenderingLineAttributes;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay.GeometryMultiProfileWayStyle;

import java.util.List;

public class MultiProfileGeometryWayDrawer extends GeometryWayDrawer<MultiProfileGeometryWayContext> {

	public MultiProfileGeometryWayDrawer(MultiProfileGeometryWayContext context) {
		super(context);
	}

	@Override
	public void drawPath(Canvas canvas, Path path, GeometryWayStyle<?> style) {
		if (style instanceof GeometryMultiProfileWayStyle) {
			RenderingLineAttributes attrs = getContext().getAttrs();

			attrs.paint.setColor(((GeometryMultiProfileWayStyle) style).getBorderColor());
			canvas.drawPath(path, attrs.paint);

			attrs.paint2.setColor(((GeometryMultiProfileWayStyle) style).getLineColor());
			canvas.drawPath(path, attrs.paint2);
		}
	}

	@Override
	public void drawArrowsOverPath(Canvas canvas, RotatedTileBox tb, List<Float> tx, List<Float> ty, List<Double> angles, List<Double> distances, double distPixToFinish, List<GeometryWayStyle<?>> styles) {
		MultiProfileGeometryWayContext context = getContext();
		GeometryMultiProfileWayStyle style = null;

		for (int i = 0; i < styles.size(); i++) {
			if (styles.get(i) != null && !styles.get(i).equals(style)) {
				style = (GeometryMultiProfileWayStyle) styles.get(i);
				double lat = style.getIconLat();
				double lon = style.getIconLon();
				if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
					float x = tb.getPixXFromLatLon(lat, lon) - context.circleSize / 2;
					float y = tb.getPixYFromLatLon(lat, lon) - context.circleSize / 2;
					canvas.drawBitmap(style.getPointBitmap(), x, y, null);
				}
			}
		}
	}
}