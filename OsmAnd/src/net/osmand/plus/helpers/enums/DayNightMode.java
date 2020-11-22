package net.osmand.plus.helpers.enums;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum DayNightMode {
	AUTO(R.string.daynight_mode_auto, R.drawable.ic_action_map_sunset),
	DAY(R.string.daynight_mode_day, R.drawable.ic_action_map_day),
	NIGHT(R.string.daynight_mode_night, R.drawable.ic_action_map_night),
	SENSOR(R.string.daynight_mode_sensor, R.drawable.ic_action_map_light_sensor);

	private final int key;
	@DrawableRes
	private final int drawableRes;

	DayNightMode(@StringRes int key, @DrawableRes int drawableRes) {
		this.key = key;
		this.drawableRes = drawableRes;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(key);
	}

	@DrawableRes
	public int getIconRes() {
		return drawableRes;
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

	public static DayNightMode[] possibleValues(Context context) {
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		boolean isLightSensorEnabled = mLight != null;
		if (isLightSensorEnabled) {
			return DayNightMode.values();
		} else {
			return new DayNightMode[]{AUTO, DAY, NIGHT};
		}
	}
}