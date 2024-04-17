package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListFColorARGB;
import net.osmand.core.jni.QListFloat;
import net.osmand.core.jni.QListVectorLine;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.VectorDouble;
import net.osmand.core.jni.VectorLine;
import net.osmand.core.jni.VectorLineBuilder;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class GeometryWayDrawer<T extends GeometryWayContext> {

	protected static final int LINE_ID = 1;
	public static final float VECTOR_LINE_SCALE_COEF = 2.0f;
	private static final Log log = PlatformUtil.getLog(GeometryWayDrawer.class);

	private final T context;

	public static class DrawPathData {
		Path path;
		PointF start;
		PointF end;
		GeometryWayStyle<?> style;

		public DrawPathData(@NonNull Path path, @NonNull PointF start, @NonNull PointF end,
		                    @Nullable GeometryWayStyle<?> style) {
			this.path = path;
			this.start = start;
			this.end = end;
			this.style = style;
		}
	}

	public static class DrawPathData31 {
		List<Integer> indexes;
		List<Integer> tx;
		List<Integer> ty;
		List<Float> heights;
		GeometryWayStyle<?> style;

		public DrawPathData31(@NonNull List<Integer> indexes,
		                      @NonNull List<Integer> tx, @NonNull List<Integer> ty,
		                      @Nullable GeometryWayStyle<?> style		) {
			this.indexes = indexes;
			this.tx = tx;
			this.ty = ty;
			this.style = style;
		}
	}

	public GeometryWayDrawer(T context) {
		this.context = context;
	}

	public T getContext() {
		return context;
	}

	public void drawArrowsOverPath(@NonNull Canvas canvas, @NonNull RotatedTileBox tb, List<GeometryWayPoint> points, double distPixToFinish) {
		List<PathPoint> arrows = new ArrayList<>();

		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int left = -w / 4;
		int right = w + w / 4;
		int top = -h / 4;
		int bottom = h + h / 4;

		double zoomCoef = tb.getZoomAnimation() > 0 ? (Math.pow(2, tb.getZoomAnimation() + tb.getZoomFloatPart())) : 1f;
		int startIndex = points.size() - 2;
		boolean hasStyles = points.get(startIndex).style != null;
		double defaultPxStep;
		if (hasStyles) {
			defaultPxStep = points.get(startIndex).style.getPointStepPx(zoomCoef);
		} else {
			Bitmap arrow = context.getArrowBitmap();
			defaultPxStep = arrow.getHeight() * 4f * zoomCoef;
		}
		double pxStep = defaultPxStep;
		double dist = 0;
		if (distPixToFinish != 0) {
			dist = distPixToFinish - pxStep * ((int) (distPixToFinish / pxStep)); // dist < 1
		}
		for (int i = startIndex; i >= 0; i--) {
			GeometryWayPoint prev = points.get(i);
			GeometryWayPoint next = points.get(i + 1);
			GeometryWayStyle<?> style = hasStyles ? prev.style : null;
			if (next.distance == 0) {
				continue;
			}
			pxStep = style != null ? style.getPointStepPx(zoomCoef) : defaultPxStep;
			if (dist >= pxStep) {
				dist = 0;
			}
			double percent = 1 - (pxStep - dist) / next.distance;
			dist += next.distance;
			while (dist >= pxStep) {
				double pdx = (next.tx - prev.tx) * percent;
				double pdy = (next.ty - prev.ty) * percent;
				float iconX = (float) (prev.tx + pdx);
				float iconY = (float) (prev.ty + pdy);
				if (GeometryWayPathAlgorithms.isIn(iconX, iconY, left, top, right, bottom)) {
					arrows.add(getArrowPathPoint(iconX, iconY, style, next.angle, percent));
				}
				dist -= pxStep;
				percent -= pxStep / next.distance;
			}
		}
		for (int i = arrows.size() - 1; i >= 0; i--) {
			PathPoint a = arrows.get(i);
			if (!tb.isZoomAnimated() || a.style.isVisibleWhileZooming()) {
				a.draw(canvas, context);
			}
		}
	}

	protected void drawFullBorder(@NonNull Canvas canvas, int zoom, @NonNull List<DrawPathData> pathsData) {
	}

	protected void drawSegmentBorder(@NonNull Canvas canvas, int zoom, @NonNull DrawPathData pathData) {
	}

	protected void buildVectorLine(@NonNull VectorLinesCollection collection, int baseOrder,
	                               int lineId, int color, float width,
	                               int outlineColor, float outlineWidth,
	                               @Nullable float[] dashPattern,
	                               boolean approximationEnabled, boolean showPathBitmaps,
	                               @Nullable Bitmap pathBitmap, @Nullable Bitmap specialPathBitmap,
	                               float bitmapStep, float specialBitmapStep, boolean bitmapOnSurface,
	                               @Nullable QListFColorARGB colorizationMapping, int colorizationScheme,
	                               @NonNull List<DrawPathData31> pathsData) {
		long startBuildVectorLineTime = System.currentTimeMillis();
		boolean hasColorizationMapping = colorizationMapping != null && !colorizationMapping.isEmpty();
		QVectorPointI points = new QVectorPointI();
		QListFloat heights = new QListFloat();
		QListFColorARGB traceColorizationMapping = new QListFColorARGB();
		float r = (float) Color.red(color) / 256;
		float g = (float) Color.green(color) / 256;
		float b = (float) Color.blue(color) / 256;
		boolean showRaised = false;
		boolean showTransparentTraces = true;
		if (pathsData.size() > 0) {
			showRaised = pathsData.get(0).style.use3DVisualization;
		}
		for (DrawPathData31 data : pathsData) {
			for (int i = 0; i < data.tx.size(); i++) {
				points.add(new PointI(data.tx.get(i), data.ty.get(i)));
				if (showRaised) {
					if (data.heights != null && i < data.heights.size()) {
						heights.add(data.heights.get(i));
					}
				}
			}
		}
		if (showRaised && hasColorizationMapping && showTransparentTraces) {
			long size = colorizationMapping.size();
			traceColorizationMapping = new QListFColorARGB();
			for (int i = 0; i < size; i++) {
				float a = (float) i / (float) size;
				FColorARGB colorARGB = colorizationMapping.get(i);
				traceColorizationMapping.add(new FColorARGB(a * a * a * a, colorARGB.getR(), colorARGB.getG(), colorARGB.getB()));
			}
		}
		QListVectorLine lines = collection.getLines();
		for (int i = 0; i < lines.size(); i++) {
			VectorLine line = lines.get(i);
			if (line.getLineId() == lineId) {
				line.setFillColor(NativeUtilities.createFColorARGB(color));
				line.setLineWidth(width * VECTOR_LINE_SCALE_COEF);
				line.setOutlineWidth(outlineWidth * VECTOR_LINE_SCALE_COEF);
				line.setPoints(points);
				if (hasColorizationMapping) {
					if (showRaised) {
						line.setColorizationMapping(traceColorizationMapping);
					} else {
						line.setColorizationMapping(colorizationMapping);
					}
				}

				line.setShowArrows(showPathBitmaps);
				if (showPathBitmaps && pathBitmap != null) {
					line.setPathIconStep(bitmapStep);
					if (specialPathBitmap != null && specialBitmapStep != -1) {
						line.setSpecialPathIconStep(specialBitmapStep);
					}
				}
				line.setHeights(heights);
				if (showRaised) {
					line.setFillColor(new FColorARGB(1.0f, r, g, b));
					line.setColorizationMapping(new QListFColorARGB());
					line.setOutlineColorizationMapping(traceColorizationMapping);
					line.setOutlineWidth(width * VECTOR_LINE_SCALE_COEF / 2.0f);
					if (showTransparentTraces) {
						line.setColorizationScheme(1);
						line.setNearOutlineColor(new FColorARGB(0.0f, r, g, b));
						line.setFarOutlineColor(new FColorARGB(1.0f, r, g, b));
					} else
						line.setOutlineColor(new FColorARGB(1.0f, 0.8f, 0.8f, 0.8f));
				}
				return;
			}
		}
		VectorLineBuilder builder = new VectorLineBuilder();
		builder.setPoints(points)
				.setIsHidden(false)
				.setLineId(lineId)
				.setLineWidth(width * VECTOR_LINE_SCALE_COEF)
				.setFillColor(NativeUtilities.createFColorARGB(color))
				.setOutlineWidth(outlineWidth * VECTOR_LINE_SCALE_COEF)
				.setOutlineColor(NativeUtilities.createFColorARGB(outlineColor))
				.setApproximationEnabled(approximationEnabled)
				.setBaseOrder(baseOrder);
		if (dashPattern != null) {
			VectorDouble vectorDouble = new VectorDouble();
			for (float i : dashPattern) {
				vectorDouble.add((double) i * 6.0d);
			}
			builder.setLineDash(vectorDouble);
		}

		builder.setShouldShowArrows(showPathBitmaps);
		if (pathBitmap != null) {
			builder.setScreenScale(1f)
					.setPathIconStep(bitmapStep)
					.setPathIcon(NativeUtilities.createSkImageFromBitmap(pathBitmap))
					.setPathIconOnSurface(bitmapOnSurface);
			if (specialPathBitmap != null) {
				builder.setSpecialPathIcon(NativeUtilities.createSkImageFromBitmap(specialPathBitmap));
				if (specialBitmapStep != -1) {
					builder.setSpecialPathIconStep(specialBitmapStep);
				}
			}
		}
		if (hasColorizationMapping) {
			builder.setColorizationMapping(colorizationMapping);
			builder.setColorizationScheme(colorizationScheme);
		}
		if (showRaised) {
			builder.setColorizationMapping(traceColorizationMapping)
					.setOutlineColorizationMapping(traceColorizationMapping)
					.setColorizationScheme(colorizationScheme)
					.setHeights(heights)
					.setFillColor(new FColorARGB(1.0f, r, g, b))
					.setOutlineWidth(width * VECTOR_LINE_SCALE_COEF / 2.0f)
					.setOutlineColor(new FColorARGB(1.0f, r, g, b));
			if (showTransparentTraces && colorizationScheme != 1) {
				builder
						.setNearOutlineColor(new FColorARGB(0.0f, r, g, b))
						.setFarOutlineColor(new FColorARGB(1.0f, r, g, b));
			} else
				builder.setOutlineColor(new FColorARGB(1.0f, 0.8f, 0.8f, 0.8f));
		}
		builder.buildAndAddToCollection(collection);
	}

	protected PathPoint getArrowPathPoint(float iconX, float iconY, GeometryWayStyle<?> style,
	                                      double angle, double percent) {
		return new PathPoint(iconX, iconY, angle, style);
	}

	public void drawPath(Canvas canvas, DrawPathData pathData) {
		context.getAttrs().customColor = pathData.style.getColor(0);
		context.getAttrs().customWidth = pathData.style.getWidth(0);
		context.getAttrs().drawPath(canvas, pathData.path);
	}

	public void drawPath(@NonNull VectorLinesCollection collection, int baseOrder, boolean shouldDrawArrows,
	                     @NonNull List<DrawPathData31> pathsData) {
		int lineId = LINE_ID;
		GeometryWayStyle<?> prevStyle = null;
		List<DrawPathData31> dataArr = new ArrayList<>();
		for (DrawPathData31 data : pathsData) {
			if (prevStyle != null && (!Algorithms.objectEquals(data.style, prevStyle) || data.style.isUnique()
					|| prevStyle.hasPathLine() != data.style.hasPathLine())) {
				drawVectorLine(collection, lineId++, baseOrder, shouldDrawArrows, true, prevStyle, dataArr);
				dataArr.clear();
			}
			prevStyle = data.style;
			dataArr.add(data);
		}
		if (!dataArr.isEmpty() && prevStyle != null) {
			drawVectorLine(collection, lineId, baseOrder, shouldDrawArrows, true, prevStyle, dataArr);
		}
	}

	protected void drawVectorLine(@NonNull VectorLinesCollection collection,
	                              int lineId, int baseOrder, boolean shouldDrawArrows, boolean approximationEnabled,
	                              @NonNull GeometryWayStyle<?> style, @NonNull List<DrawPathData31> pathsData) {
		drawVectorLine(collection, lineId, baseOrder, shouldDrawArrows, style,
				style.getColor(0), style.getWidth(0), 0, 0, style.getDashPattern(), approximationEnabled, pathsData);
	}

	protected void drawVectorLine(@NonNull VectorLinesCollection collection,
	                              int lineId, int baseOrder, boolean shouldDrawArrows,
	                              @NonNull GeometryWayStyle<?> style, int color, float width,
	                              int outlineColor, float outlineWidth,
	                              @Nullable float[] dashPattern,
	                              boolean approximationEnabled,
	                              @NonNull List<DrawPathData31> pathsData) {
		PathPoint pathPoint = getArrowPathPoint(0, 0, style, 0, 0);
		pathPoint.scaled = false;
		Bitmap pointBitmap = pathPoint.drawBitmap(getContext());
		float pxStep = (float) style.getPointStepPx(1f);
		buildVectorLine(collection, baseOrder, lineId, color, width, outlineColor, outlineWidth, dashPattern,
				approximationEnabled, shouldDrawArrows, pointBitmap, null, pxStep,
				pxStep, true, null, 0,
				pathsData);
	}

	public static class PathPoint {
		float x;
		float y;
		double angle;
		GeometryWayStyle<?> style;
		boolean scaled = true;

		private final Matrix matrix = new Matrix();

		public PathPoint(float x, float y, double angle, @Nullable GeometryWayStyle<?> style) {
			this.x = x;
			this.y = y;
			this.angle = angle;
			this.style = style;
		}

		protected Matrix getMatrix() {
			return matrix;
		}

		@Nullable
		protected Bitmap getPointBitmap() {
			return style != null ? style.getPointBitmap() : null;
		}

		@Nullable
		protected int[] getPointBitmapSize() {
			Bitmap bitmap = getPointBitmap();
			if (bitmap != null) {
				float scaleCoef = 1f;
				if (scaled) {
					float styleWidth = style.getWidth(0);
					if (styleWidth > 0) {
						scaleCoef = (styleWidth / 2) / bitmap.getWidth();
						scaleCoef = scaleCoef < 1 ? scaleCoef : 1f;
					}
				}
				return new int[] {(int) (bitmap.getWidth() * scaleCoef), (int) (bitmap.getHeight() * scaleCoef)};
			}
			return null;
		}

		protected void draw(@NonNull Canvas canvas, @NonNull GeometryWayContext context) {
			Bitmap bitmap = getPointBitmap();
			if (bitmap != null && style != null) {
				Integer pointColor = style.getPointColor();
				float paintH2 = bitmap.getHeight() / 2f;
				float paintW2 = bitmap.getWidth() / 2f;

				matrix.reset();
				float styleWidth = style.getWidth(0);
				if (styleWidth > 0 && scaled) {
					float scaleCoef = (styleWidth / 2) / bitmap.getWidth();
					if (scaleCoef < 1) {
						matrix.setScale(scaleCoef, scaleCoef, paintW2, paintH2);
					}
				}
				if (angle != 0) {
					matrix.postRotate((float) angle, paintW2, paintH2);
				}
				matrix.postTranslate(x - paintW2, y - paintH2);
				if (pointColor != null) {
					Paint paint = context.getPaintIconCustom();
					paint.setColorFilter(new PorterDuffColorFilter(pointColor, PorterDuff.Mode.SRC_IN));
					canvas.drawBitmap(bitmap, matrix, paint);
				} else {
					if (style.hasPaintedPointBitmap()) {
						Paint paint = context.getPaintIconCustom();
						paint.setColorFilter(null);
						canvas.drawBitmap(bitmap, matrix, paint);
					} else {
						canvas.drawBitmap(bitmap, matrix, context.getPaintIcon());
					}
				}
			}
		}

		@Nullable
		public Bitmap drawBitmap(@NonNull GeometryWayContext context) {
			int[] bitmapSize = getPointBitmapSize();
			if (bitmapSize != null) {
				int width;
				int height;
				if (angle == 0) {
					width = bitmapSize[0];
					height = bitmapSize[1];
				} else {
					float imageSize = (float) Math.sqrt(bitmapSize[0] * bitmapSize[0] + bitmapSize[1] * bitmapSize[1]) + 4f;
					width = (int) imageSize;
					height = (int) imageSize;
				}
				x = width / 2f;
				y = height / 2f;
				Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(image);
				draw(canvas, context);
				return image;
			}
			return null;
		}
	}
}
