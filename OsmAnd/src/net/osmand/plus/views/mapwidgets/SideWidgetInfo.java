package net.osmand.plus.views.mapwidgets;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.LinkedHashSet;
import java.util.Set;

public class SideWidgetInfo extends MapWidgetInfo {

	private final Set<ApplicationMode> visibleCollapsible = new LinkedHashSet<>();
	private final Set<ApplicationMode> visible = new LinkedHashSet<>();

	public SideWidgetInfo(@NonNull String key,
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
	public boolean isVisible(@NonNull ApplicationMode appMode) {
		return visible.contains(appMode);
	}

	@Override
	public boolean isVisibleCollapsed(@NonNull ApplicationMode appMode) {
		return visibleCollapsible.contains(appMode);
	}

	@Override
	public void addVisible(@NonNull ApplicationMode appMode) {
		visible.add(appMode);
	}

	@Override
	public void addVisibleCollapsible(@NonNull ApplicationMode appMode) {
		visibleCollapsible.add(appMode);
	}

	@Override
	public void removeVisible(@NonNull ApplicationMode appMode) {
		visible.remove(appMode);
	}

	@Override
	public void removeVisibleCollapsible(@NonNull ApplicationMode appMode) {
		visibleCollapsible.remove(appMode);
	}
}