package net.osmand.plus.weather;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WEATHER;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;

public class WeatherPlugin extends OsmandPlugin {

	public WeatherPlugin(@NonNull OsmandApplication app) {
		super(app);
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
