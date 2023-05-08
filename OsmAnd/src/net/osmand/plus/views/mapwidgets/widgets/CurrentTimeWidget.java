package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_TIME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

public class CurrentTimeWidget extends TextInfoWidget {

	private static final long UPDATE_INTERVAL_MILLIS = 5000;

	private long cachedTime;

	public CurrentTimeWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, CURRENT_TIME);
		setIcons(CURRENT_TIME);
		setText(null, null);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		long time = System.currentTimeMillis();
		if (isUpdateNeeded() || time - cachedTime > UPDATE_INTERVAL_MILLIS) {
			cachedTime = time;
			setTimeText(time);
		}
	}
}