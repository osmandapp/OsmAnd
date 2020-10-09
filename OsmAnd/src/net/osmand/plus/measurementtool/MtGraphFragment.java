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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MtGraphFragment extends Fragment
		implements MeasurementToolFragment.OnUpdateAdditionalInfoListener {

	public static final String TAG = MtGraphFragment.class.getName();

	private static String GRAPH_DATA_GPX_FILE_NAME = "graph_data_tmp";

	private View commonGraphContainer;
	private View customGraphContainer;
	private View messageContainer;
	private LineChart commonGraphChart;
	private HorizontalBarChart customGraphChart;
	private RecyclerView rvMenu;

	private boolean nightMode;
	private MeasurementEditingContext editingCtx;
	private GraphType currentGraphType;
	private Map<GraphType, Object> graphData = new HashMap<>();

	private enum GraphType {
		OVERVIEW(R.string.shared_string_overview, false, false),
		ALTITUDE(R.string.altitude, false, true),
//		SLOPE(R.string.shared_string_slope, false, true),
		SPEED(R.string.map_widget_speed, false, false),

		SURFACE(R.string.routeInfo_surface_name, true, false),
		ROAD_TYPE(R.string.routeInfo_roadClass_name, true, false),
		STEEPNESS(R.string.routeInfo_steepness_name, true, false),
		SMOOTHNESS(R.string.routeInfo_smoothness_name, true, false);

		GraphType(int titleId, boolean isCustomType, boolean canBeCalculated) {
			this.titleId = titleId;
			this.isCustomType = isCustomType;
			this.canBeCalculated = canBeCalculated;
		}

		final int titleId;
		final boolean isCustomType;
		final boolean canBeCalculated;

		private static List<GraphType> commonTypes;
		private static List<GraphType> customTypes;

		static List<GraphType> getCommonTypes() {
			if (commonTypes == null) {
				prepareLists();
			}
			return commonTypes;
		}

		static List<GraphType> getCustomTypes() {
			if (customTypes == null) {
				prepareLists();
			}
			return customTypes;
		}

		private static void prepareLists() {
			commonTypes = new ArrayList<>();
			customTypes = new ArrayList<>();
			for (GraphType type : values()) {
				if (type.isCustomType) {
					customTypes.add(type);
				} else {
					commonTypes.add(type);
				}
			}
		}
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

		rvMenu = view.findViewById(R.id.graph_types_recycler_view);
		rvMenu.setLayoutManager(
				new LinearLayoutManager(mapActivity, RecyclerView.HORIZONTAL, false));

		prepareGraphTypesSelectionMenu();
		setupVisibleGraphType(GraphType.OVERVIEW);

		return view;
	}

	private void prepareGraphTypesSelectionMenu() {
		rvMenu.removeAllViews();
		OsmandApplication app = getMyApplication();
		int activeColorId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		final HorizontalSelectionAdapter adapter = new HorizontalSelectionAdapter(app, nightMode);
		final ArrayList<HorizontalSelectionItem> items = new ArrayList<>();
		for (GraphType type : GraphType.values()) {
			String title = getString(type.titleId);
			HorizontalSelectionItem item = new HorizontalSelectionItem(title, type);
			if (type.isCustomType) {
				item.setTitleColorId(activeColorId);
			}
			if (isDataAvailableFor(type) || type.canBeCalculated) {
				items.add(item);
			}
		}
		adapter.setItems(items);
		String selectedItemKey = currentGraphType != null ?
				getString(currentGraphType.titleId) : items.get(0).getTitle();
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
		rvMenu.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onUpdateAdditionalInfo() {
		updateGraphData();
		prepareGraphTypesSelectionMenu();
		setupVisibleGraphType(currentGraphType);
	}

	private void setupVisibleGraphType(GraphType preferredType) {
		currentGraphType = isDataAvailableFor(preferredType) ?
				preferredType : getFirstAvailableGraphType();
		updateDataView();
	}

	private GraphType getFirstAvailableGraphType() {
		for (GraphType type : GraphType.values()) {
			if (isDataAvailableFor(type) || type.canBeCalculated) {
				return type;
			}
		}
		return GraphType.OVERVIEW;
	}

	private void updateDataView() {
		if (isDataAvailableFor(currentGraphType)) {
			showGraph();
		} else if (currentGraphType.canBeCalculated) {
			showMessage();
		}
	}

	private void showGraph() {
		if (currentGraphType.isCustomType) {
			customGraphChart.clear();
			commonGraphContainer.setVisibility(View.GONE);
			customGraphContainer.setVisibility(View.VISIBLE);
			messageContainer.setVisibility(View.GONE);
			prepareCustomGraphView();
		} else {
			commonGraphChart.clear();
			commonGraphContainer.setVisibility(View.VISIBLE);
			customGraphContainer.setVisibility(View.GONE);
			messageContainer.setVisibility(View.GONE);
			prepareCommonGraphView();
		}
	}

	private void showMessage() {
		commonGraphContainer.setVisibility(View.GONE);
		customGraphContainer.setVisibility(View.GONE);
		messageContainer.setVisibility(View.VISIBLE);
		TextView tvMessage = messageContainer.findViewById(R.id.message_text);
		ImageView icon = messageContainer.findViewById(R.id.message_icon);
		if (GraphType.ALTITUDE.equals(currentGraphType)) {
			tvMessage.setText(R.string.message_need_calculate_route_for_show_graph);
			icon.setImageResource(R.drawable.ic_action_altitude_average);
		}
	}

	private void prepareCommonGraphView() {
		LineData data = (LineData) graphData.get(currentGraphType);
		if (data == null) return;

		GpxUiHelper.setupGPXChart(commonGraphChart, 4, 24f, 16f, !nightMode, true);
		commonGraphChart.setData(data);
	}

	private void prepareCustomGraphView() {
		BarData data = (BarData) graphData.get(currentGraphType);
		OsmandApplication app = getMapActivity().getMyApplication();
		if (data == null || app == null) return;

		GpxUiHelper.setupHorizontalGPXChart(app, customGraphChart, 5, 9, 24, true, nightMode);
		customGraphChart.setExtraRightOffset(16);
		customGraphChart.setExtraLeftOffset(16);
		customGraphChart.setData(data);
	}

	private void updateGraphData() {
		OsmandApplication app = getMyApplication();
		GPXTrackAnalysis analysis = createGpxTrackAnalysis();

		// update common graph data
		for (GraphType type : GraphType.getCommonTypes()) {
			List<ILineDataSet> dataSets = getDataSets(type, commonGraphChart, analysis);
			if (!Algorithms.isEmpty(dataSets)) {
				graphData.put(type, new LineData(dataSets));
			} else {
				graphData.put(type, null);
			}
		}

		// update custom graph data
		List<RouteSegmentResult> routeSegments = editingCtx.getAllRouteSegments();
		List<RouteStatistics> routeStatistics = calculateRouteStatistics(routeSegments);
		for (GraphType type : GraphType.getCustomTypes()) {
			RouteStatistics statistic = getStatisticForGraphType(routeStatistics, type);
			if (statistic != null && !Algorithms.isEmpty(statistic.elements)) {
				BarData data = GpxUiHelper.buildStatisticChart(
						app, customGraphChart, statistic, analysis, true, nightMode);
				graphData.put(type, data);
			} else {
				graphData.put(type, null);
			}
		}
	}

	private RouteStatistics getStatisticForGraphType(List<RouteStatistics> routeStatistics, GraphType graphType) {
		if (routeStatistics == null) return null;
		for (RouteStatistics statistic : routeStatistics) {
			int graphTypeId = graphType.titleId;
			int statisticId = SettingsBaseActivity.getStringRouteInfoPropertyValueId(statistic.name);
			if (graphTypeId == statisticId) {
				return statistic;
			}
		}
		return null;
	}

	private List<ILineDataSet> getDataSets(GraphType graphType, LineChart chart, GPXTrackAnalysis analysis) {
		List<ILineDataSet> dataSets = new ArrayList<>();
		if (chart != null && analysis != null) {
			OsmandApplication app = getMyApplication();
			switch (graphType) {
				case OVERVIEW: {
					GpxUiHelper.OrderedLineDataSet speedDataSet = null;
					GpxUiHelper.OrderedLineDataSet elevationDataSet = null;
//					GpxUiHelper.OrderedLineDataSet slopeDataSet = null;
					if (analysis.hasSpeedData) {
						speedDataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart,
								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, true, true, false);
					}
					if (analysis.hasElevationData) {
						elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, chart,
								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, false, true, false);
//						slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart,
//								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, null, true, true, false);
					}
					List<GpxUiHelper.OrderedLineDataSet> dataList = new ArrayList<>();
					if (speedDataSet != null) {
						dataList.add(speedDataSet);
					}
					if (elevationDataSet != null) {
						dataList.add(elevationDataSet);
					}
//					if (slopeDataSet != null) {
//						dataList.add(slopeDataSet);
//					}
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
						if (elevationDataSet != null) {
							dataSets.add(elevationDataSet);
						}
					}
					break;
				}
//				case SLOPE:
//					if (analysis.hasElevationData) {
//						GpxUiHelper.OrderedLineDataSet slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart,
//								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, null, true, true, false);
//						if (slopeDataSet != null) {
//							dataSets.add(slopeDataSet);
//						}
//					}
//					break;
				case SPEED: {
					if (analysis.hasSpeedData) {
						GpxUiHelper.OrderedLineDataSet speedDataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart,
								analysis, GpxUiHelper.GPXDataSetAxisType.DISTANCE, false, true, false);//calcWithoutGaps);
						if (speedDataSet != null) {
							dataSets.add(speedDataSet);
						}
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

	private boolean isDataAvailableFor(GraphType graphType) {
		return graphData != null && graphData.get(graphType) != null;
	}

	private OsmandApplication getMyApplication() {
		return getMapActivity().getMyApplication();
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}
}
