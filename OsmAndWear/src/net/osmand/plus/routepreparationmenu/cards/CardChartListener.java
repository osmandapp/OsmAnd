package net.osmand.plus.routepreparationmenu.cards;

import android.view.MotionEvent;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture;

public interface CardChartListener {
	/**
	 * Called when a value has been selected inside the chart.
	 *
	 * @param e The selected Entry
	 * @param h The corresponding highlight object that contains information
	 *          about the highlighted position such as dataSetIndex, ...
	 */
	void onValueSelected(MapBaseCard card, Entry e, Highlight h);

	/**
	 * Called when nothing has been selected or an "un-select" has been made.
	 */
	void onNothingSelected(MapBaseCard card);

	void onChartGestureStart(MapBaseCard card, MotionEvent me, ChartGesture lastPerformedGesture);

	void onChartGestureEnd(MapBaseCard card, MotionEvent me, ChartGesture lastPerformedGesture, boolean hasTranslated);
}
