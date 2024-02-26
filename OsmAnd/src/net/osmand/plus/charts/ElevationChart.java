package net.osmand.plus.charts;

import static com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM;

import static net.osmand.plus.charts.ChartUtils.CHART_LABEL_COUNT;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;

import net.osmand.plus.R;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.utils.AndroidUtils;

import java.util.Iterator;

public class ElevationChart extends LineChart {
	private static final float PADDING_BETWEEN_LABELS_AND_CONTENT_DP = 6;
	public static final float GRID_LINE_LENGTH_X_AXIS_DP = 10;

	public ElevationChart(Context context) {
		super(context);
	}

	public ElevationChart(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ElevationChart(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void init() {
		super.init();
		//setRenderer(new OsmLineChartRenderer(this, getAnimator(), getViewPortHandler()));
		setXAxisRenderer(new OsmXAxisRenderer(this, getViewPortHandler(), getXAxis(), getTransformer(YAxis.AxisDependency.RIGHT)));
		setRendererRightYAxis(new OsmYAxisRenderer(this, this.mViewPortHandler, this.mAxisRight, this.mRightAxisTransformer));
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
		float measureText = getMeasuredMaxLabel();
		this.mViewPortHandler.setChartDimens((float) width - measureText - AndroidUtils.dpToPx(getContext(), PADDING_BETWEEN_LABELS_AND_CONTENT_DP), (float) height);

		Iterator var5 = this.mJobs.iterator();
		while (var5.hasNext()) {
			Runnable r = (Runnable) var5.next();
			this.post(r);
		}

		this.mJobs.clear();
	}

	private float getMeasuredMaxLabel() {
		int from = mAxisRight.isDrawBottomYLabelEntryEnabled() ? 0 : 1;
		int to = this.mAxisRight.isDrawTopYLabelEntryEnabled() ? this.mAxisRight.mEntryCount : this.mAxisRight.mEntryCount - 1;

		LineData chartData = getLineData();
		int dataSetCount = chartData.getDataSetCount();
		OrderedLineDataSet lastDataSet = dataSetCount > 0 ? (OrderedLineDataSet) chartData.getDataSetByIndex(dataSetCount - 1) : null;
		if (lastDataSet != null && lastDataSet.getDataSetType() == GPXDataSetType.ALTITUDE_EXTRM) {
			dataSetCount--;
		}
		Paint paint = mAxisRendererRight.getPaintAxisLabels();
		float maxMeasuredWidth = 0;
		for (int i = from; i < to; ++i) {
			float measuredLabelWidth = 0;
			if (dataSetCount == 1) {
				String plainText = getAxisRight().getFormattedLabel(i);
				measuredLabelWidth = paint.measureText(plainText);
			} else {
				String leftText = getAxisLeft().getFormattedLabel(i);
				String rightText = getAxisRight().getFormattedLabel(i) + ", ";
				measuredLabelWidth = paint.measureText(rightText) + paint.measureText(leftText);

			}
			if (measuredLabelWidth > maxMeasuredWidth) {
				maxMeasuredWidth = measuredLabelWidth;
			}
		}
		return maxMeasuredWidth;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (this.mData != null) {
			if (this.mAutoScaleMinMaxEnabled) {
				this.autoScale();
			}

			if (this.mAxisRight.isEnabled()) {
				this.mAxisRendererRight.computeAxis(this.mAxisRight.mAxisMinimum, this.mAxisRight.mAxisMaximum, this.mAxisRight.isInverted());
			}
			this.mAxisRendererRight.renderGridLines(canvas);

			if (this.mXAxis.isEnabled()) {
				this.mXAxisRenderer.computeAxis(this.mXAxis.mAxisMinimum, this.mXAxis.mAxisMaximum, false);
			}
			this.mXAxisRenderer.renderAxisLine(canvas);
			this.mXAxisRenderer.renderGridLines(canvas);

			int clipRestoreCount = canvas.save();
			if (this.isClipDataToContentEnabled()) {
				canvas.clipRect(this.mViewPortHandler.getContentRect());
			}

			this.mRenderer.drawData(canvas);
			if (this.valuesToHighlight()) {
				this.mRenderer.drawHighlighted(canvas, this.mIndicesToHighlight);
			}

			canvas.restoreToCount(clipRestoreCount);
			this.mRenderer.drawExtras(canvas);
			if (this.mXAxis.isEnabled() && !this.mXAxis.isDrawLimitLinesBehindDataEnabled()) {
				this.mXAxisRenderer.renderLimitLines(canvas);
			}

			this.mXAxisRenderer.renderAxisLabels(canvas);
			this.renderYAxisLabels(canvas);
			if (this.isClipValuesToContentEnabled()) {
				clipRestoreCount = canvas.save();
				canvas.clipRect(this.mViewPortHandler.getContentRect());
				this.mRenderer.drawValues(canvas);
				canvas.restoreToCount(clipRestoreCount);
			} else {
				this.mRenderer.drawValues(canvas);
			}

			this.mLegendRenderer.renderLegend(canvas);
			this.drawDescription(canvas);
			this.drawMarkers(canvas);
		}
	}

	public void setupGPXChart() {
		setupGPXChart(24f, 16f, true);
	}

	public void setupGPXChart(float topOffset, float bottomOffset,
							  boolean useGesturesAndScale) {
		setupGPXChart(topOffset, bottomOffset, useGesturesAndScale, null);
	}

	public void setupGPXChart(float topOffset, float bottomOffset,
							  boolean useGesturesAndScale, @Nullable Drawable markerIcon) {
		GpxMarkerView markerView = new GpxMarkerView(getContext(), markerIcon);
		setupGPXChart(markerView, topOffset, bottomOffset, useGesturesAndScale);
	}

	public void setupGPXChart(@NonNull GpxMarkerView markerView, float topOffset, float bottomOffset, boolean useGesturesAndScale) {
		Context context = getContext();

		setExtraRightOffset(16);
		setExtraLeftOffset(16);
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
		setMinOffset(0f);
		setDragDecelerationEnabled(false);

		markerView.setChartView(this);
		setMarker(markerView);
		setDrawMarkers(true);

		int labelsColor = ContextCompat.getColor(context, R.color.text_color_secondary_light);
		XAxis xAxis = getXAxis();
		xAxis.setDrawAxisLine(true);
		xAxis.setAxisLineWidth(1);
		xAxis.setAxisLineColor(labelsColor);
		xAxis.setDrawGridLines(true);
		xAxis.setGridLineWidth(1f);
		xAxis.setGridColor(labelsColor);
		xAxis.enableGridDashedLine(AndroidUtils.dpToPx(context, GRID_LINE_LENGTH_X_AXIS_DP), Float.MAX_VALUE, 0f);
		xAxis.setPosition(BOTTOM);
		xAxis.setTextColor(labelsColor);

		int dp4 = AndroidUtils.dpToPx(context, 4);
		int yAxisGridColor = AndroidUtils.getColorFromAttr(context, R.attr.chart_grid_line_color);
		Typeface typeface = FontCache.getFont(context, context.getString(R.string.font_roboto_medium));

		YAxis leftYAxis = getAxisLeft();
		leftYAxis.setEnabled(false);

		YAxis rightYAxis = getAxisRight();
		rightYAxis.enableGridDashedLine(dp4, dp4, 0f);
		rightYAxis.setGridColor(yAxisGridColor);
		rightYAxis.setGridLineWidth(1f);
		rightYAxis.setDrawBottomYGridLine(false);
		rightYAxis.setDrawAxisLine(false);
		rightYAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
		rightYAxis.setXOffset(-1f);
		rightYAxis.setYOffset(10.25f);
		rightYAxis.setTypeface(typeface);
		rightYAxis.setTextSize(10f);
		rightYAxis.setLabelCount(CHART_LABEL_COUNT, true);

		Legend legend = getLegend();
		legend.setEnabled(false);
	}
}
