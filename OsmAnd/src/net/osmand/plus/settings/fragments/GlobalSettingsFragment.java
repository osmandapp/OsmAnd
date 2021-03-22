package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.widget.ImageView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.dialogs.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.dialogs.SendAnalyticsBottomSheetDialogFragment.OnSendAnalyticsPrefsUpdate;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.profiles.SelectProfileBottomSheet;
import net.osmand.plus.profiles.SelectProfileBottomSheet.DialogMode;
import net.osmand.plus.profiles.SelectProfileBottomSheet.OnSelectProfileCallback;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.DataStorageHelper;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILE_KEY_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.USE_LAST_PROFILE_ARG;


public class GlobalSettingsFragment extends BaseSettingsFragment
		implements OnSendAnalyticsPrefsUpdate, OnPreferenceChanged, OnSelectProfileCallback {

	public static final String TAG = GlobalSettingsFragment.class.getSimpleName();

	private static final String SEND_ANONYMOUS_DATA_PREF_ID = "send_anonymous_data";
	private static final String DIALOGS_AND_NOTIFICATIONS_PREF_ID = "dialogs_and_notifications";

	@Override
	protected void setupPreferences() {
		setupDefaultAppModePref();
		setupPreferredLocalePref();
		setupExternalStorageDirPref();

		setupSendAnonymousDataPref();
		setupDialogsAndNotificationsPref();
		setupEnableProxyPref();
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
		if (DIALOGS_AND_NOTIFICATIONS_PREF_ID.equals(preference.getKey())) {
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null) {
				boolean enabled = preference.isEnabled() && (!settings.DO_NOT_SHOW_STARTUP_MESSAGES.get() || settings.SHOW_DOWNLOAD_MAP_DIALOG.get());
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
				app.restartApp(activity);
			}
		} else if (prefId.equals(settings.SPEED_CAMERAS_UNINSTALLED.getId())) {
			setupUninstallSpeedCamerasPref();
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
				SelectProfileBottomSheet.showInstance(
						getActivity(), DialogMode.DEFAULT_PROFILE, this,
						getSelectedAppMode(), defaultModeKey, false);
			}
		} else if (settings.SPEED_CAMERAS_UNINSTALLED.getId().equals(prefId) && !settings.SPEED_CAMERAS_UNINSTALLED.get()) {
			FragmentManager fm = getFragmentManager();
			if (fm != null) {
				SpeedCamerasBottomSheet.showInstance(fm, this);
			}
		}
		return super.onPreferenceClick(preference);
	}

	private void setupDefaultAppModePref() {
		Preference defaultApplicationMode = (Preference) findPreference(settings.DEFAULT_APPLICATION_MODE.getId());
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
		ListPreferenceEx preferredLocale = (ListPreferenceEx) findPreference(settings.PREFERRED_LOCALE.getId());
		preferredLocale.setIcon(getActiveIcon(R.drawable.ic_action_map_language));
		preferredLocale.setSummary(settings.PREFERRED_LOCALE.get());

		Pair<String[], String[]> preferredLocaleInfo = getPreferredLocaleIdsAndValues(ctx);
		if (preferredLocaleInfo != null) {
			preferredLocale.setEntries(preferredLocaleInfo.first);
			preferredLocale.setEntryValues(preferredLocaleInfo.second);
		}

		// Add " (Display language)" to menu title in Latin letters for all non-en languages
		if (!getResources().getString(R.string.preferred_locale).equals(getResources().getString(R.string.preferred_locale_no_translate))) {
			preferredLocale.setTitle(getString(R.string.preferred_locale) + " (" + getString(R.string.preferred_locale_no_translate) + ")");
		}
	}

	private void setupExternalStorageDirPref() {
		Preference externalStorageDir = (Preference) findPreference(OsmandSettings.EXTERNAL_STORAGE_DIR);
		externalStorageDir.setIcon(getActiveIcon(R.drawable.ic_action_folder));

		DataStorageHelper holder = new DataStorageHelper(app);
		StorageItem currentStorage = holder.getCurrentStorage();
		long totalUsed = app.getSettings().OSMAND_USAGE_SPACE.get();
		if (totalUsed > 0) {
			String[] usedMemoryFormats = new String[] {
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

	private void setupSendAnonymousDataPref() {
		boolean enabled = settings.SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.get() || settings.SEND_ANONYMOUS_APP_USAGE_DATA.get();

		SwitchPreferenceCompat sendAnonymousData = (SwitchPreferenceCompat) findPreference(SEND_ANONYMOUS_DATA_PREF_ID);
		sendAnonymousData.setChecked(enabled);
		sendAnonymousData.setIcon(getPersistentPrefIcon(R.drawable.ic_action_privacy_and_security));
	}

	private void setupDialogsAndNotificationsPref() {
		Preference dialogsAndNotifications = (Preference) findPreference(DIALOGS_AND_NOTIFICATIONS_PREF_ID);
		dialogsAndNotifications.setIcon(getPersistentPrefIcon(R.drawable.ic_action_notification));
		if (getSettings() == null) {
			return;
		}
		boolean showStartupMessages = !getSettings().DO_NOT_SHOW_STARTUP_MESSAGES.get();
		boolean showDownloadMapDialog = getSettings().SHOW_DOWNLOAD_MAP_DIALOG.get();
		String summary;
		if (showStartupMessages && showDownloadMapDialog) {
			summary = getString(R.string.shared_string_all);
		} else if (showStartupMessages || showDownloadMapDialog) {
			summary = getString(R.string.ltr_or_rtl_combine_via_slash, "1", "2");
		} else {
			summary = getString(R.string.shared_string_disabled);
		}
		dialogsAndNotifications.setSummary(summary);
	}

	private void setupEnableProxyPref() {
		SwitchPreferenceEx enableProxy = (SwitchPreferenceEx) findPreference(settings.ENABLE_PROXY.getId());
		enableProxy.setIcon(getPersistentPrefIcon(R.drawable.ic_action_proxy));
	}

	private void setupUninstallSpeedCamerasPref() {
		boolean uninstalled = settings.SPEED_CAMERAS_UNINSTALLED.get();
		Preference uninstallSpeedCameras = (Preference) findPreference(settings.SPEED_CAMERAS_UNINSTALLED.getId());
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

	public static Pair<String[], String[]> getPreferredLocaleIdsAndValues(Context ctx) {
		// See language list and statistics at: https://hosted.weblate.org/projects/osmand/main/
		// Hardy maintenance 2016-05-29:
		//  - Include languages if their translation is >= ~10%    (but any language will be visible if it is the device's system locale)
		//  - Mark as "incomplete" if                    < ~80%
		String incompleteSuffix = " (" + ctx.getString(R.string.incomplete_locale) + ")";

		// Add " (Device language)" to system default entry in Latin letters, so it can be more easily identified if a foreign language has been selected by mistake
		String latinSystemDefaultSuffix = " (" + ctx.getString(R.string.system_locale_no_translate) + ")";

		//getResources().getAssets().getLocales();
		String[] entryValues = new String[] {
				"",
				"en",
				"af",
				"ar",
				"ast",
				"az",
				"be",
				//"be_BY",
				"bg",
				"ca",
				"cs",
				"cy",
				"da",
				"de",
				"el",
				"en_GB",
				"eo",
				"es",
				"es_AR",
				"es_US",
				"eu",
				"fa",
				"fi",
				"fr",
				"gl",
				"iw",
				"hr",
				"hsb",
				"hu",
				"hy",
				"is",
				"it",
				"ja",
				"ka",
				"kab",
				"kn",
				"ko",
				"lt",
				"lv",
				"ml",
				"mr",
				"nb",
				"nl",
				"nn",
				"oc",
				"pl",
				"pt",
				"pt_BR",
				"ro",
				"ru",
				"sc",
				"sk",
				"sl",
				"sr",
				"sr+Latn",
				"sv",
				"tr",
				"uk",
				"vi",
				"zh_CN",
				"zh_TW"};

		String[] entries = new String[] {
				ctx.getString(R.string.system_locale) + latinSystemDefaultSuffix,
				ctx.getString(R.string.lang_en),
				ctx.getString(R.string.lang_af) + incompleteSuffix,
				ctx.getString(R.string.lang_ar),
				ctx.getString(R.string.lang_ast) + incompleteSuffix,
				ctx.getString(R.string.lang_az),
				ctx.getString(R.string.lang_be),
				// getString(R.string.lang_be_by),
				ctx.getString(R.string.lang_bg),
				ctx.getString(R.string.lang_ca),
				ctx.getString(R.string.lang_cs),
				ctx.getString(R.string.lang_cy) + incompleteSuffix,
				ctx.getString(R.string.lang_da),
				ctx.getString(R.string.lang_de),
				ctx.getString(R.string.lang_el),
				ctx.getString(R.string.lang_en_gb),
				ctx.getString(R.string.lang_eo),
				ctx.getString(R.string.lang_es),
				ctx.getString(R.string.lang_es_ar),
				ctx.getString(R.string.lang_es_us),
				ctx.getString(R.string.lang_eu),
				ctx.getString(R.string.lang_fa),
				ctx.getString(R.string.lang_fi) + incompleteSuffix,
				ctx.getString(R.string.lang_fr),
				ctx.getString(R.string.lang_gl),
				ctx.getString(R.string.lang_he),
				ctx.getString(R.string.lang_hr) + incompleteSuffix,
				ctx.getString(R.string.lang_hsb) + incompleteSuffix,
				ctx.getString(R.string.lang_hu),
				ctx.getString(R.string.lang_hy),
				ctx.getString(R.string.lang_is),
				ctx.getString(R.string.lang_it),
				ctx.getString(R.string.lang_ja),
				ctx.getString(R.string.lang_ka) + incompleteSuffix,
				ctx.getString(R.string.lang_kab) + incompleteSuffix,
				ctx.getString(R.string.lang_kn) + incompleteSuffix,
				ctx.getString(R.string.lang_ko),
				ctx.getString(R.string.lang_lt),
				ctx.getString(R.string.lang_lv),
				ctx.getString(R.string.lang_ml),
				ctx.getString(R.string.lang_mr) + incompleteSuffix,
				ctx.getString(R.string.lang_nb),
				ctx.getString(R.string.lang_nl),
				ctx.getString(R.string.lang_nn) + incompleteSuffix,
				ctx.getString(R.string.lang_oc) + incompleteSuffix,
				ctx.getString(R.string.lang_pl),
				ctx.getString(R.string.lang_pt),
				ctx.getString(R.string.lang_pt_br),
				ctx.getString(R.string.lang_ro) + incompleteSuffix,
				ctx.getString(R.string.lang_ru),
				ctx.getString(R.string.lang_sc),
				ctx.getString(R.string.lang_sk),
				ctx.getString(R.string.lang_sl),
				ctx.getString(R.string.lang_sr),
				ctx.getString(R.string.lang_sr_latn) + incompleteSuffix,
				ctx.getString(R.string.lang_sv),
				ctx.getString(R.string.lang_tr),
				ctx.getString(R.string.lang_uk),
				ctx.getString(R.string.lang_vi) + incompleteSuffix,
				ctx.getString(R.string.lang_zh_cn) + incompleteSuffix,
				ctx.getString(R.string.lang_zh_tw)};

		String[] valuesPl = ConfigureMapMenu.getSortedMapNamesIds(ctx, entries, entries);
		String[] idsPl = ConfigureMapMenu.getSortedMapNamesIds(ctx, entryValues, entries);

		return Pair.create(valuesPl, idsPl);
	}
}