package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE_MAP_CENTER;
import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE_MY_LOCATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.AVERAGE_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.BATTERY;
import static net.osmand.plus.views.mapwidgets.WidgetType.COORDINATES_CURRENT_LOCATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.COORDINATES_MAP_CENTER;
import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetType.DISTANCE_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.ELEVATION_PROFILE;
import static net.osmand.plus.views.mapwidgets.WidgetType.GLIDE_AVERAGE;
import static net.osmand.plus.views.mapwidgets.WidgetType.GLIDE_TARGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.GPS_INFO;
import static net.osmand.plus.views.mapwidgets.WidgetType.INTERMEDIATE_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.LANES;
import static net.osmand.plus.views.mapwidgets.WidgetType.MAGNETIC_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetType.MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.RADIUS_RULER;
import static net.osmand.plus.views.mapwidgets.WidgetType.RELATIVE_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetType.SECOND_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_1;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_2;
import static net.osmand.plus.views.mapwidgets.WidgetType.SMALL_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.STREET_NAME;
import static net.osmand.plus.views.mapwidgets.WidgetType.SUNRISE;
import static net.osmand.plus.views.mapwidgets.WidgetType.SUNSET;
import static net.osmand.plus.views.mapwidgets.WidgetType.SUN_POSITION;
import static net.osmand.plus.views.mapwidgets.WidgetType.TIME_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.TIME_TO_INTERMEDIATE;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRUE_BEARING;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class WidgetsInitializer {

	private final MapActivity mapActivity;
	private final OsmandSettings settings;
	private final ApplicationMode appMode;
	private final OsmandApplication app;

	private final MapWidgetsFactory factory;
	private final WidgetInfoCreator creator;

	private final List<MapWidgetInfo> mapWidgetsCache = new ArrayList<>();

	private WidgetsInitializer(MapActivity mapActivity, ApplicationMode appMode) {
		this.mapActivity = mapActivity;
		this.appMode = appMode;
		app = mapActivity.getMyApplication();
		settings = app.getSettings();
		factory = new MapWidgetsFactory(mapActivity);
		creator = new WidgetInfoCreator(app, appMode);
	}

	private List<MapWidgetInfo> createAllControls() {
		createCommonWidgets();
		PluginsHelper.createMapWidgets(mapActivity, mapWidgetsCache, appMode);
		app.getAidlApi().createWidgetControls(mapActivity, mapWidgetsCache, appMode);
		createCustomWidgets();
		return mapWidgetsCache;
	}

	public void createCommonWidgets() {
		createTopWidgets();
		createBottomWidgets();
		createLeftWidgets();
		createRightWidgets();
	}

	private void createTopWidgets() {
		addWidgetInfo(COORDINATES_CURRENT_LOCATION);
		addWidgetInfo(COORDINATES_MAP_CENTER);
		addWidgetInfo(STREET_NAME);
		addWidgetInfo(LANES);
		addWidgetInfo(MARKERS_TOP_BAR);
	}

	private void createBottomWidgets() {
		addWidgetInfo(ELEVATION_PROFILE);
	}

	private void createLeftWidgets() {
		addWidgetInfo(NEXT_TURN);
		addWidgetInfo(SMALL_NEXT_TURN);
		addWidgetInfo(SECOND_NEXT_TURN);
	}

	private void createRightWidgets() {
		addWidgetInfo(INTERMEDIATE_DESTINATION);
		addWidgetInfo(DISTANCE_TO_DESTINATION);
		addWidgetInfo(RELATIVE_BEARING);
		addWidgetInfo(MAGNETIC_BEARING);
		addWidgetInfo(TRUE_BEARING);
		addWidgetInfo(CURRENT_SPEED);
		addWidgetInfo(AVERAGE_SPEED);
		addWidgetInfo(MAX_SPEED);
		addWidgetInfo(ALTITUDE_MAP_CENTER);
		addWidgetInfo(ALTITUDE_MY_LOCATION);
		addWidgetInfo(GPS_INFO);
		addWidgetInfo(CURRENT_TIME);
		addWidgetInfo(BATTERY);
		addWidgetInfo(RADIUS_RULER);
		addWidgetInfo(TIME_TO_INTERMEDIATE);
		addWidgetInfo(TIME_TO_DESTINATION);
		addWidgetInfo(SIDE_MARKER_1);
		addWidgetInfo(SIDE_MARKER_2);
		addWidgetInfo(SUNRISE);
		addWidgetInfo(SUNSET);
		addWidgetInfo(SUN_POSITION);
		addWidgetInfo(GLIDE_TARGET);
		addWidgetInfo(GLIDE_AVERAGE);
	}

	private void addWidgetInfo(@NonNull WidgetType widgetType) {
		MapWidgetInfo widgetInfo = creator.createWidgetInfo(factory, widgetType);
		if (widgetInfo != null) {
			mapWidgetsCache.add(widgetInfo);
		}
	}

	public void createCustomWidgets() {
		List<String> widgetKeys = settings.CUSTOM_WIDGETS_KEYS.getStringsListForProfile(appMode);
		if (!Algorithms.isEmpty(widgetKeys)) {
			for (String key : widgetKeys) {
				WidgetType widgetType = WidgetType.getById(key);
				if (widgetType != null) {
					MapWidgetInfo widgetInfo = creator.createWidgetInfo(factory, key, widgetType);
					if (widgetInfo != null) {
						mapWidgetsCache.add(widgetInfo);
					}
				}
			}
		}
	}

	public static List<MapWidgetInfo> createAllControls(@NonNull MapActivity mapActivity,
	                                                    @NonNull ApplicationMode appMode) {
		WidgetsInitializer initializer = new WidgetsInitializer(mapActivity, appMode);
		return initializer.createAllControls();
	}
}
