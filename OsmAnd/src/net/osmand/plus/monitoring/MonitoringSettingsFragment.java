package net.osmand.plus.monitoring;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;

import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.plus.settings.bottomsheets.ChangeGeneralProfilesPrefBottomSheet;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.io.Serializable;

import static net.osmand.plus.OsmandSettings.DAILY_DIRECTORY;
import static net.osmand.plus.OsmandSettings.MONTHLY_DIRECTORY;
import static net.osmand.plus.OsmandSettings.REC_DIRECTORY;
import static net.osmand.plus.monitoring.OsmandMonitoringPlugin.MINUTES;
import static net.osmand.plus.monitoring.OsmandMonitoringPlugin.SECONDS;

public class MonitoringSettingsFragment extends BaseSettingsFragment {

	private static final String COPY_PLUGIN_SETTINGS = "copy_plugin_settings";
	private static final String RESET_TO_DEFAULT = "reset_to_default";
	private static final String OPEN_TRACKS = "open_tracks";

	@Override
	protected void setupPreferences() {
		setupSaveTrackToGpxPref();
		setupSaveTrackIntervalPref();

		setupSaveTrackMinDistancePref();
		setupSaveTrackPrecisionPref();
		setupSaveTrackMinSpeedPref();
		setupAutoSplitRecordingPref();
		setupDisableRecordingOnceAppKilledPref();

		setupTrackStorageDirectoryPref();
		setupLiveMonitoringPref();

		setupOpenNotesDescrPref();
		setupOpenNotesPref();

		setupCopyProfileSettingsPref();
		setupResetToDefaultPref();
	}

	private void setupSaveTrackToGpxPref() {
		SwitchPreferenceEx saveTrackToGpx = (SwitchPreferenceEx) findPreference(settings.SAVE_TRACK_TO_GPX.getId());
		saveTrackToGpx.setDescription(getString(R.string.save_track_to_gpx_descrp));
		saveTrackToGpx.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark));
	}

	private void setupSaveTrackIntervalPref() {
		Integer[] entryValues = new Integer[SECONDS.length + MINUTES.length];
		String[] entries = new String[entryValues.length];
		int k = 0;
		for (int second : SECONDS) {
			entryValues[k] = second * 1000;
			entries[k] = second + " " + getString(R.string.int_seconds);
			k++;
		}
		for (int minute : MINUTES) {
			entryValues[k] = (minute * 60) * 1000;
			entries[k] = minute + " " + getString(R.string.int_min);
			k++;
		}

		ListPreferenceEx saveTrackInterval = (ListPreferenceEx) findPreference(settings.SAVE_TRACK_INTERVAL.getId());
		saveTrackInterval.setEntries(entries);
		saveTrackInterval.setEntryValues(entryValues);
		saveTrackInterval.setIcon(getActiveIcon(R.drawable.ic_action_time_span));
		saveTrackInterval.setDescription(R.string.save_track_interval_descr);
	}

	private void setupSaveTrackMinDistancePref() {
		Float[] entryValues = new Float[] {0.f, 2.0f, 5.0f, 10.0f, 20.0f, 30.0f, 50.0f};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_not_selected);
		for (int i = 1; i < entryValues.length; i++) {
			entries[i] = entryValues[i].intValue() + " " + getString(R.string.m);
		}

		ListPreferenceEx saveTrackMinDistance = (ListPreferenceEx) findPreference(settings.SAVE_TRACK_MIN_DISTANCE.getId());
		saveTrackMinDistance.setEntries(entries);
		saveTrackMinDistance.setEntryValues(entryValues);
		saveTrackMinDistance.setDescription(R.string.save_track_min_distance_descr);
	}

	private void setupSaveTrackPrecisionPref() {
		Float[] entryValues = new Float[] {0.f, 1.0f, 2.0f, 5.0f, 10.0f, 15.0f, 20.0f, 50.0f, 100.0f};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_not_selected);
		for (int i = 1; i < entryValues.length; i++) {
			entries[i] = entryValues[i].intValue() + " " + getString(R.string.m) + "  (" + Math.round(entryValues[i] / 0.3048f) + " " + getString(R.string.foot) + ")";
		}

		ListPreferenceEx saveTrackPrecision = (ListPreferenceEx) findPreference(settings.SAVE_TRACK_PRECISION.getId());
		saveTrackPrecision.setEntries(entries);
		saveTrackPrecision.setEntryValues(entryValues);
		saveTrackPrecision.setDescription(R.string.save_track_precision_descr);
	}

	private void setupSaveTrackMinSpeedPref() {
		Float[] entryValues = new Float[] {0.f, 0.000001f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_not_selected);
		entries[1] = "> 0"; // This option for the GPS chipset motion detection
		for (int i = 2; i < entryValues.length; i++) {
			entries[i] = entryValues[i].intValue() + " " + getString(R.string.km_h);
			entryValues[i] = entryValues[i] / 3.6f;
		}

		ListPreferenceEx saveTrackMinSpeed = (ListPreferenceEx) findPreference(settings.SAVE_TRACK_MIN_SPEED.getId());
		saveTrackMinSpeed.setEntries(entries);
		saveTrackMinSpeed.setEntryValues(entryValues);
		saveTrackMinSpeed.setDescription(R.string.save_track_min_speed_descr);
	}

	private void setupAutoSplitRecordingPref() {
		SwitchPreferenceEx autoSplitRecording = (SwitchPreferenceEx) findPreference(settings.AUTO_SPLIT_RECORDING.getId());
		autoSplitRecording.setDescription(getString(R.string.auto_split_recording_descr));
	}

	private void setupDisableRecordingOnceAppKilledPref() {
		SwitchPreferenceEx disableRecordingOnceAppKilled = (SwitchPreferenceEx) findPreference(settings.DISABLE_RECORDING_ONCE_APP_KILLED.getId());
		disableRecordingOnceAppKilled.setDescription(getString(R.string.disable_recording_once_app_killed_descrp));
	}

	private void setupTrackStorageDirectoryPref() {
		Integer[] entryValues = new Integer[] {REC_DIRECTORY, MONTHLY_DIRECTORY, DAILY_DIRECTORY};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.store_tracks_in_rec_directory);
		entries[1] = getString(R.string.store_tracks_in_monthly_directories);
		entries[2] = getString(R.string.store_tracks_in_daily_directories);

		ListPreferenceEx trackStorageDirectory = (ListPreferenceEx) findPreference(settings.TRACK_STORAGE_DIRECTORY.getId());
		trackStorageDirectory.setEntries(entries);
		trackStorageDirectory.setEntryValues(entryValues);
		trackStorageDirectory.setDescription(R.string.track_storage_directory_descrp);
		trackStorageDirectory.setIcon(getActiveIcon(R.drawable.ic_action_folder));
	}

	private void setupLiveMonitoringPref() {
		SwitchPreferenceEx liveMonitoring = (SwitchPreferenceEx) findPreference(settings.LIVE_MONITORING.getId());
		liveMonitoring.setDescription(getString(R.string.live_monitoring_m_descr));
		liveMonitoring.setIcon(getContentIcon(R.drawable.ic_world_globe_dark));
	}

	private void setupOpenNotesDescrPref() {
		Preference nameAndPasswordPref = findPreference("open_tracks_description");
		nameAndPasswordPref.setTitle(getText(R.string.tracks_view_descr));
	}

	private void setupOpenNotesPref() {
		Preference openNotes = findPreference(OPEN_TRACKS);
		openNotes.setIcon(getActiveIcon(R.drawable.ic_action_folder));
	}

	private void setupCopyProfileSettingsPref() {
		Preference copyProfilePrefs = findPreference(COPY_PLUGIN_SETTINGS);
		copyProfilePrefs.setIcon(getActiveIcon(R.drawable.ic_action_copy));
	}

	private void setupResetToDefaultPref() {
		Preference resetToDefault = findPreference(RESET_TO_DEFAULT);
		resetToDefault.setIcon(getActiveIcon(R.drawable.ic_action_reset_to_default_dark));
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (OPEN_TRACKS.equals(preference.getKey())) {
			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			Intent favorites = new Intent(preference.getContext(), appCustomization.getFavoritesActivity());
			favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			app.getSettings().FAVORITES_TAB.set(FavoritesActivity.GPX_TAB);
			startActivity(favorites);
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();

		OsmandSettings.OsmandPreference pref = settings.getPreference(prefId);
		if (pref instanceof OsmandSettings.CommonPreference && !((OsmandSettings.CommonPreference) pref).hasDefaultValueForMode(getSelectedAppMode())) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null && newValue instanceof Serializable) {
				ChangeGeneralProfilesPrefBottomSheet.showInstance(fragmentManager, prefId,
						(Serializable) newValue, this, false, getSelectedAppMode());
			}
			return false;
		}

		return true;
	}
}