package net.osmand.plus.plugins.weather;

import static net.osmand.plus.plugins.weather.units.CloudUnit.PERCENT;
import static net.osmand.plus.plugins.weather.units.PrecipitationUnit.INCHES;
import static net.osmand.plus.plugins.weather.units.PrecipitationUnit.MILIMETERS;
import static net.osmand.plus.plugins.weather.units.PressureUnit.HECTOPASCALS;
import static net.osmand.plus.plugins.weather.units.PressureUnit.INCHES_OF_MERCURY;
import static net.osmand.plus.plugins.weather.units.PressureUnit.MILLIMETERS_OF_MERCURY;
import static net.osmand.plus.plugins.weather.units.TemperatureUnit.CELSIUS;
import static net.osmand.plus.plugins.weather.units.TemperatureUnit.FAHRENHEIT;
import static net.osmand.plus.plugins.weather.units.WindUnit.KILOMETERS_PER_HOUR;
import static net.osmand.plus.plugins.weather.units.WindUnit.KNOTS;
import static net.osmand.plus.plugins.weather.units.WindUnit.METERS_PER_SECOND;
import static net.osmand.plus.plugins.weather.units.WindUnit.MILES_PER_HOUR;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.QListDouble;
import net.osmand.core.jni.WeatherDataConverter.Precipitation;
import net.osmand.core.jni.WeatherDataConverter.Pressure;
import net.osmand.core.jni.WeatherDataConverter.Speed;
import net.osmand.core.jni.WeatherDataConverter.Temperature;
import net.osmand.core.jni.WeatherLayer;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.WeatherType;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.ZoomLevelDoubleListHash;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.units.CloudUnit;
import net.osmand.plus.plugins.weather.units.PrecipitationUnit;
import net.osmand.plus.plugins.weather.units.PressureUnit;
import net.osmand.plus.plugins.weather.units.TemperatureUnit;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.plugins.weather.units.WindUnit;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherBand {

	private static final Log log = PlatformUtil.getLog(WeatherBand.class);

	public static final short WEATHER_BAND_NOTHING = -1;
	public static final short WEATHER_BAND_WIND_ANIMATION = 0;
	public static final short WEATHER_BAND_CLOUD = 1;
	public static final short WEATHER_BAND_TEMPERATURE = 2;
	public static final short WEATHER_BAND_PRESSURE = 3;
	public static final short WEATHER_BAND_WIND_SPEED = 4;
	public static final short WEATHER_BAND_PRECIPITATION = 5;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({WEATHER_BAND_NOTHING, WEATHER_BAND_WIND_ANIMATION, WEATHER_BAND_CLOUD, WEATHER_BAND_TEMPERATURE, WEATHER_BAND_PRESSURE, WEATHER_BAND_WIND_SPEED, WEATHER_BAND_PRECIPITATION})
	public @interface WeatherBandType {
	}

	private static final List<TemperatureUnit> TEMP_UNITS = new ArrayList<>(Arrays.asList(CELSIUS, FAHRENHEIT));
	private static final List<PressureUnit> PRESSURE_UNITS = new ArrayList<>(Arrays.asList(HECTOPASCALS, MILLIMETERS_OF_MERCURY, INCHES_OF_MERCURY));
	private static final List<CloudUnit> CLOUD_UNITS = new ArrayList<>(Collections.singletonList(PERCENT));
	private static final List<WindUnit> WIND_UNITS = new ArrayList<>(Arrays.asList(METERS_PER_SECOND, KILOMETERS_PER_HOUR, MILES_PER_HOUR, KNOTS));
	private static final List<PrecipitationUnit> PRECIPITATION_UNITS = new ArrayList<>(Arrays.asList(MILIMETERS, INCHES));

	private static final String INTERNAL_TEMP_UNIT = "°C";
	private static final String INTERNAL_PRESSURE_UNIT = "Pa";
	private static final String INTERNAL_CLOUD_UNIT = "%";
	private static final String INTERNAL_WIND_SPEED_UNIT = "m/s";
	private static final String INTERNAL_PRECIP_UNIT = "kg/(m^2 s)";

	private static final String CLOUD_CONTOUR_STYLE_NAME = "cloud";
	private static final String TEMP_CONTOUR_STYLE_NAME = "temperature";
	private static final String PRESSURE_CONTOUR_STYLE_NAME = "pressure";
	private static final String WIND_SPEED_CONTOUR_STYLE_NAME = "windSpeed";
	private static final String PRECIP_CONTOUR_STYLE_NAME = "precipitation";

	private static final TemperatureUnit DEFAULT_TEMP_UNIT = CELSIUS;
	private static final PressureUnit DEFAULT_PRESSURE_UNIT = HECTOPASCALS;
	private static final CloudUnit DEFAULT_CLOUD_UNIT = PERCENT;
	private static final WindUnit DEFAULT_WIND_SPEED_UNIT = MILES_PER_HOUR;
	private static final PrecipitationUnit DEFAULT_PRECIP_UNIT = MILIMETERS;

	private static final Map<String, String> GENERAL_UNIT_FORMATS = new HashMap<>();
	private static final Map<String, String> PRECISE_UNIT_FORMATS = new HashMap<>();

	static {
		GENERAL_UNIT_FORMATS.put(CloudUnit.PERCENT.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(TemperatureUnit.CELSIUS.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(TemperatureUnit.FAHRENHEIT.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(PressureUnit.HECTOPASCALS.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(PressureUnit.MILLIMETERS_OF_MERCURY.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(PressureUnit.INCHES_OF_MERCURY.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(METERS_PER_SECOND.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(WindUnit.KILOMETERS_PER_HOUR.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(WindUnit.MILES_PER_HOUR.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(WindUnit.KNOTS.getSymbol(), "%d");
		GENERAL_UNIT_FORMATS.put(PrecipitationUnit.MILIMETERS.getSymbol(), "%.1f");
		GENERAL_UNIT_FORMATS.put(PrecipitationUnit.INCHES.getSymbol(), "%.1f");

		PRECISE_UNIT_FORMATS.put(CloudUnit.PERCENT.getSymbol(), "%d");
		PRECISE_UNIT_FORMATS.put(TemperatureUnit.CELSIUS.getSymbol(), "%.1f");
		PRECISE_UNIT_FORMATS.put(TemperatureUnit.FAHRENHEIT.getSymbol(), "%d");
		PRECISE_UNIT_FORMATS.put(PressureUnit.HECTOPASCALS.getSymbol(), "%d");
		PRECISE_UNIT_FORMATS.put(PressureUnit.MILLIMETERS_OF_MERCURY.getSymbol(), "%d");
		PRECISE_UNIT_FORMATS.put(PressureUnit.INCHES_OF_MERCURY.getSymbol(), "%.1f");
		PRECISE_UNIT_FORMATS.put(WindUnit.METERS_PER_SECOND.getSymbol(), "%d");
		PRECISE_UNIT_FORMATS.put(WindUnit.KILOMETERS_PER_HOUR.getSymbol(), "%d");
		PRECISE_UNIT_FORMATS.put(WindUnit.MILES_PER_HOUR.getSymbol(), "%d");
		PRECISE_UNIT_FORMATS.put(WindUnit.KNOTS.getSymbol(), "%d");
		PRECISE_UNIT_FORMATS.put(PrecipitationUnit.MILIMETERS.getSymbol(), "%.1f");
		PRECISE_UNIT_FORMATS.put(PrecipitationUnit.INCHES.getSymbol(), "%.1f");
	}

	private final OsmandApplication app;

	@WeatherBandType
	private short bandIndex;

	public WeatherBand(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public static WeatherBand withWeatherBand(@NonNull OsmandApplication app, @WeatherBandType short bandIndex) {
		WeatherBand band = new WeatherBand(app);
		band.bandIndex = bandIndex;
		return band;
	}

	@WeatherBandType
	public short getBandIndex() {
		return bandIndex;
	}

	@NonNull
	public WeatherSettings getWeatherSettings() {
		return app.getWeatherHelper().getWeatherSettings();
	}

	public boolean isBandVisible() {
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> getWeatherSettings().weatherCloud.get();
			case WEATHER_BAND_TEMPERATURE -> getWeatherSettings().weatherTemp.get();
			case WEATHER_BAND_PRESSURE -> getWeatherSettings().weatherPressure.get();
			case WEATHER_BAND_WIND_SPEED -> getWeatherSettings().weatherWind.get();
			case WEATHER_BAND_PRECIPITATION -> getWeatherSettings().weatherPrecip.get();
			case WEATHER_BAND_WIND_ANIMATION -> getWeatherSettings().weatherWindAnimation.get();
			default -> false;
		};
	}

	public boolean setBandVisible(boolean visible) {
		WeatherSettings settings = getWeatherSettings();
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> settings.weatherCloud.set(visible);
			case WEATHER_BAND_TEMPERATURE -> settings.weatherTemp.set(visible);
			case WEATHER_BAND_PRESSURE -> settings.weatherPressure.set(visible);
			case WEATHER_BAND_WIND_SPEED -> settings.weatherWind.set(visible);
			case WEATHER_BAND_PRECIPITATION -> settings.weatherPrecip.set(visible);
			case WEATHER_BAND_WIND_ANIMATION -> settings.weatherWindAnimation.set(visible);
			default -> false;
		};
	}

	public boolean isForecastBandVisible() {
		WeatherSettings settings = getWeatherSettings();
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> settings.weatherForecastCloud.get();
			case WEATHER_BAND_TEMPERATURE -> settings.weatherForecastTemp.get();
			case WEATHER_BAND_PRESSURE -> settings.weatherForecastPressure.get();
			case WEATHER_BAND_WIND_SPEED -> settings.weatherForecastWind.get();
			case WEATHER_BAND_PRECIPITATION -> settings.weatherForecastPrecip.get();
			case WEATHER_BAND_WIND_ANIMATION -> settings.weatherForecastWindAnimation.get();
			default -> false;
		};
	}

	public boolean setForecastBandVisible(boolean visible) {
		WeatherSettings settings = getWeatherSettings();
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> settings.weatherForecastCloud.set(visible);
			case WEATHER_BAND_TEMPERATURE -> settings.weatherForecastTemp.set(visible);
			case WEATHER_BAND_PRESSURE -> settings.weatherForecastPressure.set(visible);
			case WEATHER_BAND_WIND_SPEED -> settings.weatherForecastWind.set(visible);
			case WEATHER_BAND_PRECIPITATION -> settings.weatherForecastPrecip.set(visible);
			case WEATHER_BAND_WIND_ANIMATION -> settings.weatherForecastWindAnimation.set(visible);
			default -> false;
		};
	}

	@Nullable
	public WeatherUnit getBandUnit() {
		if (bandIndex == WEATHER_BAND_TEMPERATURE) {
			return app.getSettings().getTemperatureUnit();
		} else {
			CommonPreference<? extends WeatherUnit> preference = getBandUnitPref();
			if (preference != null) {
				return preference.get();
			}
		}
		return null;
	}

	@Nullable
	public CommonPreference<? extends WeatherUnit> getBandUnitPref() {
		WeatherSettings settings = getWeatherSettings();
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> settings.weatherCloudUnit;
			case WEATHER_BAND_PRESSURE -> settings.weatherPressureUnit;
			case WEATHER_BAND_WIND_SPEED -> settings.weatherWindUnit;
			case WEATHER_BAND_PRECIPITATION -> settings.weatherPrecipUnit;
			case WEATHER_BAND_WIND_ANIMATION -> settings.weatherWindAnimationUnit;
			default -> null;
		};
	}

	public boolean setBandUnit(@NonNull WeatherUnit unit) {
		if (!getAvailableBandUnits().contains(unit)) {
			return false;
		}
		WeatherSettings settings = getWeatherSettings();
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> settings.weatherCloudUnit.set((CloudUnit) unit);
			case WEATHER_BAND_PRESSURE -> settings.weatherPressureUnit.set((PressureUnit) unit);
			case WEATHER_BAND_WIND_SPEED -> settings.weatherWindUnit.set((WindUnit) unit);
			case WEATHER_BAND_PRECIPITATION -> settings.weatherPrecipUnit.set((PrecipitationUnit) unit);
			case WEATHER_BAND_WIND_ANIMATION -> settings.weatherWindAnimationUnit.set((WindUnit) unit);
			default -> false;
		};
	}

	public boolean isBandUnitAuto() {
		WeatherSettings settings = getWeatherSettings();
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> settings.weatherCloudUnitAuto.get();
			case WEATHER_BAND_TEMPERATURE -> settings.weatherTempUnitAuto.get();
			case WEATHER_BAND_PRESSURE -> settings.weatherPressureUnitAuto.get();
			case WEATHER_BAND_WIND_SPEED -> settings.weatherWindUnitAuto.get();
			case WEATHER_BAND_PRECIPITATION -> settings.weatherPrecipUnitAuto.get();
			case WEATHER_BAND_WIND_ANIMATION -> settings.weatherWindAnimationUnitAuto.get();
			default -> false;
		};
	}

	public void setBandUnitAuto(boolean unitAuto) {
		WeatherSettings settings = getWeatherSettings();
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				settings.weatherCloudUnitAuto.set(unitAuto);
			case WEATHER_BAND_TEMPERATURE:
				settings.weatherTempUnitAuto.set(unitAuto);
			case WEATHER_BAND_PRESSURE:
				settings.weatherPressureUnitAuto.set(unitAuto);
			case WEATHER_BAND_WIND_SPEED:
				settings.weatherWindUnitAuto.set(unitAuto);
			case WEATHER_BAND_PRECIPITATION:
				settings.weatherPrecipUnitAuto.set(unitAuto);
			case WEATHER_BAND_WIND_ANIMATION:
				settings.weatherWindAnimationUnitAuto.set(unitAuto);
		}
	}

	@DrawableRes
	public int getIconId() {
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> R.drawable.ic_action_clouds;
			case WEATHER_BAND_TEMPERATURE -> R.drawable.ic_action_thermometer;
			case WEATHER_BAND_PRESSURE -> R.drawable.ic_action_air_pressure;
			case WEATHER_BAND_WIND_ANIMATION, WEATHER_BAND_WIND_SPEED -> R.drawable.ic_action_wind;
			case WEATHER_BAND_PRECIPITATION -> R.drawable.ic_action_precipitation;
			default -> -1;
		};
	}

	@Nullable
	public String getMeasurementName() {
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> app.getString(R.string.map_settings_weather_cloud);
			case WEATHER_BAND_TEMPERATURE -> app.getString(R.string.map_settings_weather_temp);
			case WEATHER_BAND_PRESSURE -> app.getString(R.string.map_settings_weather_air_pressure);
			case WEATHER_BAND_WIND_SPEED -> app.getString(R.string.map_settings_weather_wind);
			case WEATHER_BAND_PRECIPITATION -> app.getString(R.string.map_settings_weather_precip);
			case WEATHER_BAND_WIND_ANIMATION ->
					app.getString(R.string.map_settings_weather_wind_animation);
			default -> null;
		};
	}

	@Nullable
	public String getBandGeneralUnitFormat() {
		WeatherUnit unit = getBandUnit();
		return unit != null ? GENERAL_UNIT_FORMATS.get(unit.getSymbol()) : null;
	}

	@Nullable
	public String getBandPreciseUnitFormat() {
		WeatherUnit unit = getBandUnit();
		return unit != null ? PRECISE_UNIT_FORMATS.get(unit.getSymbol()) : null;
	}

	@Nullable
	public static WeatherUnit getDefaultBandUnit(@WeatherBandType int bandIndex) {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return DEFAULT_CLOUD_UNIT;
			case WEATHER_BAND_TEMPERATURE:
				return DEFAULT_TEMP_UNIT;
			case WEATHER_BAND_PRESSURE:
				return DEFAULT_PRESSURE_UNIT;
			case WEATHER_BAND_WIND_SPEED:
			case WEATHER_BAND_WIND_ANIMATION:
				return DEFAULT_WIND_SPEED_UNIT;
			case WEATHER_BAND_PRECIPITATION:
				return DEFAULT_PRECIP_UNIT;
			case WEATHER_BAND_NOTHING:
				break;
		}
		return null;
	}

	@Nullable
	public static String getInternalBandUnit(@WeatherBandType int bandIndex) {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return INTERNAL_CLOUD_UNIT;
			case WEATHER_BAND_TEMPERATURE:
				return INTERNAL_TEMP_UNIT;
			case WEATHER_BAND_PRESSURE:
				return INTERNAL_PRESSURE_UNIT;
			case WEATHER_BAND_WIND_SPEED:
			case WEATHER_BAND_WIND_ANIMATION:
				return INTERNAL_WIND_SPEED_UNIT;
			case WEATHER_BAND_PRECIPITATION:
				return INTERNAL_PRECIP_UNIT;
			case WEATHER_BAND_NOTHING:
				break;
		}
		return null;
	}

	@Nullable
	public WeatherUnit getDefaultBandUnit() {
		return getDefaultBandUnit(bandIndex);
	}

	@Nullable
	public String getInternalBandUnit() {
		return getInternalBandUnit(bandIndex);
	}

	@NonNull
	public List<? extends WeatherUnit> getAvailableBandUnits() {
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> CLOUD_UNITS;
			case WEATHER_BAND_TEMPERATURE -> TEMP_UNITS;
			case WEATHER_BAND_PRESSURE -> PRESSURE_UNITS;
			case WEATHER_BAND_WIND_ANIMATION, WEATHER_BAND_WIND_SPEED -> WIND_UNITS;
			case WEATHER_BAND_PRECIPITATION -> PRECIPITATION_UNITS;
			default -> Collections.emptyList();
		};
	}

	public float getBandOpacity() {
		WeatherSettings settings = getWeatherSettings();
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> settings.weatherCloudAlpha.get();
			case WEATHER_BAND_TEMPERATURE -> settings.weatherTempAlpha.get();
			case WEATHER_BAND_PRESSURE -> settings.weatherPressureAlpha.get();
			case WEATHER_BAND_WIND_ANIMATION -> settings.weatherWindAnimationAlpha.get();
			case WEATHER_BAND_WIND_SPEED -> settings.weatherWindAlpha.get();
			case WEATHER_BAND_PRECIPITATION -> settings.weatherPrecipAlpha.get();
			default -> 0.0f;
		};
	}

	@Nullable
	public CommonPreference<Float> getAlphaPreference() {
		WeatherSettings settings = getWeatherSettings();
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> settings.weatherCloudAlpha;
			case WEATHER_BAND_TEMPERATURE -> settings.weatherTempAlpha;
			case WEATHER_BAND_PRESSURE -> settings.weatherPressureAlpha;
			case WEATHER_BAND_WIND_ANIMATION -> settings.weatherWindAnimationAlpha;
			case WEATHER_BAND_WIND_SPEED -> settings.weatherWindAlpha;
			case WEATHER_BAND_PRECIPITATION -> settings.weatherPrecipAlpha;
			default -> null;
		};
	}

	@Nullable
	public String getColorFilePath() {
		String folder = IndexConstants.CLR_PALETTE_DIR;
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> folder + "weather_cloud.txt";
			case WEATHER_BAND_TEMPERATURE -> folder + "weather_temperature.txt";
			case WEATHER_BAND_PRESSURE -> folder + "weather_pressure.txt";
			case WEATHER_BAND_WIND_ANIMATION -> folder + "weather_wind_animation.txt";
			case WEATHER_BAND_WIND_SPEED -> folder + "weather_wind.txt";
			case WEATHER_BAND_PRECIPITATION -> folder + "weather_precip.txt";
			default -> null;
		};
	}

	@Nullable
	public String getContourStyleName() {
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> CLOUD_CONTOUR_STYLE_NAME;
			case WEATHER_BAND_TEMPERATURE -> TEMP_CONTOUR_STYLE_NAME;
			case WEATHER_BAND_PRESSURE -> PRESSURE_CONTOUR_STYLE_NAME;
			case WEATHER_BAND_WIND_SPEED, WEATHER_BAND_WIND_ANIMATION -> WIND_SPEED_CONTOUR_STYLE_NAME;
			case WEATHER_BAND_PRECIPITATION -> PRECIP_CONTOUR_STYLE_NAME;
			default -> null;
		};
	}

	@Nullable
	public String getBandType() {
		return switch (bandIndex) {
			case WEATHER_BAND_CLOUD -> "cloud";
			case WEATHER_BAND_TEMPERATURE -> "temp";
			case WEATHER_BAND_PRESSURE -> "pressure";
			case WEATHER_BAND_WIND_SPEED -> "wind_speed";
			case WEATHER_BAND_PRECIPITATION -> "precip";
			case WEATHER_BAND_WIND_ANIMATION -> "wind_speed_animation";
			default -> null;
		};
	}

	@NonNull
	public ZoomLevelDoubleListHash getContourLevels(@NonNull WeatherTileResourcesManager weatherResourcesManager,
	                                                @Nullable MapPresentationEnvironment mapPresentationEnvironment) {
		ZoomLevelDoubleListHash contourLevels = new ZoomLevelDoubleListHash();
		if (mapPresentationEnvironment == null) {
			return contourLevels;
		}
		String type = getBandType();
		if (Algorithms.isEmpty(type)) {
			return contourLevels;
		}
		String unit = getBandUnit().getSymbol();
		String internalUnit = getInternalBandUnit();
		int minZoom = weatherResourcesManager.getMinTileZoom(WeatherType.Contour, WeatherLayer.High).swigValue();
		int maxZoom = weatherResourcesManager.getMaxTileZoom(WeatherType.Contour, WeatherLayer.High).swigValue();
		int zoom = minZoom;
		while (zoom <= maxZoom) {
			String qUnit = unit.replace("°", "");
			String result = mapPresentationEnvironment.getWeatherContourLevels(type + "_" + qUnit, ZoomLevel.swigToEnum(zoom));
			if (!Algorithms.isEmpty(result)) {
				QListDouble levels = new QListDouble();
				String[] params = result.split(",");
				for (String p : params) {
					double level;
					try {
						level = Double.parseDouble(p);
					} catch (NumberFormatException e) {
						continue;
					}
					if (!unit.equals(internalUnit)) {
						switch (bandIndex) {
							case WEATHER_BAND_CLOUD:
								// Assume cloud in % only
								break;
							case WEATHER_BAND_TEMPERATURE:
								Temperature.Unit temperatureUnit = Temperature.unitFromString(unit);
								Temperature.Unit temperatureInternalUnit = Temperature.unitFromString(internalUnit);
								Temperature temperature = new Temperature(temperatureUnit, level);
								level = temperature.toUnit(temperatureInternalUnit);
								break;
							case WEATHER_BAND_PRESSURE:
								Pressure.Unit pressureUnit = Pressure.unitFromString(unit);
								Pressure.Unit pressureInternalUnit = Pressure.unitFromString(internalUnit);
								Pressure pressure = new Pressure(pressureUnit, level);
								level = pressure.toUnit(pressureInternalUnit);
								break;
							case WEATHER_BAND_WIND_ANIMATION:
							case WEATHER_BAND_WIND_SPEED:
								Speed.Unit speedUnit = Speed.unitFromString(unit);
								Speed.Unit speedInternalUnit = Speed.unitFromString(internalUnit);
								Speed speed = new Speed(speedUnit, level);
								level = speed.toUnit(speedInternalUnit);
								break;
							case WEATHER_BAND_PRECIPITATION:
								Precipitation.Unit precipitationUnit = Precipitation.unitFromString(unit);
								Precipitation.Unit precipitationInternalUnit = Precipitation.unitFromString(internalUnit);
								Precipitation precipitation = new Precipitation(precipitationUnit, level);
								level = precipitation.toUnit(precipitationInternalUnit);
								break;
						}
					}
					levels.add(level);
				}
				contourLevels.set(ZoomLevel.swigToEnum(zoom), levels);
			}
			zoom++;
		}
		return contourLevels;
	}
}