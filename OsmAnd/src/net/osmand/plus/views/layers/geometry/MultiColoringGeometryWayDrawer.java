package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;

import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.geometry.MultiColoringGeometryWay.GeometryGradientWayStyle;
import net.osmand.plus.views.layers.geometry.MultiColoringGeometryWay.GeometrySolidWayStyle;
import net.osmand.router.RouteColorize;
import net.osmand.util.Algorithms;

import java.util.List;

import androidx.annotation.NonNull;

public class MultiColoringGeometryWayDrawer<T extends MultiColoringGeometryWayContext>
		extends GeometryWayDrawer<T> {

	private static final int BORDER_TYPE_ZOOM_THRESHOLD = MapTileLayer.DEFAULT_MAX_ZOOM + MapTileLayer.OVERZOOM_IN;
	private static final boolean DRAW_BORDER = true;

	@NonNull
	protected ColoringType coloringType;

	public MultiColoringGeometryWayDrawer(T context) {
		super(context);
		coloringType = context.getDefaultColoringType();
	}

	public void setColoringType(@NonNull ColoringType coloringType) {
		this.coloringType = coloringType;
	}

	@Override
	protected void drawFullBorder(Canvas canvas, int zoom, List<DrawPathData> pathsData) {
		if (DRAW_BORDER && zoom < BORDER_TYPE_ZOOM_THRESHOLD && requireDrawingBorder()) {
			Path fullPath = new Path();
			for (DrawPathData data : pathsData) {
				if (data.style.color != 0) {
					fullPath.addPath(data.path);
				}
			}
			canvas.drawPath(fullPath, getContext().getBorderPaint());
		}
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		Paint strokePaint = getContext().getCustomPaint();

		if (coloringType.isCustomColor() || coloringType.isTrackSolid() || coloringType.isRouteInfoAttribute()) {
			drawCustomSolid(canvas, pathData);
		} else if (coloringType.isDefault()) {
			super.drawPath(canvas, pathData);
		} else if (coloringType.isGradient()) {
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
		Paint paint = getContext().getCustomPaint();
		paint.setColor(pathData.style.color);
		paint.setStrokeWidth(pathData.style.width);
		canvas.drawPath(pathData.path, paint);
	}

	@Override
	protected void drawSegmentBorder(Canvas canvas, int zoom, DrawPathData pathData) {
		if (DRAW_BORDER && zoom >= BORDER_TYPE_ZOOM_THRESHOLD && requireDrawingBorder()) {
			if (pathData.style.color != 0) {
				canvas.drawPath(pathData.path, getContext().getBorderPaint());
			}
		}
	}

	private boolean requireDrawingBorder() {
		return coloringType.isGradient() || coloringType.isRouteInfoAttribute();
	}

	@Override
	protected PathPoint getArrowPathPoint(float iconX, float iconY, GeometryWayStyle<?> style, double angle, double percent) {
		return new ArrowPathPoint(iconX, iconY, angle, style, percent);
	}

	private static class ArrowPathPoint extends PathPoint {

		private final double percent;

		ArrowPathPoint(float x, float y, double angle, GeometryWayStyle<?> style, double percent) {
			super(x, y, angle, style);
			this.percent = percent;
		}

		@Override
		void draw(Canvas canvas, GeometryWayContext context) {
			if (style instanceof GeometrySolidWayStyle && shouldDrawArrow()) {
				Context ctx = style.getCtx();
				GeometrySolidWayStyle<?> arrowsWayStyle = (GeometrySolidWayStyle<?>) style;
				Bitmap bitmap = style.getPointBitmap();
				boolean useSpecialArrow = arrowsWayStyle.useSpecialArrow();

				float newWidth = useSpecialArrow
						? AndroidUtils.dpToPx(ctx, 12)
						: arrowsWayStyle.getWidth(0) == 0 ? 0 : arrowsWayStyle.getWidth(0) / 2f;
				float paintH2 = bitmap.getHeight() / 2f;
				float paintW2 = newWidth == 0 ? 0 : newWidth / 2f;

				Matrix matrix = getMatrix();
				matrix.reset();
				float sy = useSpecialArrow ? newWidth / bitmap.getHeight() : 1;
				matrix.postScale(newWidth / bitmap.getWidth(), sy);
				matrix.postRotate((float) angle, paintW2, paintH2);
				matrix.postTranslate(x - paintW2, y - paintH2);

				if (useSpecialArrow) {
					drawCircle(canvas, arrowsWayStyle);
				}

				Paint paint = context.getPaintIconCustom();
				int arrowColor = arrowsWayStyle.getPointColor();
				paint.setColorFilter(new PorterDuffColorFilter(arrowColor, PorterDuff.Mode.SRC_IN));
				canvas.drawBitmap(bitmap, matrix, paint);
			}
		}

		private void drawCircle(Canvas canvas, GeometrySolidWayStyle<?> style) {
			Paint paint = style.getContext().getCirclePaint();
			paint.setColor(GeometrySolidWayStyle.OUTER_CIRCLE_COLOR);
			canvas.drawCircle(x, y, style.getOuterCircleRadius(), paint);
			paint.setColor(getCircleColor(style));
			canvas.drawCircle(x, y, style.getInnerCircleRadius(), paint);
		}

		private int getCircleColor(@NonNull GeometrySolidWayStyle<?> style) {
			if (style instanceof GeometryGradientWayStyle<?>) {
				GeometryGradientWayStyle<?> gradientStyle = ((GeometryGradientWayStyle<?>) style);
				return RouteColorize.getIntermediateColor(gradientStyle.currColor, gradientStyle.nextColor, percent);
			}
			return style.getColor(0);
		}

		protected boolean shouldDrawArrow() {
			return !Algorithms.objectEquals(style.color, Color.TRANSPARENT);
		}
	}
}