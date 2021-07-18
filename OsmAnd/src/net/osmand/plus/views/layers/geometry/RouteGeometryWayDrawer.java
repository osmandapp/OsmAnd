package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import net.osmand.plus.routing.RouteColoringType;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.layers.geometry.RouteGeometryWay.GeometryGradientWayStyle;

import java.util.List;

import androidx.annotation.NonNull;

public class RouteGeometryWayDrawer extends GeometryWayDrawer<RouteGeometryWayContext> {

	private static final int BORDER_TYPE_ZOOM_THRESHOLD = MapTileLayer.DEFAULT_MAX_ZOOM + MapTileLayer.OVERZOOM_IN;

	private final boolean drawBorder;

	private RouteColoringType routeColoringType = RouteColoringType.DEFAULT;

	public RouteGeometryWayDrawer(RouteGeometryWayContext context, boolean drawBorder) {
		super(context);
		this.drawBorder = drawBorder;
	}

	public void setRouteColoringType(@NonNull RouteColoringType coloringType) {
		this.routeColoringType = coloringType;
	}

	@Override
	protected void drawFullBorder(Canvas canvas, int zoom, List<DrawPathData> pathsData) {
		if (drawBorder && zoom < BORDER_TYPE_ZOOM_THRESHOLD
				&& (routeColoringType.isGradient() || routeColoringType.isRouteInfoAttribute())) {
			Path fullPath = new Path();
			for (DrawPathData data : pathsData) {
				fullPath.addPath(data.path);
			}
			canvas.drawPath(fullPath, getContext().getBorderPaint());
		}
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		Paint customPaint = getContext().getAttrs().customColorPaint;

		if (routeColoringType.isGradient()) {
			GeometryGradientWayStyle style = (GeometryGradientWayStyle) pathData.style;
			LinearGradient gradient = new LinearGradient(pathData.start.x, pathData.start.y, pathData.end.x, pathData.end.y,
					style.currColor, style.nextColor, Shader.TileMode.CLAMP);
			customPaint.setShader(gradient);
			customPaint.setStrokeWidth(style.width);
			customPaint.setAlpha(0xFF);
			canvas.drawPath(pathData.path, customPaint);
		} else if (routeColoringType.isRouteInfoAttribute()) {
			customPaint.setColor(pathData.style.color);
			customPaint.setStrokeWidth(pathData.style.width);
			canvas.drawPath(pathData.path, customPaint);
		} else {
			super.drawPath(canvas, pathData);
		}
	}

	@Override
	protected void drawSegmentBorder(Canvas canvas, int zoom, DrawPathData pathData) {
		if (drawBorder && zoom >= BORDER_TYPE_ZOOM_THRESHOLD
				&& (routeColoringType.isGradient() || routeColoringType.isRouteInfoAttribute())) {
			canvas.drawPath(pathData.path, getContext().getBorderPaint());
		}
	}
}