package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.util.Algorithms;

import static net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import static net.osmand.GPXUtilities.GPXTrackAnalysis;
import static net.osmand.GPXUtilities.GPXFile;
import static net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MtGraphFragment extends Fragment
		implements MeasurementToolFragment.OnUpdateAdditionalInfoListener {

	private static String GRAPH_DATA_GPX_FILE_NAME = "graph_data_tmp";

	private View commonGraphContainer;
	private View customGraphContainer;
	private View messageContainer;
	private LineChart commonGraphChart;
	private HorizontalBarChart customGraphChart;
	private RecyclerView rvGraphTypesMenu;

	private boolean nightMode;
	private MeasurementEditingContext editingCtx;
	private GraphType currentGraphType;
	private List<GraphType> graphTypes = new ArrayList<>();

	private enum CommonGraphType {
		OVERVIEW(R.string.shared_string_overview, false),
		ALTITUDE(R.string.altitude, true),
		SLOPE(R.string.shared_string_slope, true),
		SPEED(R.string.map_widget_speed, false);

		CommonGraphType(int titleId, boolean canBeCalculated) {
			this.titleId = titleId;
			this.canBeCalculated = canBeCalculated;
		}

		final int titleId;
		final boolean canBeCalculated;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		final MapActivity mapActivity = (MapActivity) getActivity();
		final MeasurementToolFragment mtf = (MeasurementToolFragment) getParentFragment();
		if (mapActivity == null || mtf == null) return null;

		editingCtx = mtf.getEditingCtx();
		OsmandApplication app = mapActivity.getMyApplication();

		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		View view = UiUtilities.getInflater(app, nightMode).inflate(
				R.layout.fragment_measurement_tool_graph, container, false);
		commonGraphContainer = view.findViewById(R.id.common_graphs_container);
		customGraphContainer = view.findViewById(R.id.custom_graphs_container);
		messageContainer = view.findViewById(R.id.message_container);
		commonGraphChart = (LineChart) view.findViewById(R.id.line_chart);
		customGraphChart = (HorizontalBarChart) view.findViewById(R.id.horizontal_chart);
		updateGraphData();

		rvGraphTypesMenu = view.findViewById(R.id.graph_types_recycler_view);
		rvGraphTypesMenu.setLayoutManager(
				new LinearLayoutManager(mapActivity, RecyclerView.HORIZONTAL, false));

		refreshGraphTypesSelectionMenu();
		setupVisibleGraphType(graphTypes.get(0));

		return view;
	}

	private void refreshGraphTypesSelectionMenu() {
		rvGraphTypesMenu.removeAllViews();
		OsmandApplication app = getMyApplication();
		int activeColorId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		final HorizontalSelectionAdapter adapter = new HorizontalSelectionAdapter(app, nightMode);
		final ArrayList<HorizontalSelectionItem> items = new ArrayList<>();
		for (GraphType type : graphTypes) {
			HorizontalSelectionItem item = new HorizontalSelectionItem(type.getTitle(), type);
			if (type.isCustom()) {
				item.setTitleColorId(activeColorId);
			}
			if (type.hasData() || type.canBeCalculated) {
				items.add(item);
			}
		}
		adapter.setItems(items);
		String selectedItemKey = currentGraphType != null ? currentGraphType.getTitle() : items.get(0).getTitle();
		adapter.setSelectedItemByTitle(selectedItemKey);
		adapter.setListener(new HorizontalSelectionAdapter.HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(HorizontalSelectionAdapter.HorizontalSelectionItem item) {
				adapter.setItems(items);
				adapter.setSelectedItem(item);
				GraphType chosenGraphType = (GraphType) item.getObject();
				if (chosenGraphType != null && !chosenGraphType.equals(currentGraphType)) {
					setupVisibleGraphType(chosenGraphType);
				}
			}
		});
		rvGraphTypesMenu.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onUpdateAdditionalInfo() {
		updateGraphData();
		refreshGraphTypesSelectionMenu();
		setupVisibleGraphType(currentGraphType);
	}

	private void setupVisibleGraphType(GraphType preferredType) {
		currentGraphType = preferredType.hasData() ? preferredType : getFirstAvailableGraphType();
		updateDataView();
	}

	private GraphType getFirstAvailableGraphType() {
		for (GraphType type : graphTypes) {
			if (type.hasData() || type.canBeCalculated()) {
				return type;
			}
		}
		return null;
	}

	private void updateDataView() {
		if (currentGraphType.hasData()) {
			showGraph();
		} else if (currentGraphType.canBeCalculated()) {
			showMessage();
		}
	}

	private void showGraph() {
		if (currentGraphType.isCustom()) {
			customGraphChart.clear();
			commonGraphContainer.setVisibility(View.GONE);
			customGraphContainer.setVisibility(View.VISIBLE);
			messageContainer.setVisibility(View.GONE);
			prepareCustomGraphView((BarData) currentGraphType.getData());
		} else {
			commonGraphChart.clear();
			commonGraphContainer.setVisibility(View.VISIBLE);
			customGraphContainer.setVisibility(View.GONE);
			messageContainer.setVisibility(View.GONE);
			prepareCommonGraphView((LineData) currentGraphType.getData());
		}
	}

	private void showMessage() {
		commonGraphContainer.setVisibility(View.GONE);
		customGraphContainer.setVisibility(View.GONE);
		messageContainer.setVisibility(View.VISIBLE);
		TextView tvMessage = messageContainer.findViewById(R.id.message_text);
		ImageView icon = messageContainer.findViewById(R.id.message_icon);
		String message = getString(R.string.message_need_calculate_route_before_show_graph, currentGraphType.getTitle());
		tvMessage.setText(message);
		icon.setImageResource(R.drawable.ic_action_altitude_average);
	}

	private void prepareCommonGraphView(LineData data) {
		GpxUiHelper.setupGPXChart(commonGraphChart, 4, 24f, 16f, !nightMode, true);
		commonGraphChart.setData(data);
	}

	private void prepareCustomGraphView(BarData data) {
		OsmandApplication app = getMyApplication();
		if (app == null) return;

		GpxUiHelper.setupHorizontalGPXChart(app, customGraphChart, 5, 9, 24, true, nightMode);
		customGraphChart.setExtraRightOffset(16);
		customGraphChart.setExtraLeftOffset(16);
		customGraphChart.setData(data);
	}

	private void updateGraphData() {
		graphTypes.clear();
		OsmandApplication app = getMyApplication();
		GPXTrackAnalysis analysis = createGpxTrackAnalysis();

		// update common graph data
		for (CommonGraphType commonType : CommonGraphType.values()) {
			List<ILineDataSet> dataSets = getDataSets(commonType, commonGraphChart, analysis);
			Object data = null;
			if (!Algorithms.isEmpty(dataSets)) {
				data = new LineData(dataSets);
			}
			String title = getString(commonType.titleId);
			graphTypes.add(new GraphType(title, false, commonType.canBeCalculated, data));
		}

		// update custom graph data
		List<RouteSegmentResult> routeSegments = editingCtx.getAllRouteSegments();
		List<RouteStatistics> routeStatistics = calculateRouteStatistics(routeSegments);
		if (analysis != null && routeStatistics != null) {
			for (RouteStatistics statistics : routeStatistics) {
				String title = SettingsBaseActivity.getStringRouteInfoPropertyValue(app, statistics.name);
				BarData data = null;
				if (!Algorithms.isEmpty(statistics.elements)) {
					data = GpxUiHelper.buildStatisticChart(
							app, customGraphChart, statistics, analysis, true, nightMode);
				}
				graphTypes.add(new GraphType(title, true, false, data));
			}
		}
	}

	private List<ILineDataSet> getDataSets(CommonGraphType type, LineChart chart, GPXTrackAnalysis analysis) {
		List<ILineDataSet> dataSets = new ArrayList<>();
		if (chart != null && analysis != null) {
			OsmandApplication app = getMyApplication();
			switch (type) {
				case OVERVIEW: {
					List<GpxUiHelper.OrderedLineDataSet> dataList = new ArrayList<>();
					if (analysis.hasSpeedData) {
						GpxUiHelper.OrderedLineDataSet speedDataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart,
								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, true, true, false);
						dataList.add(speedDataSet);
					}
					if (analysis.hasElevationData) {
						GpxUiHelper.OrderedLineDataSet elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, chart,
								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, false, true, false);
						dataList.add(elevationDataSet);
						GpxUiHelper.OrderedLineDataSet slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart,
								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, null, true, true, false);
						dataList.add(slopeDataSet);
					}
					if (dataList.size() > 0) {
						Collections.sort(dataList, new Comparator<GpxUiHelper.OrderedLineDataSet>() {
							@Override
							public int compare(GpxUiHelper.OrderedLineDataSet o1, GpxUiHelper.OrderedLineDataSet o2) {
								return Float.compare(o1.getPriority(), o2.getPriority());
							}
						});
					}
					dataSets.addAll(dataList);
					break;
				}
				case ALTITUDE: {
					if (analysis.hasElevationData) {
						GpxUiHelper.OrderedLineDataSet elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, chart,
								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, false, true, false);//calcWithoutGaps);
						dataSets.add(elevationDataSet);
					}
					break;
				}
				case SLOPE:
					if (analysis.hasElevationData) {
						GpxUiHelper.OrderedLineDataSet slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart,
								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, null, true, true, false);
						dataSets.add(slopeDataSet);
					}
					break;
				case SPEED: {
					if (analysis.hasSpeedData) {
						GpxUiHelper.OrderedLineDataSet speedDataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart,
								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, false, true, false);//calcWithoutGaps);
						dataSets.add(speedDataSet);
					}
					break;
				}
			}
		}
		return dataSets;
	}

	private GPXTrackAnalysis createGpxTrackAnalysis() {
		GPXFile gpx;
		if (editingCtx.getGpxData() != null) {
			gpx = editingCtx.getGpxData().getGpxFile();
		} else {
			gpx = editingCtx.exportRouteAsGpx(GRAPH_DATA_GPX_FILE_NAME);
		}
		return gpx != null ? gpx.getAnalysis(0) : null;
	}

	private List<RouteStatistics> calculateRouteStatistics(List<RouteSegmentResult> route) {
		OsmandApplication app = getMyApplication();
		if (route == null || app == null) return null;

		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRender = app.getRendererRegistry().defaultRender();
		MapRenderRepositories maps = app.getResourceManager().getRenderer();
		RenderingRuleSearchRequest currentSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(currentRenderer, nightMode);
		RenderingRuleSearchRequest defaultSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(defaultRender, nightMode);
		return RouteStatisticsHelper.calculateRouteStatistic(route, currentRenderer,
				defaultRender, currentSearchRequest, defaultSearchRequest);
	}

	private OsmandApplication getMyApplication() {
		return getMapActivity().getMyApplication();
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private static class GraphType {
		private String title;
		private boolean isCustom;
		private boolean canBeCalculated;
		private Object data;

		public GraphType(String title, boolean isCustom, boolean canBeCalculated, Object data) {
			this.title = title;
			this.isCustom = isCustom;
			this.canBeCalculated = canBeCalculated;
			this.data = data;
		}

		public String getTitle() {
			return title;
		}

		public boolean isCustom() {
			return isCustom;
		}

		public boolean canBeCalculated() {
			return canBeCalculated;
		}

		public boolean hasData() {
			return getData() != null;
		}

		public Object getData() {
			return data;
		}
	}
}
