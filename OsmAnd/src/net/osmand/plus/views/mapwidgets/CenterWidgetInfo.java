package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class CenterWidgetInfo extends MapWidgetInfo {

	public CenterWidgetInfo(@NonNull String key,
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
	public boolean isVisibleForAppMode(@NonNull ApplicationMode appMode) {
		return true;
	}

	@Override
	public void showHideForAppMode(@NonNull ApplicationMode appMode, boolean show) {
	}
}