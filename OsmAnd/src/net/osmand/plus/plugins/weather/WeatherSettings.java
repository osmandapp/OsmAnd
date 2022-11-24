package net.osmand.plus.plugins.weather;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.weather.units.CloudUnit;
import net.osmand.plus.plugins.weather.units.PrecipitationUnit;
import net.osmand.plus.plugins.weather.units.PressureUnit;
import net.osmand.plus.plugins.weather.units.TemperatureUnit;
import net.osmand.plus.plugins.weather.units.WindUnit;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

public class WeatherSettings {

	public static final String WEATHER_TEMP_CONTOUR_LINES_ATTR = "weatherTempContours";
	public static final String WEATHER_PRESSURE_CONTOURS_LINES_ATTR = "weatherPressureContours";
	public static final String WEATHER_NONE_CONTOURS_LINES_VALUE = "none";

	public static int DEFAULT_TRANSPARENCY = 50;

	public final OsmandPreference<Boolean> WX_ENABLED;
	public final ListStringPreference WX_ENABLED_LAYERS;
	public final ListStringPreference WX_LAYERS_TRANSPARENCY;

	public final OsmandPreference<Boolean> WX_CONTOURS_ENABLED;
	public final OsmandPreference<Integer> WX_CONTOURS_TRANSPARENCY;
	public final EnumStringPreference<WeatherInfoType> WX_CONTOURS_TYPE;

	public final EnumStringPreference<WindUnit> weatherWindUnit;
	public final EnumStringPreference<CloudUnit> weatherCloudUnit;
	public final EnumStringPreference<TemperatureUnit> weatherTempUnit;
	public final EnumStringPreference<PressureUnit> weatherPressureUnit;
	public final EnumStringPreference<PrecipitationUnit> weatherPrecipUnit;

	public final CommonPreference<Boolean> weatherTemp;
	public final CommonPreference<Boolean> weatherWind;
	public final CommonPreference<Boolean> weatherCloud;
	public final CommonPreference<Boolean> weatherPrecip;
	public final CommonPreference<Boolean> weatherPressure;

	public final CommonPreference<Float> weatherTempAlpha;
	public final CommonPreference<Float> weatherWindAlpha;
	public final CommonPreference<Float> weatherCloudAlpha;
	public final CommonPreference<Float> weatherPrecipAlpha;
	public final CommonPreference<Float> weatherPressureAlpha;

	public final CommonPreference<Boolean> weatherTempUnitAuto;
	public final CommonPreference<Boolean> weatherWindUnitAuto;
	public final CommonPreference<Boolean> weatherCloudUnitAuto;
	public final CommonPreference<Boolean> weatherPrecipUnitAuto;
	public final CommonPreference<Boolean> weatherPressureUnitAuto;

	private OsmandApplication app;
	private OsmandSettings settings;

	public WeatherSettings(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();

		WX_ENABLED = settings.registerBooleanPreference("map_setting_wx_enabled", true).makeProfile();
		WX_ENABLED_LAYERS = (ListStringPreference) settings.registerStringListPreference("map_setting_wx_enabled_layers", null, ",").makeProfile();
		WX_LAYERS_TRANSPARENCY = (ListStringPreference) settings.registerStringListPreference("map_setting_wx_layers_transparency", null, ",").makeProfile();

		WX_CONTOURS_ENABLED = settings.registerBooleanPreference("map_setting_wx_contours_enabled", true).makeProfile();
		WX_CONTOURS_TRANSPARENCY = settings.registerIntPreference("map_setting_wx_contours_transparency", DEFAULT_TRANSPARENCY).makeProfile();
		WX_CONTOURS_TYPE = (EnumStringPreference<WeatherInfoType>) settings.registerEnumStringPreference(
				"map_setting_wx_contours_type", WeatherInfoType.TEMPERATURE, WeatherInfoType.values(), WeatherInfoType.class).makeProfile();

		weatherTempUnit = (EnumStringPreference<TemperatureUnit>) settings.registerEnumStringPreference(
				"map_settings_weather_temp", TemperatureUnit.CELSIUS, TemperatureUnit.values(), TemperatureUnit.class).makeProfile();
		weatherPressureUnit = (EnumStringPreference<PressureUnit>) settings.registerEnumStringPreference(
				"map_settings_weather_pressure", PressureUnit.MILLIMETERS_OF_MERCURY, PressureUnit.values(), PressureUnit.class).makeProfile();
		weatherWindUnit = (EnumStringPreference<WindUnit>) settings.registerEnumStringPreference(
				"map_settings_weather_wind", WindUnit.METERS_PER_SECOND, WindUnit.values(), WindUnit.class).makeProfile();
		weatherCloudUnit = (EnumStringPreference<CloudUnit>) settings.registerEnumStringPreference(
				"map_settings_weather_cloud", CloudUnit.PERCENT, CloudUnit.values(), CloudUnit.class).makeProfile();
		weatherPrecipUnit = (EnumStringPreference<PrecipitationUnit>) settings.registerEnumStringPreference(
				"map_settings_weather_precip", PrecipitationUnit.MILIMETERS, PrecipitationUnit.values(), PrecipitationUnit.class).makeProfile();

		weatherTemp = settings.registerBooleanPreference("weatherTemp", false).makeProfile();
		weatherTempUnitAuto = settings.registerBooleanPreference("weatherTempUnitAuto", true).makeProfile();
		weatherTempAlpha = settings.registerFloatPreference("weatherTempAlpha", 0.5f).makeProfile();
		weatherPressure = settings.registerBooleanPreference("weatherPressure", false).makeProfile();
		weatherPressureUnitAuto = settings.registerBooleanPreference("weatherPressureUnitAuto", true).makeProfile();
		weatherPressureAlpha = settings.registerFloatPreference("weatherPressureAlpha", 0.6f).makeProfile();
		weatherWind = settings.registerBooleanPreference("weatherWind", false).makeProfile();
		weatherWindUnitAuto = settings.registerBooleanPreference("weatherWindUnitAuto", true).makeProfile();
		weatherWindAlpha = settings.registerFloatPreference("weatherWindToolbarAlpha", 0.6f).makeProfile();
		weatherCloud = settings.registerBooleanPreference("weatherCloud", false).makeProfile();
		weatherCloudUnitAuto = settings.registerBooleanPreference("weatherCloudUnitAuto", true).makeProfile();
		weatherCloudAlpha = settings.registerFloatPreference("weatherCloudAlpha", 0.5f).makeProfile();
		weatherPrecip = settings.registerBooleanPreference("weatherPrecip", false).makeProfile();
		weatherPrecipUnitAuto = settings.registerBooleanPreference("weatherPrecipUnitAuto", true).makeProfile();
		weatherPrecipAlpha = settings.registerFloatPreference("weatherPrecipAlpha", 0.7f).makeProfile();
	}
}
