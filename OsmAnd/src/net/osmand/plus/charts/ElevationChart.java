package net.osmand.plus.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.Utils;

import net.osmand.plus.utils.AndroidUtils;

import java.util.Iterator;

public class ElevationChart extends LineChart {
	private static final float PADDING_BETWEEN_LABELS_AND_CONTENT_DP = 6;
	public static final float GRID_LINE_LENGTH_X_AXIS_DP = 8;
	public static final float PADDING_BETWEEN_X_LABELS_AND_X_LINE = 1;
	public static final int CHART_LABEL_COUNT = 3;

	private boolean showLastSet = true;
	private boolean drawBottomYGridLine = true;

	public ElevationChart(Context context) {
		super(context);
	}

	public ElevationChart(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ElevationChart(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setShowLastSet(boolean shouldShowLastSet) {
		this.showLastSet = shouldShowLastSet;
	}

	public boolean shouldShowLastSet() {
		return showLastSet;
	}

	public boolean isDrawBottomYGridLine() {
		return drawBottomYGridLine;
	}

	public void setDrawBottomYGridLine(boolean drawBottomYGridLine) {
		this.drawBottomYGridLine = drawBottomYGridLine;
	}

	@Override
	protected void init() {
		super.init();
		setXAxisRenderer(new ElevationXAxisRenderer(this, getViewPortHandler(), getXAxis(), getTransformer(YAxis.AxisDependency.RIGHT)));
		setRendererRightYAxis(new ElevationYAxisRenderer(this, mViewPortHandler, mAxisRight, mRightAxisTransformer));
		setRenderer(new ElevationChartRenderer(this, mAnimator, mViewPortHandler));
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updateDimens(w, h);
	}

	public void updateDimens() {
		updateDimens(getWidth(), getHeight());
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		updateDimens();
	}

	public void updateDimens(int width, int height) {
		if (mData == null) {
			return;
		}

		float measureText = getMeasuredMaxLabel();
		Utils.init(getContext());
		mViewPortHandler.setChartDimens((float) width - measureText - AndroidUtils.dpToPx(getContext(), PADDING_BETWEEN_LABELS_AND_CONTENT_DP), (float) height);

		Iterator jobIterator = mJobs.iterator();
		while (jobIterator.hasNext()) {
			Runnable r = (Runnable) jobIterator.next();
			post(r);
		}

		mJobs.clear();
	}

	private float getMeasuredMaxLabel() {
		int from = mAxisRight.isDrawBottomYLabelEntryEnabled() ? 0 : 1;
		int to = mAxisRight.isDrawTopYLabelEntryEnabled() ? mAxisRight.mEntryCount : mAxisRight.mEntryCount - 1;

		LineData chartData = getLineData();
		int dataSetCount = chartData.getDataSetCount();
		LineDataSet lastDataSet = dataSetCount > 0 ? (LineDataSet) chartData.getDataSetByIndex(dataSetCount - 1) : null;
		if (lastDataSet != null && !shouldShowLastSet()) {
			dataSetCount--;
		}

		Paint paint = mAxisRendererRight.getPaintAxisLabels();
		float maxMeasuredWidth = 0.0F;

		for (int i = from; i < to; ++i) {
			float measuredLabelWidth = 0.0F;
			String leftText;
			if (dataSetCount == 1) {
				leftText = getAxisLeft().getFormattedLabel(i);
				if (lastDataSet instanceof OrderedLineDataSet) {
					leftText = ((OrderedLineDataSet) lastDataSet).isLeftAxis() ? leftText : getAxisRight().getFormattedLabel(i);
				}
				measuredLabelWidth = paint.measureText(leftText);
			} else {
				leftText = getAxisLeft().getFormattedLabel(i) + ", ";
				String rightText = getAxisRight().getFormattedLabel(i);
				measuredLabelWidth = paint.measureText(leftText) + paint.measureText(rightText);
			}

			if (measuredLabelWidth > maxMeasuredWidth) {
				maxMeasuredWidth = measuredLabelWidth;
			}
		}

		return maxMeasuredWidth;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mData != null) {
			if (mAutoScaleMinMaxEnabled) {
				prepareValuePxMatrix();
				autoScale();
			}

			if (mAxisRight.isEnabled()) {
				mAxisRendererRight.computeAxis(mAxisRight.mAxisMinimum, mAxisRight.mAxisMaximum, mAxisRight.isInverted());
			}

			mAxisRendererLeft.computeAxis(mAxisLeft.mAxisMinimum, mAxisLeft.mAxisMaximum, mAxisLeft.isInverted());

			if (mXAxis.isEnabled()) {
				mXAxisRenderer.computeAxis(mXAxis.mAxisMinimum, mXAxis.mAxisMaximum, false);
			}

			int clipRestoreCount = canvas.save();
			if (isClipDataToContentEnabled()) {
				canvas.clipRect(mViewPortHandler.getContentRect());
			}

			mRenderer.drawData(canvas);
			if (valuesToHighlight()) {
				mRenderer.drawHighlighted(canvas, mIndicesToHighlight);
			}

			canvas.restoreToCount(clipRestoreCount);
			mRenderer.drawExtras(canvas);
			if (mXAxis.isEnabled() && !mXAxis.isDrawLimitLinesBehindDataEnabled()) {
				mXAxisRenderer.renderLimitLines(canvas);
			}

			mXAxisRenderer.renderAxisLabels(canvas);
			renderYAxisLabels(canvas);
			if (isClipValuesToContentEnabled()) {
				clipRestoreCount = canvas.save();
				canvas.clipRect(mViewPortHandler.getContentRect());
				mRenderer.drawValues(canvas);
				canvas.restoreToCount(clipRestoreCount);
			} else {
				mRenderer.drawValues(canvas);
			}

			mXAxisRenderer.renderGridLines(canvas);
			mXAxisRenderer.renderAxisLine(canvas);
			mAxisRendererRight.renderGridLines(canvas);

			mLegendRenderer.renderLegend(canvas);
			drawDescription(canvas);
			drawMarkers(canvas);
		}
	}

	@Override
	protected void autoScale() {
		final float fromX = getLowestVisibleX();
		final float toX = getHighestVisibleX();

		mData.calcMinMaxY(fromX, toX);
		mXAxis.calculate(mData.getXMin(), mData.getXMax());

		mAxisLeft.calculate(mData.getYMin(YAxis.AxisDependency.LEFT), mData.getYMax(YAxis.AxisDependency.LEFT));
		mAxisRight.calculate(mData.getYMin(YAxis.AxisDependency.RIGHT), mData.getYMax(YAxis.AxisDependency.RIGHT));

		calculateOffsets();
	}

	@Override
	protected void renderYAxisLabels(@NonNull Canvas canvas) {
		this.mAxisRendererRight.renderAxisLabels(canvas);
	}

	public void setupGPXChart(@NonNull MarkerView markerView, float topOffset, float bottomOffset, int xAxisGridColor, int labelsColor, int yAxisGridColor, Typeface typeface, boolean useGesturesAndScale) {
		Context context = getContext();

		setExtraRightOffset(16.0F);
		setExtraLeftOffset(16.0F);
		setExtraTopOffset(topOffset);
		setExtraBottomOffset(bottomOffset);

		setHardwareAccelerationEnabled(true);
		setTouchEnabled(useGesturesAndScale);
		setDragEnabled(useGesturesAndScale);
		setScaleEnabled(useGesturesAndScale);
		setPinchZoom(useGesturesAndScale);
		setScaleYEnabled(false);
		setAutoScaleMinMaxEnabled(true);
		setDrawBorders(false);
		getDescription().setEnabled(false);
		setMaxVisibleValueCount(10);
		setMinOffset(0.0F);
		setDragDecelerationEnabled(false);

		markerView.setChartView(this);
		setMarker(markerView);
		setDrawMarkers(true);
		XAxis xAxis = getXAxis();
		xAxis.setYOffset((GRID_LINE_LENGTH_X_AXIS_DP / 2) + PADDING_BETWEEN_X_LABELS_AND_X_LINE);
		xAxis.setDrawAxisLine(true);
		xAxis.setAxisLineWidth(1.0F);
		xAxis.setAxisLineColor(xAxisGridColor);
		xAxis.setDrawGridLines(true);
		xAxis.setGridLineWidth(1.0F);
		xAxis.setGridColor(xAxisGridColor);
		xAxis.enableGridDashedLine(AndroidUtils.dpToPx(context, GRID_LINE_LENGTH_X_AXIS_DP), Float.MAX_VALUE, 0.0F);
		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setTextColor(labelsColor);
		xAxis.setAvoidFirstLastClipping(true);

		int dp4 = AndroidUtils.dpToPx(context, 4.0F);

		YAxis leftYAxis = getAxisLeft();
		leftYAxis.setLabelCount(CHART_LABEL_COUNT, true);
		leftYAxis.setEnabled(false);

		YAxis rightYAxis = getAxisRight();
		rightYAxis.enableGridDashedLine(dp4, dp4, 0.0F);
		rightYAxis.setGridColor(yAxisGridColor);
		rightYAxis.setGridLineWidth(1.0F);
		setDrawBottomYGridLine(false);
		rightYAxis.setDrawAxisLine(false);
		rightYAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
		rightYAxis.setXOffset(-1.0F);
		rightYAxis.setYOffset(10.25F);
		rightYAxis.setTypeface(typeface);
		rightYAxis.setTextSize(10.0F);
		rightYAxis.setLabelCount(CHART_LABEL_COUNT, true);

		Legend legend = getLegend();
		legend.setEnabled(false);
	}
}
