package net.osmand.plus.settings.fragments;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

public class SendUniqueIdentifiersFragment extends BaseSettingsFragment {

	public static final String TAG = SendUniqueIdentifiersFragment.class.getSimpleName();

	private static final String TERMS_OF_USE = "terms_of_use";
	private static final String PRIVACY_POLICY = "privacy_policy";

	@Override
	protected void setupPreferences() {
		setupSendUuidPreference();
	}

	private void setupSendUuidPreference() {
		boolean enabled = settings.SEND_UNIQUE_USER_IDENTIFIER.get();
		SwitchPreferenceCompat sendUniqueIdentifiers = findPreference(settings.SEND_UNIQUE_USER_IDENTIFIER.getId());
		sendUniqueIdentifiers.setIconSpaceReserved(false);
		sendUniqueIdentifiers.setChecked(enabled);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (TERMS_OF_USE.equals(prefId)) {
			openUrl(R.string.docs_legal_terms_of_use);
			return true;
		} else if (PRIVACY_POLICY.equals(prefId)) {
			openUrl(R.string.docs_legal_privacy_policy);
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	private void openUrl(int urlId) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AndroidUtils.openUrl(activity, urlId, false);
		}
	}

}
