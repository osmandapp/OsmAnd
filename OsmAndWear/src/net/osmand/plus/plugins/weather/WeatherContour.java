package net.osmand.plus.plugins.weather;

import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_CLOUD;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRECIPITATION;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRESSURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_TEMPERATURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_SPEED;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_CLOUD_CONTOURS_LINES_ATTR;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_PRECIPITATION_CONTOURS_LINES_ATTR;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_PRESSURE_CONTOURS_LINES_ATTR;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_TEMP_CONTOUR_LINES_ATTR;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_WIND_CONTOURS_LINES_ATTR;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.WeatherBand.WeatherBandType;
import net.osmand.plus.plugins.weather.units.CloudUnit;
import net.osmand.plus.plugins.weather.units.PrecipitationUnit;
import net.osmand.plus.plugins.weather.units.PressureUnit;
import net.osmand.plus.plugins.weather.units.TemperatureUnit;
import net.osmand.plus.plugins.weather.units.WindUnit;

public enum WeatherContour {

	TEMPERATURE(R.string.map_settings_weather_temp, R.drawable.ic_action_thermometer, WEATHER_TEMP_CONTOUR_LINES_ATTR, WEATHER_BAND_TEMPERATURE),
	PRECIPITATION(R.string.map_settings_weather_precip, R.drawable.ic_action_precipitation, WEATHER_PRECIPITATION_CONTOURS_LINES_ATTR, WEATHER_BAND_PRECIPITATION),
	WIND(R.string.map_settings_weather_wind, R.drawable.ic_action_wind, WEATHER_WIND_CONTOURS_LINES_ATTR, WEATHER_BAND_WIND_SPEED),
	CLOUDS(R.string.map_settings_weather_cloud, R.drawable.ic_action_clouds, WEATHER_CLOUD_CONTOURS_LINES_ATTR, WEATHER_BAND_CLOUD),
	PRESSURE(R.string.map_settings_weather_air_pressure, R.drawable.ic_action_air_pressure, WEATHER_PRESSURE_CONTOURS_LINES_ATTR, WEATHER_BAND_PRESSURE);

	WeatherContour(int titleId, int iconId, String attrName, short bandIndex) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.attrName = attrName;
		this.bandIndex = bandIndex;
	}

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;
	@WeatherBandType
	private final short bandIndex;

	private final String attrName;

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@WeatherBandType
	public short getBandIndex() {
		return bandIndex;
	}

	@NonNull
	public String getAttrName() {
		return attrName;
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
