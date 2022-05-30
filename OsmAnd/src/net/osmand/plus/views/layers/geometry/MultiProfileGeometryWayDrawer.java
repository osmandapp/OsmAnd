package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.QListFColorARGB;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.layers.geometry.MultiProfileGeometryWay.GeometryMultiProfileWayStyle;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MultiProfileGeometryWayDrawer extends GeometryWayDrawer<MultiProfileGeometryWayContext> {

	private final Path path;
	private final PathMeasure pathMeasure;

	public MultiProfileGeometryWayDrawer(MultiProfileGeometryWayContext context) {
		super(context);
		path = new Path();
		pathMeasure = new PathMeasure(path, false);
	}

	@Nullable
	private GeometryMultiProfileWayStyle getMultiProfileWayStyle(@Nullable GeometryWayStyle<?> style) {
		return style instanceof GeometryMultiProfileWayStyle ? (GeometryMultiProfileWayStyle) style : null;
	}

	@Override
	protected void drawFullBorder(@NonNull VectorLinesCollection collection, int baseOrder,
	                              int zoom, @NonNull List<DrawPathData31> pathsData) {
		int outlineId = OUTLINE_ID;
		Paint paint = getContext().getPathBorderPaint();
		float outlineWidth = getContext().getBorderOutlineWidth();
		float width = paint.getStrokeWidth() + outlineWidth;
		GeometryMultiProfileWayStyle prevStyle = null;
		List<DrawPathData31> dataArr = new ArrayList<>();
		for (DrawPathData31 data : pathsData) {
			GeometryMultiProfileWayStyle style = getMultiProfileWayStyle(data.style);
			if (prevStyle != null && !Algorithms.objectEquals(style, prevStyle) && !dataArr.isEmpty()) {
				buildVectorOutline(collection, baseOrder, outlineId++, prevStyle.getPathBorderColor(),
						width, outlineWidth, dataArr);
				dataArr.clear();
			}
			prevStyle = style;
			if (style != null && !style.isGap()) {
				dataArr.add(data);
			}
		}
		if (!dataArr.isEmpty()) {
			buildVectorOutline(collection, baseOrder, outlineId, prevStyle.getPathBorderColor(),
					width, outlineWidth, dataArr);
		}
	}

	@Override
	public void drawPath(@NonNull VectorLinesCollection collection, int baseOrder, boolean shouldDrawArrows, @NonNull List<DrawPathData31> pathsData) {
		int lineId = LINE_ID;
		GeometryWayStyle<?> prevStyle = null;
		List<DrawPathData31> dataArr = new ArrayList<>();
		for (DrawPathData31 data : pathsData) {
			GeometryMultiProfileWayStyle style = getMultiProfileWayStyle(data.style);
			if (prevStyle != null && !Algorithms.objectEquals(style, prevStyle) && !dataArr.isEmpty()) {
				drawVectorLine(collection, lineId++, baseOrder, shouldDrawArrows, true, prevStyle, dataArr);
				dataArr.clear();
			}
			prevStyle = style;
			if (style != null && !style.isGap()) {
				dataArr.add(data);
			}
		}
		if (!dataArr.isEmpty()) {
			drawVectorLine(collection, lineId, baseOrder, shouldDrawArrows, true, prevStyle, dataArr);
		}
	}

	@Override
	protected void drawVectorLine(@NonNull VectorLinesCollection collection,
	                              int lineId, int baseOrder, boolean shouldDrawArrows, boolean approximationEnabled,
	                              @NonNull GeometryWayStyle<?> style, @NonNull List<DrawPathData31> pathsData) {
		PathPoint pathPoint = getArrowPathPoint(0, 0, style, 0, 0);
		pathPoint.scaled = false;
		Bitmap pointBitmap = pathPoint.drawBitmap(getContext());
		double pxStep = style.getPointStepPx(1f);
		buildVectorLine(collection, baseOrder, lineId,
				style.getColor(0), style.getWidth(0), style.getDashPattern(), approximationEnabled, shouldDrawArrows,
				pointBitmap, pointBitmap, (float) pxStep, false, null, 0, pathsData);
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		Path path = pathData.path;
		GeometryMultiProfileWayStyle style = getMultiProfileWayStyle(pathData.style);
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
			GeometryMultiProfileWayStyle style = getMultiProfileWayStyle(styles.get(i));
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