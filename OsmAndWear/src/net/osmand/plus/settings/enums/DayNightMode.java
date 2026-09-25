package net.osmand.plus.settings.enums;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum DayNightMode {
	DAY(R.string.daynight_mode_day, R.string.daynight_mode_day_summary,
			R.drawable.ic_action_map_day, R.drawable.ic_action_map_day_filled),
	NIGHT(R.string.daynight_mode_night, R.string.daynight_mode_night_summary,
			R.drawable.ic_action_map_night, R.drawable.ic_action_map_night_filled),
	AUTO(R.string.daynight_mode_auto, R.string.daynight_mode_sunrise_sunset_summary,
			R.drawable.ic_action_map_sunset, R.drawable.ic_action_map_sunset_filled),
	SENSOR(R.string.daynight_mode_sensor, R.string.daynight_mode_sensor_summary,
			R.drawable.ic_action_map_light_sensor, R.drawable.ic_action_map_light_sensor_filled),
	APP_THEME(R.string.daynight_mode_app_theme, R.string.daynight_mode_app_theme_summary,
			R.drawable.ic_action_map_mode_app_theme, R.drawable.ic_action_map_mode_app_theme_filled);

	private final int key;
	@DrawableRes
	private final int defaultIcon;
	private final int selectedIcon;
	private final int summaryRes;

	DayNightMode(@StringRes int key, @StringRes int summaryRes,
	             @DrawableRes int defaultIcon, @DrawableRes int selectedIcon) {
		this.key = key;
		this.summaryRes = summaryRes;
		this.defaultIcon = defaultIcon;
		this.selectedIcon = selectedIcon;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(key);
	}

	@StringRes
	public int getSummaryRes() {
		return summaryRes;
	}

	@DrawableRes
	public int getDefaultIcon() {
		return defaultIcon;
	}

	@DrawableRes
	public int getSelectedIcon() {
		return selectedIcon;
	}

	public boolean isSensor() {
		return this == SENSOR;
	}

	public boolean isAuto() {
		return this == AUTO;
	}

	public boolean isDay() {
		return this == DAY;
	}

	public boolean isNight() {
		return this == NIGHT;
	}

	public boolean isAppTheme() {
		return this == APP_THEME;
	}

	public static DayNightMode[] possibleValues(Context context) {
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		boolean isLightSensorEnabled = mLight != null;
		if (isLightSensorEnabled) {
			return values();
		} else {
			return new DayNightMode[]{DAY, NIGHT, AUTO, APP_THEME};
		}
	}
}