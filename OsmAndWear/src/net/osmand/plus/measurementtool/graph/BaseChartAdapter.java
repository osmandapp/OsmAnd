package net.osmand.plus.measurementtool.graph;

import android.view.MotionEvent;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;

import net.osmand.plus.OsmandApplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class BaseChartAdapter<_Chart extends Chart<_ChartData>, _ChartData extends ChartData<?>, _Data> {

	private Highlight lastKnownHighlight;
	protected OsmandApplication app;
	protected _Chart chart;
	protected _ChartData chartData;
	protected _Data additionalData;
	protected boolean usedOnMap;

	protected ViewGroup bottomInfoContainer;
	private LayoutChangeListener layoutChangeListener;

	public BaseChartAdapter(@NonNull OsmandApplication app, @NonNull _Chart chart, boolean usedOnMap) {
		this.app = app;
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

	public void updateView() {
		chart.setData(chartData);
		updateHighlight();
		updateBottomInfo();
	}

	protected void updateBottomInfo() {
		if (bottomInfoContainer != null) {
			bottomInfoContainer.removeAllViews();
			attachBottomInfo();
			if (layoutChangeListener != null) {
				layoutChangeListener.onLayoutChanged();
			}
		}
	}

	protected abstract void attachBottomInfo();

	public void setBottomInfoContainer(@Nullable ViewGroup bottomInfoContainer) {
		this.bottomInfoContainer = bottomInfoContainer;
	}

	public void setLayoutChangeListener(@Nullable LayoutChangeListener layoutChangeListener) {
		this.layoutChangeListener = layoutChangeListener;
	}

	protected boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
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
