package net.osmand.plus.measurementtool.graph;

import android.view.MotionEvent;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;

import net.osmand.plus.OsmandApplication;

public abstract class BaseGraphAdapter<_Chart extends Chart, _ChartData extends ChartData, _Data> {

	private Highlight lastKnownHighlight;
	protected _Chart chart;
	protected _ChartData chartData;
	protected _Data additionalData;
	protected boolean usedOnMap;

	public BaseGraphAdapter(_Chart chart, boolean usedOnMap) {
		this.chart = chart;
		this.usedOnMap = usedOnMap;
		prepareChartView();
	}

	protected void prepareChartView() {
		chart.setExtraRightOffset(16);
		chart.setExtraLeftOffset(16);
	}

	public _Chart getChart() {
		return chart;
	}

	protected void updateHighlight() {
		highlight(lastKnownHighlight);
	}

	public void highlight(Highlight h) {
		this.lastKnownHighlight = h;
	}

	public void updateContent(_ChartData chartData, _Data data) {
		updateData(chartData, data);
		updateView();
	}

	public void updateData(_ChartData chartData, _Data data) {
		this.chartData = chartData;
		this.additionalData = data;
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
		return (OsmandApplication) chart.getContext().getApplicationContext();
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
