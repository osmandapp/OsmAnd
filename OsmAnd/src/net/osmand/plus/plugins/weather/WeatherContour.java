package net.osmand.plus.plugins.weather;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.units.CloudUnit;
import net.osmand.plus.plugins.weather.units.PrecipitationUnit;
import net.osmand.plus.plugins.weather.units.PressureUnit;
import net.osmand.plus.plugins.weather.units.TemperatureUnit;
import net.osmand.plus.plugins.weather.units.WindUnit;

public enum WeatherContour {

	TEMPERATURE(R.string.map_settings_weather_temp, R.drawable.ic_action_thermometer),
	PRECIPITATION(R.string.map_settings_weather_precip, R.drawable.ic_action_precipitation),
	WIND(R.string.map_settings_weather_wind, R.drawable.ic_action_wind),
	CLOUDS(R.string.map_settings_weather_cloud, R.drawable.ic_action_clouds),
	PRESSURE(R.string.map_settings_weather_air_pressure, R.drawable.ic_action_air_pressure);

	WeatherContour(int titleId, int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

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
				return TemperatureUnit.values();
			case PRESSURE:
				return PressureUnit.values();
			case WIND:
				return WindUnit.values();
			case CLOUDS:
				return CloudUnit.values();
			case PRECIPITATION:
				return PrecipitationUnit.values();
			default:
				return null;
		}
	}

	@Nullable
	public String getUnitName(@NonNull Context ctx, @NonNull Enum<?> value) {
		if (value instanceof TemperatureUnit) {
			return ((TemperatureUnit) value).toHumanString(ctx);
		} else if (value instanceof PressureUnit) {
			return ((PressureUnit) value).toHumanString(ctx);
		} else if (value instanceof WindUnit) {
			return ((WindUnit) value).toHumanString(ctx);
		} else if (value instanceof CloudUnit) {
			return ((CloudUnit) value).getSymbol();
		} else if (value instanceof PrecipitationUnit) {
			return ((PrecipitationUnit) value).toHumanString(ctx);
		}
		return null;
	}
}
