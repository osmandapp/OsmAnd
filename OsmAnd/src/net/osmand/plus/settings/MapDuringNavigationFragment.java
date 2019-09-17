package net.osmand.plus.settings;

import android.support.v7.preference.Preference;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.AutoZoomMap;
import net.osmand.plus.R;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class MapDuringNavigationFragment extends BaseSettingsFragment {

	public static final String TAG = "MapDuringNavigationFragment";

	@Override
	protected int getPreferencesResId() {
		return R.xml.map_during_navigation;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	@Override
	protected int getToolbarTitle() {
		return R.string.map_during_navigation;
	}

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
		if (!settings.AUTO_ZOOM_MAP.get()) {
			selectedIndex = 0;
		}
		i++;
		for (AutoZoomMap autoZoomMap : AutoZoomMap.values()) {
			entries[i] = getString(autoZoomMap.name);
			entryValues[i] = i;
			if (selectedIndex == -1 && settings.AUTO_ZOOM_MAP_SCALE.get() == autoZoomMap) {
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
		String[] entries;
		Float[] entryValues;
		if (settings.METRIC_SYSTEM.get() == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			entryValues = new Float[] {0f, 5f, 7f, 10f, 15f, 20f};
			entries = new String[entryValues.length];

			for (int i = 0; i < entryValues.length; i++) {
				entries[i] = entryValues[i].intValue() + " " + getString(R.string.km_h);
			}
		} else {
			Float[] speedLimitsMiles = new Float[] {-7f, -5f, -3f, 0f, 3f, 5f, 7f, 10f, 15f};
			entryValues = new Float[] {0f, 3f, 5f, 7f, 10f, 15f};
			entries = new String[entryValues.length];

			for (int i = 0; i < entries.length; i++) {
				entries[i] = speedLimitsMiles[i].intValue() + " " + getString(R.string.mile_per_hour);
			}
		}

		ListPreferenceEx switchMapDirectionToCompass = (ListPreferenceEx) findPreference(settings.SWITCH_MAP_DIRECTION_TO_COMPASS.getId());
		switchMapDirectionToCompass.setEntries(entries);
		switchMapDirectionToCompass.setEntryValues(entryValues);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(settings.AUTO_ZOOM_MAP.getId())) {
			if (newValue instanceof Integer) {
				int position = (int) newValue;
				if (position == 0) {
					settings.AUTO_ZOOM_MAP.set(false);
				} else {
					settings.AUTO_ZOOM_MAP.set(true);
					settings.AUTO_ZOOM_MAP_SCALE.set(OsmandSettings.AutoZoomMap.values()[position - 1]);
				}
				return true;
			}
		}
		return super.onPreferenceChange(preference, newValue);
	}
}