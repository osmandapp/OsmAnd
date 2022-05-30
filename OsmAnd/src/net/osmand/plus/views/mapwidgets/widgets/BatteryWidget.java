package net.osmand.plus.views.mapwidgets.widgets;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BatteryWidget extends TextInfoWidget {

	private static final long UPDATE_INTERVAL_MILLIS = 1000;

	private long cachedTime = 0;

	public BatteryWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		setIcons(false);
		setText(null, null);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		long time = System.currentTimeMillis();
		if (isUpdateNeeded() || time - cachedTime > UPDATE_INTERVAL_MILLIS) {
			cachedTime = time;
			Intent batteryIntent = app.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

			if (level == -1 || scale == -1 || status == -1) {
				setText("?", null);
				setIcons(false);
			} else {
				boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
						|| status == BatteryManager.BATTERY_STATUS_FULL;
				setText(String.format("%d%%", (level * 100) / scale), null);
				setIcons(charging);
			}
		}
	}

	private void setIcons(boolean charging) {
		if (charging) {
			setIcons(R.drawable.widget_battery_charging_day, R.drawable.widget_battery_charging_night);
		} else {
			setIcons(WidgetParams.BATTERY);
		}
	}
}