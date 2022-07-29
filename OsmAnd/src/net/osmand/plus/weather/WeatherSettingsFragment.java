package net.osmand.plus.weather;

import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;

public class WeatherSettingsFragment extends BaseSettingsFragment {

	@Override
	protected void setupPreferences() {
		setupTemperaturePref();
		setupPressurePref();
		setupWindPref();
		setupCloudsPref();
		setupPrecipitationPref();

		setupOfflineForecastPref();
		setupOnlineCachePref();
	}

	private void setupTemperaturePref() {
		Preference nameAndPasswordPref = findPreference("map_settings_weather_temp");
		nameAndPasswordPref.setIcon(getActiveIcon(R.drawable.ic_action_thermometer));
	}

	private void setupPressurePref() {
		Preference nameAndPasswordPref = findPreference("map_settings_weather_pressure");
		nameAndPasswordPref.setIcon(getActiveIcon(R.drawable.ic_action_air_pressure));
	}

	private void setupWindPref() {
		Preference nameAndPasswordPref = findPreference("map_settings_weather_wind");
		nameAndPasswordPref.setIcon(getActiveIcon(R.drawable.ic_action_wind));
	}

	private void setupCloudsPref() {
		Preference nameAndPasswordPref = findPreference("map_settings_weather_cloud");
		nameAndPasswordPref.setIcon(getActiveIcon(R.drawable.ic_action_clouds));
	}

	private void setupPrecipitationPref() {
		Preference nameAndPasswordPref = findPreference("map_settings_weather_precip");
		nameAndPasswordPref.setIcon(getActiveIcon(R.drawable.ic_action_precipitation));
	}

	private void setupOfflineForecastPref() {
		Preference nameAndPasswordPref = findPreference("weather_online_cache");
		nameAndPasswordPref.setSummary(AndroidUtils.formatSize(app, AndroidUtils.getAvailableSpace(app)));
	}

	private void setupOnlineCachePref() {
		Preference nameAndPasswordPref = findPreference("weather_offline_forecast");
		nameAndPasswordPref.setSummary(AndroidUtils.formatSize(app, AndroidUtils.getTotalSpace(app)));
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);

		View switchProfile = view.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			AndroidUiHelper.updateVisibility(switchProfile, true);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		return super.onPreferenceClick(preference);
	}
}
