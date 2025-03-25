package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.DisplayPriority;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoDisplayMode;
import net.osmand.util.Algorithms;

public class RouteInfoWidgetState extends ResizableWidgetState {

	private static final String DISPLAY_MODE_PREF_ID = "route_info_widget_display_mode";
	private static final String DISPLAY_PRIORITY_PREF_ID = "route_info_widget_display_priority";

	private final CommonPreference<RouteInfoDisplayMode> displayModePref;
	private final CommonPreference<DisplayPriority> displayPriorityPref;

	public RouteInfoWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app, customId, WidgetType.ROUTE_INFO, WidgetSize.MEDIUM);
		displayModePref = registerDisplayModePreference(customId);
		displayPriorityPref = registerDisplayPriorityPreference(customId);
	}

	@NonNull
	public RouteInfoDisplayMode getDisplayMode() {
		return getDisplayMode(settings.getApplicationMode());
	}

	@NonNull
	public RouteInfoDisplayMode getDisplayMode(@NonNull ApplicationMode appMode) {
		return displayModePref.getModeValue(appMode);
	}

	public void setDisplayMode(@NonNull ApplicationMode appMode, @NonNull RouteInfoDisplayMode displayMode) {
		displayModePref.setModeValue(appMode, displayMode);
	}

	@NonNull
	public DisplayPriority getDisplayPriority() {
		return getDisplayPriority(settings.getApplicationMode());
	}

	@NonNull
	public DisplayPriority getDisplayPriority(@NonNull ApplicationMode appMode) {
		return displayPriorityPref.getModeValue(appMode);
	}

	public void setDisplayPriority(@NonNull ApplicationMode appMode, @NonNull DisplayPriority displayPriority) {
		displayPriorityPref.setModeValue(appMode, displayPriority);
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copyPrefs(appMode, customId);
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copyPrefsFromMode(sourceAppMode, appMode, customId);
		registerDisplayModePreference(customId).setModeValue(appMode, displayModePref.getModeValue(sourceAppMode));
		registerDisplayPriorityPreference(customId).setModeValue(appMode, displayPriorityPref.getModeValue(sourceAppMode));
	}

	@NonNull
	private CommonPreference<RouteInfoDisplayMode> registerDisplayModePreference(@Nullable String customId) {
		String prefId = DISPLAY_MODE_PREF_ID;
		if (!Algorithms.isEmpty(customId)) {
			prefId += "_" + customId;
		}
		RouteInfoDisplayMode defValue = RouteInfoDisplayMode.ARRIVAL_TIME;
		RouteInfoDisplayMode[] values = RouteInfoDisplayMode.values();
		return settings.registerEnumStringPreference(prefId, defValue, values, RouteInfoDisplayMode.class).makeProfile().cache();
	}

	@NonNull
	private CommonPreference<DisplayPriority> registerDisplayPriorityPreference(@Nullable String customId) {
		String prefId = DISPLAY_PRIORITY_PREF_ID;
		if (!Algorithms.isEmpty(customId)) {
			prefId += "_" + customId;
		}
		DisplayPriority defValue = DisplayPriority.DESTINATION_FIRST;
		DisplayPriority[] values = DisplayPriority.values();
		return settings.registerEnumStringPreference(prefId, defValue, values, DisplayPriority.class).makeProfile().cache();
	}
}