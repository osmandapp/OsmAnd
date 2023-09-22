package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.LEFT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.RIGHT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

public class SimpleWidgetInfo extends MapWidgetInfo {

	private String externalProviderPackage;

	public SimpleWidgetInfo(@NonNull String key,
							@NonNull SimpleWidget widget,
							@DrawableRes int daySettingsIconId,
							@DrawableRes int nightSettingsIconId,
							@StringRes int messageId,
							@Nullable String message,
							int page,
							int order,
							@NonNull WidgetsPanel widgetPanel) {
		super(key, widget, daySettingsIconId, nightSettingsIconId, messageId, message, page, order, widgetPanel);
		widget.recreateViewIfNeeded(widgetPanel);

		if (getMessage() != null) {
			widget.setContentTitle(getMessage());
		} else if (getMessageId() != MapWidgetInfo.INVALID_ID) {
			widget.setContentTitle(getMessageId());
		}
	}

	public void setExternalProviderPackage(@NonNull String externalProviderPackage) {
		this.externalProviderPackage = externalProviderPackage;
	}

	@Nullable
	public String getExternalProviderPackage() {
		return externalProviderPackage;
	}

	@NonNull
	@Override
	public WidgetsPanel getUpdatedPanel() {
		OsmandSettings settings = widget.getMyApplication().getSettings();
		WidgetType widgetType = getWidgetType();
		if (widgetType != null) {
			if (widgetType.defaultPanel == LEFT && RIGHT.contains(key, settings)) {
				widgetPanel = RIGHT;
			} else if (widgetType.defaultPanel == RIGHT && LEFT.contains(key, settings)) {
				widgetPanel = LEFT;
			} else {
				if (TOP.contains(key, settings)) {
					widgetPanel = TOP;
				} else if (BOTTOM.contains(key, settings)) {
					widgetPanel = BOTTOM;
				} else {
					widgetPanel = widgetType.defaultPanel;
				}
			}
		} else {
			widgetPanel = LEFT.contains(key, settings) ? LEFT : RIGHT;
		}

		return widgetPanel;
	}
}