package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.settings.backend.OsmandSettings;
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
		OsmandSettings settings = widget.getMyApplication().getSettings();
		WidgetType widgetType = getWidgetType();
		if (widgetType != null) {
			if (widgetType.defaultPanel == BOTTOM && TOP.contains(key, settings)) {
				widgetPanel = TOP;
			} else if (widgetType.defaultPanel == TOP && BOTTOM.contains(key, settings)) {
				widgetPanel = BOTTOM;
			} else {
				widgetPanel = widgetType.defaultPanel;
			}
		} else {
			widgetPanel = TOP.contains(key, settings) ? TOP : BOTTOM;
		}
		return widgetPanel;
	}
}