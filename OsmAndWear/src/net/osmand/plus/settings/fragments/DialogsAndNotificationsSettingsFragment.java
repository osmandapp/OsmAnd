package net.osmand.plus.settings.fragments;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;

public class DialogsAndNotificationsSettingsFragment extends BaseSettingsFragment {

	public static final String TAG = DialogsAndNotificationsSettingsFragment.class.getSimpleName();

	@Override
	protected void setupPreferences() {
		Preference mapDuringNavigationInfo = findPreference("dialogs_and_notifications_preferences_info");
		mapDuringNavigationInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupShowStartupMessagesPref();
		setupShowDownloadMapDialogPref();
	}

	private void setupShowStartupMessagesPref() {
		boolean enabled = !settings.DO_NOT_SHOW_STARTUP_MESSAGES.get(); // pref ui was inverted
		SwitchPreferenceCompat sendAnonymousData = findPreference(settings.DO_NOT_SHOW_STARTUP_MESSAGES.getId());
		sendAnonymousData.setChecked(enabled);
		sendAnonymousData.setIcon(getPersistentPrefIcon(R.drawable.ic_action_notification));
	}

	private void setupShowDownloadMapDialogPref() {
		SwitchPreferenceCompat showDownloadMapDialog = findPreference(settings.SHOW_DOWNLOAD_MAP_DIALOG.getId());
		showDownloadMapDialog.setIcon(getPersistentPrefIcon(R.drawable.ic_action_import));
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();

		if (prefId.equals(settings.DO_NOT_SHOW_STARTUP_MESSAGES.getId())) {
			if (newValue instanceof Boolean) {
				boolean enabled = !(Boolean) newValue;
				return settings.DO_NOT_SHOW_STARTUP_MESSAGES.set(enabled);
			}
		}
		return super.onPreferenceChange(preference, newValue);
	}

}
