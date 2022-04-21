package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.LinkedHashSet;
import java.util.Set;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class SideWidgetInfo extends MapWidgetInfo {

	private final Set<ApplicationMode> visibilityForAppModes = new LinkedHashSet<>();

	public SideWidgetInfo(@NonNull String key,
	                      @NonNull MapWidget widget,
	                      @Nullable WidgetState widgetState,
	                      @DrawableRes int daySettingsIconId,
	                      @DrawableRes int nightSettingsIconId,
	                      @StringRes int messageId,
	                      @Nullable String message,
	                      int page,
	                      int order,
	                      @NonNull WidgetsPanel widgetPanel) {
		super(key, widget, widgetState, daySettingsIconId, nightSettingsIconId, messageId, message,
				page, order, widgetPanel);
	}

	@Override
	public boolean isEnabledForAppMode(@NonNull ApplicationMode appMode) {
		return visibilityForAppModes.contains(appMode);
	}

	@Override
	public void showHideForAppMode(@NonNull ApplicationMode appMode, boolean show) {
		if (show) {
			visibilityForAppModes.add(appMode);
		} else {
			visibilityForAppModes.remove(appMode);
		}
	}
}