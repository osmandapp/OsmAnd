package net.osmand.plus.weather;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WEATHER;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;

public class WeatherPlugin extends OsmandPlugin {

	public WeatherPlugin(@NonNull OsmandApplication app) {
		super(app);

		EnumStringPreference weatherTemp = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_temp", TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
		EnumStringPreference weatherPressure = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_pressure", PressureConstants.MILLIMETERS_OF_MERCURY, PressureConstants.values(), PressureConstants.class).makeProfile();
		EnumStringPreference weatherWind = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_wind", TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
		EnumStringPreference weatherCloud = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_cloud", TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
		EnumStringPreference weatherPrecipitation = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_precip", TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
	}

	@Override
	public String getId() {
		return PLUGIN_WEATHER;
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_weather);
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.weather_plugin_description);
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.weather_prefs_descr);
	}

	@Override
	public int getLogoResourceId() {
//		return R.drawable.img_plugin_weather;
		return R.drawable.ic_extension_dark;
	}

	@Override
	public boolean isPaid() {
		return true;
	}

	@Override
	public boolean isLocked() {
		return !Version.isPaidVersion(app);
	}

	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.WEATHER;
	}
}
