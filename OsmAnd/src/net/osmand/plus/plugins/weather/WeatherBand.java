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
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return getWeatherSettings().weatherCloud.get();
			case WEATHER_BAND_TEMPERATURE:
				return getWeatherSettings().weatherTemp.get();
			case WEATHER_BAND_PRESSURE:
				return getWeatherSettings().weatherPressure.get();
			case WEATHER_BAND_WIND_SPEED:
				return getWeatherSettings().weatherWind.get();
			case WEATHER_BAND_PRECIPITATION:
				return getWeatherSettings().weatherPrecip.get();
			case WEATHER_BAND_WIND_ANIMATION:
				return getWeatherSettings().weatherWindAnimation.get();
		}
		return false;
	}

	public boolean setBandVisible(boolean visible) {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return getWeatherSettings().weatherCloud.set(visible);
			case WEATHER_BAND_TEMPERATURE:
				return getWeatherSettings().weatherTemp.set(visible);
			case WEATHER_BAND_PRESSURE:
				return getWeatherSettings().weatherPressure.set(visible);
			case WEATHER_BAND_WIND_SPEED:
				return getWeatherSettings().weatherWind.set(visible);
			case WEATHER_BAND_PRECIPITATION:
				return getWeatherSettings().weatherPrecip.set(visible);
			case WEATHER_BAND_WIND_ANIMATION:
				return getWeatherSettings().weatherWindAnimation.set(visible);
		}
		return false;
	}

	public boolean isForecastBandVisible() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return getWeatherSettings().weatherForecastCloud.get();
			case WEATHER_BAND_TEMPERATURE:
				return getWeatherSettings().weatherForecastTemp.get();
			case WEATHER_BAND_PRESSURE:
				return getWeatherSettings().weatherForecastPressure.get();
			case WEATHER_BAND_WIND_SPEED:
				return getWeatherSettings().weatherForecastWind.get();
			case WEATHER_BAND_PRECIPITATION:
				return getWeatherSettings().weatherForecastPrecip.get();
			case WEATHER_BAND_WIND_ANIMATION:
				return getWeatherSettings().weatherForecastWindAnimation.get();
		}
		return false;
	}

	public boolean setForecastBandVisible(boolean visible) {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return getWeatherSettings().weatherForecastCloud.set(visible);
			case WEATHER_BAND_TEMPERATURE:
				return getWeatherSettings().weatherForecastTemp.set(visible);
			case WEATHER_BAND_PRESSURE:
				return getWeatherSettings().weatherForecastPressure.set(visible);
			case WEATHER_BAND_WIND_SPEED:
				return getWeatherSettings().weatherForecastWind.set(visible);
			case WEATHER_BAND_PRECIPITATION:
				return getWeatherSettings().weatherForecastPrecip.set(visible);
			case WEATHER_BAND_WIND_ANIMATION:
				return getWeatherSettings().weatherForecastWindAnimation.set(visible);
		}
		return false;
	}

	@Nullable
	public WeatherUnit getBandUnit() {
		CommonPreference<? extends WeatherUnit> preference = getBandUnitPref();
		if (preference != null) {
			return preference.get();
		}
		return null;
	}

	@Nullable
	public CommonPreference<? extends WeatherUnit> getBandUnitPref() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return getWeatherSettings().weatherCloudUnit;
			case WEATHER_BAND_TEMPERATURE:
				return getWeatherSettings().weatherTempUnit;
			case WEATHER_BAND_PRESSURE:
				return getWeatherSettings().weatherPressureUnit;
			case WEATHER_BAND_WIND_SPEED:
				return getWeatherSettings().weatherWindUnit;
			case WEATHER_BAND_PRECIPITATION:
				return getWeatherSettings().weatherPrecipUnit;
			case WEATHER_BAND_WIND_ANIMATION:
				return getWeatherSettings().weatherWindAnimationUnit;
		}
		return null;
	}

	public boolean setBandUnit(@NonNull WeatherUnit unit) {
		if (!getAvailableBandUnits().contains(unit)) {
			return false;
		}
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return getWeatherSettings().weatherCloudUnit.set((CloudUnit) unit);
			case WEATHER_BAND_TEMPERATURE:
				return getWeatherSettings().weatherTempUnit.set((TemperatureUnit) unit);
			case WEATHER_BAND_PRESSURE:
				return getWeatherSettings().weatherPressureUnit.set((PressureUnit) unit);
			case WEATHER_BAND_WIND_SPEED:
				return getWeatherSettings().weatherWindUnit.set((WindUnit) unit);
			case WEATHER_BAND_PRECIPITATION:
				return getWeatherSettings().weatherPrecipUnit.set((PrecipitationUnit) unit);
			case WEATHER_BAND_WIND_ANIMATION:
				return getWeatherSettings().weatherWindAnimationUnit.set((WindUnit) unit);
		}
		return false;
	}

	public boolean isBandUnitAuto() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return getWeatherSettings().weatherCloudUnitAuto.get();
			case WEATHER_BAND_TEMPERATURE:
				return getWeatherSettings().weatherTempUnitAuto.get();
			case WEATHER_BAND_PRESSURE:
				return getWeatherSettings().weatherPressureUnitAuto.get();
			case WEATHER_BAND_WIND_SPEED:
				return getWeatherSettings().weatherWindUnitAuto.get();
			case WEATHER_BAND_PRECIPITATION:
				return getWeatherSettings().weatherPrecipUnitAuto.get();
			case WEATHER_BAND_WIND_ANIMATION:
				return getWeatherSettings().weatherWindAnimationUnitAuto.get();
		}
		return false;
	}

	public void setBandUnitAuto(boolean unitAuto) {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				getWeatherSettings().weatherCloudUnitAuto.set(unitAuto);
			case WEATHER_BAND_TEMPERATURE:
				getWeatherSettings().weatherTempUnitAuto.set(unitAuto);
			case WEATHER_BAND_PRESSURE:
				getWeatherSettings().weatherPressureUnitAuto.set(unitAuto);
			case WEATHER_BAND_WIND_SPEED:
				getWeatherSettings().weatherWindUnitAuto.set(unitAuto);
			case WEATHER_BAND_PRECIPITATION:
				getWeatherSettings().weatherPrecipUnitAuto.set(unitAuto);
			case WEATHER_BAND_WIND_ANIMATION:
				getWeatherSettings().weatherWindAnimationUnitAuto.set(unitAuto);
		}
	}

	@DrawableRes
	public int getIconId() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return R.drawable.ic_action_clouds;
			case WEATHER_BAND_TEMPERATURE:
				return R.drawable.ic_action_thermometer;
			case WEATHER_BAND_PRESSURE:
				return R.drawable.ic_action_air_pressure;
			case WEATHER_BAND_WIND_ANIMATION:
			case WEATHER_BAND_WIND_SPEED:
				return R.drawable.ic_action_wind;
			case WEATHER_BAND_PRECIPITATION:
				return R.drawable.ic_action_precipitation;
			default:
				return -1;
		}
	}

	@Nullable
	public String getMeasurementName() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return app.getString(R.string.map_settings_weather_cloud);
			case WEATHER_BAND_TEMPERATURE:
				return app.getString(R.string.map_settings_weather_temp);
			case WEATHER_BAND_PRESSURE:
				return app.getString(R.string.map_settings_weather_air_pressure);
			case WEATHER_BAND_WIND_SPEED:
				return app.getString(R.string.map_settings_weather_wind);
			case WEATHER_BAND_PRECIPITATION:
				return app.getString(R.string.map_settings_weather_precip);
			case WEATHER_BAND_WIND_ANIMATION:
				return app.getString(R.string.map_settings_weather_wind_animation);
		}
		return null;
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
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return CLOUD_UNITS;
			case WEATHER_BAND_TEMPERATURE:
				return TEMP_UNITS;
			case WEATHER_BAND_PRESSURE:
				return PRESSURE_UNITS;
			case WEATHER_BAND_WIND_ANIMATION:
			case WEATHER_BAND_WIND_SPEED:
				return WIND_UNITS;
			case WEATHER_BAND_PRECIPITATION:
				return PRECIPITATION_UNITS;
			default:
				return Collections.emptyList();
		}
	}

	public float getBandOpacity() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return getWeatherSettings().weatherCloudAlpha.get();
			case WEATHER_BAND_TEMPERATURE:
				return getWeatherSettings().weatherTempAlpha.get();
			case WEATHER_BAND_PRESSURE:
				return getWeatherSettings().weatherPressureAlpha.get();
			case WEATHER_BAND_WIND_ANIMATION:
				return getWeatherSettings().weatherWindAnimationAlpha.get();
			case WEATHER_BAND_WIND_SPEED:
				return getWeatherSettings().weatherWindAlpha.get();
			case WEATHER_BAND_PRECIPITATION:
				return getWeatherSettings().weatherPrecipAlpha.get();
		}
		return 0.0f;
	}

	@Nullable
	public CommonPreference<Float> getAlphaPreference() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return getWeatherSettings().weatherCloudAlpha;
			case WEATHER_BAND_TEMPERATURE:
				return getWeatherSettings().weatherTempAlpha;
			case WEATHER_BAND_PRESSURE:
				return getWeatherSettings().weatherPressureAlpha;
			case WEATHER_BAND_WIND_ANIMATION:
				return getWeatherSettings().weatherWindAnimationAlpha;
			case WEATHER_BAND_WIND_SPEED:
				return getWeatherSettings().weatherWindAlpha;
			case WEATHER_BAND_PRECIPITATION:
				return getWeatherSettings().weatherPrecipAlpha;
		}
		return null;
	}

	@Nullable
	public String getColorFilePath() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return IndexConstants.CLR_PALETTE_DIR + "weather_cloud.txt";
			case WEATHER_BAND_TEMPERATURE:
				return IndexConstants.CLR_PALETTE_DIR + "weather_temperature.txt";
			case WEATHER_BAND_PRESSURE:
				return IndexConstants.CLR_PALETTE_DIR + "weather_pressure.txt";
			case WEATHER_BAND_WIND_ANIMATION:
				return IndexConstants.CLR_PALETTE_DIR + "weather_wind_animation.txt";
			case WEATHER_BAND_WIND_SPEED:
				return IndexConstants.CLR_PALETTE_DIR + "weather_wind.txt";
			case WEATHER_BAND_PRECIPITATION:
				return IndexConstants.CLR_PALETTE_DIR + "weather_precip.txt";
		}
		return null;
	}

	@Nullable
	public String getContourStyleName() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return CLOUD_CONTOUR_STYLE_NAME;
			case WEATHER_BAND_TEMPERATURE:
				return TEMP_CONTOUR_STYLE_NAME;
			case WEATHER_BAND_PRESSURE:
				return PRESSURE_CONTOUR_STYLE_NAME;
			case WEATHER_BAND_WIND_SPEED:
			case WEATHER_BAND_WIND_ANIMATION:
				return WIND_SPEED_CONTOUR_STYLE_NAME;
			case WEATHER_BAND_PRECIPITATION:
				return PRECIP_CONTOUR_STYLE_NAME;
			default:
				return null;
		}
	}

	@Nullable
	public String getBandType() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return "cloud";
			case WEATHER_BAND_TEMPERATURE:
				return "temp";
			case WEATHER_BAND_PRESSURE:
				return "pressure";
			case WEATHER_BAND_WIND_SPEED:
				return "wind_speed";
			case WEATHER_BAND_PRECIPITATION:
				return "precip";
			case WEATHER_BAND_WIND_ANIMATION:
				return "wind_speed_animation";
		}
		return null;
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