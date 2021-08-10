package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay.GeometryMultiProfileWayStyle;

import java.util.List;

public class MultiProfileGeometryWayDrawer extends GeometryWayDrawer<MultiProfileGeometryWayContext> {

	private final Path path;
	private final PathMeasure pathMeasure;

	public MultiProfileGeometryWayDrawer(MultiProfileGeometryWayContext context) {
		super(context);
		path = new Path();
		pathMeasure = new PathMeasure(path, false);
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
		path.reset();
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