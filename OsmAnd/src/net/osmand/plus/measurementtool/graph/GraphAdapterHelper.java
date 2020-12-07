package net.osmand.plus.measurementtool.graph;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.graph.BaseGraphAdapter.ExternalValueSelectedListener;
import net.osmand.plus.measurementtool.graph.BaseGraphAdapter.ExternalGestureListener;

import java.util.List;

public class GraphAdapterHelper {

	public static final String BIND_GRAPH_ADAPTERS_KEY = "bind_graph_adapters_key";
	public static final String BIND_TO_MAP_KEY = "bind_to_map_key";

	public static void bindGraphAdapters(final CommonGraphAdapter mainGraphAdapter,
	                                     final List<BaseGraphAdapter> otherGraphAdapters,
	                                     final ViewGroup mainView) {
		if (mainGraphAdapter == null || mainGraphAdapter.getChart() == null
				|| otherGraphAdapters == null || otherGraphAdapters.size() == 0) {
			return;
		}

		final LineChart mainChart = mainGraphAdapter.getChart();
		View.OnTouchListener mainChartTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent ev) {
				if (mainView != null) {
					mainView.requestDisallowInterceptTouchEvent(true);
				}
				for (BaseGraphAdapter adapter : otherGraphAdapters) {
					if (adapter.getChart() != null) {
						MotionEvent event = MotionEvent.obtainNoHistory(ev);
						event.setSource(0);
						adapter.getChart().dispatchTouchEvent(event);
					}
				}
				return false;
			}
		};
		mainChart.setOnTouchListener(mainChartTouchListener);

		mainGraphAdapter.addValueSelectedListener(BIND_GRAPH_ADAPTERS_KEY,
				new ExternalValueSelectedListener() {
					@Override
					public void onValueSelected(Entry e, Highlight h) {
						for (BaseGraphAdapter adapter : otherGraphAdapters) {
							adapter.highlight(h);
						}
					}

					@Override
					public void onNothingSelected() {
						for (BaseGraphAdapter adapter : otherGraphAdapters) {
							adapter.highlight(null);
						}
					}
				}
		);

		View.OnTouchListener otherChartsTouchListener = new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent ev) {
				if (ev.getSource() != 0) {
					final MotionEvent event = MotionEvent.obtainNoHistory(ev);
					event.setSource(0);
					mainChart.dispatchTouchEvent(event);
					return true;
				}
				return false;
			}
		};

		for (BaseGraphAdapter adapter : otherGraphAdapters) {
			if (adapter.getChart() != null) {
				if (adapter.getChart() instanceof BarChart) {
					// maybe we should find min and max axis from all charters
					BarChart barChart = (BarChart) adapter.getChart();
					barChart.getAxisRight().setAxisMinimum(mainChart.getXChartMin());
					barChart.getAxisRight().setAxisMaximum(mainChart.getXChartMax());
					barChart.setHighlightPerDragEnabled(false);
					barChart.setHighlightPerTapEnabled(false);
				}
				adapter.getChart().setOnTouchListener(otherChartsTouchListener);
			}
		}
	}

	public static RefreshMapCallback bindToMap(@NonNull final CommonGraphAdapter graphAdapter,
	                                           @NonNull final MapActivity mapActivity,
	                                           @NonNull final TrackDetailsMenu detailsMenu) {
		final RefreshMapCallback refreshMapCallback = new RefreshMapCallback() {
			@Override
			public void refreshMap(boolean fitTrackOnMap, boolean forceFit) {
				LineChart chart = graphAdapter.getChart();
				OsmandApplication app = mapActivity.getMyApplication();
				if (!app.getRoutingHelper().isFollowingMode()) {
					detailsMenu.refreshChart(chart, fitTrackOnMap, forceFit);
					mapActivity.refreshMap();
				}
			}
		};

		graphAdapter.addValueSelectedListener(BIND_TO_MAP_KEY,
				new CommonGraphAdapter.ExternalValueSelectedListener() {

					@Override
					public void onValueSelected(Entry e, Highlight h) {
						refreshMapCallback.refreshMap(true, false);
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
					refreshMapCallback.refreshMap(true, true);
				}
			}
		});

		return refreshMapCallback;
	}

	public interface RefreshMapCallback {
		void refreshMap(boolean fitTrackOnMap, boolean forceFit);
	}
}
