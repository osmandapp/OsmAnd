package net.osmand.plus.settings.fragments;

import androidx.fragment.app.Fragment;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.fragments.MainExternalInputDevicesFragment;
import net.osmand.plus.plugins.accessibility.AccessibilitySettingsFragment;
import net.osmand.plus.plugins.audionotes.MultimediaNotesFragment;
import net.osmand.plus.plugins.development.DevelopmentSettingsFragment;
import net.osmand.plus.plugins.externalsensors.ExternalSettingsWriteToTrackSettingsFragment;
import net.osmand.plus.plugins.externalsensors.dialogs.ExternalDevicesListFragment;
import net.osmand.plus.plugins.monitoring.MonitoringSettingsFragment;
import net.osmand.plus.plugins.odb.dialogs.OBDDevicesListFragment;
import net.osmand.plus.plugins.odb.dialogs.OBDMainFragment;
import net.osmand.plus.plugins.osmedit.fragments.OsmEditingFragment;
import net.osmand.plus.plugins.weather.dialogs.WeatherSettingsFragment;
import net.osmand.plus.settings.datastorage.DataStorageFragment;
import net.osmand.plus.settings.fragments.profileappearance.ProfileAppearanceFragment;
import net.osmand.plus.settings.fragments.voice.VoiceAnnouncesFragment;

public enum SettingsScreenType {

	MAIN_SETTINGS(MainSettingsFragment.class, false, null, R.xml.settings_main_screen, R.layout.global_preference_toolbar),
	GLOBAL_SETTINGS(GlobalSettingsFragment.class, false, null, R.xml.global_settings, R.layout.global_preference_toolbar),
	CONFIGURE_PROFILE(ConfigureProfileFragment.class, true, null, R.xml.configure_profile, R.layout.profile_preference_toolbar_with_switch),
	PROXY_SETTINGS(ProxySettingsFragment.class, false, null, R.xml.proxy_preferences, R.layout.global_preferences_toolbar_with_switch),
	SEND_UUID(SendUniqueIdentifiersFragment.class, false, null, R.xml.send_uuid_preferences, R.layout.global_preference_toolbar),
	GENERAL_PROFILE(GeneralProfileSettingsFragment.class, true, ApplyQueryType.BOTTOM_SHEET, R.xml.general_profile_settings, R.layout.profile_preference_toolbar),
	NAVIGATION(NavigationFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.navigation_settings_new, R.layout.profile_preference_toolbar),
	COORDINATES_FORMAT(CoordinatesFormatFragment.class, true, ApplyQueryType.BOTTOM_SHEET, R.xml.coordinates_format, R.layout.profile_preference_toolbar),
	ROUTE_PARAMETERS(RouteParametersFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.route_parameters, R.layout.profile_preference_toolbar),
	SCREEN_ALERTS(ScreenAlertsFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.screen_alerts, R.layout.profile_preference_toolbar_with_switch),
	VOICE_ANNOUNCES(VoiceAnnouncesFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.voice_announces, R.layout.profile_preference_toolbar_with_switch),
	VEHICLE_PARAMETERS(VehicleParametersFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.vehicle_parameters, R.layout.profile_preference_toolbar),
	MAP_DURING_NAVIGATION(MapDuringNavigationFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.map_during_navigation, R.layout.profile_preference_toolbar),
	TURN_SCREEN_ON(TurnScreenOnFragment.class, true, ApplyQueryType.BOTTOM_SHEET, R.xml.turn_screen_on, R.layout.profile_preference_toolbar),
	DATA_STORAGE(DataStorageFragment.class, false, null, R.xml.data_storage, R.layout.global_preference_toolbar),
	DIALOGS_AND_NOTIFICATIONS_SETTINGS(DialogsAndNotificationsSettingsFragment.class, false, null, R.xml.dialogs_and_notifications_preferences, R.layout.global_preference_toolbar),
	HISTORY_SETTINGS(HistorySettingsFragment.class, false, null, R.xml.history_preferences, R.layout.global_preference_toolbar),
	PROFILE_APPEARANCE(ProfileAppearanceFragment.class, true, null, R.xml.profile_appearance_screen, R.layout.profile_preference_toolbar),
	OPEN_STREET_MAP_EDITING(OsmEditingFragment.class, false, null, R.xml.osm_editing, R.layout.global_preference_toolbar),
	// FK-TODO: make searchable:
	MULTIMEDIA_NOTES(MultimediaNotesFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.multimedia_notes, R.layout.profile_preference_toolbar),
	// FK-TODO: make searchable:
	MONITORING_SETTINGS(MonitoringSettingsFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.monitoring_settings, R.layout.profile_preference_toolbar),
	LIVE_MONITORING(LiveMonitoringFragment.class, false, ApplyQueryType.SNACK_BAR, R.xml.live_monitoring, R.layout.global_preferences_toolbar_with_switch),
	ACCESSIBILITY_SETTINGS(AccessibilitySettingsFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.accessibility_settings, R.layout.profile_preference_toolbar),
	DEVELOPMENT_SETTINGS(DevelopmentSettingsFragment.class, false, null, R.xml.development_settings, R.layout.global_preference_toolbar),
	// FK-TODO: make searchable:
	SIMULATION_NAVIGATION(SimulationNavigationSettingFragment.class, true, ApplyQueryType.NONE, R.xml.simulation_navigation_setting, R.layout.profile_preference_toolbar_with_switch),
	ANT_PLUS_SETTINGS(ExternalDevicesListFragment.class, false, null, R.xml.antplus_settings, R.layout.global_preference_toolbar),
	VEHICLE_METRICS_SETTINGS(OBDDevicesListFragment.class, false, null, R.xml.antplus_settings, R.layout.global_preference_toolbar),
	VEHICLE_CONNECTED_METRICS_SETTINGS(OBDMainFragment.class, false, null, R.xml.antplus_settings, R.layout.global_preference_toolbar),
	WEATHER_SETTINGS(WeatherSettingsFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.weather_settings, R.layout.profile_preference_toolbar),
	EXTERNAL_SETTINGS_WRITE_TO_TRACK_SETTINGS(ExternalSettingsWriteToTrackSettingsFragment.class, true, ApplyQueryType.BOTTOM_SHEET, R.xml.external_sensors_write_to_track_settings, R.layout.profile_preference_toolbar),
	DANGEROUS_GOODS(DangerousGoodsFragment.class, true, ApplyQueryType.NONE, R.xml.dangerous_goods_parameters, R.layout.global_preference_toolbar),
	EXTERNAL_INPUT_DEVICE(MainExternalInputDevicesFragment.class, true, ApplyQueryType.SNACK_BAR, R.xml.external_input_device_settings, R.layout.profile_preference_toolbar_with_switch);

	public final Class<? extends Fragment> fragmentClass;
	public final String fragmentName;
	public final boolean profileDependent;
	public final ApplyQueryType applyQueryType;
	public final int preferencesResId;
	public final int toolbarResId;

	SettingsScreenType(Class<? extends Fragment> fragmentClass, boolean profileDependent, ApplyQueryType applyQueryType, int preferencesResId, int toolbarResId) {
		this.fragmentClass = fragmentClass;
		this.fragmentName = fragmentClass.getName();
		this.profileDependent = profileDependent;
		this.applyQueryType = applyQueryType;
		this.preferencesResId = preferencesResId;
		this.toolbarResId = toolbarResId;
	}
}
