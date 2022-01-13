package net.osmand.plus.measurementtool;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.LineGraphType;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer;
import net.osmand.plus.measurementtool.MeasurementToolFragment.OnUpdateInfoListener;
import net.osmand.plus.measurementtool.graph.ChartAdapterHelper;
import net.osmand.plus.measurementtool.graph.ChartAdapterHelper.RefreshMapCallback;
import net.osmand.plus.measurementtool.graph.CommonChartAdapter;
import net.osmand.plus.measurementtool.graph.CustomChartAdapter;
import net.osmand.plus.measurementtool.graph.CustomChartAdapter.LegendViewType;
import net.osmand.plus.myplaces.GPXTabItemType;
import net.osmand.plus.routepreparationmenu.RouteDetailsFragment;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static net.osmand.GPXUtilities.GPXFile;
import static net.osmand.GPXUtilities.GPXTrackAnalysis;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.ALTITUDE;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.SLOPE;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.SPEED;
import static net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import static net.osmand.router.RouteStatisticsHelper.RouteStatistics;

public class ChartsCard extends MapBaseCard implements OnUpdateInfoListener {

	private static final String GRAPH_DATA_GPX_FILE_NAME = "graph_data_tmp";
	private static final int INVALID_ID = -1;

	private final MeasurementToolFragment fragment;
	private final TrackDetailsMenu trackDetailsMenu;

	private MeasurementEditingContext editingCtx;
	private RefreshMapCallback refreshMapCallback;
	private GPXTrackAnalysis analysis;
	private GpxDisplayItem gpxItem;

	private OnScrollChangedListener scrollChangedListener;
	private View commonGraphContainer;
	private View customGraphContainer;
	private View messageContainer;
	private CommonChartAdapter commonGraphAdapter;
	private CustomChartAdapter customGraphAdapter;
	private RecyclerView graphTypesMenu;

	private ChartType<?> visibleType;
	private final List<ChartType<?>> chartTypes = new ArrayList<>();

	public ChartsCard(@NonNull MapActivity mapActivity,
	                  @NonNull TrackDetailsMenu trackDetailsMenu,
	                  @NonNull MeasurementToolFragment fragment) {
		super(mapActivity);
		this.trackDetailsMenu = trackDetailsMenu;
		this.fragment = fragment;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.measurement_tool_graph_card;
	}

	@Override
	protected void updateContent() {
		editingCtx = fragment.getEditingCtx();

		graphTypesMenu = view.findViewById(R.id.graph_types_recycler_view);
		graphTypesMenu.setLayoutManager(new LinearLayoutManager(mapActivity, RecyclerView.HORIZONTAL, false));

		setupScrollListener();

		LineChart lineChart = view.findViewById(R.id.line_chart);
		HorizontalBarChart barChart = view.findViewById(R.id.horizontal_chart);
		commonGraphAdapter = new CommonChartAdapter(app, lineChart, true);
		customGraphAdapter = new CustomChartAdapter(app, barChart, true);

		commonGraphContainer = view.findViewById(R.id.common_graphs_container);
		commonGraphAdapter.setBottomInfoContainer(view.findViewById(R.id.statistics_container));
		customGraphContainer = view.findViewById(R.id.custom_graphs_container);
		customGraphAdapter.setBottomInfoContainer(view.findViewById(R.id.route_legend));
		customGraphAdapter.setLayoutChangeListener(this::setLayoutNeeded);

		messageContainer = view.findViewById(R.id.message_container);

		ViewGroup scrollView = view.findViewById(R.id.scroll_view);
		ChartAdapterHelper.bindGraphAdapters(commonGraphAdapter, Collections.singletonList(customGraphAdapter), scrollView);
		refreshMapCallback = ChartAdapterHelper.bindToMap(commonGraphAdapter, mapActivity, trackDetailsMenu);

		updateTopPadding();
		fullUpdate();
	}

	private void setupScrollListener() {
		if (scrollChangedListener == null) {
			View scrollContainer = view.findViewById(R.id.scroll_container);
			View scrollView = view.findViewById(R.id.scroll_view);
			scrollChangedListener = () -> {
				boolean scrollToTopAvailable = scrollView.canScrollVertically(-1);
				if (scrollToTopAvailable) {
					scrollContainer.setForeground(getIcon(R.drawable.bg_contextmenu_shadow));
				} else {
					scrollContainer.setForeground(null);
				}
			};
			scrollView.getViewTreeObserver().addOnScrollChangedListener(scrollChangedListener);
		}
	}

	private void updateTopPadding() {
		int topPadding = AndroidUiHelper.isOrientationPortrait(mapActivity) ?
				0 : app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		view.setPadding(0, topPadding, 0, 0);
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

	@SuppressLint("NotifyDataSetChanged")
	private void fillInMenu() {
		int activeColorId = ColorUtilities.getActiveColorId(nightMode);
		final HorizontalSelectionAdapter adapter = new HorizontalSelectionAdapter(app, nightMode);
		final ArrayList<HorizontalSelectionItem> items = new ArrayList<>();
		for (ChartType<?> type : chartTypes) {
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
		adapter.setListener(item -> {
			adapter.setItems(items);
			adapter.setSelectedItem(item);
			ChartType<?> chosenType = (ChartType<?>) item.getObject();
			if (!isVisibleType(chosenType)) {
				changeVisibleType(chosenType);
			}
		});
		graphTypesMenu.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	private void changeVisibleType(ChartType<?> type) {
		visibleType = type;
		updateView();
	}

	private boolean isVisibleType(ChartType<?> type) {
		return visibleType != null && type != null
				&& Algorithms.objectEquals(visibleType.getTitle(), type.getTitle());
	}

	private ChartType<?> getFirstAvailableType() {
		for (ChartType<?> type : chartTypes) {
			if (type.isAvailable()) {
				return type;
			}
		}
		return null;
	}

	private void updateView() {
		hideAll();
		if (!editingCtx.isPointsEnoughToCalculateRoute()) {
			showMessage(app.getString(R.string.message_you_need_add_two_points_to_show_graphs), false);
		} else if (isRouteCalculating()) {
			showMessage(app.getString(R.string.message_graph_will_be_available_after_recalculation), true);
		} else if (visibleType.hasData()) {
			showGraph();
		} else if (visibleType.canBeCalculated()) {
			showMessage(app.getString(R.string.message_need_calculate_route_before_show_graph,
					visibleType.getTitle()), R.drawable.ic_action_altitude_average,
					app.getString(R.string.route_between_points),
					v -> fragment.startSnapToRoad(false));
		}
	}

	private void hideAll() {
		commonGraphContainer.setVisibility(View.GONE);
		customGraphContainer.setVisibility(View.GONE);
		messageContainer.setVisibility(View.GONE);
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
			CustomChartType customGraphType = (CustomChartType) visibleType;
			customGraphContainer.setVisibility(View.VISIBLE);
			customGraphAdapter.setLegendViewType(LegendViewType.ALL_AS_LIST);
			customGraphAdapter.updateContent(customGraphType.getChartData(), customGraphType.getStatistics());
		} else {
			CommonChartType commonGraphType = (CommonChartType) visibleType;
			customGraphAdapter.setLegendViewType(LegendViewType.GONE);
			commonGraphContainer.setVisibility(View.VISIBLE);
			commonGraphAdapter.setGpxGraphType(commonGraphType.getGpxGraphType());
			commonGraphAdapter.updateContent(commonGraphType.getChartData(), gpxItem);
		}
	}

	private void updateData() {
		chartTypes.clear();
		GPXFile gpxFile = getGpxFile();
		analysis = gpxFile != null ? gpxFile.getAnalysis(0) : null;
		gpxItem = gpxFile != null
				? GpxUiHelper.makeGpxDisplayItem(app, gpxFile, ChartPointLayer.MEASUREMENT_TOOL)
				: null;
		if (gpxItem != null) {
			trackDetailsMenu.setGpxItem(gpxItem);
		}
		if (analysis == null) {
			return;
		}

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
				chartTypes.add(new CustomChartType(title, statistics));
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
		String title = app.getString(titleId);
		chartTypes.add(new CommonChartType(title, canBeCalculated, hasData, firstType, secondType));
	}

	private void setupVisibleType() {
		if (visibleType == null) {
			visibleType = getFirstAvailableType();
		} else {
			for (ChartType<?> type : chartTypes) {
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
		List<RouteSegmentResult> route = editingCtx.getOrderedRoadSegmentData();
		if (route != null) {
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

	private abstract class ChartType<T extends ChartData<?>> {
		private final String title;
		private final boolean canBeCalculated;

		public ChartType(String title, boolean canBeCalculated) {
			this.title = title;
			this.canBeCalculated = canBeCalculated;
		}

		public String getTitle() {
			return title;
		}

		public boolean isCustom() {
			return this instanceof CustomChartType;
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

	private class CommonChartType extends ChartType<LineData> {

		private final boolean hasData;
		private final LineGraphType firstType;
		private final LineGraphType secondType;

		public CommonChartType(String title, boolean canBeCalculated, boolean hasData, @NonNull LineGraphType firstType, @Nullable LineGraphType secondType) {
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
			GpxUiHelper.setupGPXChart(commonGraphAdapter.getChart(), 24f, 16f, true);
			List<ILineDataSet> dataSets = GpxUiHelper.getDataSets(commonGraphAdapter.getChart(),
					app, analysis, firstType, secondType, false);
			return new LineData(dataSets);
		}

		@Nullable
		public GPXTabItemType getGpxGraphType() {
			if (firstType == ALTITUDE && secondType == SLOPE) {
				return GPXTabItemType.GPX_TAB_ITEM_GENERAL;
			} else if (firstType == ALTITUDE || firstType == SLOPE) {
				return GPXTabItemType.GPX_TAB_ITEM_ALTITUDE;
			} else if (firstType == SPEED) {
				return GPXTabItemType.GPX_TAB_ITEM_SPEED;
			}
			return null;
		}
	}

	private class CustomChartType extends ChartType<BarData> {

		private final RouteStatistics statistics;

		public CustomChartType(String title, RouteStatistics statistics) {
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
