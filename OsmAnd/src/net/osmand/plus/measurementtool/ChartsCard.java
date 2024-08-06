package net.osmand.plus.measurementtool;

import static net.osmand.plus.charts.GPXDataSetType.ALTITUDE;
import static net.osmand.plus.charts.GPXDataSetType.SLOPE;
import static net.osmand.plus.charts.GPXDataSetType.SPEED;
import static net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer.MEASUREMENT_TOOL;
import static net.osmand.router.RouteStatisticsHelper.RouteStatistics;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.ElevationChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.MeasurementToolFragment.OnUpdateInfoListener;
import net.osmand.plus.measurementtool.graph.ChartAdapterHelper;
import net.osmand.plus.measurementtool.graph.ChartAdapterHelper.RefreshMapCallback;
import net.osmand.plus.measurementtool.graph.CommonChartAdapter;
import net.osmand.plus.measurementtool.graph.CustomChartAdapter;
import net.osmand.plus.measurementtool.graph.CustomChartAdapter.LegendViewType;
import net.osmand.plus.myplaces.tracks.GPXTabItemType;
import net.osmand.plus.routepreparationmenu.RouteDetailsFragment;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChartsCard extends MapBaseCard implements OnUpdateInfoListener {

	private static final String GRAPH_DATA_GPX_FILE_NAME = "graph_data_tmp";
	private static final int INVALID_ID = -1;

	private final MeasurementToolFragment fragment;
	private final TrackDetailsMenu trackDetailsMenu;

	private MeasurementEditingContext editingCtx;
	private RefreshMapCallback refreshMapCallback;
	private GpxTrackAnalysis analysis;
	private GpxDisplayItem gpxItem;

	private OnScrollChangedListener scrollChangedListener;
	private View commonGraphContainer;
	private View customGraphContainer;
	private View messageContainer;
	private View buttonContainer;
	private CommonChartAdapter commonGraphAdapter;
	private CustomChartAdapter customGraphAdapter;
	private HorizontalChipsView graphTypesMenu;

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

		graphTypesMenu = view.findViewById(R.id.graph_types_selector);

		setupScrollListener();

		ElevationChart lineChart = view.findViewById(R.id.line_chart);
		HorizontalBarChart barChart = view.findViewById(R.id.horizontal_chart);
		commonGraphAdapter = new CommonChartAdapter(app, lineChart, true);
		customGraphAdapter = new CustomChartAdapter(app, barChart, true);

		commonGraphContainer = view.findViewById(R.id.common_graphs_container);
		commonGraphAdapter.setBottomInfoContainer(view.findViewById(R.id.statistics_container));
		customGraphContainer = view.findViewById(R.id.custom_graphs_container);
		customGraphAdapter.setBottomInfoContainer(view.findViewById(R.id.route_legend));
		customGraphAdapter.setLayoutChangeListener(this::setLayoutNeeded);

		messageContainer = view.findViewById(R.id.message_container);
		buttonContainer = view.findViewById(R.id.btn_container);

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
				0 : getDimen(R.dimen.content_padding_small);
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
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);

		List<ChipItem> items = new ArrayList<>();
		for (ChartType<?> type : chartTypes) {
			if (!type.isAvailable()) {
				continue;
			}
			String title = type.getTitle();
			ChipItem item = new ChipItem(title);
			item.title = title;
			item.contentDescription = title;
			item.tag = type;
			if (type.isCustom()) {
				item.titleColor = activeColor;
			}
			items.add(item);
		}
		graphTypesMenu.setItems(items);

		ChipItem selected = graphTypesMenu.getChipById(visibleType.getTitle());
		graphTypesMenu.setSelected(selected);

		graphTypesMenu.setOnSelectChipListener(chip -> {
			ChartType<?> chosenType = (ChartType<?>) chip.tag;
			if (!isVisibleType(chosenType)) {
				changeVisibleType(chosenType);
			}
			graphTypesMenu.smoothScrollTo(chip);
			return true;
		});

		graphTypesMenu.notifyDataSetChanged();
	}

	private void changeVisibleType(ChartType<?> type) {
		visibleType = type;
		updateView();
	}

	private boolean isVisibleType(ChartType<?> type) {
		return visibleType != null && type != null
				&& Algorithms.objectEquals(visibleType.getTitle(), type.getTitle());
	}

	@Nullable
	private ChartType<?> getFirstAvailableType() {
		ChartType<?> onlineType = getFirstOnlineType();
		ChartType<?> offlineType = getFirstOfflineType();
		if (onlineType == null && offlineType == null) {
			for (ChartType<?> type : chartTypes) {
				if (type.isAvailable()) {
					return type;
				}
			}
		}
		return offlineType != null ? offlineType : onlineType;
	}

	private ChartType<?> getFirstOnlineType() {
		if (fragment.isCalculateSrtmMode()) {
			for (ChartType<?> type : chartTypes) {
				if (type.isAvailable() && Algorithms.stringsEqual(type.title, app.getString(R.string.altitude))) {
					return type;
				}
			}
		}
		return null;
	}

	private ChartType<?> getFirstOfflineType() {
		if (fragment.isCalculateHeightmapMode()) {
			for (ChartType<?> type : chartTypes) {
				if (type.isAvailable() && Algorithms.stringsEqual(type.title, app.getString(R.string.altitude))) {
					return type;
				}
			}
		}
		return null;
	}

	private void updateView() {
		hideAll();
		updateMessage();
	}

	private void hideAll() {
		AndroidUiHelper.setVisibility(View.GONE,
				commonGraphContainer,
				customGraphContainer,
				messageContainer,
				buttonContainer);
	}

	private void updateMessage() {
		if (!editingCtx.isPointsEnoughToCalculateRoute()) {
			String desc = app.getString(R.string.message_you_need_add_two_points_to_show_graphs);
			showMessage(null, desc, INVALID_ID, 0);
		} else if (isRouteCalculating()) {
			int progressSize = app.getResources().getDimensionPixelSize(R.dimen.standard_icon_size);
			String desc = app.getString(R.string.message_graph_will_be_available_after_recalculation);
			showMessage(null, desc, INVALID_ID, progressSize);
		} else if (visibleType.canBeCalculated() && fragment.isCalculatingSrtmData()) {
			String desc = app.getString(R.string.calculating_altitude);
			int progressSize = app.getResources().getDimensionPixelSize(R.dimen.icon_size_double);
			String buttonText = app.getString(R.string.shared_string_cancel);
			showMessage(null, desc, INVALID_ID, progressSize);
			showButton(buttonText, v -> fragment.stopUploadFileTask(), true);
		} else if (visibleType.canBeCalculated() && fragment.isCalculatingHeightmapData()) {
			String desc = app.getString(R.string.calculating_altitude);
			int progressSize = app.getResources().getDimensionPixelSize(R.dimen.icon_size_double);
			String buttonText = app.getString(R.string.shared_string_cancel);
			showMessage(null, desc, INVALID_ID, progressSize);
			showButton(buttonText, v -> fragment.stopCalculatingHeightMapTask(true), true);
		} else if (visibleType.canBeCalculated() && !visibleType.hasData()) {
			String title = app.getString(R.string.no_altitude_data);
			String desc = app.getString(R.string.retrieve_elevation_data_summary);
			showMessage(title, desc, R.drawable.ic_action_desert, 0);
			showCalculateAltitudeButton(true);
		} else if (visibleType.hasData()) {
			showGraph();
			if (visibleType.canBeCalculated) {
				showCalculateAltitudeButton(false);
			}
		}
	}

	private void showCalculateAltitudeButton(boolean addStartPadding) {
		showButton(app.getString(R.string.get_altitude_data), v -> fragment.getAltitudeClick(), addStartPadding);
	}

	private void showMessage(@Nullable String title,
	                         @NonNull String description,
	                         @DrawableRes int iconId,
	                         int progressSize) {
		TextView messageTitle = messageContainer.findViewById(R.id.message_title);
		if (!Algorithms.isEmpty(title)) {
			messageTitle.setText(title);
		}
		AndroidUiHelper.updateVisibility(messageTitle, title != null);

		TextView messageDesc = messageContainer.findViewById(R.id.message_text);
		messageDesc.setText(description);

		ImageView icon = messageContainer.findViewById(R.id.message_icon);
		if (iconId != INVALID_ID) {
			icon.setImageResource(iconId);
		}
		AndroidUiHelper.updateVisibility(icon, iconId != INVALID_ID);

		ProgressBar progressBar = messageContainer.findViewById(R.id.progress_bar);
		LayoutParams params = progressBar.getLayoutParams();
		params.height = progressSize;
		params.width = progressSize;
		progressBar.setLayoutParams(params);
		AndroidUiHelper.updateVisibility(progressBar, progressSize != 0);

		AndroidUiHelper.updateVisibility(messageContainer, true);
	}

	private void showButton(@NonNull String buttonTitle, @NonNull OnClickListener listener, boolean addStartPadding) {
		View buttonPadding = buttonContainer.findViewById(R.id.button_padding);
		AndroidUiHelper.updateVisibility(buttonPadding, addStartPadding);

		View buttonDivider = buttonContainer.findViewById(R.id.button_divider);
		MarginLayoutParams layoutParams = (MarginLayoutParams) buttonDivider.getLayoutParams();
		layoutParams.setMarginStart(addStartPadding ? getDimen(R.dimen.list_content_padding_large) : 0);
		buttonDivider.setLayoutParams(layoutParams);

		TextView title = buttonContainer.findViewById(R.id.btn_text);
		title.setText(buttonTitle);

		buttonContainer.setOnClickListener(listener);
		AndroidUiHelper.updateVisibility(buttonContainer, true);
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

		GpxFile gpxFile = fragment.generateGpxFile();
		analysis = gpxFile.getAnalysis(0);
		gpxItem = GpxUiHelper.makeGpxDisplayItem(app, gpxFile, MEASUREMENT_TOOL, analysis);

		if (gpxItem != null) {
			trackDetailsMenu.setGpxItem(gpxItem);
		}
		if (analysis == null) {
			return;
		}

		// update common graph data
		boolean hasElevationData = analysis.hasElevationData();
		boolean hasSpeedData = analysis.isSpeedSpecified();
		addCommonType(R.string.shared_string_overview, true, hasElevationData, ALTITUDE, SLOPE);
		addCommonType(R.string.altitude, true, hasElevationData, ALTITUDE, null);
		addCommonType(R.string.shared_string_slope, true, hasElevationData, SLOPE, null);
		addCommonType(R.string.shared_string_speed, false, hasSpeedData, SPEED, null);

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
			refreshMapCallback.refreshMap(false, false, true);
		}
	}

	private void addCommonType(int titleId,
	                           boolean canBeCalculated,
	                           boolean hasData,
	                           GPXDataSetType firstType,
	                           GPXDataSetType secondType) {
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
		private final GPXDataSetType firstType;
		private final GPXDataSetType secondType;

		public CommonChartType(String title, boolean canBeCalculated, boolean hasData, @NonNull GPXDataSetType firstType, @Nullable GPXDataSetType secondType) {
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
			ChartUtils.setupElevationChart(commonGraphAdapter.getChart(), 24f, 16f, true);
			List<ILineDataSet> dataSets = ChartUtils.getDataSets(commonGraphAdapter.getChart(),
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
			ChartUtils.setupHorizontalGPXChart(app, customGraphAdapter.getChart(), 5, 9, 24, true, nightMode);
			if (!Algorithms.isEmpty(statistics.elements)) {
				return ChartUtils.buildStatisticChart(app, customGraphAdapter.getChart(),
						statistics, analysis, true, nightMode);
			}
			return new BarData();
		}
	}
}
