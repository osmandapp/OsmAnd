package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoDisplayMode;
import net.osmand.util.Algorithms;

public class RouteInfoWidgetState extends ResizableWidgetState {

	private static final String DISPLAY_MODE_PREF_ID = "route_info_widget_display_mode";

	private final CommonPreference<RouteInfoDisplayMode> displayModePref;

	public RouteInfoWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app, customId, WidgetType.ROUTE_INFO);
		displayModePref = registerDisplayModePreference(customId);
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

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copyPrefs(appMode, customId);
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copyPrefsFromMode(sourceAppMode, appMode, customId);
		registerDisplayModePreference(customId).setModeValue(appMode, displayModePref.getModeValue(sourceAppMode));
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
}
