package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.BATTERY;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class BatteryWidget extends SimpleWidget {

	private static final long UPDATE_INTERVAL_MILLIS = 1000;

	private long cachedTime;

	public BatteryWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, BATTERY, customId, widgetsPanel);
		setIcons(false);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
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
			setIcons(BATTERY);
		}
	}
}