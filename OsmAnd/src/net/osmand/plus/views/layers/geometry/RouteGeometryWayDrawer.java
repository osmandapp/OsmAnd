package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import net.osmand.plus.views.layers.geometry.RouteGeometryWay.GeometryGradientWayStyle;

public class RouteGeometryWayDrawer extends GeometryWayDrawer<RouteGeometryWayContext> {


	public RouteGeometryWayDrawer(RouteGeometryWayContext context) {
		super(context);
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
}