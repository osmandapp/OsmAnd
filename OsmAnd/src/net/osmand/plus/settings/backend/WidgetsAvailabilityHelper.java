package net.osmand.plus.settings.backend;

import static net.osmand.plus.settings.backend.ApplicationMode.AIRCRAFT;
import static net.osmand.plus.settings.backend.ApplicationMode.BICYCLE;
import static net.osmand.plus.settings.backend.ApplicationMode.BOAT;
import static net.osmand.plus.settings.backend.ApplicationMode.CAR;
import static net.osmand.plus.settings.backend.ApplicationMode.HORSE;
import static net.osmand.plus.settings.backend.ApplicationMode.MOPED;
import static net.osmand.plus.settings.backend.ApplicationMode.MOTORCYCLE;
import static net.osmand.plus.settings.backend.ApplicationMode.PEDESTRIAN;
import static net.osmand.plus.settings.backend.ApplicationMode.PUBLIC_TRANSPORT;
import static net.osmand.plus.settings.backend.ApplicationMode.SKI;
import static net.osmand.plus.settings.backend.ApplicationMode.TRAIN;
import static net.osmand.plus.settings.backend.ApplicationMode.TRUCK;
import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE_MY_LOCATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE_MAP_CENTER;
import static net.osmand.plus.views.mapwidgets.WidgetType.AVERAGE_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.BATTERY;
import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetType.DISTANCE_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.GLIDE_AVERAGE;
import static net.osmand.plus.views.mapwidgets.WidgetType.GLIDE_TARGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.GPS_INFO;
import static net.osmand.plus.views.mapwidgets.WidgetType.INTERMEDIATE_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.LANES;
import static net.osmand.plus.views.mapwidgets.WidgetType.MAGNETIC_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetType.MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.OBD_FUEL_TYPE;
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
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.mapwidgets.WidgetType;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class WidgetsAvailabilityHelper {

	private static final Map<String, Set<ApplicationMode>> widgetsVisibilityMap = new LinkedHashMap<>();
	private static final Map<String, Set<ApplicationMode>> widgetsAvailabilityMap = new LinkedHashMap<>();

	public static boolean isWidgetAvailable(@NonNull OsmandApplication app, @NonNull String widgetId, @NonNull ApplicationMode appMode) {
		if (app.getAppCustomization().areWidgetsCustomized()) {
			return app.getAppCustomization().isWidgetAvailable(widgetId, appMode);
		}
		String defaultWidgetId = WidgetType.getDefaultWidgetId(widgetId);
		Set<ApplicationMode> availableForModes = widgetsAvailabilityMap.get(defaultWidgetId);
		return availableForModes == null || availableForModes.contains(appMode);
	}

	public static boolean isWidgetVisibleByDefault(@NonNull OsmandApplication app, @NonNull String widgetId, @NonNull ApplicationMode appMode) {
		if (app.getAppCustomization().areWidgetsCustomized()) {
			return app.getAppCustomization().isWidgetVisible(widgetId, appMode);
		}
		Set<ApplicationMode> widgetsVisibility = widgetsVisibilityMap.get(widgetId);
		return widgetsVisibility != null && widgetsVisibility.contains(appMode);
	}

	public static void initRegVisibility() {
		ApplicationMode[] exceptDefault = {CAR, BICYCLE, PEDESTRIAN, PUBLIC_TRANSPORT, BOAT,
				AIRCRAFT, SKI, TRUCK, MOTORCYCLE, HORSE, MOPED, TRAIN};
		ApplicationMode[] all = null;
		ApplicationMode[] none = {};

		// left
		ApplicationMode[] nextTurnSet = {CAR, BICYCLE, BOAT, SKI, TRUCK, MOTORCYCLE, HORSE, MOPED};
		ApplicationMode[] smallNextTurnSet = {PEDESTRIAN, PUBLIC_TRANSPORT, AIRCRAFT, TRAIN};
		ApplicationMode[] secondNextTurnSet = {CAR, BICYCLE, PEDESTRIAN, BOAT, SKI, TRUCK, MOTORCYCLE, HORSE, MOPED};

		regWidgetVisibility(NEXT_TURN, nextTurnSet);
		regWidgetVisibility(SMALL_NEXT_TURN, smallNextTurnSet);
		regWidgetVisibility(SECOND_NEXT_TURN, secondNextTurnSet);
		
		regWidgetAvailability(NEXT_TURN, exceptDefault);
		regWidgetAvailability(SMALL_NEXT_TURN, exceptDefault);
		regWidgetAvailability(SECOND_NEXT_TURN, exceptDefault);

		// right
		regWidgetVisibility(INTERMEDIATE_DESTINATION, all);
		regWidgetVisibility(DISTANCE_TO_DESTINATION, all);
		regWidgetVisibility(TIME_TO_INTERMEDIATE, all);
		regWidgetVisibility(TIME_TO_DESTINATION, all);
		regWidgetVisibility(OBD_FUEL_TYPE, all);
		regWidgetVisibility(CURRENT_SPEED, BICYCLE, BOAT, SKI, PUBLIC_TRANSPORT, AIRCRAFT,
				HORSE, TRAIN);
		regWidgetVisibility(MAX_SPEED, none);
		regWidgetVisibility(ALTITUDE_MAP_CENTER, PEDESTRIAN, BICYCLE);
		regWidgetVisibility(ALTITUDE_MY_LOCATION, PEDESTRIAN, BICYCLE);
		regWidgetAvailability(INTERMEDIATE_DESTINATION, all);
		regWidgetAvailability(DISTANCE_TO_DESTINATION, all);
		regWidgetAvailability(TIME_TO_INTERMEDIATE, all);
		regWidgetAvailability(TIME_TO_DESTINATION, all);
		regWidgetAvailability(CURRENT_SPEED, all);
		regWidgetAvailability(MAX_SPEED, all);
		regWidgetAvailability(AVERAGE_SPEED, all);
		regWidgetAvailability(ALTITUDE_MY_LOCATION, all);
		regWidgetAvailability(ALTITUDE_MAP_CENTER, all);
		regWidgetAvailability(SUNRISE, all);
		regWidgetAvailability(SUNSET, all);
		regWidgetAvailability(SUN_POSITION, all);
		regWidgetAvailability(GLIDE_TARGET, all);
		regWidgetAvailability(GLIDE_AVERAGE, all);
		regWidgetAvailability(OBD_FUEL_TYPE, all);

		// vertical
		regWidgetVisibility(STREET_NAME, CAR);
		regWidgetVisibility(LANES, CAR, BICYCLE);
		regWidgetVisibility(MARKERS_TOP_BAR, all);

		// all = null everything
		regWidgetAvailability(SIDE_MARKER_1, all);
		regWidgetAvailability(SIDE_MARKER_2, all);
		regWidgetAvailability(GPS_INFO, all);
		regWidgetAvailability(BATTERY, all);
		regWidgetAvailability(RELATIVE_BEARING, all);
		regWidgetAvailability(MAGNETIC_BEARING, all);
		regWidgetAvailability(TRUE_BEARING, all);
		regWidgetAvailability(RADIUS_RULER, all);
		regWidgetAvailability(CURRENT_TIME, all);
	}

	@NonNull
	public static Set<ApplicationMode> regWidgetVisibility(@NonNull WidgetType widgetType, @Nullable ApplicationMode... appModes) {
		return regWidgetVisibility(widgetType.id, appModes);
	}

	@NonNull
	public static Set<ApplicationMode> regWidgetVisibility(@NonNull String widgetId, @Nullable ApplicationMode... appModes) {
		return registerWidget(widgetId, widgetsVisibilityMap, appModes);
	}

	@NonNull
	public static Set<ApplicationMode> regWidgetAvailability(@NonNull WidgetType widgetType, @Nullable ApplicationMode... appModes) {
		return regWidgetAvailability(widgetType.id, appModes);
	}

	@NonNull
	public static Set<ApplicationMode> regWidgetAvailability(@NonNull String widgetId, @Nullable ApplicationMode... appModes) {
		return registerWidget(widgetId, widgetsAvailabilityMap, appModes);
	}

	// returns modifiable ! Set<ApplicationMode> to exclude non-wanted derived
	@NonNull
	private static Set<ApplicationMode> registerWidget(@NonNull String widgetId,
	                                                   @NonNull Map<String, Set<ApplicationMode>> map,
	                                                   @Nullable ApplicationMode... appModes) {
		HashSet<ApplicationMode> set = new HashSet<>();
		if (appModes == null) {
			set.addAll(ApplicationMode.allPossibleValues());
		} else {
			Collections.addAll(set, appModes);
		}
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			// add derived modes
			if (set.contains(mode.getParent())) {
				set.add(mode);
			}
		}
		map.put(widgetId, set);
		return set;
	}
}
