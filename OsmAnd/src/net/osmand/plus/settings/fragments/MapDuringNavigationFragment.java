package net.osmand.plus.settings.fragments;

import androidx.preference.Preference;

import net.osmand.plus.helpers.enums.MetricsConstants;
import net.osmand.plus.helpers.enums.AutoZoomMap;
import net.osmand.plus.R;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class MapDuringNavigationFragment extends BaseSettingsFragment {

	public static final String TAG = MapDuringNavigationFragment.class.getSimpleName();

	@Override
	protected void setupPreferences() {
		Preference mapDuringNavigationInfo = findPreference("map_during_navigation_info");
		mapDuringNavigationInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupAutoFollowPref();
		setupAutoZoomMapPref();
		setupSnapToRoadPref();
		setupMapDirectionToCompassPref();
	}

	private void setupAutoFollowPref() {
		Integer[] entryValues = new Integer[] {0, 5, 10, 15, 20, 25, 30, 45, 60, 90};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_never);
		for (int i = 1; i < entryValues.length; i++) {
			entries[i] = (int) entryValues[i] + " " + getString(R.string.int_seconds);
		}

		ListPreferenceEx autoFollowRoute = (ListPreferenceEx) findPreference(settings.AUTO_FOLLOW_ROUTE.getId());
		autoFollowRoute.setEntries(entries);
		autoFollowRoute.setEntryValues(entryValues);
	}

	private void setupAutoZoomMapPref() {
		Integer[] entryValues = new Integer[AutoZoomMap.values().length + 1];
		String[] entries = new String[entryValues.length];

		int i = 0;
		int selectedIndex = -1;
		entries[i] = getString(R.string.auto_zoom_none);
		entryValues[0] = 0;
		if (!settings.AUTO_ZOOM_MAP.getModeValue(getSelectedAppMode())) {
			selectedIndex = 0;
		}
		i++;
		for (AutoZoomMap autoZoomMap : AutoZoomMap.values()) {
			entries[i] = getString(autoZoomMap.name);
			entryValues[i] = i;
			if (selectedIndex == -1 && settings.AUTO_ZOOM_MAP_SCALE.getModeValue(getSelectedAppMode()) == autoZoomMap) {
				selectedIndex = i;
			}
			i++;
		}
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		ListPreferenceEx autoZoomMapPref = (ListPreferenceEx) findPreference(settings.AUTO_ZOOM_MAP.getId());
		autoZoomMapPref.setEntries(entries);
		autoZoomMapPref.setEntryValues(entryValues);
		autoZoomMapPref.setValue(selectedIndex);
		autoZoomMapPref.setPersistent(false);
	}

	private void setupSnapToRoadPref() {
		SwitchPreferenceEx snapToRoad = (SwitchPreferenceEx) findPreference(settings.SNAP_TO_ROAD.getId());
		snapToRoad.setTitle(getString(R.string.snap_to_road));
		snapToRoad.setDescription(getString(R.string.snap_to_road_descr));
	}

	private void setupMapDirectionToCompassPref() {
		//array size must be equal!
		Float[] valuesKmh = new Float[]{0f, 5f, 7f, 10f, 15f, 20f};
		Float[] valuesMph = new Float[]{0f, 3f, 5f, 7f, 10f, 15f};
		String[] names;
		if (settings.METRIC_SYSTEM.getModeValue(getSelectedAppMode()) == MetricsConstants.KILOMETERS_AND_METERS) {
			names = new String[valuesKmh.length];
			for (int i = 0; i < names.length; i++) {
				names[i] = valuesKmh[i].intValue() + " " + getString(R.string.km_h);
			}
		} else {
			names = new String[valuesMph.length];
			for (int i = 0; i < names.length; i++) {
				names[i] = valuesMph[i].intValue() + " " + getString(R.string.mile_per_hour);
			}
		}
		ListPreferenceEx switchMapDirectionToCompass = (ListPreferenceEx) findPreference(settings.SWITCH_MAP_DIRECTION_TO_COMPASS_KMH.getId());
		switchMapDirectionToCompass.setDescription(R.string.map_orientation_threshold_descr);
		switchMapDirectionToCompass.setEntries(names);
		switchMapDirectionToCompass.setEntryValues(valuesKmh);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(settings.AUTO_ZOOM_MAP.getId())) {
			onConfirmPreferenceChange(settings.AUTO_ZOOM_MAP.getId(), newValue, ApplyQueryType.SNACK_BAR);
			return true;
		}
		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		if (settings.AUTO_ZOOM_MAP.getId().equals(prefId)) {
			if (newValue instanceof Integer) {
				int position = (int) newValue;
				if (position == 0) {
					applyPreference(settings.AUTO_ZOOM_MAP.getId(), applyToAllProfiles, false);
				} else {
					applyPreference(settings.AUTO_ZOOM_MAP.getId(), applyToAllProfiles, true);
					applyPreference(settings.AUTO_ZOOM_MAP_SCALE.getId(),
							applyToAllProfiles, AutoZoomMap.values()[position - 1]);
				}
			}
		} else {
			super.onApplyPreferenceChange(prefId, applyToAllProfiles, newValue);
		}
	}
}