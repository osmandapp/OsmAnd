package net.osmand.plus.weather;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.weather.units.CloudConstants;
import net.osmand.plus.weather.units.PrecipConstants;
import net.osmand.plus.weather.units.PressureConstants;
import net.osmand.plus.weather.units.TemperatureConstants;
import net.osmand.plus.weather.units.WindConstants;
import net.osmand.util.Algorithms;

import static net.osmand.plus.weather.WeatherPlugin.PREFERENCE_ID_CLOUDS;
import static net.osmand.plus.weather.WeatherPlugin.PREFERENCE_ID_PRECIP;
import static net.osmand.plus.weather.WeatherPlugin.PREFERENCE_ID_PRESSURE;
import static net.osmand.plus.weather.WeatherPlugin.PREFERENCE_ID_TEMPERATURE;
import static net.osmand.plus.weather.WeatherPlugin.PREFERENCE_ID_WIND;

public class WeatherSettingsFragment extends BaseSettingsFragment {

	@Override
	protected void setupPreferences() {
		setupUnitsPreference(PREFERENCE_ID_TEMPERATURE);
		setupUnitsPreference(PREFERENCE_ID_PRESSURE);
		setupUnitsPreference(PREFERENCE_ID_WIND);
		setupUnitsPreference(PREFERENCE_ID_CLOUDS);
		setupUnitsPreference(PREFERENCE_ID_PRECIP);

		setupOfflineForecastPref();
		setupOnlineCachePref();
	}

	private void setupUnitsPreference(@NonNull String prefId) {
		Enum<?>[] values = getUnitValues(prefId);

		String[] entries = new String[values.length];
		Integer[] entryValues = new Integer[values.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = getUnitTypeName(values[i]);
			entryValues[i] = values[i].ordinal();
		}

		ListPreferenceEx preference = findPreference(prefId);
		preference.setEntries(entries);
		preference.setEntryValues(entryValues);
		preference.setIcon(getActiveIcon(getPrefIconId(prefId)));
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
		if (Algorithms.equalsToAny(preference.getKey(), WeatherPlugin.getUnitsPreferencesIds())) {
			showChooseUnitDialog((ListPreferenceEx) preference);
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	private void showChooseUnitDialog(ListPreferenceEx listPreference) {
		boolean nightMode = isNightMode();
		Context ctx = UiUtilities.getThemedContext(getActivity(), nightMode);
		int profileColor = getSelectedAppMode().getProfileColor(nightMode);

		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(listPreference.getTitle());
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		String[] entries = listPreference.getEntries();
		Integer[] entryValues = (Integer[]) listPreference.getEntryValues();
		int i = listPreference.getValueIndex();

		DialogListItemAdapter adapter = DialogListItemAdapter.createSingleChoiceAdapter(
				entries, nightMode, i, app, profileColor, themeRes, v -> {
					int selectedEntryIndex = (int) v.getTag();
					Object value = entryValues[selectedEntryIndex];
					if (listPreference.callChangeListener(value)) {
						listPreference.setValue(value);
					}
					Fragment target = getTargetFragment();
					if (target instanceof OnPreferenceChanged) {
						((OnPreferenceChanged) target).onPreferenceChanged(listPreference.getKey());
					}
				}
		);

		builder.setAdapter(adapter, null);
		adapter.setDialog(builder.show());
	}

	@Nullable
	private Enum<?>[] getUnitValues(@NonNull String prefId) {
		switch (prefId) {
			case PREFERENCE_ID_TEMPERATURE:
				return TemperatureConstants.values();
			case PREFERENCE_ID_PRESSURE:
				return PressureConstants.values();
			case PREFERENCE_ID_WIND:
				return WindConstants.values();
			case PREFERENCE_ID_CLOUDS:
				return CloudConstants.values();
			case PREFERENCE_ID_PRECIP:
				return PrecipConstants.values();
			default:
				return null;
		}
	}

	@Nullable
	private String getUnitTypeName(@NonNull Enum<?> value) {
		if (value instanceof TemperatureConstants) {
			return ((TemperatureConstants) value).toHumanString(app);
		} else if (value instanceof PressureConstants) {
			return ((PressureConstants) value).toHumanString(app);
		} else if (value instanceof WindConstants) {
			return ((WindConstants) value).toHumanString(app);
		} else if (value instanceof CloudConstants) {
			return ((CloudConstants) value).getUnit();
		} else if (value instanceof PrecipConstants) {
			return ((PrecipConstants) value).toHumanString(app);
		} else {
			return null;
		}
	}

	@DrawableRes
	private int getPrefIconId(@NonNull String prefId) {
		switch (prefId) {
			case PREFERENCE_ID_TEMPERATURE:
				return R.drawable.ic_action_thermometer;
			case PREFERENCE_ID_PRESSURE:
				return R.drawable.ic_action_air_pressure;
			case PREFERENCE_ID_WIND:
				return R.drawable.ic_action_wind;
			case PREFERENCE_ID_CLOUDS:
				return R.drawable.ic_action_clouds;
			case PREFERENCE_ID_PRECIP:
				return R.drawable.ic_action_precipitation;
			default:
				return R.drawable.ic_action_info_dark;
		}
	}
}
