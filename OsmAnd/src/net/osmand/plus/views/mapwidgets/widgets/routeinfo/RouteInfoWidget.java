package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

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
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsContextMenu;
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

	@Nullable
	protected String customId;
	private boolean isFullRow;
	private TextState textState;
	private final RouteInfoCalculator calculator;
	private List<DestinationInfo> calculatedRouteInfo;
	private int cachedContentLayoutId;

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

	public RouteInfoWidget(@NonNull MapActivity mapActivity, @Nullable String customId) {
		super(mapActivity, WidgetType.ROUTE_INFO);
		this.customId = customId;
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
			WidgetsContextMenu.showMenu(view, mapActivity, widgetType, customId, null, true, nightMode);
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
		boolean shouldHideTopWidgets = mapActivity.getWidgetsVisibilityHelper().shouldHideVerticalWidgets();
		boolean typeAllowed = widgetType != null && widgetType.isAllowed();
		boolean visible = typeAllowed && !shouldHideTopWidgets;
		updateVisibility(visible);
		if (visible) {
			updateNavigationInfo();
		}
	}

	private void updateNavigationInfo() {
		calculatedRouteInfo = calculator.calculateRouteInformation();
		if (Algorithms.isEmpty(calculatedRouteInfo)) {
			updateVisibility(false);
			return;
		}
		updateVisibility(true);

		RouteInfoDisplayMode primaryDisplayMode = getDisplayMode(settings.getApplicationMode());
		RouteInfoDisplayMode[] orderedDisplayModes = RouteInfoDisplayMode.values(primaryDisplayMode);

		updatePrimaryBlock(calculatedRouteInfo.get(0), orderedDisplayModes);

		if (secondaryBlock != null) {
			boolean isSecondaryDataAvailable = isSecondaryDataAvailable();
			AndroidUiHelper.setVisibility(isSecondaryDataAvailable, blocksDivider, secondaryBlock);

			if (isSecondaryDataAvailable) {
				updateSecondaryBlock(calculatedRouteInfo.get(1), orderedDisplayModes);
			}
		}
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
		setupViews();
		updateInfoInternal();
	}

	private boolean isSecondaryDataAvailable() {
		return calculatedRouteInfo != null && calculatedRouteInfo.size() > 1;
	}

	@NonNull
	public RouteInfoDisplayMode getDisplayMode(@NonNull ApplicationMode appMode) {
		return widgetState.getDisplayMode(appMode);
	}

	public void setDisplayMode(@NonNull ApplicationMode appMode, @NonNull RouteInfoDisplayMode displayMode) {
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
	public static String formatDuration(@NonNull Context ctx, long timeLeft) {
		long diffInMinutes = TimeUnit.MINUTES.convert(timeLeft, TimeUnit.MILLISECONDS);
		String hour = ctx.getString(R.string.int_hour);
		String formattedDuration = Algorithms.formatMinutesDuration((int) diffInMinutes, true);
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, formattedDuration, hour);
	}

	@NonNull
	public static String formatDistance(@NonNull OsmandApplication ctx, float meters) {
		return OsmAndFormatter.getFormattedDistance(meters, ctx, OsmAndFormatterParams.USE_LOWER_BOUNDS);
	}
}
