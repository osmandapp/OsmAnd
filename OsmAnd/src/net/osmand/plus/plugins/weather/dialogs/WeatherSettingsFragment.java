package net.osmand.plus.plugins.weather.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherSettingsFragment extends BaseSettingsFragment {

	private final Map<Preference, WeatherBand> unitPrefs = new HashMap<>();

	@Override
	protected void setupPreferences() {
		unitPrefs.clear();
		WeatherHelper weatherHelper = app.getWeatherHelper();
		for (WeatherBand weatherBand : weatherHelper.getWeatherBands()) {
			setupWeatherBandPref(weatherBand);
		}
	}

	private void setupWeatherBandPref(@NonNull WeatherBand weatherBand) {
		List<? extends WeatherUnit> bandUnits = weatherBand.getAvailableBandUnits();

		String[] entries = new String[bandUnits.size()];
		Integer[] entryValues = new Integer[bandUnits.size()];
		for (int i = 0; i < entries.length; i++) {
			WeatherUnit unit = bandUnits.get(i);
			entries[i] = unit.toHumanString(app);
			entryValues[i] = ((Enum<?>) unit).ordinal();
		}

		ListPreferenceEx preference = createListPreferenceEx(weatherBand.getBandUnitPref().getId(), entries,
				entryValues, weatherBand.getMeasurementName(), R.layout.preference_with_descr);
		preference.setEntries(entries);
		preference.setEntryValues(entryValues);
		preference.setIcon(getActiveIcon(weatherBand.getIconId()));

		unitPrefs.put(preference, weatherBand);
		getPreferenceScreen().addPreference(preference);
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		View switchProfile = view.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			AndroidUiHelper.updateVisibility(switchProfile, true);
		}
	}

	@Override
	public void onDisplayPreferenceDialog(@NonNull Preference preference) {
		if (preference instanceof ListPreferenceEx && unitPrefs.containsKey(preference)) {
			WeatherBand weatherBand = unitPrefs.get(preference);
			showChooseUnitDialog((ListPreferenceEx) preference, weatherBand);
			return;
		}
		super.onDisplayPreferenceDialog(preference);
	}

	private void showChooseUnitDialog(@NonNull ListPreferenceEx preference, @NonNull WeatherBand weatherBand) {
		boolean nightMode = isNightMode();
		int profileColor = getSelectedAppMode().getProfileColor(nightMode);
		Integer[] entryValues = (Integer[]) preference.getEntryValues();

		OnClickListener listener = v -> {
			int selectedEntryIndex = (int) v.getTag();
			Object value = entryValues[selectedEntryIndex];
			if (preference.callChangeListener(value)) {
				preference.setValue(value);
			}
			Fragment target = getTargetFragment();
			if (target instanceof OnPreferenceChanged) {
				((OnPreferenceChanged) target).onPreferenceChanged(preference.getKey());
			}
			app.getWeatherHelper().updateBandsSettings();

			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.refreshMap();
			}
		};
		showChooseUnitDialog(requireContext(), weatherBand, preference.getValueIndex(), profileColor, nightMode, listener);
	}

	public static void showChooseUnitDialog(@NonNull Context ctx, @NonNull WeatherBand band,
	                                        int selected, int profileColor, boolean nightMode,
	                                        @NonNull OnClickListener listener) {
		List<? extends WeatherUnit> bandUnits = band.getAvailableBandUnits();
		String[] entries = new String[bandUnits.size()];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = bandUnits.get(i).toHumanString(ctx);
		}
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		DialogListItemAdapter adapter = DialogListItemAdapter.createSingleChoiceAdapter(entries,
				nightMode, selected, app, profileColor, themeRes, listener);

		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(band.getMeasurementName());
		builder.setAdapter(adapter, null);
		adapter.setDialog(builder.show());
	}
}