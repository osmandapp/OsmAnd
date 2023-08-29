package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.mapwidgets.WidgetType;

public abstract class GlideBaseWidget extends TextInfoWidget {

	private static final int UPDATE_INTERVAL_MILLIS = 1000;
	protected static final int LONG_UPDATE_INTERVAL_MILLIS = 10_000;

	private long lastUpdateTime;

	public GlideBaseWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		super(mapActivity, widgetType);
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
