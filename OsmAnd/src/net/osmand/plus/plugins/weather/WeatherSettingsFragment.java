package net.osmand.plus.plugins.weather;

import static net.osmand.plus.plugins.weather.WeatherInfoType.CLOUDS;
import static net.osmand.plus.plugins.weather.WeatherInfoType.PRECIPITATION;
import static net.osmand.plus.plugins.weather.WeatherInfoType.PRESSURE;
import static net.osmand.plus.plugins.weather.WeatherInfoType.TEMPERATURE;
import static net.osmand.plus.plugins.weather.WeatherInfoType.WIND;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class WeatherSettingsFragment extends BaseSettingsFragment {

	@Override
	protected void setupPreferences() {
		WeatherPlugin plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		if (plugin != null) {
			setupUnitsPref(TEMPERATURE, plugin.weatherTempUnit.getId());
			setupUnitsPref(PRESSURE, plugin.weatherPressureUnit.getId());
			setupUnitsPref(WIND, plugin.weatherWindUnit.getId());
			setupUnitsPref(CLOUDS, plugin.weatherCloudUnit.getId());
			setupUnitsPref(PRECIPITATION, plugin.weatherPrecipUnit.getId());
			setupOfflineForecastPref();
			setupOnlineCachePref();
		}
	}

	private void setupUnitsPref(@NonNull WeatherInfoType layer, @NonNull String prefId) {
		Enum<?>[] values = layer.getUnits();

		String[] entries = new String[values.length];
		Integer[] entryValues = new Integer[values.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = layer.getUnitName(app, values[i]);
			entryValues[i] = values[i].ordinal();
		}

		ListPreferenceEx preference = findPreference(prefId);
		preference.setEntries(entries);
		preference.setEntryValues(entryValues);
		preference.setIcon(getActiveIcon(layer.getIconId()));
	}

	private void setupOfflineForecastPref() {
		Preference preference = findPreference("weather_online_cache");
		preference.setSummary(AndroidUtils.formatSize(app, AndroidUtils.getAvailableSpace(app)));
	}

	private void setupOnlineCachePref() {
		Preference preference = findPreference("weather_offline_forecast");
		preference.setSummary(AndroidUtils.formatSize(app, AndroidUtils.getTotalSpace(app)));
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
	public boolean onPreferenceChange(@NonNull Preference preference, @Nullable Object newValue) {
		String prefId = preference.getKey();
		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public boolean onPreferenceClick(@NonNull Preference preference) {
		String prefId = preference.getKey();
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onDisplayPreferenceDialog(@NonNull Preference preference) {
		WeatherPlugin plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		if (Algorithms.equalsToAny(preference.getKey(), plugin.getUnitsPreferencesIds())) {
			showChooseUnitDialog((ListPreferenceEx) preference);
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	private void showChooseUnitDialog(@NonNull ListPreferenceEx preference) {
		boolean nightMode = isNightMode();
		int profileColor = getSelectedAppMode().getProfileColor(nightMode);

		String[] entries = preference.getEntries();
		Integer[] entryValues = (Integer[]) preference.getEntryValues();
		int i = preference.getValueIndex();

		DialogListItemAdapter adapter = DialogListItemAdapter.createSingleChoiceAdapter(entries,
				nightMode, i, app, profileColor, themeRes, v -> {
					int selectedEntryIndex = (int) v.getTag();
					Object value = entryValues[selectedEntryIndex];
					if (preference.callChangeListener(value)) {
						preference.setValue(value);
					}
					Fragment target = getTargetFragment();
					if (target instanceof OnPreferenceChanged) {
						((OnPreferenceChanged) target).onPreferenceChanged(preference.getKey());
					}
				}
		);

		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(getActivity(), nightMode));
		builder.setTitle(preference.getTitle());
		builder.setAdapter(adapter, null);
		adapter.setDialog(builder.show());
	}
}
