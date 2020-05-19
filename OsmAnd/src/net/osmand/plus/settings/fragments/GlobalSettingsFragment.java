package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.util.Pair;
import android.widget.ImageView;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsGeneralActivity;
import net.osmand.plus.dialogs.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;


public class GlobalSettingsFragment extends BaseSettingsFragment implements SendAnalyticsBottomSheetDialogFragment.OnSendAnalyticsPrefsUpdate, OnPreferenceChanged {

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
		setupUseSystemScreenTimeout();
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
		if (prefId.equals(settings.DEFAULT_APPLICATION_MODE.getId())) {
			setupDefaultAppModePref();
		} else if (prefId.equals(settings.PREFERRED_LOCALE.getId())) {
			// recreate activity to update locale
			Activity activity = getActivity();
			OsmandApplication app = getMyApplication();
			if (app != null && activity != null) {
				app.checkPreferredLocale();
				app.restartApp(activity);
			}
		}
	}

	@Override
	public void onAnalyticsPrefsUpdate() {
		setupSendAnonymousDataPref();
	}

	private void setupDefaultAppModePref() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		ApplicationMode[] appModes = ApplicationMode.values(app).toArray(new ApplicationMode[0]);
		String[] entries = new String[appModes.length];
		String[] entryValues = new String[appModes.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = appModes[i].toHumanString();
			entryValues[i] = appModes[i].getStringKey();
		}

		ListPreferenceEx defaultApplicationMode = (ListPreferenceEx) findPreference(settings.DEFAULT_APPLICATION_MODE.getId());
		defaultApplicationMode.setIcon(getIcon(settings.DEFAULT_APPLICATION_MODE.get().getIconRes(),
				settings.getApplicationMode().getIconColorInfo().getColor(isNightMode())));
		defaultApplicationMode.setEntries(entries);
		defaultApplicationMode.setEntryValues(entryValues);
	}

	private void setupPreferredLocalePref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		ListPreferenceEx preferredLocale = (ListPreferenceEx) findPreference(settings.PREFERRED_LOCALE.getId());
		preferredLocale.setIcon(getActiveIcon(R.drawable.ic_action_map_language));
		preferredLocale.setSummary(settings.PREFERRED_LOCALE.get());

		Pair<String[], String[]> preferredLocaleInfo = SettingsGeneralActivity.getPreferredLocaleIdsAndValues(ctx);
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
		DataStorageMenuItem currentStorage = holder.getCurrentStorage();
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
	}

	private void setupEnableProxyPref() {
		SwitchPreferenceEx enableProxy = (SwitchPreferenceEx) findPreference(settings.ENABLE_PROXY.getId());
		enableProxy.setIcon(getPersistentPrefIcon(R.drawable.ic_action_proxy));
	}

	private void setupUseSystemScreenTimeout() {
		SwitchPreferenceEx useSystemScreenTimeout = (SwitchPreferenceEx) findPreference(settings.USE_SYSTEM_SCREEN_TIMEOUT.getId());
		useSystemScreenTimeout.setTitle(app.getString(R.string.use_system_screen_timeout));
		useSystemScreenTimeout.setDescription(app.getString(R.string.use_system_screen_timeout_promo));
	}
}