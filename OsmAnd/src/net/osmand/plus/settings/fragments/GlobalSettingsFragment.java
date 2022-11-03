package net.osmand.plus.settings.fragments;

import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILE_KEY_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.USE_LAST_PROFILE_ARG;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
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
import net.osmand.plus.Version;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.dialogs.LocationSourceBottomSheet;
import net.osmand.plus.dialogs.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.dialogs.SendAnalyticsBottomSheetDialogFragment.OnSendAnalyticsPrefsUpdate;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.profiles.SelectDefaultProfileBottomSheet;
import net.osmand.plus.profiles.SelectProfileBottomSheet.OnSelectProfileCallback;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.DataStorageHelper;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.enums.LocationSource;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class GlobalSettingsFragment extends BaseSettingsFragment
		implements OnSendAnalyticsPrefsUpdate, OnSelectProfileCallback {

	public static final String TAG = GlobalSettingsFragment.class.getSimpleName();

	private static final String HISTORY_PREF_ID = "history";
	private static final String MAP_RENDERING_ENGINE_ID = "map_rendering_engine";
	private static final String SEND_ANONYMOUS_DATA_PREF_ID = "send_anonymous_data";
	private static final String DIALOGS_AND_NOTIFICATIONS_PREF_ID = "dialogs_and_notifications";
	private static final String SEND_UNIQUE_USER_IDENTIFIER_PREF_ID = "send_unique_user_identifier";

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
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
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
	public void onPreferenceChanged(String prefId) {
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
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		ListPreferenceEx preferredLocale = findPreference(settings.PREFERRED_LOCALE.getId());
		preferredLocale.setIcon(getContentIcon(R.drawable.ic_action_map_language));
		preferredLocale.setSummary(settings.PREFERRED_LOCALE.get());

		Map<String, String> preferredLanguages = getPreferredDisplayLanguages(ctx);
		String[] languagesNames = preferredLanguages.values().toArray(new String[0]);
		String[] languagesIds = preferredLanguages.keySet().toArray(new String[0]);
		preferredLocale.setEntries(languagesNames);
		preferredLocale.setEntryValues(languagesIds);

		// Add " (Display language)" to menu title in Latin letters for all non-en languages
		if (!getResources().getString(R.string.preferred_locale).equals(getResources().getString(R.string.preferred_locale_no_translate))) {
			preferredLocale.setTitle(getString(R.string.preferred_locale) + " (" + getString(R.string.preferred_locale_no_translate) + ")");
		}
	}

	private void setupExternalStorageDirPref() {
		Preference externalStorageDir = findPreference(OsmandSettings.EXTERNAL_STORAGE_DIR);
		externalStorageDir.setIcon(getContentIcon(R.drawable.ic_action_folder));

		DataStorageHelper holder = new DataStorageHelper(app);
		StorageItem currentStorage = holder.getCurrentStorage();
		long totalUsed = app.getSettings().OSMAND_USAGE_SPACE.get();
		if (totalUsed > 0) {
			String[] usedMemoryFormats = {
					getString(R.string.shared_string_memory_used_kb_desc),
					getString(R.string.shared_string_memory_used_mb_desc),
					getString(R.string.shared_string_memory_used_gb_desc),
					getString(R.string.shared_string_memory_used_tb_desc)
			};
			String sTotalUsed = DataStorageHelper.getFormattedMemoryInfo(totalUsed, usedMemoryFormats);
			String summary = String.format(getString(R.string.data_storage_preference_summary),
					currentStorage.getTitle(),
					sTotalUsed);
			summary = summary.replaceAll(" • ", "  •  ");
			externalStorageDir.setSummary(summary);
		} else {
			externalStorageDir.setSummary(currentStorage.getTitle());
		}
	}

	private void setupMapRenderingEnginePref() {
		Preference preference = findPreference(MAP_RENDERING_ENGINE_ID);
		preference.setIcon(getContentIcon(R.drawable.ic_map));
		preference.setSummary(settings.USE_OPENGL_RENDER.get() ? R.string.map_rendering_engine_v2 : R.string.map_rendering_engine_v1);
		preference.setVisible(Version.isOpenGlAvailable(app));
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
		SwitchPreferenceEx enableProxy = findPreference(settings.ENABLE_PROXY.getId());
		enableProxy.setIcon(getPersistentPrefIcon(R.drawable.ic_action_proxy));
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

	@NonNull
	private static Map<String, String> getPreferredDisplayLanguages(@NonNull Context ctx) {
		// See language list and statistics at: https://hosted.weblate.org/projects/osmand/main/
		// Hardy maintenance 2016-05-29:
		//  - Include languages if their translation is >= ~10%    (but any language will be visible if it is the device's system locale)
		//  - Mark as "incomplete" if                    < ~80%
		String incompleteSuffix = " (" + ctx.getString(R.string.incomplete_locale) + ")";

		// Add " (Device language)" to system default entry in Latin letters, so it can be more easily identified if a foreign language has been selected by mistake
		String deviceLanguageInLatin = " (" + ctx.getString(R.string.system_locale_no_translate) + ")";
		String systemDeviceLanguage = ctx.getString(R.string.system_locale) + deviceLanguageInLatin;

		Map<String, String> languages = new HashMap<>();
		languages.put("", systemDeviceLanguage);
		languages.put("en", ctx.getString(R.string.lang_en));
		languages.put("af", ctx.getString(R.string.lang_af) + incompleteSuffix);
		languages.put("ar", ctx.getString(R.string.lang_ar));
		languages.put("ast", ctx.getString(R.string.lang_ast) + incompleteSuffix);
		languages.put("az", ctx.getString(R.string.lang_az));
		languages.put("be", ctx.getString(R.string.lang_be));
		languages.put("bg", ctx.getString(R.string.lang_bg));
		languages.put("ca", ctx.getString(R.string.lang_ca));
		languages.put("cs", ctx.getString(R.string.lang_cs));
		languages.put("cy", ctx.getString(R.string.lang_cy) + incompleteSuffix);
		languages.put("da", ctx.getString(R.string.lang_da));
		languages.put("de", ctx.getString(R.string.lang_de));
		languages.put("el", ctx.getString(R.string.lang_el));
		languages.put("en_GB", ctx.getString(R.string.lang_en_gb));
		languages.put("eo", ctx.getString(R.string.lang_eo));
		languages.put("es", ctx.getString(R.string.lang_es));
		languages.put("es_AR", ctx.getString(R.string.lang_es_ar));
		languages.put("es_US", ctx.getString(R.string.lang_es_us));
		languages.put("eu", ctx.getString(R.string.lang_eu));
		languages.put("fa", ctx.getString(R.string.lang_fa));
		languages.put("fi", ctx.getString(R.string.lang_fi) + incompleteSuffix);
		languages.put("fr", ctx.getString(R.string.lang_fr));
		languages.put("gl", ctx.getString(R.string.lang_gl));
		languages.put("iw", ctx.getString(R.string.lang_he));
		languages.put("hr", ctx.getString(R.string.lang_hr) + incompleteSuffix);
		languages.put("hsb", ctx.getString(R.string.lang_hsb) + incompleteSuffix);
		languages.put("hu", ctx.getString(R.string.lang_hu));
		languages.put("hy", ctx.getString(R.string.lang_hy));
		languages.put("id", ctx.getString(R.string.lang_id));
		languages.put("is", ctx.getString(R.string.lang_is));
		languages.put("it", ctx.getString(R.string.lang_it));
		languages.put("ja", ctx.getString(R.string.lang_ja));
		languages.put("ka", ctx.getString(R.string.lang_ka) + incompleteSuffix);
		languages.put("kab", ctx.getString(R.string.lang_kab) + incompleteSuffix);
		languages.put("kn", ctx.getString(R.string.lang_kn) + incompleteSuffix);
		languages.put("ko", ctx.getString(R.string.lang_ko));
		languages.put("lt", ctx.getString(R.string.lang_lt));
		languages.put("lv", ctx.getString(R.string.lang_lv));
		languages.put("mk", ctx.getString(R.string.lang_mk));
		languages.put("ml", ctx.getString(R.string.lang_ml));
		languages.put("mr", ctx.getString(R.string.lang_mr) + incompleteSuffix);
		languages.put("nb", ctx.getString(R.string.lang_nb));
		languages.put("nl", ctx.getString(R.string.lang_nl));
		languages.put("nn", ctx.getString(R.string.lang_nn) + incompleteSuffix);
		languages.put("oc", ctx.getString(R.string.lang_oc) + incompleteSuffix);
		languages.put("pl", ctx.getString(R.string.lang_pl));
		languages.put("pt", ctx.getString(R.string.lang_pt));
		languages.put("pt_BR", ctx.getString(R.string.lang_pt_br));
		languages.put("ro", ctx.getString(R.string.lang_ro) + incompleteSuffix);
		languages.put("ru", ctx.getString(R.string.lang_ru));
		languages.put("sat", ctx.getString(R.string.lang_sat) + incompleteSuffix);
		languages.put("sc", ctx.getString(R.string.lang_sc));
		languages.put("sk", ctx.getString(R.string.lang_sk));
		languages.put("sl", ctx.getString(R.string.lang_sl));
		languages.put("sr", ctx.getString(R.string.lang_sr));
		languages.put("sr+Latn", ctx.getString(R.string.lang_sr_latn) + incompleteSuffix);
		languages.put("sv", ctx.getString(R.string.lang_sv));
		languages.put("tr", ctx.getString(R.string.lang_tr));
		languages.put("uk", ctx.getString(R.string.lang_uk));
		languages.put("vi", ctx.getString(R.string.lang_vi) + incompleteSuffix);
		languages.put("zh_CN", ctx.getString(R.string.lang_zh_cn) + incompleteSuffix);
		languages.put("zh_TW", ctx.getString(R.string.lang_zh_tw));

		Map<String, String> sortedLanguages = new TreeMap<>(ConfigureMapUtils.getLanguagesComparator(languages));
		sortedLanguages.putAll(languages);
		return sortedLanguages;
	}
}