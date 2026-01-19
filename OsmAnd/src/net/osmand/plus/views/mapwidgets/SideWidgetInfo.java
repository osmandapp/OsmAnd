package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetsPanel.LEFT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.RIGHT;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class SideWidgetInfo extends MapWidgetInfo {

	public SideWidgetInfo(@NonNull String key,
	                      @NonNull TextInfoWidget widget,
	                      @DrawableRes int daySettingsIconId,
	                      @DrawableRes int nightSettingsIconId,
	                      @StringRes int messageId,
	                      @Nullable String message,
	                      int page,
	                      int order,
	                      @NonNull WidgetsPanel widgetPanel) {
		super(key, widget, daySettingsIconId, nightSettingsIconId, messageId, message, page, order, widgetPanel);

		if (getMessage() != null) {
			widget.setContentTitle(getMessage());
		} else if (getMessageId() != MapWidgetInfo.INVALID_ID) {
			widget.setContentTitle(getMessageId());
		}
	}

	@NonNull
	@Override
	public WidgetsPanel getUpdatedPanel() {
		WidgetType widgetType = getWidgetType();
		if (widgetType != null) {
			if (widgetType.defaultPanel == LEFT && RIGHT.contains(key, widget.getMapActivity())) {
				widgetPanel = RIGHT;
			} else if (widgetType.defaultPanel == RIGHT && LEFT.contains(key, widget.getMapActivity())) {
				widgetPanel = LEFT;
			} else {
				widgetPanel = widgetType.defaultPanel;
			}
		} else {
			widgetPanel = LEFT.contains(key, widget.getMapActivity()) ? LEFT : RIGHT;
		}

		return widgetPanel;
	}
}