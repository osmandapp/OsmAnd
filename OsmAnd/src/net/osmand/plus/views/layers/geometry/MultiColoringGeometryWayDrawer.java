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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ColorPalette;
import net.osmand.core.jni.QListFColorARGB;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

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
	protected void drawFullBorder(@NonNull Canvas canvas, int zoom, @NonNull List<DrawPathData> pathsData) {
		if (DRAW_BORDER && zoom < BORDER_TYPE_ZOOM_THRESHOLD && requireDrawingBorder()) {
			Path fullPath = new Path();
			for (DrawPathData data : pathsData) {
				if (data.style != null && data.style.color != 0) {
					fullPath.addPath(data.path);
				}
			}
			canvas.drawPath(fullPath, getContext().getBorderPaint());
		}
	}

	@Override
	public void drawPath(@NonNull VectorLinesCollection collection, int baseOrder, boolean shouldDrawArrows,
	                     @NonNull List<DrawPathData31> pathsData) {
		int lineId = LINE_ID;
		if (coloringType.isDefault() || coloringType.isCustomColor() || coloringType.isTrackSolid() || coloringType.isRouteInfoAttribute()) {
			super.drawPath(collection, baseOrder, shouldDrawArrows, pathsData);
		} else if (coloringType.isGradient()) {
			GeometryWayStyle<?> prevStyle = null;
			List<DrawPathData31> dataArr = new ArrayList<>();
			for (DrawPathData31 data : pathsData) {
				if (prevStyle != null && data.style == null) {
					drawVectorLine(collection, lineId++, baseOrder--, shouldDrawArrows, true, prevStyle, dataArr);
					dataArr.clear();
				}
				prevStyle = data.style;
				dataArr.add(data);
			}
			if (!dataArr.isEmpty() && prevStyle != null) {
				drawVectorLine(collection, lineId, baseOrder, shouldDrawArrows, true, prevStyle, dataArr);
			}
		}
	}

	@Override
	protected void drawVectorLine(@NonNull VectorLinesCollection collection,
	                              int lineId, int baseOrder, boolean shouldDrawArrows, boolean approximationEnabled,
	                              @NonNull GeometryWayStyle<?> style,
	                              @NonNull List<DrawPathData31> pathsData) {
		Paint borderPaint = getContext().getBorderPaint();
		int borderColor = coloringType.isGradient() ? borderPaint.getColor() : 0;
		float borderWidth = coloringType.isGradient() ? borderPaint.getStrokeWidth() : 0;

		PathPoint arrowPathPointSample = getArrowPathPointSample(style, false);
		arrowPathPointSample.scaled = false;
		PathPoint specialArrowPathPointSample = getArrowPathPointSample(style, true);
		specialArrowPathPointSample.scaled = false;
		Bitmap pointBitmap = arrowPathPointSample.drawBitmap(getContext());
		Bitmap specialPointBitmap = specialArrowPathPointSample.drawBitmap(getContext());

		GeometrySolidWayStyle<?> solidWayStyle = (GeometrySolidWayStyle<?>) style;
		float bitmapStep = (float) solidWayStyle.getRegularPointStepPx();
		float specialBitmapStep = (float) solidWayStyle.getSpecialPointStepPx();

		QListFColorARGB colorizationMapping = getColorizationMapping(pathsData);
		buildVectorLine(collection, baseOrder, lineId,
				style.getColor(0), style.getWidth(0), borderColor, borderWidth, style.getDashPattern(), approximationEnabled, shouldDrawArrows,
				pointBitmap, specialPointBitmap, bitmapStep, specialBitmapStep, true, colorizationMapping, style.getColorizationScheme(),
				pathsData);
	}

	@NonNull
	private QListFColorARGB getColorizationMapping(@NonNull List<DrawPathData31> pathsData) {
		QListFColorARGB colors = new QListFColorARGB();
		if (!pathsData.isEmpty() && !coloringType.isSolidSingleColor()) {
			int lastColor = 0;
			for (DrawPathData31 data : pathsData) {
				int color = 0;
				GeometryWayStyle<?> style = data.style;
				if (style != null) {
					if (style instanceof GeometryGradientWayStyle) {
						color = ((GeometryGradientWayStyle<?>) style).currColor;
						lastColor = ((GeometryGradientWayStyle<?>) style).nextColor;
					} else {
						color = style.getColor() == null ? 0 : style.getColor();
						lastColor = color;
					}
				}
				for (int i = 0; i < data.tx.size() - 1; i++) {
					colors.add(NativeUtilities.createFColorARGB(color));
				}
				colors.add(NativeUtilities.createFColorARGB(lastColor));
			}
		}
		return colors;
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		Paint strokePaint = getContext().getCustomPaint();
		if (coloringType.isCustomColor() || coloringType.isTrackSolid() || coloringType.isRouteInfoAttribute()) {
			drawCustomSolid(canvas, pathData);
		} else if (coloringType.isDefault()) {
			super.drawPath(canvas, pathData);
		} else if (coloringType.isGradient()) {
			GeometryGradientWayStyle<?> style = (GeometryGradientWayStyle<?>) pathData.style;
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
	protected void drawSegmentBorder(@NonNull Canvas canvas, int zoom, @NonNull DrawPathData pathData) {
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
		GeometrySolidWayStyle<?> solidWayStyle = (GeometrySolidWayStyle<?>) style;
		return new ArrowPathPoint(iconX, iconY, angle, style, percent, solidWayStyle.useSpecialArrow());
	}

	@NonNull
	private PathPoint getArrowPathPointSample(GeometryWayStyle<?> style, boolean useSpecialArrow) {
		return new ArrowPathPoint(0, 0, 0, style, 0, useSpecialArrow);
	}

	private static class ArrowPathPoint extends PathPoint {

		private final double percent;
		private final boolean useSpecialArrow;

		ArrowPathPoint(float x, float y, double angle, GeometryWayStyle<?> style, double percent, boolean useSpecialArrow) {
			super(x, y, angle, style);
			this.percent = percent;
			this.useSpecialArrow = useSpecialArrow;
		}

		@Nullable
		@Override
		protected int[] getPointBitmapSize() {
			Bitmap bitmap = getPointBitmap();
			if (bitmap != null) {
				Context ctx = style.getCtx();
				GeometrySolidWayStyle<?> arrowsWayStyle = (GeometrySolidWayStyle<?>) style;
				if (useSpecialArrow) {
					int bitmapSize = (int) (arrowsWayStyle.getOuterCircleRadius() * 2 + AndroidUtils.dpToPxAuto(ctx, 2));
					return new int[]{bitmapSize, bitmapSize};
				} else {
					float scaleCoef = 1f;
					float styleWidth = arrowsWayStyle.getWidth(0);
					if (styleWidth > 0 && scaled) {
						scaleCoef = (styleWidth / 2f) / bitmap.getWidth();
					}
					return new int[]{(int) (bitmap.getWidth() * scaleCoef), bitmap.getHeight()};
				}
			}
			return null;
		}

		@Override
		protected void draw(@NonNull Canvas canvas, @NonNull GeometryWayContext context) {
			if (style instanceof GeometrySolidWayStyle && shouldDrawArrow()) {
				Bitmap bitmap = getPointBitmap();
				if (bitmap == null) {
					return;
				}
				Context ctx = style.getCtx();
				GeometrySolidWayStyle<?> arrowsWayStyle = (GeometrySolidWayStyle<?>) style;

				float newWidth;
				if (useSpecialArrow) {
					newWidth = AndroidUtils.dpToPxAuto(ctx, 12);
				} else {
					newWidth = scaled ? arrowsWayStyle.getWidth(0) / 2f : bitmap.getWidth();
				}

				float scale = newWidth / bitmap.getWidth();
				float paintW2 = newWidth / 2f;
				float paintH2 = bitmap.getHeight() * scale / 2f;

				Matrix matrix = getMatrix();
				matrix.reset();
				matrix.postScale(scale, scale);
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
				return ColorPalette.getIntermediateColor(gradientStyle.currColor, gradientStyle.nextColor, percent);
			}
			return style.getColor(0);
		}

		protected boolean shouldDrawArrow() {
			return !Algorithms.objectEquals(style.color, Color.TRANSPARENT);
		}

		@Nullable
		@Override
		protected Bitmap getPointBitmap() {
			MultiColoringGeometryWayContext context = (MultiColoringGeometryWayContext) style.getContext();
			return useSpecialArrow ? context.getSpecialArrowBitmap() : context.getArrowBitmap();
		}
	}
}