package net.osmand.plus.plugins.weather.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.weather.OfflineForecastHelper;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.plugins.weather.listener.WeatherCacheSizeChangeListener;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.plugins.weather.viewholder.WeatherTotalCacheSizeViewHolder;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherSettingsFragment extends BaseSettingsFragment implements WeatherCacheSizeChangeListener {

	private static final String WEATHER_ONLINE_CACHE = "weather_online_cache";
	private static final String WEATHER_OFFLINE_CACHE = "weather_offline_cache";

	private final Map<Preference, WeatherBand> unitPrefs = new HashMap<>();
	private final Map<String, WeatherTotalCacheSizeViewHolder> viewHolders = new HashMap<>();
	private OfflineForecastHelper offlineForecastHelper;

	@Override
	protected void setupPreferences() {
		unitPrefs.clear();
		setupUnitsCategory();
		setupCacheSizeCategory();
	}

	private void setupUnitsCategory() {
		WeatherHelper weatherHelper = app.getWeatherHelper();
		for (WeatherBand weatherBand : weatherHelper.getWeatherBands()) {
			setupWeatherUnitPreference(weatherBand);
		}
	}

	private void setupWeatherUnitPreference(@NonNull WeatherBand weatherBand) {
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
		addOnPreferencesScreen(preference);
	}

	private void setupCacheSizeCategory() {
		offlineForecastHelper = app.getOfflineForecastHelper();
		Context ctx = requireContext();
		Preference divider = new Preference(ctx);
		divider.setLayoutResource(R.layout.simple_divider_item);
		divider.setSelectable(false);
		addOnPreferencesScreen(divider);

		PreferenceCategory category = new PreferenceCategory(ctx);
		category.setLayoutResource(R.layout.preference_category_with_descr);
		category.setTitle(R.string.data_settings);
		category.setIconSpaceReserved(true);
		addOnPreferencesScreen(category);

		Preference onlineCache = new Preference(ctx);
		onlineCache.setKey(WEATHER_ONLINE_CACHE);
		onlineCache.setLayoutResource(R.layout.preference_with_progress_and_secondary_icon);
		onlineCache.setTitle(R.string.weather_online_cache);
		onlineCache.setIconSpaceReserved(true);
		addOnPreferencesScreen(onlineCache);

		Preference offlineCache = new Preference(ctx);
		offlineCache.setKey(WEATHER_OFFLINE_CACHE);
		offlineCache.setLayoutResource(R.layout.preference_with_progress_and_secondary_icon);
		offlineCache.setTitle(R.string.offline_cache);
		offlineCache.setIconSpaceReserved(true);
		addOnPreferencesScreen(offlineCache);
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
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		String key = preference.getKey();
		if (Algorithms.equalsToAny(key, WEATHER_ONLINE_CACHE, WEATHER_OFFLINE_CACHE)) {
			boolean forLocal = key.equals(WEATHER_OFFLINE_CACHE);
			viewHolders.put(key, new WeatherTotalCacheSizeViewHolder(app, holder.itemView, forLocal));
			updateCacheSizePreferences();
		}
		super.onBindPreferenceViewHolder(preference, holder);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		if (WEATHER_ONLINE_CACHE.equals(key) && offlineForecastHelper.canClearOnlineCache()) {
			Context ctx = getContext();
			if (ctx != null) {
				WeatherDialogs.showClearOnlineCacheDialog(ctx, isNightMode());
			}
			return false;
		}
		return super.onPreferenceClick(preference);
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
		WeatherDialogs.showChooseUnitDialog(requireContext(), weatherBand, preference.getValueIndex(), profileColor, nightMode, listener);
	}

	@Override
	public void onResume() {
		super.onResume();
		offlineForecastHelper.registerWeatherCacheSizeChangeListener(this);
		offlineForecastHelper.calculateTotalCacheSizeAsync(true);
		updateCacheSizePreferences();
	}

	@Override
	public void onPause() {
		super.onPause();
		offlineForecastHelper.unregisterWeatherCacheSizeChangeListener(this);
	}

	@Override
	public void onWeatherCacheSizeChanged() {
		offlineForecastHelper.calculateTotalCacheSizeAsync(false);
		updateCacheSizePreferences();
	}

	private void updateCacheSizePreferences() {
		for (WeatherTotalCacheSizeViewHolder viewHolder : viewHolders.values()) {
			viewHolder.update();
		}
	}
}