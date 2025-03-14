package net.osmand.plus.views.mapwidgets.configure.panel;

import androidx.annotation.NonNull;

import net.osmand.plus.views.mapwidgets.MapWidgetInfo;

public interface WidgetsConfigurationChangeListener {

	void onWidgetsConfigurationChanged();
	void onWidgetAdded(@NonNull MapWidgetInfo widgetInfo);
}