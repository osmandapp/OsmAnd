package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay.GeometryMultiProfileWayStyle;
import net.osmand.util.Algorithms;

import java.util.List;

import androidx.annotation.Nullable;

public class MultiProfileGeometryWayDrawer extends GeometryWayDrawer<MultiProfileGeometryWayContext> {

	public MultiProfileGeometryWayDrawer(MultiProfileGeometryWayContext context) {
		super(context);
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		Path path = pathData.path;
		GeometryMultiProfileWayStyle style = pathData.style instanceof GeometryMultiProfileWayStyle
				? (GeometryMultiProfileWayStyle) pathData.style
				: null;
		if (style != null && !style.isGap()) {
			Paint pathBorderPaint = getContext().getPathBorderPaint();
			pathBorderPaint.setColor(style.getPathBorderColor());
			canvas.drawPath(path, pathBorderPaint);

			Paint pathPaint = getContext().getPathPaint();
			pathPaint.setColor(style.getPathColor());
			canvas.drawPath(path, pathPaint);
		}
	}

	@Override
	public void drawArrowsOverPath(Canvas canvas, RotatedTileBox tb, List<Float> tx, List<Float> ty, List<Double> angles, List<Double> distances, double distPixToFinish, List<GeometryWayStyle<?>> styles) {
		Path path = new Path();
		PathMeasure pathMeasure = new PathMeasure();
		MultiProfileGeometryWayContext context = getContext();
		GeometryMultiProfileWayStyle prevStyle = null;

		for (int i = 0; i < styles.size(); i++) {
			GeometryMultiProfileWayStyle style = styles.get(i) instanceof GeometryMultiProfileWayStyle
					? (GeometryMultiProfileWayStyle) styles.get(i)
					: null;

			if (style != null && !style.equals(prevStyle) && !style.isGap()) {
				PointF center = getIconCenter(tb, style.getRoutePoints(), path, pathMeasure);
				if (center != null && tb.containsPoint(center.x, center.y, context.profileIconFrameSizePx)) {
					float x = center.x - context.profileIconFrameSizePx / 2;
					float y = center.y - context.profileIconFrameSizePx / 2;
					canvas.drawBitmap(style.getPointBitmap(), x, y, null);
				}
			}
			prevStyle = style;
		}
	}

	@Nullable
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
		if ((routeLength - getContext().profileIconFrameSizePx) / 2 < getContext().minProfileIconMarginPx) {
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