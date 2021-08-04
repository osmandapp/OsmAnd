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
				PointF center = MultiProfileGeometryWay.getIconCenter(tb, style.getRoutePoints(), path, pathMeasure);
				float profileIconSize = MultiProfileGeometryWayContext.getProfileIconSizePx(getContext().getDensity());
				if (center != null && tb.containsPoint(center.x, center.y, profileIconSize)) {
					float x = center.x - profileIconSize / 2;
					float y = center.y - profileIconSize / 2;
					canvas.drawBitmap(style.getPointBitmap(), x, y, null);
				}
			}
			prevStyle = style;
		}
	}
}