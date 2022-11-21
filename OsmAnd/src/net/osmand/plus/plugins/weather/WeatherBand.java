package net.osmand.plus.plugins.weather;

import static net.osmand.IndexConstants.WEATHER_INDEX_DIR;
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
import static net.osmand.plus.plugins.weather.units.WindUnit.MILES_PER_HOUR;

import android.graphics.drawable.Drawable;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.units.CloudUnit;
import net.osmand.plus.plugins.weather.units.PrecipitationUnit;
import net.osmand.plus.plugins.weather.units.PressureUnit;
import net.osmand.plus.plugins.weather.units.TemperatureUnit;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.plugins.weather.units.WindUnit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherBand {

	public static final short CONTOUR_VALUE_LEVELS = 0;
	public static final short CONTOUR_VALUE_TYPES = 1;

	public static final short WEATHER_BAND_UNDEFINED = 0;
	public static final short WEATHER_BAND_CLOUD = 1;
	public static final short WEATHER_BAND_TEMPERATURE = 2;
	public static final short WEATHER_BAND_PRESSURE = 3;
	public static final short WEATHER_BAND_WIND_SPEED = 4;
	public static final short WEATHER_BAND_PRECIPITATION = 5;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({CONTOUR_VALUE_LEVELS, CONTOUR_VALUE_TYPES})
	public @interface ContourValueType {
	}

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({WEATHER_BAND_UNDEFINED, WEATHER_BAND_CLOUD, WEATHER_BAND_TEMPERATURE, WEATHER_BAND_PRESSURE, WEATHER_BAND_WIND_SPEED, WEATHER_BAND_PRECIPITATION})
	public @interface WeatherBandType {
	}

	private static final List<TemperatureUnit> kTempUnits = new ArrayList<>(Arrays.asList(CELSIUS, FAHRENHEIT));
	private static final List<PressureUnit> kPressureUnits = new ArrayList<>(Arrays.asList(HECTOPASCALS, MILLIMETERS_OF_MERCURY, INCHES_OF_MERCURY));
	private static final List<CloudUnit> kCloudUnits = new ArrayList<>(Collections.singletonList(PERCENT));
	private static final List<WindUnit> kWindSpeedUnits = new ArrayList<>(Arrays.asList(MILES_PER_HOUR, KILOMETERS_PER_HOUR, MILES_PER_HOUR, KNOTS));
	private static final List<PrecipitationUnit> kPrecipUnits = new ArrayList<>(Arrays.asList(MILIMETERS, INCHES));

	private static String kInternalTempUnit = "°C";
	private static String kInternalPressureUnit = "Pa";
	private static String kInternalCloudUnit = "%";
	private static String kInternalWindSpeedUnit = "m/s";
	private static String kInternalPrecipUnit = "kg/(m^2 s)";

	private static String kCloudContourStyleName = "cloud";
	private static String kTempContourStyleName = "temperature";
	private static String kPressureContourStyleName = "pressure";
	private static String kWindSpeedContourStyleName = "windSpeed";
	private static String kPrecipContourStyleName = "precipitation";

	private static TemperatureUnit kDefaultTempUnit = CELSIUS;
	private static PressureUnit kDefaultPressureUnit = HECTOPASCALS;
	private static CloudUnit kDefaultCloudUnit = PERCENT;
	private static WindUnit kDefaultWindSpeedUnit = MILES_PER_HOUR;
	private static PrecipitationUnit kDefaultPrecipUnit = MILIMETERS;

	private static Map<String, String> kGeneralUnitFormats = new HashMap<>();
	private static Map<String, String> kPreciseUnitFormats = new HashMap<>();

	static {
		kGeneralUnitFormats.put(CloudUnit.PERCENT.getSymbol(), "%d");
		kGeneralUnitFormats.put(TemperatureUnit.CELSIUS.getSymbol(), "%d");
		kGeneralUnitFormats.put(TemperatureUnit.FAHRENHEIT.getSymbol(), "%d");
		kGeneralUnitFormats.put(PressureUnit.HECTOPASCALS.getSymbol(), "%d");
		kGeneralUnitFormats.put(PressureUnit.MILLIMETERS_OF_MERCURY.getSymbol(), "%d");
		kGeneralUnitFormats.put(PressureUnit.INCHES_OF_MERCURY.getSymbol(), "%d");
		kGeneralUnitFormats.put(WindUnit.METERS_PER_SECOND.getSymbol(), "%d");
		kGeneralUnitFormats.put(WindUnit.KILOMETERS_PER_HOUR.getSymbol(), "%d");
		kGeneralUnitFormats.put(WindUnit.MILES_PER_HOUR.getSymbol(), "%d");
		kGeneralUnitFormats.put(WindUnit.KNOTS.getSymbol(), "%d");
		kGeneralUnitFormats.put(PrecipitationUnit.MILIMETERS.getSymbol(), "%d");
		kGeneralUnitFormats.put(PrecipitationUnit.INCHES.getSymbol(), "%d");

		kPreciseUnitFormats.put(CloudUnit.PERCENT.getSymbol(), "%d");
		kPreciseUnitFormats.put(TemperatureUnit.CELSIUS.getSymbol(), "%.1f");
		kPreciseUnitFormats.put(TemperatureUnit.FAHRENHEIT.getSymbol(), "%d");
		kPreciseUnitFormats.put(PressureUnit.HECTOPASCALS.getSymbol(), "%d");
		kPreciseUnitFormats.put(PressureUnit.MILLIMETERS_OF_MERCURY.getSymbol(), "%d");
		kPreciseUnitFormats.put(PressureUnit.INCHES_OF_MERCURY.getSymbol(), "%.1f");
		kPreciseUnitFormats.put(WindUnit.METERS_PER_SECOND.getSymbol(), "%d");
		kPreciseUnitFormats.put(WindUnit.KILOMETERS_PER_HOUR.getSymbol(), "%d");
		kPreciseUnitFormats.put(WindUnit.MILES_PER_HOUR.getSymbol(), "%d");
		kPreciseUnitFormats.put(WindUnit.KNOTS.getSymbol(), "%d");
		kPreciseUnitFormats.put(PrecipitationUnit.MILIMETERS.getSymbol(), "%.1f");
		kPreciseUnitFormats.put(PrecipitationUnit.INCHES.getSymbol(), "%.1f");
	}

	private final OsmandApplication app;
	private final WeatherPlugin plugin;

	@WeatherBandType
	private int bandIndex;

	public WeatherBand(@NonNull OsmandApplication app) {
		this.app = app;
		plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
	}

	public static WeatherBand withWeatherBand(@NonNull OsmandApplication app, @WeatherBandType int bandIndex) {
		WeatherBand band = new WeatherBand(app);
		band.bandIndex = bandIndex;
		return band;
	}

	@WeatherBandType
	public int getBandIndex() {
		return bandIndex;
	}

	public boolean isBandVisible() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return plugin.weatherCloud.get();
			case WEATHER_BAND_TEMPERATURE:
				return plugin.weatherTemp.get();
			case WEATHER_BAND_PRESSURE:
				return plugin.weatherPressure.get();
			case WEATHER_BAND_WIND_SPEED:
				return plugin.weatherWind.get();
			case WEATHER_BAND_PRECIPITATION:
				return plugin.weatherPrecip.get();
			case WEATHER_BAND_UNDEFINED:
				return false;
		}
		return false;
	}

	@Nullable
	public WeatherUnit getBandUnit() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return plugin.weatherCloudUnit.get();
			case WEATHER_BAND_TEMPERATURE:
				return plugin.weatherTempUnit.get();
			case WEATHER_BAND_PRESSURE:
				return plugin.weatherPressureUnit.get();
			case WEATHER_BAND_WIND_SPEED:
				return plugin.weatherWindUnit.get();
			case WEATHER_BAND_PRECIPITATION:
				return plugin.weatherPrecipUnit.get();
			case WEATHER_BAND_UNDEFINED:
				return null;
		}
		return null;
	}

	public boolean setBandUnit(@NonNull WeatherUnit unit) {
		if (!getAvailableBandUnits().contains(unit)) {
			return false;
		}
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return plugin.weatherCloudUnit.set((CloudUnit) unit);
			case WEATHER_BAND_TEMPERATURE:
				return plugin.weatherTempUnit.set((TemperatureUnit) unit);
			case WEATHER_BAND_PRESSURE:
				return plugin.weatherPressureUnit.set((PressureUnit) unit);
			case WEATHER_BAND_WIND_SPEED:
				return plugin.weatherWindUnit.set((WindUnit) unit);
			case WEATHER_BAND_PRECIPITATION:
				return plugin.weatherPrecipUnit.set((PrecipitationUnit) unit);
			case WEATHER_BAND_UNDEFINED:
				break;
		}
		return false;
	}

	public boolean isBandUnitAuto() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return plugin.weatherCloudUnitAuto.get();
			case WEATHER_BAND_TEMPERATURE:
				return plugin.weatherTempUnitAuto.get();
			case WEATHER_BAND_PRESSURE:
				return plugin.weatherPressureUnitAuto.get();
			case WEATHER_BAND_WIND_SPEED:
				return plugin.weatherWindUnitAuto.get();
			case WEATHER_BAND_PRECIPITATION:
				return plugin.weatherPrecipUnitAuto.get();
			case WEATHER_BAND_UNDEFINED:
				return false;
		}
		return false;
	}

	public void setBandUnitAuto(boolean unitAuto) {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				plugin.weatherCloudUnitAuto.set(unitAuto);
			case WEATHER_BAND_TEMPERATURE:
				plugin.weatherTempUnitAuto.set(unitAuto);
			case WEATHER_BAND_PRESSURE:
				plugin.weatherPressureUnitAuto.set(unitAuto);
			case WEATHER_BAND_WIND_SPEED:
				plugin.weatherWindUnitAuto.set(unitAuto);
			case WEATHER_BAND_PRECIPITATION:
				plugin.weatherPrecipUnitAuto.set(unitAuto);
			case WEATHER_BAND_UNDEFINED:
		}
	}

	@Nullable
	public Drawable getIcon() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return app.getUIUtilities().getThemedIcon(R.drawable.ic_action_clouds);
			case WEATHER_BAND_TEMPERATURE:
				return app.getUIUtilities().getThemedIcon(R.drawable.ic_action_thermometer);
			case WEATHER_BAND_PRESSURE:
				return app.getUIUtilities().getThemedIcon(R.drawable.ic_action_air_pressure);
			case WEATHER_BAND_WIND_SPEED:
				return app.getUIUtilities().getThemedIcon(R.drawable.ic_action_wind);
			case WEATHER_BAND_PRECIPITATION:
				return app.getUIUtilities().getThemedIcon(R.drawable.ic_action_precipitation);
			case WEATHER_BAND_UNDEFINED:
				return null;
		}
		return null;
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
			case WEATHER_BAND_UNDEFINED:
				return null;
		}
		return null;
	}

	@Nullable
	public String getBandGeneralUnitFormat() {
		WeatherUnit unit = getBandUnit();
		return unit != null ? kGeneralUnitFormats.get(unit.getSymbol()) : null;
	}

	@Nullable
	public String getBandPreciseUnitFormat() {
		WeatherUnit unit = getBandUnit();
		return unit != null ? kPreciseUnitFormats.get(unit.getSymbol()) : null;
	}

	@Nullable
	public static WeatherUnit getDefaultBandUnit(@WeatherBandType int bandIndex) {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return kDefaultCloudUnit;
			case WEATHER_BAND_TEMPERATURE:
				return kDefaultTempUnit;
			case WEATHER_BAND_PRESSURE:
				return kDefaultPressureUnit;
			case WEATHER_BAND_WIND_SPEED:
				return kDefaultWindSpeedUnit;
			case WEATHER_BAND_PRECIPITATION:
				return kDefaultPrecipUnit;
			case WEATHER_BAND_UNDEFINED:
				return null;
		}
		return null;
	}

	@Nullable
	public static String getInternalBandUnit(@WeatherBandType int bandIndex) {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return kInternalCloudUnit;
			case WEATHER_BAND_TEMPERATURE:
				return kInternalTempUnit;
			case WEATHER_BAND_PRESSURE:
				return kInternalPressureUnit;
			case WEATHER_BAND_WIND_SPEED:
				return kInternalWindSpeedUnit;
			case WEATHER_BAND_PRECIPITATION:
				return kInternalPrecipUnit;
			case WEATHER_BAND_UNDEFINED:
				return null;
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
				return kCloudUnits;
			case WEATHER_BAND_TEMPERATURE:
				return kTempUnits;
			case WEATHER_BAND_PRESSURE:
				return kPressureUnits;
			case WEATHER_BAND_WIND_SPEED:
				return kWindSpeedUnits;
			case WEATHER_BAND_PRECIPITATION:
				return kPrecipUnits;
			case WEATHER_BAND_UNDEFINED:
			default:
				return Collections.emptyList();
		}
	}

	public float getBandOpacity() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return plugin.weatherCloudAlpha.get();
			case WEATHER_BAND_TEMPERATURE:
				return plugin.weatherTempAlpha.get();
			case WEATHER_BAND_PRESSURE:
				return plugin.weatherPressureAlpha.get();
			case WEATHER_BAND_WIND_SPEED:
				return plugin.weatherWindAlpha.get();
			case WEATHER_BAND_PRECIPITATION:
				return plugin.weatherPrecipAlpha.get();
			case WEATHER_BAND_UNDEFINED:
				return 0.0f;
		}
		return 0.0f;
	}

	@Nullable
	public String getColorFilePath() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return WEATHER_INDEX_DIR + "cloud_color.txt";
			case WEATHER_BAND_TEMPERATURE:
				return WEATHER_INDEX_DIR + "temperature_color.txt";
			case WEATHER_BAND_PRESSURE:
				return WEATHER_INDEX_DIR + "pressure_color.txt";
			case WEATHER_BAND_WIND_SPEED:
				return WEATHER_INDEX_DIR + "wind_color.txt";
			case WEATHER_BAND_PRECIPITATION:
				return WEATHER_INDEX_DIR + "precip_color.txt";
			case WEATHER_BAND_UNDEFINED:
				return null;
		}
		return null;
	}

	@Nullable
	public String getContourStyleName() {
		switch (bandIndex) {
			case WEATHER_BAND_CLOUD:
				return kCloudContourStyleName;
			case WEATHER_BAND_TEMPERATURE:
				return kTempContourStyleName;
			case WEATHER_BAND_PRESSURE:
				return kPressureContourStyleName;
			case WEATHER_BAND_WIND_SPEED:
				return kWindSpeedContourStyleName;
			case WEATHER_BAND_PRECIPITATION:
				return kPrecipContourStyleName;
			case WEATHER_BAND_UNDEFINED:
				return null;
		}
		return null;
	}
}