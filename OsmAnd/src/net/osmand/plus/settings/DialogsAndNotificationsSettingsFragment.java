package net.osmand.plus.settings;

import android.support.v7.preference.Preference;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.LayoutInflater;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class DialogsAndNotificationsSettingsFragment extends BaseSettingsFragment {

	public static final String TAG = DialogsAndNotificationsSettingsFragment.class.getSimpleName();

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);
		view.findViewById(R.id.toolbar_switch_container).setVisibility(View.GONE);
	}

	@Override
	protected void setupPreferences() {
		Preference mapDuringNavigationInfo = findPreference("dialogs_and_notifications_preferences_info");
		mapDuringNavigationInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupShowStartupMessagesPref();
		setupShowDownloadMapDialogPref();
	}

	private void setupShowStartupMessagesPref() {
		boolean enabled = !settings.DO_NOT_SHOW_STARTUP_MESSAGES.get(); // pref ui was inverted
		SwitchPreferenceCompat sendAnonymousData = (SwitchPreferenceCompat) findPreference(settings.DO_NOT_SHOW_STARTUP_MESSAGES.getId());
		sendAnonymousData.setChecked(enabled);
	}

	private void setupShowDownloadMapDialogPref() {
		SwitchPreferenceCompat showDownloadMapDialog = (SwitchPreferenceCompat) findPreference(settings.SHOW_DOWNLOAD_MAP_DIALOG.getId());
		showDownloadMapDialog.setIcon(getContentIcon(R.drawable.ic_action_import));
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
