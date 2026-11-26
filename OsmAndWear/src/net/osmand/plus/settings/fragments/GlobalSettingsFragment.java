package net.osmand.plus.settings.fragments;

import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILE_KEY_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.USE_LAST_PROFILE_ARG;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.dialogs.LocationSourceBottomSheet;
import net.osmand.plus.dialogs.MapRenderingEngineDialog;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.feedback.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.feedback.SendAnalyticsBottomSheetDialogFragment.OnSendAnalyticsPrefsUpdate;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.profiles.SelectDefaultProfileBottomSheet;
import net.osmand.plus.profiles.SelectProfileBottomSheet.OnSelectProfileCallback;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.DataStorageHelper;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.enums.LocationSource;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.Map;


public class GlobalSettingsFragment extends BaseSettingsFragment
		implements OnSendAnalyticsPrefsUpdate, OnSelectProfileCallback {

	public static final String TAG = GlobalSettingsFragment.class.getSimpleName();

	private static final String HISTORY_PREF_ID = "history";
	private static final String MAP_RENDERING_ENGINE_ID = "map_rendering_engine";
	private static final String SEND_ANONYMOUS_DATA_PREF_ID = "send_anonymous_data";
	private static final String DIALOGS_AND_NOTIFICATIONS_PREF_ID = "dialogs_and_notifications";
	private static final String SEND_UNIQUE_USER_IDENTIFIER_PREF_ID = "send_unique_user_identifier";
	private static final String ENABLE_PROXY_PREF_ID = "enable_proxy";

	@Override
	protected void setupPreferences() {
		setupDefaultAppModePref();
		setupPreferredLocalePref();
		setupExternalStorageDirPref();
		setupMapRenderingEnginePref();

		setupSendAnonymousDataPref();
		setupSendUniqueIdentifiersPreference();
		setupDialogsAndNotificationsPref();
		setupHistoryPref();
		setupEnableProxyPref();
		setupLocationSourcePref();
		setupAutoBackupPref();
		setupUninstallSpeedCamerasPref();
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		String prefId = preference.getKey();

		if (prefId.equals(SEND_ANONYMOUS_DATA_PREF_ID)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SendAnalyticsBottomSheetDialogFragment.showInstance(app, fragmentManager, this);
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		String prefId = preference.getKey();
		if (DIALOGS_AND_NOTIFICATIONS_PREF_ID.equals(prefId)) {
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null) {
				boolean enabled = preference.isEnabled() && (!settings.DO_NOT_SHOW_STARTUP_MESSAGES.get() || settings.SHOW_DOWNLOAD_MAP_DIALOG.get());
				imageView.setEnabled(enabled);
			}
		} else if (SEND_UNIQUE_USER_IDENTIFIER_PREF_ID.equals(prefId)) {
			boolean enabled = settings.SEND_UNIQUE_USER_IDENTIFIER.get();
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null) {
				imageView.setEnabled(enabled);
			}
			TextView tvNumbers = (TextView) holder.findViewById(R.id.secondary_description);
			if (tvNumbers != null) {
				int totalCount = 1;
				int enabledCount = enabled ? 1 : 0;
				String pattern = getString(R.string.ltr_or_rtl_combine_via_slash,
						String.valueOf(enabledCount), String.valueOf(totalCount));
				tvNumbers.setText(pattern);
			}
		} else if (HISTORY_PREF_ID.equals(prefId)) {
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null) {
				boolean enabled = preference.isEnabled()
						&& (settings.SEARCH_HISTORY.get()
						|| settings.NAVIGATION_HISTORY.get()
						|| settings.MAP_MARKERS_HISTORY.get());
				imageView.setEnabled(enabled);
			}
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();

		if (prefId.equals(SEND_ANONYMOUS_DATA_PREF_ID)) {
			if (newValue instanceof Boolean) {
				boolean enabled = (Boolean) newValue;
				if (enabled) {
					FragmentManager fragmentManager = getFragmentManager();
					if (fragmentManager != null) {
						SendAnalyticsBottomSheetDialogFragment.showInstance(app, fragmentManager, this);
					}
				} else {
					settings.SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.set(false);
					settings.SEND_ANONYMOUS_APP_USAGE_DATA.set(false);
					return true;
				}
			}
			return false;
		} else if (prefId.equals(settings.AUTO_BACKUP_ENABLED.getId())) {
			BackupManager.dataChanged(app.getPackageName());
		}
		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		if (prefId.equals(settings.PREFERRED_LOCALE.getId())) {
			// recreate activity to update locale
			Activity activity = getActivity();
			OsmandApplication app = getMyApplication();
			if (app != null && activity != null) {
				app.getLocaleHelper().checkPreferredLocale();
				RestartActivity.doRestart(activity);
			}
		} else if (prefId.equals(settings.SPEED_CAMERAS_UNINSTALLED.getId())) {
			setupUninstallSpeedCamerasPref();
		} else if (prefId.equals(settings.LOCATION_SOURCE.getId())) {
			setupLocationSourcePref();
		} else if (prefId.equals(settings.AUTO_BACKUP_ENABLED.getId())) {
			BackupManager.dataChanged(app.getPackageName());
		}
	}

	@Override
	public void onAnalyticsPrefsUpdate() {
		setupSendAnonymousDataPref();
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (prefId.equals(settings.DEFAULT_APPLICATION_MODE.getId())) {
			if (getActivity() != null) {
				String defaultModeKey = settings.DEFAULT_APPLICATION_MODE.get().getStringKey();
				SelectDefaultProfileBottomSheet.showInstance(
						getActivity(), this, getSelectedAppMode(), defaultModeKey, false);
			}
		} else if (settings.SPEED_CAMERAS_UNINSTALLED.getId().equals(prefId) && !settings.SPEED_CAMERAS_UNINSTALLED.get()) {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				SpeedCamerasBottomSheet.showInstance(manager, this);
			}
		} else if (prefId.equals(settings.LOCATION_SOURCE.getId())) {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				LocationSourceBottomSheet.showInstance(manager, this);
			}
		} else if (prefId.equals(MAP_RENDERING_ENGINE_ID)) {
			new MapRenderingEngineDialog(app, getActivity()).showDialog(this::setupMapRenderingEnginePref);
		}

		return super.onPreferenceClick(preference);
	}

	private void setupDefaultAppModePref() {
		Preference defaultApplicationMode = findPreference(settings.DEFAULT_APPLICATION_MODE.getId());
		String summary;
		int iconId;
		if (settings.USE_LAST_APPLICATION_MODE_BY_DEFAULT.get()) {
			summary = getString(R.string.shared_string_last_used);
			iconId = R.drawable.ic_action_manage_profiles;
		} else {
			ApplicationMode appMode = settings.DEFAULT_APPLICATION_MODE.get();
			summary = appMode.toHumanString();
			iconId = appMode.getIconRes();
		}
		defaultApplicationMode.setIcon(getPaintedIcon(iconId, settings.getApplicationMode().getProfileColor(isNightMode())));
		defaultApplicationMode.setSummary(summary);
	}

	private void setupPreferredLocalePref() {
		boolean visible = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU;
		ListPreferenceEx preference = findPreference(settings.PREFERRED_LOCALE.getId());
		preference.setVisible(visible);
		if (visible) {
			preference.setIcon(getContentIcon(R.drawable.ic_action_map_language));
			preference.setSummary(settings.PREFERRED_LOCALE.get());

			Map<String, String> preferredLanguages = LocaleHelper.getPreferredDisplayLanguages(app);
			String[] languagesNames = preferredLanguages.values().toArray(new String[0]);
			String[] languagesIds = preferredLanguages.keySet().toArray(new String[0]);
			preference.setEntries(languagesNames);
			preference.setEntryValues(languagesIds);

			// Add " (Display language)" to menu title in Latin letters for all non-en languages
			if (!getResources().getString(R.string.preferred_locale).equals(getResources().getString(R.string.preferred_locale_no_translate))) {
				preference.setTitle(getString(R.string.preferred_locale) + " (" + getString(R.string.preferred_locale_no_translate) + ")");
			}
		}
	}

	private void setupExternalStorageDirPref() {
		Preference preference = findPreference(OsmandSettings.EXTERNAL_STORAGE_DIR);
		preference.setIcon(getContentIcon(R.drawable.ic_action_folder));

		DataStorageHelper storageHelper = new DataStorageHelper(app);
		StorageItem currentStorage = storageHelper.getCurrentStorage();

		long totalUsed = settings.OSMAND_USAGE_SPACE.get();
		if (totalUsed > 0) {
			String[] usedMemoryFormats = {
					getString(R.string.shared_string_memory_used_kb_desc),
					getString(R.string.shared_string_memory_used_mb_desc),
					getString(R.string.shared_string_memory_used_gb_desc),
					getString(R.string.shared_string_memory_used_tb_desc)
			};
			String usedSpace = DataStorageHelper.getFormattedMemoryInfo(totalUsed, usedMemoryFormats);
			String summary = getString(R.string.data_storage_preference_summary, currentStorage.getTitle(), usedSpace);
			preference.setSummary(summary.replaceAll(" • ", "  •  "));
		} else {
			preference.setSummary(currentStorage.getTitle());
		}
	}

	private void setupMapRenderingEnginePref() {
		Preference preference = findPreference(MAP_RENDERING_ENGINE_ID);
		preference.setIcon(getContentIcon(R.drawable.ic_map));
		preference.setSummary(settings.USE_OPENGL_RENDER.get() ? R.string.map_rendering_engine_v2 : R.string.map_rendering_engine_v1);
		//preference.setVisible(Version.isOpenGlAvailable(app));
	}

	private void setupSendAnonymousDataPref() {
		boolean enabled = settings.SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.get() || settings.SEND_ANONYMOUS_APP_USAGE_DATA.get();

		SwitchPreferenceCompat sendAnonymousData = findPreference(SEND_ANONYMOUS_DATA_PREF_ID);
		sendAnonymousData.setChecked(enabled);
		sendAnonymousData.setIcon(getPersistentPrefIcon(R.drawable.ic_action_privacy_and_security));
	}

	private void setupSendUniqueIdentifiersPreference() {
		Preference sendUuid = findPreference(SEND_UNIQUE_USER_IDENTIFIER_PREF_ID);
		sendUuid.setIcon(getPersistentPrefIcon(R.drawable.ic_action_world_globe));
	}

	private void setupDialogsAndNotificationsPref() {
		boolean showStartupMessages = !settings.DO_NOT_SHOW_STARTUP_MESSAGES.get();
		boolean showDownloadMapDialog = settings.SHOW_DOWNLOAD_MAP_DIALOG.get();
		String summary;
		if (showStartupMessages && showDownloadMapDialog) {
			summary = getString(R.string.shared_string_all);
		} else if (showStartupMessages || showDownloadMapDialog) {
			summary = getString(R.string.ltr_or_rtl_combine_via_slash, "1", "2");
		} else {
			summary = getString(R.string.shared_string_disabled);
		}
		Preference dialogsAndNotifications = findPreference(DIALOGS_AND_NOTIFICATIONS_PREF_ID);
		dialogsAndNotifications.setIcon(getPersistentPrefIcon(R.drawable.ic_action_notification));
		dialogsAndNotifications.setSummary(summary);
	}

	private void setupLocationSourcePref() {
		Preference preference = findPreference(settings.LOCATION_SOURCE.getId());
		preference.setIcon(getContentIcon(R.drawable.ic_action_device_location));

		LocationSource source = settings.LOCATION_SOURCE.get();
		preference.setSummary(source.nameId);
	}

	private void setupEnableProxyPref() {
		int iconId = R.drawable.ic_action_proxy;
		boolean enabled = settings.isProxyEnabled();
		Preference preference = findPreference(ENABLE_PROXY_PREF_ID);
		preference.setIcon(enabled ? getActiveIcon(iconId) : getContentIcon(iconId));
		preference.setSummary(enabled ? R.string.shared_string_on : R.string.shared_string_off);
	}

	private void setupHistoryPref() {
		Preference enableProxy = findPreference(HISTORY_PREF_ID);
		enableProxy.setIcon(getPersistentPrefIcon(R.drawable.ic_action_history));
	}

	private void setupAutoBackupPref() {
		SwitchPreferenceEx preference = findPreference(settings.AUTO_BACKUP_ENABLED.getId());
		preference.setIcon(getPersistentPrefIcon(R.drawable.ic_action_android));
		preference.setDescription(R.string.auto_backup_preference_desc);
	}

	private void setupUninstallSpeedCamerasPref() {
		boolean uninstalled = settings.SPEED_CAMERAS_UNINSTALLED.get();
		Preference uninstallSpeedCameras = findPreference(settings.SPEED_CAMERAS_UNINSTALLED.getId());
		if (!uninstalled) {
			uninstallSpeedCameras.setIcon(getActiveIcon(R.drawable.ic_speed_camera_disabled));
		}
		uninstallSpeedCameras.setTitle(uninstalled ? R.string.speed_cameras_removed_descr : R.string.uninstall_speed_cameras);
	}

	@Override
	public void onProfileSelected(Bundle args) {
		if (args.getBoolean(USE_LAST_PROFILE_ARG)) {
			settings.USE_LAST_APPLICATION_MODE_BY_DEFAULT.set(true);
		} else {
			settings.USE_LAST_APPLICATION_MODE_BY_DEFAULT.set(false);
			String value = args.getString(PROFILE_KEY_ARG);
			settings.setPreference(settings.DEFAULT_APPLICATION_MODE.getId(), value);
		}
		setupDefaultAppModePref();
	}
}
