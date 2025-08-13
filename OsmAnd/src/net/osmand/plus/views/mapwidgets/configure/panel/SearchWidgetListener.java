package net.osmand.plus.views.mapwidgets.configure.panel;

import androidx.annotation.NonNull;

import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;

public interface SearchWidgetListener {
	void widgetSelected(@NonNull WidgetType widgetType);

	void externalWidgetSelected(@NonNull MapWidgetInfo widgetInfo);

	void groupSelected(@NonNull WidgetGroup group);
}
