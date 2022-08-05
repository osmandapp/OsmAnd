package net.osmand.plus.weather;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.weather.units.CloudConstants;
import net.osmand.plus.weather.units.PrecipConstants;
import net.osmand.plus.weather.units.PressureConstants;
import net.osmand.plus.weather.units.TemperatureConstants;
import net.osmand.plus.weather.units.WindConstants;

public enum WeatherLayerType {

	TEMPERATURE(R.string.map_settings_weather_temp, R.drawable.ic_action_thermometer),
	PRECIPITATION(R.string.map_settings_weather_precip, R.drawable.ic_action_precipitation),
	WIND(R.string.map_settings_weather_wind, R.drawable.ic_action_wind),
	CLOUDS(R.string.map_settings_weather_cloud, R.drawable.ic_action_clouds),
	PRESSURE(R.string.map_settings_weather_pressure, R.drawable.ic_action_air_pressure);

	WeatherLayerType(int titleId, int iconId) {
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

	public boolean isEnabled() {
		WeatherPlugin plugin = OsmandPlugin.getPlugin(WeatherPlugin.class);
		return plugin != null && plugin.isLayerEnabled(this);
	}

	public void setEnabled(boolean enabled) {
		WeatherPlugin plugin = OsmandPlugin.getPlugin(WeatherPlugin.class);
		if (plugin != null) {
			plugin.toggleLayerEnable(this, enabled);
		}
	}

	@NonNull
	public Integer getTransparency(@NonNull ApplicationMode appMode) {
		WeatherPlugin plugin = OsmandPlugin.getPlugin(WeatherPlugin.class);
		return plugin != null ? plugin.getLayerTransparency(appMode, this) : 0;
	}

	public void setTransparency(@NonNull ApplicationMode appMode, @Nullable Integer transparency) {
		WeatherPlugin plugin = OsmandPlugin.getPlugin(WeatherPlugin.class);
		if (plugin != null) {
			plugin.setLayerTransparency(appMode, this, transparency);
		}
	}

	@Nullable
	public String getSelectedUnitName(@NonNull Context ctx, @NonNull ApplicationMode appMode) {
		Enum<?> selectedUnit = getSelectedUnit(appMode);
		return selectedUnit != null ? getUnitName(ctx, selectedUnit) : null;
	}

	@NonNull
	public Enum<?> getSelectedUnit(@NonNull ApplicationMode appMode) {
		WeatherPlugin plugin = OsmandPlugin.getPlugin(WeatherPlugin.class);
		return plugin != null ? plugin.getSelectedLayerUnit(appMode, this) : null;
	}

	public void setSelectedUnit(@NonNull ApplicationMode appMode, @NonNull Enum<?> value) {
		WeatherPlugin plugin = OsmandPlugin.getPlugin(WeatherPlugin.class);
		if (plugin != null) {
			plugin.setSelectedLayerUnit(appMode, this, value);
		}
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
		} else {
			return null;
		}
	}

	@NonNull
	public String getUnitsPreferenceId() {
		if (this == TEMPERATURE) {
			return "map_settings_weather_temp";
		} else if (this == PRECIPITATION) {
			return "map_settings_weather_precip";
		} else if (this == WIND) {
			return "map_settings_weather_wind";
		} else if (this == CLOUDS) {
			return "map_settings_weather_cloud";
		}
		return "map_settings_weather_pressure";
	}

	@Nullable
	public String getEmprtyStateDesc(@NonNull Context ctx) {
		switch (this) {
			case TEMPERATURE:
				return ctx.getString(R.string.empty_screen_weather_temperature_layer);
			case PRECIPITATION:
				return ctx.getString(R.string.empty_screen_weather_precipitation_layer);
			case WIND:
				return ctx.getString(R.string.empty_screen_weather_wind_layer);
			case CLOUDS:
				return ctx.getString(R.string.empty_screen_weather_clouds_layer);
			case PRESSURE:
				return ctx.getString(R.string.empty_screen_weather_pressure_layer);
		}
		return null;
	}
}
