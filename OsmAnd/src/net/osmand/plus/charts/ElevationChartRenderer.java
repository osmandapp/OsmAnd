package net.osmand.plus.charts;

import android.graphics.Canvas;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet.Mode;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * {@link LineChartRenderer} that skips the off-screen bitmap.
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

	private static boolean requiresBitmapCanvas(@NonNull ILineDataSet dataSet) {
		Mode mode = dataSet.getMode();
		return mode == Mode.CUBIC_BEZIER || mode == Mode.HORIZONTAL_BEZIER
				|| dataSet.isDashedLineEnabled();
	}
}
