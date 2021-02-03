package net.osmand.plus.openplacereviews;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.openplacereviews.OprAuthHelper.OprAuthorizationListener;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class OprSettingsFragment extends BaseSettingsFragment implements OnPreferenceChanged, OprAuthorizationListener {

	private static final String OPR_LOGOUT = "opr_logout";
	public static final String OPR_LOGIN_DATA = "opr_login_data";

	private OprAuthHelper authHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		authHelper = app.getOprAuthHelper();

		FragmentActivity activity = requireMyActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					mapActivity.launchPrevActivityIntent();
				}
				dismiss();
			}
		});
	}

	@Override
	protected void setupPreferences() {
		Preference oprSettingsInfo = findPreference("opr_settings_info");
		oprSettingsInfo.setIconSpaceReserved(false);

		setupLoginPref();
		setupLogoutPref();
		setupUseDevUrlPref();
	}

	private void setupLoginPref() {
		Preference nameAndPasswordPref = findPreference(OPR_LOGIN_DATA);
		nameAndPasswordPref.setVisible(!authHelper.isLoginExists());
		nameAndPasswordPref.setIcon(getContentIcon(R.drawable.ic_action_user_account));
	}

	private void setupLogoutPref() {
		Preference nameAndPasswordPref = findPreference(OPR_LOGOUT);
		nameAndPasswordPref.setVisible(authHelper.isLoginExists());
		nameAndPasswordPref.setSummary(settings.OPR_USERNAME.get());
		nameAndPasswordPref.setIcon(getContentIcon(R.drawable.ic_action_user_account));
	}

	private void setupUseDevUrlPref() {
		SwitchPreferenceEx useDevUrlPref = findPreference(settings.OPR_USE_DEV_URL.getId());
		useDevUrlPref.setVisible(OsmandPlugin.isDevelopment());
		useDevUrlPref.setIcon(getPersistentPrefIcon(R.drawable.ic_plugin_developer));
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (OPR_LOGIN_DATA.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				OprStartFragment.showInstance(fragmentManager);
				return true;
			}
		} else if (OPR_LOGOUT.equals(prefId)) {
			oprLogout();
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		if (settings.OPR_USE_DEV_URL.getId().equals(prefId) && newValue instanceof Boolean) {
			settings.OPR_USE_DEV_URL.set((Boolean) newValue);
			oprLogout();
			return true;
		}
		return super.onPreferenceChange(preference, newValue);
	}

	public void oprLogout() {
		authHelper.resetAuthorization();
		app.showShortToastMessage(R.string.osm_edit_logout_success);
		updateAllSettings();
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		if (settings.OPR_USE_DEV_URL.getId().equals(prefId)) {
			oprLogout();
		}
		updateAllSettings();
	}

	@Override
	public void authorizationCompleted() {
		if (getContext() != null) {
			updateAllSettings();
		}
	}
}