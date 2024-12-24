package net.osmand.plus.plugins.weather;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.weather.enums.WeatherSource;
import net.osmand.plus.plugins.weather.units.CloudUnit;
import net.osmand.plus.plugins.weather.units.PrecipitationUnit;
import net.osmand.plus.plugins.weather.units.PressureUnit;
import net.osmand.plus.plugins.weather.units.TemperatureUnit;
import net.osmand.plus.plugins.weather.units.WindUnit;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

public class WeatherSettings {

	public static final String WEATHER_TEMP_CONTOUR_LINES_ATTR = "weatherTempContours";
	public static final String WEATHER_PRESSURE_CONTOURS_LINES_ATTR = "weatherPressureContours";
	public static final String WEATHER_CLOUD_CONTOURS_LINES_ATTR = "weatherCloudContours";
	public static final String WEATHER_WIND_CONTOURS_LINES_ATTR = "weatherWindSpeedContours";
	public static final String WEATHER_PRECIPITATION_CONTOURS_LINES_ATTR = "weatherPrecipitationContours";
	public static final String WEATHER_NONE_CONTOURS_LINES_VALUE = "none";

	public static int DEFAULT_TRANSPARENCY = 50;

	public final OsmandPreference<Boolean> weatherEnabled;

	public final OsmandPreference<String> weatherSource;
	public final OsmandPreference<Boolean> weatherContoursEnabled;
	public final OsmandPreference<Integer> weatherContoursTransparency;
	public final EnumStringPreference<WeatherContour> weatherContoursType;
	public final EnumStringPreference<WeatherContour> weatherForecastContoursType;

	public final EnumStringPreference<WindUnit> weatherWindUnit;
	public final EnumStringPreference<WindUnit> weatherWindAnimationUnit;
	public final EnumStringPreference<CloudUnit> weatherCloudUnit;
	public final EnumStringPreference<TemperatureUnit> weatherTempUnit;
	public final EnumStringPreference<PressureUnit> weatherPressureUnit;
	public final EnumStringPreference<PrecipitationUnit> weatherPrecipUnit;

	public final CommonPreference<Boolean> weatherTemp;
	public final CommonPreference<Boolean> weatherWind;
	public final CommonPreference<Boolean> weatherWindAnimation;
	public final CommonPreference<Boolean> weatherCloud;
	public final CommonPreference<Boolean> weatherPrecip;
	public final CommonPreference<Boolean> weatherPressure;

	public final CommonPreference<Boolean> weatherForecastTemp;
	public final CommonPreference<Boolean> weatherForecastWind;
	public final CommonPreference<Boolean> weatherForecastWindAnimation;
	public final CommonPreference<Boolean> weatherForecastCloud;
	public final CommonPreference<Boolean> weatherForecastPrecip;
	public final CommonPreference<Boolean> weatherForecastPressure;

	public final CommonPreference<Float> weatherTempAlpha;
	public final CommonPreference<Float> weatherWindAlpha;
	public final CommonPreference<Float> weatherWindAnimationAlpha;
	public final CommonPreference<Float> weatherCloudAlpha;
	public final CommonPreference<Float> weatherPrecipAlpha;
	public final CommonPreference<Float> weatherPressureAlpha;

	public final CommonPreference<Boolean> weatherTempUnitAuto;
	public final CommonPreference<Boolean> weatherWindUnitAuto;
	public final CommonPreference<Boolean> weatherWindAnimationUnitAuto;
	public final CommonPreference<Boolean> weatherCloudUnitAuto;
	public final CommonPreference<Boolean> weatherPrecipUnitAuto;
	public final CommonPreference<Boolean> weatherPressureUnitAuto;

	public WeatherSettings(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();

		weatherEnabled = settings.registerBooleanPreference("weatherEnabled", true).makeProfile();

		weatherSource = settings.registerStringPreference("weatherSource", WeatherSource.Companion.getDefaultSource().getSettingValue()).makeProfile();
		weatherContoursEnabled = settings.registerBooleanPreference("weatherContoursEnabled", true).makeProfile();
		weatherContoursTransparency = settings.registerIntPreference("weatherContoursTransparency", DEFAULT_TRANSPARENCY).makeProfile();
		weatherContoursType = (EnumStringPreference<WeatherContour>) settings.registerEnumStringPreference(
				"weatherContoursType", WeatherContour.TEMPERATURE, WeatherContour.values(), WeatherContour.class).makeProfile();
		weatherForecastContoursType = (EnumStringPreference<WeatherContour>) settings.registerEnumStringPreference(
				"weatherForecastContoursType", null, WeatherContour.values(), WeatherContour.class).makeProfile();

		weatherTemp = settings.registerBooleanPreference("weatherTemp", false).makeProfile();
		weatherPressure = settings.registerBooleanPreference("weatherPressure", false).makeProfile();
		weatherWind = settings.registerBooleanPreference("weatherWind", false).makeProfile();
		weatherWindAnimation = settings.registerBooleanPreference("weatherWindAnimation", false).makeProfile();
		weatherCloud = settings.registerBooleanPreference("weatherCloud", false).makeProfile();
		weatherPrecip = settings.registerBooleanPreference("weatherPrecip", false).makeProfile();

		weatherForecastTemp = settings.registerBooleanPreference("weatherForecastTemp", false).makeProfile();
		weatherForecastPressure = settings.registerBooleanPreference("weatherForecastPressure", false).makeProfile();
		weatherForecastWind = settings.registerBooleanPreference("weatherForecastWind", false).makeProfile();
		weatherForecastWindAnimation = settings.registerBooleanPreference("weatherForecastWindAnimation", false).makeProfile();
		weatherForecastCloud = settings.registerBooleanPreference("weatherForecastCloud", false).makeProfile();
		weatherForecastPrecip = settings.registerBooleanPreference("weatherForecastPrecip", false).makeProfile();

		weatherTempUnit = (EnumStringPreference<TemperatureUnit>) settings.registerEnumStringPreference(
				"weatherTempUnit", TemperatureUnit.CELSIUS, TemperatureUnit.values(), TemperatureUnit.class).makeProfile();
		weatherPressureUnit = (EnumStringPreference<PressureUnit>) settings.registerEnumStringPreference(
				"weatherPressureUnit", PressureUnit.MILLIMETERS_OF_MERCURY, PressureUnit.values(), PressureUnit.class).makeProfile();
		weatherWindUnit = (EnumStringPreference<WindUnit>) settings.registerEnumStringPreference(
				"weatherWindUnit", WindUnit.METERS_PER_SECOND, WindUnit.values(), WindUnit.class).makeProfile();
		weatherWindAnimationUnit = (EnumStringPreference<WindUnit>) settings.registerEnumStringPreference(
				"weatherWindAnimationUnit", WindUnit.METERS_PER_SECOND, WindUnit.values(), WindUnit.class).makeProfile();
		weatherCloudUnit = (EnumStringPreference<CloudUnit>) settings.registerEnumStringPreference(
				"weatherCloudUnit", CloudUnit.PERCENT, CloudUnit.values(), CloudUnit.class).makeProfile();
		weatherPrecipUnit = (EnumStringPreference<PrecipitationUnit>) settings.registerEnumStringPreference(
				"weatherPrecipUnit", PrecipitationUnit.MILIMETERS, PrecipitationUnit.values(), PrecipitationUnit.class).makeProfile();

		weatherTempAlpha = settings.registerFloatPreference("weatherTempAlpha", 0.5f).makeProfile();
		weatherPressureAlpha = settings.registerFloatPreference("weatherPressureAlpha", 0.6f).makeProfile();
		weatherWindAlpha = settings.registerFloatPreference("weatherWindToolbarAlpha", 0.6f).makeProfile();
		weatherWindAnimationAlpha = settings.registerFloatPreference("weatherWindAnimationToolbarAlpha", 0.6f).makeProfile();
		weatherCloudAlpha = settings.registerFloatPreference("weatherCloudAlpha", 0.5f).makeProfile();
		weatherPrecipAlpha = settings.registerFloatPreference("weatherPrecipAlpha", 0.7f).makeProfile();

		weatherTempUnitAuto = settings.registerBooleanPreference("weatherTempUnitAuto", true).makeProfile();
		weatherPressureUnitAuto = settings.registerBooleanPreference("weatherPressureUnitAuto", true).makeProfile();
		weatherWindUnitAuto = settings.registerBooleanPreference("weatherWindUnitAuto", true).makeProfile();
		weatherWindAnimationUnitAuto = settings.registerBooleanPreference("weatherWindAnimationUnitAuto", true).makeProfile();
		weatherCloudUnitAuto = settings.registerBooleanPreference("weatherCloudUnitAuto", true).makeProfile();
		weatherPrecipUnitAuto = settings.registerBooleanPreference("weatherPrecipUnitAuto", true).makeProfile();
	}
}
