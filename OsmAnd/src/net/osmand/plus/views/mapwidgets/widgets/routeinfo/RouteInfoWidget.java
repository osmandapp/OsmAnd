package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

import static net.osmand.plus.views.mapwidgets.WidgetType.ROUTE_INFO;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget.DISTANCE_CHANGE_THRESHOLD;
import static net.osmand.plus.views.mapwidgets.widgets.TimeToNavigationPointWidget.UPDATE_INTERVAL_SECONDS;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatterParams;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsContextMenu;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportMultiRow;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportVerticalPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoCalculator.DestinationInfo;
import net.osmand.plus.views.mapwidgets.widgetstates.RouteInfoWidgetState;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RouteInfoWidget extends MapWidget implements ISupportVerticalPanel, ISupportWidgetResizing, ISupportMultiRow {

	private final RouteInfoWidgetState widgetState;

	private boolean isFullRow;
	private TextState textState;
	private final RouteInfoCalculator calculator;
	private List<DestinationInfo> cachedRouteInfo;
	private RouteInfoDisplayMode cachedDisplayMode;
	private int cachedContentLayoutId;
	private Integer cachedMetricSystem;
	private boolean forceUpdate = false;

	// views
	private View buttonTappableArea;
	private View buttonBody;
	private TextView tvPrimaryValue1;
	private TextView tvSecondaryValue1;
	private TextView tvTertiaryValue1;
	private View secondaryBlock;
	private TextView tvPrimaryValue2;
	private TextView tvSecondaryValue2;
	private TextView tvTertiaryValue2;
	private View blocksDivider;

	public RouteInfoWidget(@NonNull MapActivity mapActivity, @Nullable String customId,
			@Nullable WidgetsPanel panel) {
		super(mapActivity, ROUTE_INFO, customId, panel);
		widgetState = new RouteInfoWidgetState(app, customId);
		calculator = new RouteInfoCalculator(mapActivity);

		setupViews();
		updateVisibility(false);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.simple_widget_vertical_content_container;
	}

	@LayoutRes
	protected int getContentLayoutId() {
		WidgetSize selectedSize = widgetState.getWidgetSizePref().get();
		return switch (selectedSize) {
			case SMALL -> isFullRow
					? isSecondaryDataAvailable()
					? R.layout.widget_route_information_small_duo
					: R.layout.widget_route_information_small
					: R.layout.widget_route_information_small_half;
			case MEDIUM -> isFullRow
					? R.layout.widget_route_information_medium
					: R.layout.widget_route_information_medium_half;
			case LARGE -> isFullRow
					? R.layout.widget_route_information_large
					: R.layout.widget_route_information_large_half;
		};
	}

	private void setupViews() {
		LinearLayout container = (LinearLayout) view;
		container.removeAllViews();
		LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		cachedContentLayoutId = getContentLayoutId();
		inflater.inflate(cachedContentLayoutId, container);
		collectViews();
		if (textState != null) {
			view.setBackgroundResource(textState.widgetBackgroundId);
			updateNavigationButtonBg();
		}
		updateWidgetView();

		buttonTappableArea.setOnClickListener(v -> mapActivity.getMapActions().doRoute());

		view.setOnLongClickListener(v -> {
			WidgetsContextMenu.showMenu(view, mapActivity, widgetType, customId, null, panel, nightMode);
			return true;
		});
	}

	private void collectViews() {
		buttonTappableArea = view.findViewById(R.id.button_tappable_area);
		buttonBody = view.findViewById(R.id.button_body);
		blocksDivider = view.findViewById(R.id.blocks_divider);

		// Initialization of primary block elements
		tvPrimaryValue1 = view.findViewById(R.id.primary_value_1);
		tvSecondaryValue1 = view.findViewById(R.id.secondary_value_1);
		tvTertiaryValue1 = view.findViewById(R.id.tertiary_value_1);

		// Initialization of secondary block elements
		secondaryBlock = view.findViewById(R.id.secondary_block);
		tvPrimaryValue2 = view.findViewById(R.id.primary_value_2);
		tvSecondaryValue2 = view.findViewById(R.id.secondary_value_2);
		tvTertiaryValue2 = view.findViewById(R.id.tertiary_value_2);
	}

	@Override
	public void updateValueAlign(boolean fullRow) {
	}

	@Override
	public void updateFullRowState(boolean fullRow) {
		if (isFullRow != fullRow) {
			isFullRow = fullRow;
			recreateView();
			if (textState != null) {
				updateColors(textState);
			}
		}
	}

	private void updateNavigationButtonBg() {
		int color = ColorUtilities.getSecondaryActiveColor(app, nightMode);
		Drawable normal = UiUtilities.createTintedDrawable(app, R.drawable.rectangle_rounded_small, color);

		int rippleDrawableId = nightMode ? R.drawable.ripple_solid_dark_3dp : R.drawable.ripple_solid_light_3dp;
		Drawable selected = AppCompatResources.getDrawable(app, rippleDrawableId);

		Drawable drawable = UiUtilities.getLayeredIcon(normal, selected);
		AndroidUtils.setBackground(buttonBody, drawable);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		updateInfoInternal();
	}

	private void updateInfoInternal() {
		if (cachedContentLayoutId != getContentLayoutId()) {
			// Recreating the widget is necessary because small widget size uses
			// different layouts depending on the number of route points.
			recreateView();
			return;
		}
		boolean shouldHide = shouldHide();
		boolean typeAllowed = widgetType != null && widgetType.isAllowed();
		if (typeAllowed && !shouldHide) {
			updateRouteInformation();
		} else {
			updateVisibility(false);
		}
	}

	protected boolean shouldHide() {
		return visibilityHelper.shouldHideVerticalWidgets()
				|| panel == BOTTOM && visibilityHelper.shouldHideBottomWidgets();
	}

	private void updateRouteInformation() {
		List<DestinationInfo> calculatedRouteInfo = calculator.calculateRouteInformation();
		if (Algorithms.isEmpty(calculatedRouteInfo)) {
			updateVisibility(false);
			return;
		}
		boolean visibilityChanged = updateVisibility(true);

		if (!forceUpdate && !visibilityChanged && !isUpdateNeeded(calculatedRouteInfo)) {
			return;
		}
		cachedRouteInfo = calculatedRouteInfo;

		RouteInfoDisplayMode primaryDisplayMode = getDisplayMode(settings.getApplicationMode());
		RouteInfoDisplayMode[] orderedDisplayModes = RouteInfoDisplayMode.values(primaryDisplayMode);

		updatePrimaryBlock(cachedRouteInfo.get(0), orderedDisplayModes);

		if (secondaryBlock != null) {
			boolean isSecondaryDataAvailable = isSecondaryDataAvailable();
			AndroidUiHelper.setVisibility(isSecondaryDataAvailable, blocksDivider, secondaryBlock);

			if (isSecondaryDataAvailable) {
				updateSecondaryBlock(cachedRouteInfo.get(1), orderedDisplayModes);
			}
		}
		forceUpdate = false;
	}

	private void updatePrimaryBlock(@NonNull DestinationInfo destinationInfo,
			@NonNull RouteInfoDisplayMode[] modes) {
		Map<RouteInfoDisplayMode, String> displayData = prepareDisplayData(destinationInfo);

		tvPrimaryValue1.setText(displayData.get(modes[0]));
		tvSecondaryValue1.setText(displayData.get(modes[1]));
		tvTertiaryValue1.setText(displayData.get(modes[2]));
	}

	private void updateSecondaryBlock(@NonNull DestinationInfo destinationInfo,
			@NonNull RouteInfoDisplayMode[] modes) {
		Map<RouteInfoDisplayMode, String> displayData = prepareDisplayData(destinationInfo);

		tvPrimaryValue2.setText(displayData.get(modes[0]));
		tvSecondaryValue2.setText(displayData.get(modes[1]));
		tvTertiaryValue2.setText(displayData.get(modes[2]));
	}

	@NonNull
	private Map<RouteInfoDisplayMode, String> prepareDisplayData(@NonNull DestinationInfo info) {
		Map<RouteInfoDisplayMode, String> displayData = new HashMap<>();
		displayData.put(RouteInfoDisplayMode.ARRIVAL_TIME, formatArrivalTime(app, info.arrivalTime()));
		displayData.put(RouteInfoDisplayMode.TIME_TO_GO, formatDuration(app, info.timeToGo()));
		displayData.put(RouteInfoDisplayMode.DISTANCE, formatDistance(app, info.distance()));
		return displayData;
	}

	private boolean isUpdateNeeded(@NonNull List<DestinationInfo> routeInfo) {
		int metricSystem = settings.METRIC_SYSTEM.get().ordinal();
		boolean metricSystemChanged = cachedMetricSystem == null || cachedMetricSystem != metricSystem;
		cachedMetricSystem = metricSystem;
		if (metricSystemChanged) {
			return true;
		}
		RouteInfoDisplayMode displayMode = widgetState.getDisplayMode();
		if (cachedDisplayMode != displayMode) {
			cachedDisplayMode = displayMode;
			return true;
		}
		if (Algorithms.isEmpty(cachedRouteInfo) || isDataChanged(cachedRouteInfo.get(0), routeInfo.get(0))) {
			return true;
		}
		return cachedRouteInfo.size() > 1 && isDataChanged(cachedRouteInfo.get(1), routeInfo.get(1));
	}

	private boolean isDataChanged(@NonNull DestinationInfo i1, @NonNull DestinationInfo i2) {
		int distanceDif = Math.abs(i1.distance() - i2.distance());
		long timeToGoDif = Math.abs(i1.timeToGo() - i2.timeToGo());
		return distanceDif > DISTANCE_CHANGE_THRESHOLD || timeToGoDif > UPDATE_INTERVAL_SECONDS * 1000L;
	}

	@Override
	public void updateColors(@NonNull TextState textState) {
		this.textState = textState;
		this.nightMode = textState.night;
		recreateView();
	}

	@Override
	public boolean updateVisibility(boolean visible) {
		if (super.updateVisibility(visible)) {
			updateWidgetView();
			return true;
		}
		return false;
	}

	public void updateWidgetView() {
		app.getOsmandMap().getMapLayers().getMapInfoLayer().updateRow(this);
	}

	@Override
	public boolean allowResize() {
		return true;
	}

	@NonNull
	@Override
	public OsmandPreference<WidgetSize> getWidgetSizePref() {
		return widgetState.getWidgetSizePref();
	}

	@Override
	public void recreateView() {
		forceUpdate = true;
		setupViews();
		updateInfoInternal();
	}

	private boolean isSecondaryDataAvailable() {
		return cachedRouteInfo != null && cachedRouteInfo.size() > 1;
	}

	@NonNull
	public RouteInfoDisplayMode getDisplayMode(@NonNull ApplicationMode appMode) {
		return widgetState.getDisplayMode(appMode);
	}

	public void setDisplayMode(@NonNull ApplicationMode appMode,
			@NonNull RouteInfoDisplayMode displayMode) {
		widgetState.setDisplayMode(appMode, displayMode);
	}

	@NonNull
	public static String formatArrivalTime(@NonNull Context ctx, long time) {
		Pair<String, String> formattedTime = OsmAndFormatter.getFormattedTime(ctx, time);
		if (formattedTime.second != null) {
			String pattern = ctx.getString(R.string.ltr_or_rtl_combine_via_space);
			return String.format(pattern, formattedTime.first, formattedTime.second);
		}
		return formattedTime.first;
	}

	@NonNull
	public static String formatDuration(@NonNull OsmandApplication app, long timeLeft) {
		long diffInMinutes = TimeUnit.SECONDS.convert(timeLeft, TimeUnit.MILLISECONDS);
		return OsmAndFormatter.getFormattedDuration(diffInMinutes, app);
	}

	@NonNull
	public static String formatDistance(@NonNull OsmandApplication ctx, float meters) {
		return OsmAndFormatter.getFormattedDistance(meters, ctx, OsmAndFormatterParams.USE_LOWER_BOUNDS);
	}
}
