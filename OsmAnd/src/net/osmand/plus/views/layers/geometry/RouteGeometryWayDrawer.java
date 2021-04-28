package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.layers.geometry.RouteGeometryWay.GeometryGradientWayStyle;

import java.util.List;

public class RouteGeometryWayDrawer extends GeometryWayDrawer<RouteGeometryWayContext> {

	private static final int BORDER_TYPE_ZOOM_THRESHOLD = MapTileLayer.DEFAULT_MAX_ZOOM + MapTileLayer.OVERZOOM_IN;

	private final boolean drawBorder;

	public RouteGeometryWayDrawer(RouteGeometryWayContext context, boolean drawBorder) {
		super(context);
		this.drawBorder = drawBorder;
	}

	@Override
	protected void drawFullBorder(Canvas canvas, int zoom, List<DrawPathData> pathsData) {
		if (drawBorder && zoom < BORDER_TYPE_ZOOM_THRESHOLD) {
			Paint borderPaint = getContext().getAttrs().shadowPaint;
			Path fullPath = new Path();
			for (DrawPathData data : pathsData) {
				if (data.style instanceof GeometryGradientWayStyle) {
					fullPath.addPath(data.path);
				}
			}
			canvas.drawPath(fullPath, borderPaint);
		}
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		if (pathData.style instanceof GeometryGradientWayStyle) {
			GeometryGradientWayStyle style = (GeometryGradientWayStyle) pathData.style;
			LinearGradient gradient = new LinearGradient(pathData.start.x, pathData.start.y, pathData.end.x, pathData.end.y,
					style.currColor, style.nextColor, Shader.TileMode.CLAMP);
			Paint customPaint = getContext().getAttrs().customColorPaint;
			customPaint.setShader(gradient);
			customPaint.setStrokeWidth(style.width);
			customPaint.setAlpha(0xFF);
			canvas.drawPath(pathData.path, customPaint);
		} else {
			super.drawPath(canvas, pathData);
		}
	}

	@Override
	protected void drawSegmentBorder(Canvas canvas, int zoom, DrawPathData pathData) {
		if (drawBorder && zoom >= BORDER_TYPE_ZOOM_THRESHOLD) {
			canvas.drawPath(pathData.path, getContext().getAttrs().shadowPaint);
		}
	}
}