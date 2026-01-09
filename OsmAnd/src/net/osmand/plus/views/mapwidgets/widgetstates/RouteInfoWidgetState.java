package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.DisplayValue;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.DisplayPriority;
import net.osmand.util.Algorithms;

public class RouteInfoWidgetState extends ResizableWidgetState {

	private static final String SHOW_EXPAND_BUTTON_PREF_ID = "show_expand_button";
	private static final String DEFAULT_VALUE_PREF_ID = "route_info_widget_display_mode";
	private static final String DISPLAY_PRIORITY_PREF_ID = "route_info_widget_display_priority";

	private final CommonPreference<DisplayValue> defaultViewPref;
	private final CommonPreference<DisplayPriority> displayPriorityPref;
	private final CommonPreference<Boolean> showExpandButtonPref;

	public RouteInfoWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app, customId, WidgetType.ROUTE_INFO, WidgetSize.MEDIUM);
		defaultViewPref = registerDefaultViewPreference(customId);
		displayPriorityPref = registerDisplayPriorityPreference(customId);
		showExpandButtonPref = registerShowExpandButtonPreference(customId);
	}

	public boolean isShowExpandButtonEnabled(@NonNull ApplicationMode appMode) {
		return showExpandButtonPref.getModeValue(appMode);
	}

	public void setShowExpandButtonEnabled(@NonNull ApplicationMode appMode, boolean value) {
		showExpandButtonPref.setModeValue(appMode, value);
	}
	@NonNull
	public DisplayValue getDefaultView() {
		return getDefaultView(settings.getApplicationMode());
	}

	@NonNull
	public DisplayValue getDefaultView(@NonNull ApplicationMode appMode) {
		return defaultViewPref.getModeValue(appMode);
	}

	public void setDefaultView(@NonNull ApplicationMode appMode, @NonNull DisplayValue defaultView) {
		defaultViewPref.setModeValue(appMode, defaultView);
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
		registerDefaultViewPreference(customId).setModeValue(appMode, defaultViewPref.getModeValue(sourceAppMode));
		registerDisplayPriorityPreference(customId).setModeValue(appMode, displayPriorityPref.getModeValue(sourceAppMode));
		registerShowExpandButtonPreference(customId).setModeValue(appMode, showExpandButtonPref.getModeValue(sourceAppMode));
	}

	@NonNull
	private CommonPreference<DisplayValue> registerDefaultViewPreference(@Nullable String customId) {
		String prefId = DEFAULT_VALUE_PREF_ID;
		if (!Algorithms.isEmpty(customId)) {
			prefId += "_" + customId;
		}
		DisplayValue defaultValue = DisplayValue.ARRIVAL_TIME;
		DisplayValue[] values = DisplayValue.values();
		return settings.registerEnumStringPreference(prefId, defaultValue, values, DisplayValue.class).makeProfile().cache();
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

	@NonNull
	private CommonPreference<Boolean> registerShowExpandButtonPreference(@Nullable String customId) {
		String prefId = SHOW_EXPAND_BUTTON_PREF_ID;
		if (!Algorithms.isEmpty(customId)) {
			prefId += "_" + customId;
		}
		return settings.registerBooleanPreference(prefId, true).makeProfile().cache();
	}
}