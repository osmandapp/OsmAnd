package net.osmand.plus.measurementtool;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.AndroidUtils;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.LineGraphType;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionAdapterListener;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.MeasurementToolFragment.OnUpdateInfoListener;
import net.osmand.plus.measurementtool.graph.BaseGraphAdapter;
import net.osmand.plus.measurementtool.graph.CommonGraphAdapter;
import net.osmand.plus.measurementtool.graph.CustomGraphAdapter;
import net.osmand.plus.measurementtool.graph.CustomGraphAdapter.LegendViewType;
import net.osmand.plus.measurementtool.graph.GraphAdapterHelper;
import net.osmand.plus.measurementtool.graph.GraphAdapterHelper.RefreshMapCallback;
import net.osmand.plus.routepreparationmenu.RouteDetailsFragment;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.osmand.GPXUtilities.GPXFile;
import static net.osmand.GPXUtilities.GPXTrackAnalysis;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.ALTITUDE;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.SLOPE;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.SPEED;
import static net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import static net.osmand.router.RouteStatisticsHelper.RouteStatistics;

public class GraphsCard extends BaseCard implements OnUpdateInfoListener {

	private static String GRAPH_DATA_GPX_FILE_NAME = "graph_data_tmp";
	private static int INVALID_ID = -1;

	private MeasurementEditingContext editingCtx;
	private MeasurementToolFragment fragment;
	private TrackDetailsMenu trackDetailsMenu;
	private RefreshMapCallback refreshMapCallback;
	private GPXFile gpxFile;
	private GPXTrackAnalysis analysis;
	private GpxDisplayItem gpxItem;

	private View commonGraphContainer;
	private View customGraphContainer;
	private View messageContainer;
	private CommonGraphAdapter commonGraphAdapter;
	private CustomGraphAdapter customGraphAdapter;
	private RecyclerView graphTypesMenu;

	private GraphType visibleType;
	private List<GraphType> graphTypes = new ArrayList<>();

	public GraphsCard(@NonNull MapActivity mapActivity,
	                  TrackDetailsMenu trackDetailsMenu,
	                  MeasurementToolFragment fragment) {
		super(mapActivity);
		this.trackDetailsMenu = trackDetailsMenu;
		this.fragment = fragment;
	}

	@Override
	protected void updateContent() {
		if (mapActivity == null || fragment == null) return;
		editingCtx = fragment.getEditingCtx();

		graphTypesMenu = view.findViewById(R.id.graph_types_recycler_view);
		graphTypesMenu.setLayoutManager(new LinearLayoutManager(mapActivity, RecyclerView.HORIZONTAL, false));
		commonGraphContainer = view.findViewById(R.id.common_graphs_container);
		customGraphContainer = view.findViewById(R.id.custom_graphs_container);
		messageContainer = view.findViewById(R.id.message_container);
		LineChart lineChart = (LineChart) view.findViewById(R.id.line_chart);
		HorizontalBarChart barChart = (HorizontalBarChart) view.findViewById(R.id.horizontal_chart);
		commonGraphAdapter = new CommonGraphAdapter(lineChart, true);
		customGraphAdapter = new CustomGraphAdapter(barChart, true);

		customGraphAdapter.setLegendContainer((ViewGroup) view.findViewById(R.id.route_legend));
		customGraphAdapter.setLayoutChangeListener(new BaseGraphAdapter.LayoutChangeListener() {
			@Override
			public void onLayoutChanged() {
				setLayoutNeeded();
			}
		});

		GraphAdapterHelper.bindGraphAdapters(commonGraphAdapter, Collections.singletonList((BaseGraphAdapter) customGraphAdapter), (ViewGroup) view);
		refreshMapCallback = GraphAdapterHelper.bindToMap(commonGraphAdapter, mapActivity, trackDetailsMenu);
		updateTopPadding();
		fullUpdate();
	}

	private void updateTopPadding() {
		int topPadding = AndroidUiHelper.isOrientationPortrait(mapActivity) ?
				0 : app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		view.setPadding(0, topPadding, 0, 0);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.measurement_tool_graph_card;
	}

	@Override
	public void onUpdateInfo() {
		if (editingCtx != null) {
			fullUpdate();
		}
	}

	private void fullUpdate() {
		if (!isRouteCalculating()) {
			updateData();
			setupVisibleType();
			updateMenu();
		}
		updateView();
		updateChartOnMap();
	}

	private void updateMenu() {
		if (editingCtx.isPointsEnoughToCalculateRoute()) {
			graphTypesMenu.setVisibility(View.VISIBLE);
			graphTypesMenu.removeAllViews();
			fillInMenu();
		} else {
			graphTypesMenu.setVisibility(View.GONE);
		}
	}

	private void fillInMenu() {
		OsmandApplication app = getMyApplication();
		int activeColorId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		final HorizontalSelectionAdapter adapter = new HorizontalSelectionAdapter(app, nightMode);
		final ArrayList<HorizontalSelectionItem> items = new ArrayList<>();
		for (GraphType type : graphTypes) {
			HorizontalSelectionItem item = new HorizontalSelectionItem(type.getTitle(), type);
			if (type.isCustom()) {
				item.setTitleColorId(activeColorId);
			}
			if (type.isAvailable()) {
				items.add(item);
			}
		}
		adapter.setItems(items);
		String selectedItemKey = visibleType.getTitle();
		adapter.setSelectedItemByTitle(selectedItemKey);
		adapter.setListener(new HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(HorizontalSelectionItem item) {
				adapter.setItems(items);
				adapter.setSelectedItem(item);
				GraphType chosenType = (GraphType) item.getObject();
				if (!isVisibleType(chosenType)) {
					changeVisibleType(chosenType);
				}
			}
		});
		graphTypesMenu.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	private void changeVisibleType(GraphType type) {
		visibleType = type;
		updateView();
	}

	private boolean isVisibleType(GraphType type) {
		if (visibleType != null && type != null) {
			return Algorithms.objectEquals(visibleType.getTitle(), type.getTitle());
		}
		return false;
	}

	private GraphType getFirstAvailableType() {
		for (GraphType type : graphTypes) {
			if (type.isAvailable()) {
				return type;
			}
		}
		return null;
	}

	private void updateView() {
		hideAll();
		if (!editingCtx.isPointsEnoughToCalculateRoute()) {
			showMessage(app.getString(R.string.message_you_need_add_two_points_to_show_graphs));
		} else if (isRouteCalculating()) {
			showMessage(app.getString(R.string.message_graph_will_be_available_after_recalculation), true);
		} else if (visibleType.hasData()) {
			showGraph();
		} else if (visibleType.canBeCalculated()) {
			showMessage(app.getString(R.string.message_need_calculate_route_before_show_graph,
					visibleType.getTitle()), R.drawable.ic_action_altitude_average,
					app.getString(R.string.route_between_points), new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							fragment.startSnapToRoad(false);
						}
					});
		}
	}

	private void hideAll() {
		commonGraphContainer.setVisibility(View.GONE);
		customGraphContainer.setVisibility(View.GONE);
		messageContainer.setVisibility(View.GONE);
	}

	private void showMessage(String text) {
		showMessage(text, INVALID_ID, false, null, null);
	}

	private void showMessage(String text, @DrawableRes int iconResId, String btnTitle, View.OnClickListener btnListener) {
		showMessage(text, iconResId, false, btnTitle, btnListener);
	}

	private void showMessage(String text, boolean showProgressBar) {
		showMessage(text, INVALID_ID, showProgressBar, null, null);
	}

	private void showMessage(@NonNull String text,
	                         @DrawableRes int iconResId,
	                         boolean showProgressBar,
	                         String btnTitle,
	                         View.OnClickListener btnListener) {
		messageContainer.setVisibility(View.VISIBLE);
		TextView tvMessage = messageContainer.findViewById(R.id.message_text);
		tvMessage.setText(text);
		ImageView icon = messageContainer.findViewById(R.id.message_icon);
		if (iconResId != INVALID_ID) {
			icon.setVisibility(View.VISIBLE);
			icon.setImageResource(iconResId);
		} else {
			icon.setVisibility(View.GONE);
		}
		ProgressBar pb = messageContainer.findViewById(R.id.progress_bar);
		pb.setVisibility(showProgressBar ? View.VISIBLE : View.GONE);
		View btnContainer = messageContainer.findViewById(R.id.btn_container);
		if (btnTitle != null) {
			TextView tvBtnTitle = btnContainer.findViewById(R.id.btn_text);
			tvBtnTitle.setText(btnTitle);
			btnContainer.setVisibility(View.VISIBLE);
		} else {
			btnContainer.setVisibility(View.GONE);
		}
		if (btnListener != null) {
			btnContainer.setOnClickListener(btnListener);
		}
	}

	private void showGraph() {
		if (visibleType.isCustom()) {
			CustomGraphType customGraphType = (CustomGraphType) visibleType;
			customGraphContainer.setVisibility(View.VISIBLE);
			customGraphAdapter.setLegendViewType(LegendViewType.ONE_ELEMENT);
			customGraphAdapter.updateContent(customGraphType.getChartData(), customGraphType.getStatistics());
		} else {
			CommonGraphType commonGraphType = (CommonGraphType) visibleType;
			commonGraphContainer.setVisibility(View.VISIBLE);
			customGraphAdapter.setLegendViewType(LegendViewType.GONE);
			commonGraphAdapter.updateContent(commonGraphType.getChartData(), gpxItem);
		}
	}

	private void updateData() {
		graphTypes.clear();
		OsmandApplication app = getMyApplication();
		gpxFile = getGpxFile();
		analysis = gpxFile != null ? gpxFile.getAnalysis(0) : null;
		gpxItem = gpxFile != null ? GpxUiHelper.makeGpxDisplayItem(app, gpxFile) : null;
		if (gpxItem != null) {
			trackDetailsMenu.setGpxItem(gpxItem);
		}
		if (analysis == null) return;

		// update common graph data
		boolean hasElevationData = analysis.hasElevationData;
		boolean hasSpeedData = analysis.isSpeedSpecified();
		addCommonType(R.string.shared_string_overview, true, hasElevationData, ALTITUDE, SLOPE);
		addCommonType(R.string.altitude, true, hasElevationData, ALTITUDE, null);
		addCommonType(R.string.shared_string_slope, true, hasElevationData, SLOPE, null);
		addCommonType(R.string.map_widget_speed, false, hasSpeedData, SPEED, null);

		// update custom graph data
		List<RouteStatistics> routeStatistics = calculateRouteStatistics();
		if (analysis != null && routeStatistics != null) {
			for (RouteStatistics statistics : routeStatistics) {
				String title = AndroidUtils.getStringRouteInfoPropertyValue(app, statistics.name);
				graphTypes.add(new CustomGraphType(title, statistics));
			}
		}
	}

	private void updateChartOnMap() {
		if (hasVisibleGraph()) {
			trackDetailsMenu.reset();
			refreshMapCallback.refreshMap(false, false);
		}
	}

	private void addCommonType(int titleId,
	                           boolean canBeCalculated,
	                           boolean hasData,
	                           LineGraphType firstType,
	                           LineGraphType secondType) {
		OsmandApplication app = getMyApplication();
		String title = app.getString(titleId);
		graphTypes.add(new CommonGraphType(title, canBeCalculated, hasData, firstType, secondType));
	}

	private void setupVisibleType() {
		if (visibleType == null) {
			visibleType = getFirstAvailableType();
		} else {
			for (GraphType type : graphTypes) {
				if (isVisibleType(type)) {
					visibleType = type.isAvailable() ? type : getFirstAvailableType();
					break;
				}
			}
		}
	}

	private GPXFile getGpxFile() {
		if (fragment.isTrackReadyToCalculate()) {
			return editingCtx.exportGpx(GRAPH_DATA_GPX_FILE_NAME);
		} else {
			GpxData gpxData = editingCtx.getGpxData();
			return gpxData != null ? gpxData.getGpxFile() : null;
		}
	}

	private List<RouteStatistics> calculateRouteStatistics() {
		OsmandApplication app = getMyApplication();
		List<RouteSegmentResult> route = editingCtx.getOrderedRoadSegmentData();
		if (route != null && app != null) {
			return RouteDetailsFragment.calculateRouteStatistics(app, route, nightMode);
		}
		return null;
	}

	private boolean isRouteCalculating() {
		return fragment.isProgressBarVisible();
	}

	public boolean hasVisibleGraph() {
		return (commonGraphContainer != null && commonGraphContainer.getVisibility() == View.VISIBLE)
				|| (customGraphContainer != null && customGraphContainer.getVisibility() == View.VISIBLE);
	}

	private abstract class GraphType<T extends ChartData> {
		private String title;
		private boolean canBeCalculated;

		public GraphType(String title, boolean canBeCalculated) {
			this.title = title;
			this.canBeCalculated = canBeCalculated;
		}

		public String getTitle() {
			return title;
		}

		public boolean isCustom() {
			return this instanceof CustomGraphType;
		}

		public boolean isAvailable() {
			return isPointsCountEnoughToCalculateRoute() && (hasData() || canBeCalculated());
		}

		private boolean isPointsCountEnoughToCalculateRoute() {
			return editingCtx.getPointsCount() >= 2;
		}

		public boolean canBeCalculated() {
			return canBeCalculated;
		}

		public abstract boolean hasData();

		public abstract T getChartData();
	}

	private class CommonGraphType extends  GraphType<LineData> {

		private boolean hasData;
		private LineGraphType firstType;
		private LineGraphType secondType;

		public CommonGraphType(String title, boolean canBeCalculated, boolean hasData, @NonNull LineGraphType firstType, @Nullable LineGraphType secondType) {
			super(title, canBeCalculated);
			this.hasData = hasData;
			this.firstType = firstType;
			this.secondType = secondType;
		}

		@Override
		public boolean hasData() {
			return hasData;
		}

		@Override
		public LineData getChartData() {
			GpxUiHelper.setupGPXChart(commonGraphAdapter.getChart(), 4, 24f, 16f, !nightMode, true);
			List<ILineDataSet> dataSets = GpxUiHelper.getDataSets(commonGraphAdapter.getChart(),
					app, analysis, firstType, secondType, false);
			return new LineData(dataSets);
		}
	}

	private class CustomGraphType extends GraphType<BarData> {

		private RouteStatistics statistics;

		public CustomGraphType(String title, RouteStatistics statistics) {
			super(title, false);
			this.statistics = statistics;
		}

		public RouteStatistics getStatistics() {
			return statistics;
		}

		@Override
		public boolean hasData() {
			return !Algorithms.isEmpty(statistics.elements);
		}

		@Override
		public BarData getChartData() {
			GpxUiHelper.setupHorizontalGPXChart(app, customGraphAdapter.getChart(), 5, 9, 24, true, nightMode);
			if (!Algorithms.isEmpty(statistics.elements)) {
				return GpxUiHelper.buildStatisticChart(app, customGraphAdapter.getChart(),
						statistics, analysis, true, nightMode);
			}
			return new BarData();
		}
	}
}
