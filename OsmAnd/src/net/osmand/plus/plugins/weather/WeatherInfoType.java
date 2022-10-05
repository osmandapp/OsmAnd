package net.osmand.plus.plugins.weather;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.units.CloudConstants;
import net.osmand.plus.plugins.weather.units.PrecipConstants;
import net.osmand.plus.plugins.weather.units.PressureConstants;
import net.osmand.plus.plugins.weather.units.TemperatureConstants;
import net.osmand.plus.plugins.weather.units.WindConstants;

public enum WeatherInfoType {

	TEMPERATURE(R.string.map_settings_weather_temp, R.drawable.ic_action_thermometer),
	PRECIPITATION(R.string.map_settings_weather_precip, R.drawable.ic_action_precipitation),
	WIND(R.string.map_settings_weather_wind, R.drawable.ic_action_wind),
	CLOUDS(R.string.map_settings_weather_cloud, R.drawable.ic_action_clouds),
	PRESSURE(R.string.map_settings_weather_air_pressure, R.drawable.ic_action_air_pressure);

	WeatherInfoType(int titleId, int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@StringRes
	private int titleId;
	@DrawableRes
	private int iconId;

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String toHumanString(@NonNull Context context) {
		return context.getString(getTitleId());
	}

	@Nullable
	public Enum<?>[] getUnits() {
		switch (this) {
			case TEMPERATURE:
				return TemperatureConstants.values();
			case PRESSURE:
				return PressureConstants.values();
			case WIND:
				return WindConstants.values();
			case CLOUDS:
				return CloudConstants.values();
			case PRECIPITATION:
				return PrecipConstants.values();
			default:
				return null;
		}
	}

	@Nullable
	public String getUnitName(@NonNull Context ctx, @NonNull Enum<?> value) {
		if (value instanceof TemperatureConstants) {
			return ((TemperatureConstants) value).toHumanString(ctx);
		} else if (value instanceof PressureConstants) {
			return ((PressureConstants) value).toHumanString(ctx);
		} else if (value instanceof WindConstants) {
			return ((WindConstants) value).toHumanString(ctx);
		} else if (value instanceof CloudConstants) {
			return ((CloudConstants) value).getUnit();
		} else if (value instanceof PrecipConstants) {
			return ((PrecipConstants) value).toHumanString(ctx);
		}
		return null;
	}

}
