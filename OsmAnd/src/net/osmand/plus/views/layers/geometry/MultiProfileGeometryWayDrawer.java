package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapLayer.RenderingLineAttributes;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay.GeometryMultiProfileWayStyle;
import net.osmand.util.Algorithms;

import java.util.List;

public class MultiProfileGeometryWayDrawer extends GeometryWayDrawer<MultiProfileGeometryWayContext> {

	public MultiProfileGeometryWayDrawer(MultiProfileGeometryWayContext context) {
		super(context);
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		Path path = pathData.path;
		GeometryWayStyle<?> style = pathData.style;
		if (style instanceof GeometryMultiProfileWayStyle && !((GeometryMultiProfileWayStyle) style).isGap()) {
			RenderingLineAttributes attrs = getContext().getAttrs();

			attrs.paint.setColor(((GeometryMultiProfileWayStyle) style).getBorderColor());
			canvas.drawPath(path, attrs.paint);

			attrs.paint2.setColor(((GeometryMultiProfileWayStyle) style).getLineColor());
			canvas.drawPath(path, attrs.paint2);
		}
	}

	@Override
	public void drawArrowsOverPath(Canvas canvas, RotatedTileBox tb, List<Float> tx, List<Float> ty, List<Double> angles, List<Double> distances, double distPixToFinish, List<GeometryWayStyle<?>> styles) {
		Path path = new Path();
		PathMeasure pathMeasure = new PathMeasure();
		MultiProfileGeometryWayContext context = getContext();
		GeometryMultiProfileWayStyle style = null;

		for (int i = 0; i < styles.size(); i++) {
			GeometryWayStyle<?> s = styles.get(i);
			if (s != null && !s.equals(style) || !((GeometryMultiProfileWayStyle) s).isGap()) {
				style = (GeometryMultiProfileWayStyle) styles.get(i);
				PointF center = getIconCenter(tb, style.getRoutePoints(), path, pathMeasure);
				if (center != null && tb.containsPoint(center.x, center.y, context.circleSize)) {
					float x = center.x - context.circleSize / 2;
					float y = center.y - context.circleSize / 2;
					canvas.drawBitmap(style.getPointBitmap(), x, y, null);
				}
			}
		}
	}

	private PointF getIconCenter(RotatedTileBox tileBox, List<LatLon> routePoints, Path path, PathMeasure pathMeasure) {
		if (Algorithms.isEmpty(routePoints)) {
			return null;
		}

		path.reset();
		PointF first = getPoint(tileBox, routePoints.get(0));
		path.moveTo(first.x, first.y);
		for (int i = 1; i < routePoints.size(); i++) {
			PointF pt = getPoint(tileBox, routePoints.get(i));
			path.lineTo(pt.x, pt.y);
		}

		pathMeasure.setPath(path, false);
		float routeLength = pathMeasure.getLength();
		if ((routeLength - getContext().circleSize) / 2 < getContext().minIconMargin) {
			return null;
		}

		float[] xy = new float[2];
		pathMeasure.getPosTan(routeLength * 0.5f, xy, null);
		return new PointF(xy[0], xy[1]);
	}

	private PointF getPoint(RotatedTileBox tileBox, LatLon latLon) {
		return new PointF(tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude()),
				tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude()));
	}
}