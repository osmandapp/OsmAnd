package net.osmand.plus.views.mapwidgets.widgetinterfaces;

import androidx.annotation.NonNull;

import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public interface IComplexWidget {
	void recreateViewIfNeeded(@NonNull WidgetsPanel panel);
}
