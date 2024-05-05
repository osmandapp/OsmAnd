package net.osmand.plus.settings.fragments;

import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.enums.AutoZoomMap;
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
	}

	private void setupAutoFollowPref() {
		Integer[] entryValues = {0, 5, 10, 15, 20, 25, 30, 45, 60, 90};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_never);
		for (int i = 1; i < entryValues.length; i++) {
			entries[i] = entryValues[i] + " " + getString(R.string.int_seconds);
		}

		ListPreferenceEx autoFollowRoute = findPreference(settings.AUTO_FOLLOW_ROUTE.getId());
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

		ListPreferenceEx autoZoomMapPref = findPreference(settings.AUTO_ZOOM_MAP.getId());
		autoZoomMapPref.setEntries(entries);
		autoZoomMapPref.setEntryValues(entryValues);
		autoZoomMapPref.setValue(selectedIndex);
		autoZoomMapPref.setPersistent(false);
	}

	private void setupSnapToRoadPref() {
		SwitchPreferenceEx snapToRoad = findPreference(settings.SNAP_TO_ROAD.getId());
		snapToRoad.setTitle(getString(R.string.snap_to_road));
		snapToRoad.setDescription(getString(R.string.snap_to_road_descr));
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