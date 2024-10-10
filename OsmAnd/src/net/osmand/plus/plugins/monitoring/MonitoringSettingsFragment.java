package net.osmand.plus.plugins.monitoring;

import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.plugins.PluginInfoFragment.PLUGIN_INFO;
import static net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin.MINUTES;
import static net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin.SECONDS;
import static net.osmand.plus.settings.backend.OsmandSettings.MONTHLY_DIRECTORY;
import static net.osmand.plus.settings.backend.OsmandSettings.REC_DIRECTORY;
import static net.osmand.plus.settings.controllers.BatteryOptimizationController.isIgnoringBatteryOptimizations;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorTrackDataType;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet.ResetAppModePrefsListener;
import net.osmand.plus.settings.bottomsheets.SingleSelectPreferenceBottomSheet;
import net.osmand.plus.settings.controllers.BatteryOptimizationController;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.track.fragments.controller.SelectRouteActivityController;
import net.osmand.plus.track.helpers.RouteActivitySelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class MonitoringSettingsFragment extends BaseSettingsFragment implements CopyAppModePrefsListener, ResetAppModePrefsListener {

	private static final String DISABLE_BATTERY_OPTIMIZATION = "disable_battery_optimization";
	private static final String COPY_PLUGIN_SETTINGS = "copy_plugin_settings";
	private static final String RESET_TO_DEFAULT = "reset_to_default";
	private static final String OPEN_TRACKS = "open_tracks";
	private static final String EXTERNAL_SENSORS = "open_sensor_settings";
	private static final String PRESELECTED_ROUTE_ACTIVITY = "current_track_route_activity";
	private static final String SAVE_GLOBAL_TRACK_INTERVAL = "save_global_track_interval";

	private RouteActivitySelectionHelper routeActivitySelectionHelper;
	boolean showSwitchProfile;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			showSwitchProfile = args.getBoolean(PLUGIN_INFO, false);
		}
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		View switchProfile = view.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			AndroidUiHelper.updateVisibility(switchProfile, showSwitchProfile);
		}
	}

	@Override
	public Bundle buildArguments() {
		Bundle args = super.buildArguments();
		args.putBoolean(PLUGIN_INFO, showSwitchProfile);
		return args;
	}

	@Override
	protected void setupPreferences() {
		setupDisableBatteryOptimizationPref();
		setupShowStartDialog();

		setupSaveTrackToGpxPref();
		setupSaveTrackIntervalPref();

		setupSaveGlobalTrackIntervalPref();
		setupSaveTrackMinDistancePref();
		setupSaveTrackPrecisionPref();
		setupSaveTrackMinSpeedPref();
		setupAutoSplitRecordingPref();
		setupDisableRecordingOnceAppKilledPref();

		setupTrackStorageDirectoryPref();
		setupExternalSensorsPref();
		setupPreselectedRouteActivityPref();
		setupShowTripRecNotificationPref();
		setupLiveMonitoringPref();

		setupOpenNotesDescrPref();
		setupOpenNotesPref();

		setupCopyProfileSettingsPref();
		setupResetToDefaultPref();
	}

	private void setupDisableBatteryOptimizationPref() {
		Preference preference = findPreference(DISABLE_BATTERY_OPTIMIZATION);
		preference.setIcon(getIcon(R.drawable.ic_action_warning_colored));
		preference.setVisible(!isIgnoringBatteryOptimizations(app));
	}

	private void setupShowStartDialog() {
		SwitchPreferenceEx showStartDialog = findPreference(settings.SHOW_TRIP_REC_START_DIALOG.getId());
		showStartDialog.setDescription(getString(R.string.trip_recording_show_start_dialog_setting));
		showStartDialog.setIcon(getPersistentPrefIcon(R.drawable.ic_action_dialog));
	}

	private void setupSaveTrackToGpxPref() {
		SwitchPreferenceCompat saveTrackToGpx = findPreference(settings.SAVE_TRACK_TO_GPX.getId());
		saveTrackToGpx.setSummary(getString(R.string.save_track_to_gpx_descrp));
		saveTrackToGpx.setIcon(getPersistentPrefIcon(R.drawable.ic_action_gdirections_dark));
	}

	private void setupSaveTrackIntervalPref() {
		HashMap<Object, String> entry = getTimeValues();
		ListPreferenceEx saveTrackInterval = findPreference(settings.SAVE_TRACK_INTERVAL.getId());
		saveTrackInterval.setEntries(entry.values().toArray(new String[0]));
		saveTrackInterval.setEntryValues(entry.keySet().toArray());
		saveTrackInterval.setIcon(getActiveIcon(R.drawable.ic_action_time_span));
		saveTrackInterval.setDescription(R.string.save_track_interval_descr);
		saveTrackInterval.setVisible(settings.SAVE_TRACK_TO_GPX.getModeValue(getSelectedAppMode()));
	}

	private void setupSaveGlobalTrackIntervalPref() {
		HashMap<Object, String> entry = getTimeValues();
		ListPreferenceEx saveTrackInterval = findPreference(settings.SAVE_GLOBAL_TRACK_INTERVAL.getId());
		saveTrackInterval.setEntries(entry.values().toArray(new String[0]));
		saveTrackInterval.setEntryValues(entry.keySet().toArray());
		ApplicationMode selectedAppMode = getSelectedAppMode();
		saveTrackInterval.setValue(settings.SAVE_GLOBAL_TRACK_INTERVAL.getModeValue(selectedAppMode));
		saveTrackInterval.setIcon(getActiveIcon(R.drawable.ic_action_time_span));
		saveTrackInterval.setDescription(R.string.save_global_track_interval_descr);
	}

	private HashMap<Object, String> getTimeValues() {
		HashMap<Object, String> entry = new LinkedHashMap<>();
		for (int second : SECONDS) {
			entry.put(second * 1000, second + " " + getString(R.string.int_seconds));
		}
		for (int minute : MINUTES) {
			entry.put((minute * 60) * 1000, minute + " " + getString(R.string.int_min));
		}
		return entry;
	}

	private void setupSaveTrackMinDistancePref() {
		Float[] entryValues = {0.f, 2.0f, 5.0f, 10.0f, 20.0f, 30.0f, 50.0f};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_not_selected);
		for (int i = 1; i < entryValues.length; i++) {
			entries[i] = entryValues[i].intValue() + " " + getString(R.string.m);
		}

		ListPreferenceEx saveTrackMinDistance = findPreference(settings.SAVE_TRACK_MIN_DISTANCE.getId());
		saveTrackMinDistance.setEntries(entries);
		saveTrackMinDistance.setEntryValues(entryValues);

		SpannableStringBuilder stringBuilder = new SpannableStringBuilder(getString(R.string.monitoring_min_distance_descr));
		stringBuilder.append("\n");
		stringBuilder.append(getString(R.string.monitoring_min_distance_descr_side_effect));
		stringBuilder.append("\n");
		stringBuilder.append(getString(R.string.monitoring_min_distance_descr_recommendation));

		saveTrackMinDistance.setDescription(stringBuilder.toString());
	}

	private void setupSaveTrackPrecisionPref() {
		Float[] entryValues = {0.f, 1.0f, 2.0f, 5.0f, 10.0f, 15.0f, 20.0f, 50.0f, 100.0f};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_not_selected);
		for (int i = 1; i < entryValues.length; i++) {
			entries[i] = entryValues[i].intValue() + " " + getString(R.string.m) + "  (" + Math.round(entryValues[i] / 0.3048f) + " " + getString(R.string.foot) + ")";
		}

		ListPreferenceEx saveTrackPrecision = findPreference(settings.SAVE_TRACK_PRECISION.getId());
		saveTrackPrecision.setEntries(entries);
		saveTrackPrecision.setEntryValues(entryValues);

		SpannableStringBuilder stringBuilder = new SpannableStringBuilder(getString(R.string.monitoring_min_accuracy_descr));
		stringBuilder.append("\n");
		stringBuilder.append(getString(R.string.monitoring_min_accuracy_descr_side_effect));
		stringBuilder.append("\n");
		stringBuilder.append(getString(R.string.monitoring_min_accuracy_descr_recommendation));
		stringBuilder.append("\n");
		stringBuilder.append(getString(R.string.monitoring_min_accuracy_descr_remark));

		saveTrackPrecision.setDescription(stringBuilder.toString());
	}

	private void setupSaveTrackMinSpeedPref() {
		Float[] entryValues = {0.f, 0.000001f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_not_selected);
		entries[1] = "> 0"; // This option for the GPS chipset motion detection
		for (int i = 2; i < entryValues.length; i++) {
			entries[i] = entryValues[i].intValue() + " " + getString(R.string.km_h);
			entryValues[i] = entryValues[i] / 3.6f;
		}

		ListPreferenceEx saveTrackMinSpeed = findPreference(settings.SAVE_TRACK_MIN_SPEED.getId());
		saveTrackMinSpeed.setEntries(entries);
		saveTrackMinSpeed.setEntryValues(entryValues);

		SpannableStringBuilder stringBuilder = new SpannableStringBuilder(getString(R.string.monitoring_min_speed_descr));
		stringBuilder.append("\n");
		stringBuilder.append(getString(R.string.monitoring_min_speed_descr_side_effect));
		stringBuilder.append("\n");
		stringBuilder.append(getString(R.string.monitoring_min_speed_descr_recommendation));
		stringBuilder.append("\n");
		stringBuilder.append(getString(R.string.monitoring_min_speed_descr_remark));

		saveTrackMinSpeed.setDescription(stringBuilder.toString());
	}

	private void setupAutoSplitRecordingPref() {
		SwitchPreferenceEx autoSplitRecording = findPreference(settings.AUTO_SPLIT_RECORDING.getId());
		autoSplitRecording.setDescription(getString(R.string.auto_split_recording_descr));
	}

	private void setupDisableRecordingOnceAppKilledPref() {
		SwitchPreferenceEx disableRecordingOnceAppKilled = findPreference(settings.DISABLE_RECORDING_ONCE_APP_KILLED.getId());
		disableRecordingOnceAppKilled.setDescription(getString(R.string.disable_recording_once_app_killed_descrp));
	}

	private void setupShowTripRecNotificationPref() {
		SwitchPreferenceEx showTripRecNotification = findPreference(settings.SHOW_TRIP_REC_NOTIFICATION.getId());
		showTripRecNotification.setDescription(getString(R.string.trip_rec_notification_settings_desc));
		showTripRecNotification.setIcon(getPersistentPrefIcon(R.drawable.ic_action_notification));
	}

	private void setupTrackStorageDirectoryPref() {
		Integer[] entryValues = {REC_DIRECTORY, MONTHLY_DIRECTORY};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.store_tracks_in_rec_directory);
		entries[1] = getString(R.string.store_tracks_in_monthly_directories);
//		entries[2] = getString(R.string.store_tracks_in_daily_directories);

		ListPreferenceEx trackStorageDirectory = findPreference(settings.TRACK_STORAGE_DIRECTORY.getId());
		trackStorageDirectory.setEntries(entries);
		trackStorageDirectory.setEntryValues(entryValues);
		trackStorageDirectory.setDescription(R.string.track_storage_directory_descrp);
		trackStorageDirectory.setIcon(getActiveIcon(R.drawable.ic_action_folder));
	}

	private void setupExternalSensorsPref() {
		Preference openExternalSensors = findPreference(EXTERNAL_SENSORS);
		if (openExternalSensors != null) {
			if (PluginsHelper.isEnabled(ExternalSensorsPlugin.class)) {
				openExternalSensors.setVisible(true);
				List<String> linkedSensorNames = getLinkedSensorNames();
				if (linkedSensorNames.isEmpty()) {
					@ColorRes int iconColor = isNightMode() ? R.color.icon_color_default_light : R.color.icon_color_default_dark;
					openExternalSensors.setIcon(getIcon(R.drawable.ic_action_sensor, iconColor));
					openExternalSensors.setSummary(R.string.shared_string_none);
				} else {
					openExternalSensors.setIcon(getActiveIcon(R.drawable.ic_action_sensor));
					StringBuilder summary = new StringBuilder();
					for (String sensorName : linkedSensorNames) {
						if (!Algorithms.isEmpty(summary)) {
							summary.append(", ");
						}
						summary.append(sensorName);
					}
					openExternalSensors.setSummary(summary);
				}
			}
		}
	}

	private void setupPreselectedRouteActivityPref() {
		Preference preference = findPreference(PRESELECTED_ROUTE_ACTIVITY);
		if (preference != null) {
			ApplicationMode selectedAppMode = getSelectedAppMode();
			String selectedId = settings.CURRENT_TRACK_ROUTE_ACTIVITY.getModeValue(selectedAppMode);
			RouteActivityHelper helper = app.getRouteActivityHelper();
			RouteActivity activity = helper.findRouteActivity(selectedId);
			if (activity != null) {
				int iconId = AndroidUtils.getIconId(app, activity.getIconName());
				preference.setIcon(getContentIcon(iconId));
				preference.setSummary(activity.getLabel());
			} else {
				preference.setIcon(getContentIcon(R.drawable.ic_action_activity));
				preference.setSummary(getString(R.string.shared_string_none));
			}
		}
	}

	private void setPreferenceVisible(@NonNull String preferenceId, boolean isVisible) {
		Preference preference = findPreference(preferenceId);
		if (preference != null) {
			preference.setVisible(isVisible);
		}
	}

	@NonNull
	private List<String> getLinkedSensorNames() {
		List<String> names = new ArrayList<>();
		ExternalSensorsPlugin plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
		if (plugin != null) {
			ApplicationMode appMode = getSelectedAppMode();
			for (ExternalSensorTrackDataType dataType : ExternalSensorTrackDataType.values()) {
				CommonPreference<String> pref = plugin.getWriteToTrackDeviceIdPref(dataType);

				String deviceId = pref.getModeValue(appMode);
				if (!Algorithms.isEmpty(deviceId)) {
					boolean sensorLinked = false;
					if (!plugin.isAnyConnectedDeviceId(deviceId)) {
						if (plugin.getDevice(deviceId) != null) {
							sensorLinked = true;
						}
					} else if (plugin.getAnyDevice(dataType.getSensorType()) != null) {
						sensorLinked = true;
					}
					if (sensorLinked) {
						names.add(getString(dataType.getTitleId()));
					}
				}
			}
		}
		return names;
	}

	private void setupLiveMonitoringPref() {
		Drawable disabled = getContentIcon(R.drawable.ic_action_offline);
		Drawable enabled = getActiveIcon(R.drawable.ic_world_globe_dark);
		Drawable icon = getPersistentPrefIcon(enabled, disabled);

		SwitchPreferenceEx liveMonitoring = findPreference(settings.LIVE_MONITORING.getId());
		liveMonitoring.setDescription(getString(R.string.live_monitoring_m_descr));
		liveMonitoring.setIcon(icon);
	}

	private void setupOpenNotesDescrPref() {
		String menu = getString(R.string.shared_string_menu);
		String myPlaces = getString(R.string.shared_string_my_places);
		String tracks = getString(R.string.shared_string_tracks);
		String tracksPath = getString(R.string.ltr_or_rtl_triple_combine_via_dash, menu, myPlaces, tracks);
		String tracksPathDescr = getString(R.string.tracks_view_descr, tracksPath);

		int startIndex = tracksPathDescr.indexOf(tracksPath);
		SpannableString titleSpan = new SpannableString(tracksPathDescr);
		titleSpan.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), startIndex, startIndex + tracksPath.length(), 0);

		Preference openTracksDescription = findPreference("open_tracks_description");
		openTracksDescription.setTitle(titleSpan);
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
	public void onResume() {
		super.onResume();
		setupDisableBatteryOptimizationPref();
	}

	@Override
	public void onDestroy() {
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			Fragment target = getTargetFragment();
			if (target instanceof TripRecordingStartingBottomSheet) {
				((TripRecordingStartingBottomSheet) target).show();
			}
		}
		super.onDestroy();
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (DISABLE_BATTERY_OPTIMIZATION.equals(preference.getKey())) {
			setupPrefRoundedBg(holder);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (OPEN_TRACKS.equals(prefId)) {
			Bundle bundle = new Bundle();
			bundle.putInt(TAB_ID, MyPlacesActivity.GPX_TAB);

			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			Intent favorites = new Intent(preference.getContext(), appCustomization.getMyPlacesActivity());
			favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			favorites.putExtra(MapActivity.INTENT_PARAMS, bundle);
			startActivity(favorites);
			return true;
		} else if (COPY_PLUGIN_SETTINGS.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SelectCopyAppModeBottomSheet.showInstance(fragmentManager, this, getSelectedAppMode());
			}
		} else if (RESET_TO_DEFAULT.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ResetProfilePrefsBottomSheet.showInstance(fragmentManager, getSelectedAppMode(), this);
			}
		} else if (DISABLE_BATTERY_OPTIMIZATION.endsWith(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				BatteryOptimizationController.showDialog(mapActivity, false, null);
			}
		} else if (PRESELECTED_ROUTE_ACTIVITY.equals(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				SelectRouteActivityController.showDialog(mapActivity, getRouteActivitySelectionHelper());
			}
		}
		return super.onPreferenceClick(preference);
	}

	@NonNull
	private RouteActivitySelectionHelper getRouteActivitySelectionHelper() {
		if (routeActivitySelectionHelper == null) {
			SelectRouteActivityController controller = SelectRouteActivityController.getExistedInstance(app);
			if (controller != null) {
				routeActivitySelectionHelper = controller.getRouteActivityHelper();
			}
			if (routeActivitySelectionHelper == null) {
				RouteActivityHelper helper = app.getRouteActivityHelper();
				routeActivitySelectionHelper = new RouteActivitySelectionHelper();
				ApplicationMode selectedAppMode = getSelectedAppMode();
				String selectedId = settings.CURRENT_TRACK_ROUTE_ACTIVITY.getModeValue(selectedAppMode);
				RouteActivity selected = helper.findRouteActivity(selectedId);
				routeActivitySelectionHelper.setSelectedActivity(selected);
			}
		}
		routeActivitySelectionHelper.setActivitySelectionListener(newRouteActivity -> {
			String id = newRouteActivity != null ? newRouteActivity.getId() : "";
			onPreferenceChange(requirePreference(PRESELECTED_ROUTE_ACTIVITY), id);
		});
		return routeActivitySelectionHelper;
	}

	@Override
	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		if (SAVE_GLOBAL_TRACK_INTERVAL.equals(prefId)) {
			OsmandPreference pref = settings.getPreference(prefId);
			if (newValue instanceof Boolean) {
				applyPreference(settings.SAVE_GLOBAL_TRACK_REMEMBER.getId(), applyToAllProfiles, false);
			} else if (pref instanceof CommonPreference
					&& !((CommonPreference) pref).hasDefaultValueForMode(getSelectedAppMode())) {
				applyPreference(SAVE_GLOBAL_TRACK_INTERVAL, applyToAllProfiles, newValue);
				applyPreference(settings.SAVE_GLOBAL_TRACK_REMEMBER.getId(), applyToAllProfiles, true);
			}
		} else {
			super.onApplyPreferenceChange(prefId, applyToAllProfiles, newValue);
		}
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		String prefId = preference.getKey();
		if (settings.SAVE_TRACK_MIN_DISTANCE.getId().equals(prefId)
				|| settings.SAVE_TRACK_PRECISION.getId().equals(prefId)
				|| settings.SAVE_TRACK_MIN_SPEED.getId().equals(prefId)) {
			FragmentManager fm = getFragmentManager();
			if (fm != null) {
				ApplicationMode appMode = getSelectedAppMode();
				SingleSelectPreferenceBottomSheet.showInstance(fm, prefId, this, false, appMode, true, true);
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			app.getSettings().copyProfilePreferences(appMode, getSelectedAppMode(), plugin.getPreferences());
			updateAllSettings();
		}
	}

	@Override
	public void resetAppModePrefs(ApplicationMode appMode) {
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			app.getSettings().resetProfilePreferences(appMode, plugin.getPreferences());
			app.showToastMessage(R.string.plugin_prefs_reset_successful);
			updateAllSettings();
		}
	}
}