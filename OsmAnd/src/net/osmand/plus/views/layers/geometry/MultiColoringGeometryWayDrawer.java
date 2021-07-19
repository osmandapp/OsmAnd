package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.layers.geometry.MultiColoringGeometryWay.GeometryGradientWayStyle;

import java.util.List;

import androidx.annotation.Nullable;

public class MultiColoringGeometryWayDrawer<T extends MultiColoringGeometryWayContext>
		extends GeometryWayDrawer<T> {

	private static final int BORDER_TYPE_ZOOM_THRESHOLD = MapTileLayer.DEFAULT_MAX_ZOOM + MapTileLayer.OVERZOOM_IN;
	private static final boolean DRAW_BORDER = true;

	@Nullable
	protected ColoringType routeColoringType = null;

	public MultiColoringGeometryWayDrawer(T context) {
		super(context);
	}

	public void setRouteColoringType(@Nullable ColoringType coloringType) {
		this.routeColoringType = coloringType;
	}

	@Override
	protected void drawFullBorder(Canvas canvas, int zoom, List<DrawPathData> pathsData) {
		if (DRAW_BORDER && zoom < BORDER_TYPE_ZOOM_THRESHOLD && shouldDrawBorder()) {
			Path fullPath = new Path();
			for (DrawPathData data : pathsData) {
				fullPath.addPath(data.path);
			}
			canvas.drawPath(fullPath, getContext().getBorderPaint());
		}
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		Paint strokePaint = getContext().getStrokePaint();

		if (routeColoringType == null || routeColoringType.isCustomColor()
				|| routeColoringType.isRouteInfoAttribute()) {
			drawCustomSolid(canvas, pathData);
		} else if (routeColoringType.isDefault()) {
			super.drawPath(canvas, pathData);
		} else if (routeColoringType.isGradient()) {
			GeometryGradientWayStyle style = (GeometryGradientWayStyle) pathData.style;
			LinearGradient gradient = new LinearGradient(pathData.start.x, pathData.start.y,
					pathData.end.x, pathData.end.y, style.currColor, style.nextColor, Shader.TileMode.CLAMP);
			strokePaint.setShader(gradient);
			strokePaint.setStrokeWidth(style.width);
			strokePaint.setAlpha(0xFF);
			canvas.drawPath(pathData.path, strokePaint);
		}
	}

	protected void drawCustomSolid(Canvas canvas, DrawPathData pathData) {
		Paint paint = getContext().getStrokePaint();
		paint.setColor(pathData.style.color);
		paint.setStrokeWidth(pathData.style.width);
		canvas.drawPath(pathData.path, paint);
	}

	@Override
	protected void drawSegmentBorder(Canvas canvas, int zoom, DrawPathData pathData) {
		if (DRAW_BORDER && zoom >= BORDER_TYPE_ZOOM_THRESHOLD && shouldDrawBorder()) {
			canvas.drawPath(pathData.path, getContext().getBorderPaint());
		}
	}

	private boolean shouldDrawBorder() {
		return routeColoringType != null
				&& (routeColoringType.isGradient() || routeColoringType.isRouteInfoAttribute());
	}
}