package net.osmand.plus.views.mapwidgets.widgets;

import android.text.format.DateFormat;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CurrentTimeWidget extends TextInfoWidget {

	private static final long UPDATE_INTERVAL_MILLIS = 5000;

	private long cachedTime = 0;

	public CurrentTimeWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		setIcons(WidgetParams.CURRENT_TIME);
		setText(null, null);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		long time = System.currentTimeMillis();
		if (isUpdateNeeded() || time - cachedTime > UPDATE_INTERVAL_MILLIS) {
			cachedTime = time;
			if (DateFormat.is24HourFormat(app)) {
				setText(DateFormat.format("k:mm", time).toString(), null);
			} else {
				setText(DateFormat.format("h:mm", time).toString(),
						DateFormat.format("aa", time).toString());
			}
		}
	}
}