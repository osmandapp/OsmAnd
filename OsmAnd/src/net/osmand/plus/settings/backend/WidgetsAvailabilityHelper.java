package net.osmand.plus.settings.backend;

import static net.osmand.plus.settings.backend.ApplicationMode.AIRCRAFT;
import static net.osmand.plus.settings.backend.ApplicationMode.BICYCLE;
import static net.osmand.plus.settings.backend.ApplicationMode.BOAT;
import static net.osmand.plus.settings.backend.ApplicationMode.CAR;
import static net.osmand.plus.settings.backend.ApplicationMode.HORSE;
import static net.osmand.plus.settings.backend.ApplicationMode.MOTORCYCLE;
import static net.osmand.plus.settings.backend.ApplicationMode.PEDESTRIAN;
import static net.osmand.plus.settings.backend.ApplicationMode.PUBLIC_TRANSPORT;
import static net.osmand.plus.settings.backend.ApplicationMode.SKI;
import static net.osmand.plus.settings.backend.ApplicationMode.TRUCK;
import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE;
import static net.osmand.plus.views.mapwidgets.WidgetType.AVERAGE_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.BATTERY;
import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetType.DISTANCE_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.GPS_INFO;
import static net.osmand.plus.views.mapwidgets.WidgetType.INTERMEDIATE_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.MAGNETIC_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetType.MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.RADIUS_RULER;
import static net.osmand.plus.views.mapwidgets.WidgetType.RELATIVE_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetType.SECOND_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_1;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_2;
import static net.osmand.plus.views.mapwidgets.WidgetType.SMALL_NEXT_TURN;
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
				AIRCRAFT, SKI, TRUCK, MOTORCYCLE, HORSE};
		ApplicationMode[] all = null;
		ApplicationMode[] none = {};

		// left
		ApplicationMode[] navigationSet1 = {CAR, BICYCLE, BOAT, SKI, TRUCK, MOTORCYCLE, HORSE};
		ApplicationMode[] navigationSet2 = {PEDESTRIAN, PUBLIC_TRANSPORT, AIRCRAFT};

		regWidgetVisibility(NEXT_TURN, navigationSet1);
		regWidgetVisibility(SMALL_NEXT_TURN, navigationSet2);
		regWidgetVisibility(SECOND_NEXT_TURN, navigationSet1);
		regWidgetAvailability(NEXT_TURN, exceptDefault);
		regWidgetAvailability(SMALL_NEXT_TURN, exceptDefault);
		regWidgetAvailability(SECOND_NEXT_TURN, exceptDefault);

		// right
		regWidgetVisibility(INTERMEDIATE_DESTINATION, all);
		regWidgetVisibility(DISTANCE_TO_DESTINATION, all);
		regWidgetVisibility(TIME_TO_INTERMEDIATE, all);
		regWidgetVisibility(TIME_TO_DESTINATION, all);
		regWidgetVisibility(CURRENT_SPEED, CAR, BICYCLE, BOAT, SKI, PUBLIC_TRANSPORT, AIRCRAFT,
				TRUCK, MOTORCYCLE, HORSE);
		regWidgetVisibility(MAX_SPEED, CAR, TRUCK, MOTORCYCLE);
		regWidgetVisibility(ALTITUDE, PEDESTRIAN, BICYCLE);
		regWidgetAvailability(INTERMEDIATE_DESTINATION, all);
		regWidgetAvailability(DISTANCE_TO_DESTINATION, all);
		regWidgetAvailability(TIME_TO_INTERMEDIATE, all);
		regWidgetAvailability(TIME_TO_DESTINATION, all);
		regWidgetAvailability(CURRENT_SPEED, all);
		regWidgetAvailability(MAX_SPEED, all);
		regWidgetAvailability(AVERAGE_SPEED, all);
		regWidgetAvailability(ALTITUDE, all);

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
