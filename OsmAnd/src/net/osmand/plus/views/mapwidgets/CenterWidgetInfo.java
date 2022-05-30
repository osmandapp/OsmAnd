package net.osmand.plus.views.mapwidgets;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;

public class CenterWidgetInfo extends MapWidgetInfo {

	public CenterWidgetInfo(@NonNull String key,
	                        @NonNull MapWidget widget,
	                        @DrawableRes int daySettingsIconId,
	                        @DrawableRes int nightSettingsIconId,
	                        @StringRes int messageId,
	                        @Nullable String message,
	                        int page,
	                        int order,
	                        @NonNull WidgetsPanel widgetPanel) {
		super(key, widget, daySettingsIconId, nightSettingsIconId, messageId, message,
				page, order, widgetPanel);
	}

	@NonNull
	@Override
	public WidgetsPanel getUpdatedPanel() {
		return widgetPanel;
	}

	@Override
	public boolean isEnabledForAppMode(@NonNull ApplicationMode appMode) {
		OsmandPreference<Boolean> visibilityPref = widget.getWidgetVisibilityPref();
		return visibilityPref == null || visibilityPref.getModeValue(appMode);
	}

	@Override
	public void enableDisableForMode(@NonNull ApplicationMode appMode, boolean enabled) {
		OsmandPreference<Boolean> visibilityPref = widget.getWidgetVisibilityPref();
		if (visibilityPref != null) {
			visibilityPref.setModeValue(appMode, enabled);
		}
	}
}