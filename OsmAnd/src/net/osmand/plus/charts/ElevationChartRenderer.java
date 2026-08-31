package net.osmand.plus.charts;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet.Mode;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;

/**
 * {@link LineChartRenderer} that skips the off-screen bitmap and can colour a line by the values of
 * a second data set.
 * <p>
 * The stock renderer keeps a full-screen ARGB_8888 bitmap, clears it and blits it onto the canvas
 * on every frame. Only the bezier paths and dashed data sets ever draw into that bitmap; linear,
 * non-dashed sets are drawn straight onto the real canvas. All chart data sets here keep the
 * default {@link Mode#LINEAR}, so the erase and the blit are pure overhead.
 * <p>
 * If a data set that does need the bitmap ever shows up, drawing is delegated back to the stock
 * renderer, which owns it.
 */
public class ElevationChartRenderer extends LineChartRenderer {

	private static final float GRADIENT_STOP_STEP_PX = 3f;
	private static final int MAX_GRADIENT_STOPS = 512;

	private final GradientFillDrawable gradientFill = new GradientFillDrawable();

	private int[] gradientColors = new int[0];
	private float[] gradientPositions = new float[0];

	public ElevationChartRenderer(LineDataProvider chart, ChartAnimator animator,
	                              ViewPortHandler viewPortHandler) {
		super(chart, animator, viewPortHandler);
	}

	@Override
	public void drawData(Canvas canvas) {
		LineData lineData = mChart.getLineData();

		for (ILineDataSet dataSet : lineData.getDataSets()) {
			if (dataSet.isVisible() && requiresBitmapCanvas(dataSet)) {
				super.drawData(canvas);
				return;
			}
		}
		for (ILineDataSet dataSet : lineData.getDataSets()) {
			if (dataSet.isVisible()) {
				drawDataSet(canvas, dataSet);
			}
		}
	}

	@Override
	protected void drawDataSet(Canvas canvas, ILineDataSet dataSet) {
		ChartColorSource colorSource = getColorSource(dataSet);
		LinearGradient gradient = colorSource != null ? createGradient(dataSet, colorSource) : null;
		if (gradient == null) {
			super.drawDataSet(canvas, dataSet);
			return;
		}
		OrderedLineDataSet orderedDataSet = (OrderedLineDataSet) dataSet;
		Drawable previousFill = orderedDataSet.getFillDrawable();
		boolean fillWasEnabled = orderedDataSet.isDrawFilledEnabled();
		try {
			// the fill is painted by clipping this drawable to the area under the line
			gradientFill.setGradient(gradient, orderedDataSet.getFillAlpha());
			orderedDataSet.setFillDrawable(gradientFill);
			// a shader on the render paint takes precedence over the flat line colour
			if (OsmandDevelopmentPlugin.CHART_COLOR_LINE_STROKE) {
				mRenderPaint.setShader(gradient);
			}
			super.drawDataSet(canvas, orderedDataSet);
		} finally {
			mRenderPaint.setShader(null);
			orderedDataSet.setFillDrawable(previousFill);
			orderedDataSet.setDrawFilled(fillWasEnabled);
		}
	}

	@Nullable
	private LinearGradient createGradient(@NonNull ILineDataSet dataSet,
	                                      @NonNull ChartColorSource colorSource) {
		float left = mViewPortHandler.contentLeft();
		float right = mViewPortHandler.contentRight();
		if (right - left < 1) {
			return null;
		}
		Transformer transformer = mChart.getTransformer(dataSet.getAxisDependency());
		MPPointD leftValue = transformer.getValuesByTouchPoint(left, 0);
		MPPointD rightValue = transformer.getValuesByTouchPoint(right, 0);
		float fromX = (float) leftValue.x;
		float toX = (float) rightValue.x;
		MPPointD.recycleInstance(leftValue);
		MPPointD.recycleInstance(rightValue);

		int stops = (int) ((right - left) / GRADIENT_STOP_STEP_PX) + 1;
		stops = Math.max(2, Math.min(stops, MAX_GRADIENT_STOPS));
		if (gradientColors.length != stops) {
			gradientColors = new int[stops];
			gradientPositions = new float[stops];
		}
		boolean discrete = OsmandDevelopmentPlugin.CHART_DISCRETE_COLORS;
		for (int i = 0; i < stops; i++) {
			float ratio = i / (float) (stops - 1);
			float x = fromX + ratio * (toX - fromX);
			// stops sit a few pixels apart, so band colours read as hard edges without extra work
			gradientColors[i] = discrete ? colorSource.getBandColorAt(x) : colorSource.getColorAt(x);
			gradientPositions[i] = ratio;
		}
		return new LinearGradient(left, 0, right, 0, gradientColors, gradientPositions,
				Shader.TileMode.CLAMP);
	}

	@Nullable
	private static ChartColorSource getColorSource(@NonNull ILineDataSet dataSet) {
		if (dataSet instanceof OrderedLineDataSet orderedDataSet) {
			return orderedDataSet.getColorSource();
		}
		return null;
	}

	private static boolean requiresBitmapCanvas(@NonNull ILineDataSet dataSet) {
		Mode mode = dataSet.getMode();
		return mode == Mode.CUBIC_BEZIER || mode == Mode.HORIZONTAL_BEZIER
				|| dataSet.isDashedLineEnabled();
	}

	private static class GradientFillDrawable extends Drawable {

		private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		void setGradient(@NonNull Shader shader, int alpha) {
			paint.setShader(shader);
			paint.setAlpha(alpha);
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			canvas.drawRect(getBounds(), paint);
		}

		@Override
		public void setAlpha(int alpha) {
			paint.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter) {
			paint.setColorFilter(colorFilter);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}
	}
}
