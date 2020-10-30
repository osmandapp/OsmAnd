package net.osmand.plus.measurementtool.graph;

import android.view.MotionEvent;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;

import net.osmand.plus.OsmandApplication;

public abstract class BaseGraphAdapter<CHART extends Chart, CHART_DATA extends ChartData, DATA> {

	private Highlight lastKnownHighlight;
	protected CHART mChart;
	protected CHART_DATA mChartData;
	protected DATA mAdditionalData;
	protected boolean usedOnMap;

	public BaseGraphAdapter(CHART chart, boolean usedOnMap) {
		this.mChart = chart;
		this.usedOnMap = usedOnMap;
		prepareCharterView();
	}

	protected void prepareCharterView() {
		mChart.setExtraRightOffset(16);
		mChart.setExtraLeftOffset(16);
	}

	public CHART getChart() {
		return mChart;
	}

	protected void updateHighlight() {
		highlight(lastKnownHighlight);
	}

	public void highlight(Highlight h) {
		this.lastKnownHighlight = h;
	}

	public void fullUpdate(CHART_DATA chartData, DATA data) {
		updateData(chartData, data);
		updateView();
	}

	public void updateData(CHART_DATA chartData, DATA data) {
		this.mChartData = chartData;
		this.mAdditionalData = data;
	}

	public abstract void updateView();

	protected boolean isNightMode() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			return usedOnMap ? app.getDaynightHelper().isNightModeForMapControls()
					: !app.getSettings().isLightContent();
		}
		return false;
	}

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) mChart.getContext().getApplicationContext();
	}

	public interface ExternalValueSelectedListener {
		void onValueSelected(Entry e, Highlight h);
		void onNothingSelected();
	}

	public interface ExternalGestureListener {
		void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture);
		void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture, boolean hasTranslated);
	}

	public interface LayoutChangeListener {
		void onLayoutChanged();
	}
}
