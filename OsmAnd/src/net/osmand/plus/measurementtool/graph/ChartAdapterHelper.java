package net.osmand.plus.measurementtool.graph;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.ElevationChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.graph.BaseChartAdapter.ExternalValueSelectedListener;
import net.osmand.plus.measurementtool.graph.BaseChartAdapter.ExternalGestureListener;

import java.util.ArrayList;
import java.util.List;

public class ChartAdapterHelper {

	public static final String BIND_GRAPH_ADAPTERS_KEY = "bind_graph_adapters_key";
	public static final String BIND_TO_MAP_KEY = "bind_to_map_key";

	private static final int MAX_HIGHLIGHT_DISTANCE_DP = 10_000;

	private static Chart activeTouchSourceChart;
	private static float activeTouchValue;

	public static void bindGraphAdapters(CommonChartAdapter mainGraphAdapter,
	                                     List<BaseChartAdapter> otherGraphAdapters,
	                                     ViewGroup mainView) {
		if (mainGraphAdapter == null || mainGraphAdapter.getChart() == null
				|| otherGraphAdapters == null || otherGraphAdapters.size() == 0) {
			return;
		}

		ElevationChart mainChart = mainGraphAdapter.getChart();
		List<BaseChartAdapter> graphAdapters = new ArrayList<>();
		graphAdapters.add(mainGraphAdapter);
		graphAdapters.addAll(otherGraphAdapters);
		for (BaseChartAdapter adapter : graphAdapters) {
			adapter.setHighlightByValueFromTouchX(true);
		}

		mainGraphAdapter.addValueSelectedListener(BIND_GRAPH_ADAPTERS_KEY,
				new ExternalValueSelectedListener() {
					@Override
					public void onValueSelected(Entry e, Highlight h) {
						for (BaseChartAdapter adapter : graphAdapters) {
							if (activeTouchSourceChart != null) {
								adapter.highlight(h, activeTouchSourceChart, activeTouchValue);
							} else {
								adapter.highlight(h, mainChart, getHighlightValueByTouchX(mainChart, h.getXPx()));
							}
						}
					}

					@Override
					public void onNothingSelected() {
						for (BaseChartAdapter adapter : otherGraphAdapters) {
							adapter.highlight(null);
						}
					}
				}
		);

		@SuppressLint("ClickableViewAccessibility")
		View.OnTouchListener chartTouchListener = (v, ev) -> {
			if (ev.getSource() != 0) {
				Chart sourceChart = (Chart) v;
				float[] visibleWindowBeforeTouch = getVisibleValueWindow(sourceChart);
				float value = getHighlightValueByTouchX(sourceChart, ev.getX());
				if (mainView != null) {
					mainView.requestDisallowInterceptTouchEvent(true);
				}
				activeTouchSourceChart = sourceChart;
				activeTouchValue = value;
				try {
					for (BaseChartAdapter adapter : graphAdapters) {
						Chart targetChart = adapter.getChart();
						if (targetChart != null && targetChart != sourceChart) {
							MotionEvent event = mapMotionEvent(targetChart, ev, value);
							event.setSource(0);
							targetChart.dispatchTouchEvent(event);
						}
					}
				} finally {
					activeTouchSourceChart = null;
				}

				sourceChart.post(() -> {
					if (hasVisibleValueWindowChanged(sourceChart, visibleWindowBeforeTouch)) {
						syncVisibleValueWindow(sourceChart, graphAdapters);
					}
				});
			}
			return false;
		};

		for (BaseChartAdapter adapter : graphAdapters) {
			if (adapter.getChart() != null) {
				adapter.getChart().setMaxHighlightDistance(MAX_HIGHLIGHT_DISTANCE_DP);
				if (adapter.getChart() instanceof BarChart) {
					// maybe we should find min and max axis from all charts
					BarChart barChart = (BarChart) adapter.getChart();
					barChart.getAxisRight().setAxisMinimum(mainChart.getXChartMin());
					barChart.getAxisRight().setAxisMaximum(mainChart.getXChartMax());
					barChart.setHighlightPerDragEnabled(false);
					barChart.setHighlightPerTapEnabled(false);
				}
				adapter.getChart().setOnTouchListener(chartTouchListener);
			}
		}
	}

	public static float getHighlightValueByTouchX(@NonNull Chart chart, float touchX) {
		if (chart instanceof HorizontalBarChart) {
			MPPointD point = ((HorizontalBarChart) chart).getTransformer(AxisDependency.LEFT)
					.getValuesByTouchPoint(touchX, 1f);
			float value = (float) point.x;
			MPPointD.recycleInstance(point);
			return value;
		} else if (chart instanceof BarLineChartBase) {
			MPPointD point = ((BarLineChartBase) chart).getValuesByTouchPoint(touchX, 0f, AxisDependency.LEFT);
			float value = (float) point.x;
			MPPointD.recycleInstance(point);
			return value;
		}
		return touchX;
	}

	public static float getHighlightTouchXByValue(@NonNull Chart chart, float value) {
		if (chart instanceof HorizontalBarChart) {
			MPPointD point = ((HorizontalBarChart) chart).getPixelForValues(value, 0f, AxisDependency.LEFT);
			float x = (float) point.x;
			MPPointD.recycleInstance(point);
			return x;
		} else if (chart instanceof BarLineChartBase) {
			MPPointD point = ((BarLineChartBase) chart).getPixelForValues(value, 0f, AxisDependency.LEFT);
			float x = (float) point.x;
			MPPointD.recycleInstance(point);
			return x;
		}
		return value;
	}

	private static MotionEvent mapMotionEvent(@NonNull Chart targetChart, @NonNull MotionEvent event, float value) {
		MotionEvent mappedEvent = MotionEvent.obtainNoHistory(event);
		float mappedX = getHighlightTouchXByValue(targetChart, value);
		mappedEvent.offsetLocation(mappedX - event.getX(), 0);
		return mappedEvent;
	}

	private static void syncVisibleValueWindow(@NonNull Chart sourceChart,
	                                           @NonNull List<BaseChartAdapter> graphAdapters) {
		float[] window = getVisibleValueWindow(sourceChart);
		float min = window[0];
		float max = window[1];
		if (!Float.isFinite(min) || !Float.isFinite(max) || max <= min) {
			return;
		}
		for (BaseChartAdapter adapter : graphAdapters) {
			Chart targetChart = adapter.getChart();
			if (targetChart != null && targetChart != sourceChart) {
				applyVisibleValueWindow(targetChart, min, max);
			}
		}
	}

	private static float[] getVisibleValueWindow(@NonNull Chart chart) {
		ViewPortHandler handler = chart.getViewPortHandler();
		if (chart instanceof HorizontalBarChart) {
			Transformer transformer = ((HorizontalBarChart) chart).getTransformer(AxisDependency.LEFT);
			MPPointD leftPoint = transformer.getValuesByTouchPoint(handler.contentLeft(), 1f);
			MPPointD rightPoint = transformer.getValuesByTouchPoint(handler.contentRight(), 1f);
			float min = (float) Math.min(leftPoint.x, rightPoint.x);
			float max = (float) Math.max(leftPoint.x, rightPoint.x);
			MPPointD.recycleInstance(leftPoint);
			MPPointD.recycleInstance(rightPoint);
			return new float[] {min, max};
		} else if (chart instanceof BarLineChartBase) {
			return new float[] {
					((BarLineChartBase) chart).getLowestVisibleX(),
					((BarLineChartBase) chart).getHighestVisibleX()
			};
		}
		return new float[] {0, 0};
	}

	private static boolean hasVisibleValueWindowChanged(@NonNull Chart chart, @NonNull float[] previousWindow) {
		float[] currentWindow = getVisibleValueWindow(chart);
		return currentWindow.length == previousWindow.length
				&& (currentWindow[0] != previousWindow[0] || currentWindow[1] != previousWindow[1]);
	}

	private static void applyVisibleValueWindow(@NonNull Chart chart, float min, float max) {
		if (!(chart instanceof BarLineChartBase)) {
			return;
		}
		BarLineChartBase barLineChart = (BarLineChartBase) chart;
		float range = max - min;
		float axisRange = getHorizontalAxisRange(barLineChart);
		if (range <= 0 || axisRange <= 0) {
			return;
		}

		barLineChart.fitScreen();
		ViewPortHandler handler = barLineChart.getViewPortHandler();
		Matrix matrix = new Matrix(handler.getMatrixTouch());
		float scale = axisRange / range;
		handler.zoom(scale, 1f, matrix);
		handler.refresh(matrix, barLineChart, false);
		moveVisibleValueStartTo(barLineChart, min);
		barLineChart.calculateOffsets();
		barLineChart.invalidate();
	}

	private static float getHorizontalAxisRange(@NonNull BarLineChartBase chart) {
		if (chart instanceof HorizontalBarChart) {
			return chart.getAxisLeft().mAxisRange;
		}
		return chart.getXRange();
	}

	private static void moveVisibleValueStartTo(@NonNull BarLineChartBase chart, float min) {
		ViewPortHandler handler = chart.getViewPortHandler();
		Transformer transformer = chart.getTransformer(AxisDependency.LEFT);
		MPPointD point = transformer.getPixelForValues(min, 0f);

		Matrix matrix = new Matrix(handler.getMatrixTouch());
		matrix.postTranslate(handler.contentLeft() - (float) point.x, 0f);
		MPPointD.recycleInstance(point);
		handler.refresh(matrix, chart, false);
	}

	public static RefreshMapCallback bindToMap(@NonNull CommonChartAdapter graphAdapter,
	                                           @NonNull MapActivity mapActivity,
	                                           @NonNull TrackDetailsMenu detailsMenu) {
		boolean[] refreshingMap = {false};
		RefreshMapCallback refreshMapCallback = (fitTrackOnMap, forceFit, recalculateXAxis) -> {
			if (refreshingMap[0]) {
				return;
			}
			ElevationChart chart = graphAdapter.getChart();
			OsmandApplication app = mapActivity.getApp();
			if (!app.getRoutingHelper().isFollowingMode()) {
				refreshingMap[0] = true;
				try {
					detailsMenu.refreshChart(chart, fitTrackOnMap, forceFit, recalculateXAxis);
					mapActivity.refreshMap();
				} finally {
					refreshingMap[0] = false;
				}
			}
		};

		graphAdapter.addValueSelectedListener(BIND_TO_MAP_KEY,
				new CommonChartAdapter.ExternalValueSelectedListener() {

					@Override
					public void onValueSelected(Entry e, Highlight h) {
						refreshMapCallback.refreshMap(true, false, false);
					}

					@Override
					public void onNothingSelected() {
					}
				});

		graphAdapter.setExternalGestureListener(new ExternalGestureListener() {
			@Override
			public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
			}

			@Override
			public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture, boolean hasTranslated) {
				if ((lastPerformedGesture == ChartTouchListener.ChartGesture.DRAG && hasTranslated) ||
						lastPerformedGesture == ChartTouchListener.ChartGesture.X_ZOOM ||
						lastPerformedGesture == ChartTouchListener.ChartGesture.Y_ZOOM ||
						lastPerformedGesture == ChartTouchListener.ChartGesture.PINCH_ZOOM ||
						lastPerformedGesture == ChartTouchListener.ChartGesture.DOUBLE_TAP ||
						lastPerformedGesture == ChartTouchListener.ChartGesture.ROTATE) {
					refreshMapCallback.refreshMap(true, true, true);
				}
			}
		});

		return refreshMapCallback;
	}

	public interface RefreshMapCallback {
		void refreshMap(boolean fitTrackOnMap, boolean forceFit, boolean recalculateXAxis);
	}
}
