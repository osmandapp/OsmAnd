package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.Pair;

import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.layers.geometry.RouteGeometryWay.GeometryGradientWayStyle;

import java.util.List;

public class RouteGeometryWayDrawer extends GeometryWayDrawer<RouteGeometryWayContext> {

	private final int BORDER_TYPE_ZOOM_THRESHOLD = MapTileLayer.DEFAULT_MIN_ZOOM;

	private final boolean drawBorder;

	public RouteGeometryWayDrawer(RouteGeometryWayContext context, boolean drawBorder) {
		super(context);
		this.drawBorder = drawBorder;
	}

	@Override
	protected void drawFullBorder(Canvas canvas, int zoom, List<Pair<Path, GeometryWayStyle<?>>> paths) {
		if (drawBorder && zoom > BORDER_TYPE_ZOOM_THRESHOLD) {
			Paint borderPaint = getContext().getAttrs().shadowPaint;
			Path fullPath = new Path();
			for (Pair<Path, GeometryWayStyle<?>> path : paths) {
				if (path.second instanceof GeometryGradientWayStyle) {
					fullPath.addPath(path.first);
				}
			}
			canvas.drawPath(fullPath, borderPaint);
		}
	}

	@Override
	public void drawPath(Canvas canvas, Path path, GeometryWayStyle<?> s) {
		if (s instanceof GeometryGradientWayStyle) {
			GeometryGradientWayStyle style = (GeometryGradientWayStyle) s;
			LinearGradient gradient = new LinearGradient(style.startXY.x, style.startXY.y, style.endXY.x, style.endXY.y,
					style.startColor, style.endColor, Shader.TileMode.CLAMP);
			getContext().getAttrs().customColorPaint.setShader(gradient);
			getContext().getAttrs().customColorPaint.setStrokeCap(Paint.Cap.ROUND);
		}
		super.drawPath(canvas, path, s);
	}

	@Override
	protected void drawSegmentBorder(Canvas canvas, int zoom, Path path, GeometryWayStyle<?> style) {
		if (drawBorder && zoom < BORDER_TYPE_ZOOM_THRESHOLD) {
			canvas.drawPath(path, getContext().getAttrs().shadowPaint);
		}
	}
}