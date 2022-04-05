package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.WIDGET_AUDIO_VIDEO_NOTES;
import static net.osmand.plus.plugins.development.OsmandDevelopmentPlugin.WIDGET_FPS;
import static net.osmand.plus.plugins.mapillary.MapillaryPlugin.WIDGET_MAPILLARY;
import static net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin.WIDGET_TRIP_RECORDING;
import static net.osmand.plus.plugins.parking.ParkingPositionPlugin.WIDGET_PARKING;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_ALTITUDE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_BATTERY;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_BEARING;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_GPS_INFO;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_TIME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_1;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_2;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_TURN_SMALL;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_PLAIN_TIME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_RADIUS_RULER;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_TIME;
import static net.osmand.plus.views.mapwidgets.MarkersWidgetsHelper.WIDGET_MAP_MARKERS;
import static net.osmand.plus.views.mapwidgets.widgets.CoordinatesWidget.WIDGET_COORDINATES;
import static net.osmand.plus.views.mapwidgets.widgets.ElevationProfileWidget.WIDGET_ELEVATION_PROFILE;
import static net.osmand.plus.views.mapwidgets.widgets.LanesWidget.WIDGET_LANES;
import static net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget.WIDGET_STREET_NAME;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public enum WidgetsPanel {

	LEFT(R.drawable.ic_action_screen_side_left, R.string.map_widget_left, R.id.left_side),
	RIGHT(R.drawable.ic_action_screen_side_right, R.string.map_widget_right, R.id.right_side),
	TOP(R.drawable.ic_action_screen_side_top, R.string.top_widgets_panel, R.id.top_side),
	BOTTOM(R.drawable.ic_action_screen_side_bottom, R.string.bottom_widgets_panel, R.id.bottom_side);

	private static final List<String> originalLeftOrder = new ArrayList<>();
	private static final List<String> originalRightOrder = new ArrayList<>();
	private static final List<String> originalTopOrder = new ArrayList<>();
	private static final List<String> originalBottomOrder = new ArrayList<>();

	static {
		originalLeftOrder.add(WIDGET_NEXT_TURN);
		originalLeftOrder.add(WIDGET_NEXT_TURN_SMALL);
		originalLeftOrder.add(WIDGET_NEXT_NEXT_TURN);

		originalRightOrder.add(WIDGET_INTERMEDIATE_DISTANCE);
		originalRightOrder.add(WIDGET_INTERMEDIATE_TIME);
		originalRightOrder.add(WIDGET_DISTANCE);
		originalRightOrder.add(WIDGET_TIME);
		originalRightOrder.add(WIDGET_MARKER_1);
		originalRightOrder.add(WIDGET_BEARING);
		originalRightOrder.add(WIDGET_MARKER_2);
		originalRightOrder.add(WIDGET_SPEED);
		originalRightOrder.add(WIDGET_MAX_SPEED);
		originalRightOrder.add(WIDGET_ALTITUDE);
		originalRightOrder.add(WIDGET_GPS_INFO);
		originalRightOrder.add(WIDGET_TRIP_RECORDING);
		originalRightOrder.add(WIDGET_AUDIO_VIDEO_NOTES);
		originalRightOrder.add(WIDGET_MAPILLARY);
		originalRightOrder.add(WIDGET_PARKING);
		originalRightOrder.add(WIDGET_PLAIN_TIME);
		originalRightOrder.add(WIDGET_BATTERY);
		originalRightOrder.add(WIDGET_RADIUS_RULER);
		originalRightOrder.add(WIDGET_FPS);

		originalTopOrder.add(WIDGET_COORDINATES);
		originalTopOrder.add(WIDGET_STREET_NAME);
		originalTopOrder.add(WIDGET_MAP_MARKERS);
		originalTopOrder.add(WIDGET_LANES);

		originalBottomOrder.add(WIDGET_ELEVATION_PROFILE);
	}

	private final int iconId;
	private final int titleId;
	private final int tabId;

	WidgetsPanel(int iconId, int titleId, int tabId) {
		this.iconId = iconId;
		this.titleId = titleId;
		this.tabId = tabId;
	}

	public int getIconId() {
		return iconId;
	}

	public int getTitleId() {
		return titleId;
	}

	public int getTabId() {
		return tabId;
	}

	@NonNull
	public List<String> getOriginalOrder() {
		if (this == LEFT) {
			return new ArrayList<>(originalLeftOrder);
		} else if (this == RIGHT) {
			return new ArrayList<>(originalRightOrder);
		} else if (this == TOP) {
			return new ArrayList<>(originalTopOrder);
		} else {
			return new ArrayList<>(originalBottomOrder);
		}
	}

	public int getWidgetOrder(@NonNull String widgetId, @NonNull OsmandSettings settings) {
		return getWidgetOrder(settings.getApplicationMode(), widgetId, settings);
	}

	public int getWidgetOrder(@NonNull ApplicationMode appMode, @NonNull String widgetId, @NonNull OsmandSettings settings) {
		ListStringPreference orderPreference;
		if (this == LEFT) {
			orderPreference = settings.LEFT_WIDGET_PANEL_ORDER;
		} else if (this == RIGHT) {
			orderPreference = settings.RIGHT_WIDGET_PANEL_ORDER;
		} else if (this == TOP) {
			orderPreference = settings.TOP_WIDGET_PANEL_ORDER;
		} else {
			orderPreference = settings.BOTTOM_WIDGET_PANEL_ORDER;
		}
		List<String> orderIds = orderPreference.getStringsListForProfile(appMode);
		if (Algorithms.isEmpty(orderIds)) {
			return 0;
		}

		int order = orderIds.indexOf(widgetId);
		return order == -1 ? orderIds.size() + 1 : order;
	}

	public boolean setWidgetsOrder(@NonNull ApplicationMode appMode, @NonNull List<String> widgetIds, @NonNull OsmandSettings settings) {
		ListStringPreference orderPreference;
		if (this == LEFT) {
			orderPreference = settings.LEFT_WIDGET_PANEL_ORDER;
		} else if (this == RIGHT) {
			orderPreference = settings.RIGHT_WIDGET_PANEL_ORDER;
		} else if (this == TOP) {
			orderPreference = settings.TOP_WIDGET_PANEL_ORDER;
		} else {
			orderPreference = settings.BOTTOM_WIDGET_PANEL_ORDER;
		}
		String widgetsOrder = TextUtils.join(",", widgetIds);
		return orderPreference.setModeValue(appMode, widgetsOrder);
	}
}