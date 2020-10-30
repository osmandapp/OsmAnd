package net.osmand.plus.measurementtool.graph;

import android.graphics.Matrix;
import android.view.MotionEvent;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.helpers.GpxUiHelper;

import java.util.HashMap;
import java.util.Map;

public class CommonGraphAdapter extends BaseGraphAdapter<LineChart, LineData, GpxDisplayItem> {

	private Highlight highlight;
	private Map<String, ExternalValueSelectedListener> externalValueSelectedListeners = new HashMap<>();
	private ExternalGestureListener externalGestureListener;

	public CommonGraphAdapter(LineChart chart, boolean usedOnMap) {
		super(chart, usedOnMap);
	}

	@Override
	protected void prepareCharterView() {
		super.prepareCharterView();

		mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				highlight = h;
				for (ExternalValueSelectedListener listener : externalValueSelectedListeners.values()) {
					listener.onValueSelected(e, h);
				}
			}

			@Override
			public void onNothingSelected() {
				for (ExternalValueSelectedListener listener : externalValueSelectedListeners.values()) {
					listener.onNothingSelected();
				}
			}
		});

		mChart.setOnChartGestureListener(new OnChartGestureListener() {
			boolean hasTranslated = false;
			float highlightDrawX = -1;

			@Override
			public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
				hasTranslated = false;
				if (mChart.getHighlighted() != null && mChart.getHighlighted().length > 0) {
					highlightDrawX = mChart.getHighlighted()[0].getDrawX();
				} else {
					highlightDrawX = -1;
				}
				if (externalGestureListener != null) {
					externalGestureListener.onChartGestureStart(me, lastPerformedGesture);
				}
			}

			@Override
			public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
				GpxDisplayItem gpxItem = getGpxItem();
				gpxItem.chartMatrix = new Matrix(mChart.getViewPortHandler().getMatrixTouch());
				Highlight[] highlights = mChart.getHighlighted();
				if (highlights != null && highlights.length > 0) {
					gpxItem.chartHighlightPos = highlights[0].getX();
				} else {
					gpxItem.chartHighlightPos = -1;
				}
				if (externalGestureListener != null) {
					externalGestureListener.onChartGestureEnd(me, lastPerformedGesture, hasTranslated);
				}
			}

			@Override
			public void onChartLongPressed(MotionEvent me) {
			}

			@Override
			public void onChartDoubleTapped(MotionEvent me) {
			}

			@Override
			public void onChartSingleTapped(MotionEvent me) {
			}

			@Override
			public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
			}

			@Override
			public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
			}

			@Override
			public void onChartTranslate(MotionEvent me, float dX, float dY) {
				hasTranslated = true;
				if (highlightDrawX != -1) {
					Highlight h = mChart.getHighlightByTouchPoint(highlightDrawX, 0f);
					if (h != null) {
							/*
							ILineDataSet set = mChart.getLineData().getDataSetByIndex(h.getDataSetIndex());
							if (set != null && set.isHighlightEnabled()) {
								Entry e = set.getEntryForXValue(h.getX(), h.getY());
								MPPointD pix = mChart.getTransformer(set.getAxisDependency()).getPixelForValues(e.getX(), e.getY());
								h.setDraw((float) pix.x, (float) pix.y);
							}
							*/
						mChart.highlightValue(h, true);
					}
				}
			}
		});
	}

	public void addValueSelectedListener(String key, ExternalValueSelectedListener listener) {
		this.externalValueSelectedListeners.put(key, listener);
	}

	public void removeValueSelectedListener(String key) {
		this.externalValueSelectedListeners.remove(key);
	}

	public void setExternalGestureListener(ExternalGestureListener listener) {
		this.externalGestureListener = listener;
	}

	@Override
	public void updateView() {
		GpxUiHelper.setupGPXChart(mChart, 4, 24f, 16f, !isNightMode(), true);
		mChart.setData(mChartData);
		updateHighlight();
	}

	@Override
	public void highlight(Highlight h) {
		super.highlight(h);
		mChart.highlightValue(highlight);
	}

	public GpxDisplayItem getGpxItem() {
		return mAdditionalData;
	}
}
