package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class WidgetsInitializer {

	private final MapActivity mapActivity;
	private final OsmandSettings settings;
	private final ApplicationMode appMode;
	private final ScreenLayoutMode layoutMode;
	private final OsmandApplication app;

	private final MapWidgetsFactory factory;
	private final WidgetInfoCreator creator;

	private final List<MapWidgetInfo> mapWidgetsCache = new ArrayList<>();

	private WidgetsInitializer(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode,
			@Nullable ScreenLayoutMode layoutMode) {
		this.mapActivity = mapActivity;
		this.appMode = appMode;
		this.layoutMode = layoutMode;
		app = mapActivity.getApp();
		settings = app.getSettings();
		factory = new MapWidgetsFactory(mapActivity);
		creator = new WidgetInfoCreator(app, appMode, layoutMode);
	}

	private List<MapWidgetInfo> createAllControls() {
		createCommonWidgets();
		PluginsHelper.createMapWidgets(mapActivity, mapWidgetsCache, appMode, layoutMode);
		app.getAidlApi().createWidgetControls(mapActivity, mapWidgetsCache, appMode, layoutMode);
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
		addWidgetInfo(ROUTE_INFO);
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
		List<String> widgetKeys = settings.getCustomWidgetsKeys(layoutMode).getStringsListForProfile(appMode);
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
	                                                    @NonNull ApplicationMode appMode,
	                                                    @Nullable ScreenLayoutMode layoutMode) {
		WidgetsInitializer initializer = new WidgetsInitializer(mapActivity, appMode, layoutMode);
		return initializer.createAllControls();
	}
}
