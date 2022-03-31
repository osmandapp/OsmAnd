package net.osmand.plus.views.mapwidgets;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

public class CenterWidgetInfo extends MapWidgetInfo {

	public CenterWidgetInfo(@NonNull String key,
	                        @NonNull MapWidget widget,
	                        @Nullable WidgetState widgetState,
	                        @DrawableRes int settingsIconId,
	                        @StringRes int messageId,
	                        @Nullable String message,
	                        int priority,
	                        @NonNull WidgetsPanel widgetPanel) {
		super(key, widget, widgetState, settingsIconId, messageId, message, priority, widgetPanel);
	}

	@Override
	public boolean isVisibleCollapsed(@NonNull ApplicationMode appMode) {
		return false;
	}

	@Override
	public boolean isVisible(@NonNull ApplicationMode appMode) {
		return true;
	}

	@Override
	public void addVisible(@NonNull ApplicationMode appMode) {
	}

	@Override
	public void addVisibleCollapsible(@NonNull ApplicationMode appMode) {
	}

	@Override
	public void removeVisible(@NonNull ApplicationMode appMode) {
	}

	@Override
	public void removeVisibleCollapsible(@NonNull ApplicationMode appMode) {
	}
}