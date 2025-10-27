package net.osmand.plus.views.mapwidgets;

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
		if (getMessage() != null) {
			widget.setContentTitle(getMessage());
		} else if (getMessageId() != MapWidgetInfo.INVALID_ID) {
			widget.setContentTitle(getMessageId());
		}
	}

	@Override
	public void setWidgetPanel(@NonNull WidgetsPanel widgetPanel) {
		this.widgetPanel = widgetPanel;
		SimpleWidget simpleWidget = (SimpleWidget) widget;
		simpleWidget.recreateViewIfNeeded(widgetPanel);
	}

	@NonNull
	@Override
	public WidgetsPanel getUpdatedPanel() {
		OsmandSettings settings = widget.getMyApplication().getSettings();
		WidgetType widgetType = getWidgetType();
		if (widgetType != null) {
			return widgetType.getPanel(key, settings);
		} else {
			WidgetType.findWidgetPanel(key, settings, null);
		}
		return widgetPanel;
	}

	public void setExternalProviderPackage(@NonNull String externalProviderPackage) {
		this.externalProviderPackage = externalProviderPackage;
	}

	@Nullable
	public String getExternalProviderPackage() {
		return externalProviderPackage;
	}
}