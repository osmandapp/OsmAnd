package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public abstract class GlideBaseWidget extends SimpleWidget {

	private static final int UPDATE_INTERVAL_MILLIS = 1000;
	protected static final int LONG_UPDATE_INTERVAL_MILLIS = 10_000;

	private long lastUpdateTime;

	public GlideBaseWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetType, customId, widgetsPanel);
		setText(NO_VALUE, null);
		setIcons(widgetType);
	}

	protected void markUpdated() {
		lastUpdateTime = System.currentTimeMillis();
	}

	protected boolean isTimeToUpdate() {
		return isTimeToUpdate(UPDATE_INTERVAL_MILLIS);
	}

	protected boolean isTimeToUpdate(long interval) {
		return System.currentTimeMillis() - lastUpdateTime > interval;
	}
}
