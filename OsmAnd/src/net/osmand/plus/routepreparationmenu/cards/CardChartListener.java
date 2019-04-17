package net.osmand.plus.routepreparationmenu.cards;

import android.view.MotionEvent;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;

public interface CardChartListener {
	/**
	 * Called when a value has been selected inside the chart.
	 *
	 * @param e The selected Entry
	 * @param h The corresponding highlight object that contains information
	 *          about the highlighted position such as dataSetIndex, ...
	 */
	void onValueSelected(BaseCard card, Entry e, Highlight h);

	/**
	 * Called when nothing has been selected or an "un-select" has been made.
	 */
	void onNothingSelected(BaseCard card);

	/**
	 * Callbacks when a touch-gesture has started on the chart (ACTION_DOWN)
	 *
	 * @param me
	 * @param lastPerformedGesture
	 */
	void onChartGestureStart(BaseCard card, MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture);

	/**
	 * Callbacks when a touch-gesture has ended on the chart (ACTION_UP, ACTION_CANCEL)
	 *
	 * @param me
	 * @param lastPerformedGesture
	 */
	void onChartGestureEnd(BaseCard card, MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture);

	/**
	 * Callbacks when the chart is longpressed.
	 *
	 * @param me
	 */
	void onChartLongPressed(BaseCard card, MotionEvent me);

	/**
	 * Callbacks when the chart is double-tapped.
	 *
	 * @param me
	 */
	void onChartDoubleTapped(BaseCard card, MotionEvent me);

	/**
	 * Callbacks when the chart is single-tapped.
	 *
	 * @param me
	 */
	void onChartSingleTapped(BaseCard card, MotionEvent me);

	/**
	 * Callbacks then a fling gesture is made on the chart.
	 *
	 * @param me1
	 * @param me2
	 * @param velocityX
	 * @param velocityY
	 */
	void onChartFling(BaseCard card, MotionEvent me1, MotionEvent me2, float velocityX, float velocityY);

	/**
	 * Callbacks when the chart is scaled / zoomed via pinch zoom gesture.
	 *
	 * @param me
	 * @param scaleX scalefactor on the x-axis
	 * @param scaleY scalefactor on the y-axis
	 */
	void onChartScale(BaseCard card, MotionEvent me, float scaleX, float scaleY);

	/**
	 * Callbacks when the chart is moved / translated via drag gesture.
	 *
	 * @param me
	 * @param dX translation distance on the x-axis
	 * @param dY translation distance on the y-axis
	 */
	void onChartTranslate(BaseCard card, Highlight h, MotionEvent me, float dX, float dY);
}
