package net.osmand.plus.settings;

import android.content.Intent;
import android.support.annotation.ColorRes;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.profiles.SettingsProfileActivity;
import net.osmand.util.Algorithms;

public class MainSettingsFragment extends BaseSettingsFragment {

	public static final String TAG = "MainSettingsFragment";

	@Override
	protected int getPreferencesResId() {
		return R.xml.settings_main_screen;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.global_preference_toolbar;
	}

	@Override
	protected int getToolbarTitle() {
		return R.string.shared_string_settings;
	}

	@Override
	public int getStatusBarColorId() {
		return isNightMode() ? R.color.status_bar_color_light : R.color.status_bar_color_dark;
	}

	@ColorRes
	protected int getBackgroundColorRes() {
		return isNightMode() ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	@Override
	protected void setupPreferences() {
		Preference globalSettings = findPreference("global_settings");
		globalSettings.setIcon(getContentIcon(R.drawable.ic_action_settings));

		setupConfigureProfilePref();
		setupManageProfilesPref();
	}

	private void setupManageProfilesPref() {
		Preference manageProfiles = findPreference("manage_profiles");
		manageProfiles.setIcon(getIcon(R.drawable.ic_action_manage_profiles));
		manageProfiles.setIntent(new Intent(getActivity(), SettingsProfileActivity.class));
	}

	private void setupConfigureProfilePref() {
		ApplicationMode selectedMode = getSelectedAppMode();

		String title = selectedMode.toHumanString(getContext());
		String profileType;
		if (selectedMode.isCustomProfile()) {
			profileType = String.format(getString(R.string.profile_type_descr_string), Algorithms.capitalizeFirstLetterAndLowercase(selectedMode.getParent().toHumanString(getContext())));
		} else {
			profileType = getString(R.string.profile_type_base_string);
		}

		int iconRes = selectedMode.getIconRes();

		Preference configureProfile = findPreference("configure_profile");
		configureProfile.setIcon(getPaintedIcon(iconRes, getActiveProfileColor()));
		configureProfile.setTitle(title);
		configureProfile.setSummary(profileType);
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			MainSettingsFragment MainSettingsFragment = new MainSettingsFragment();
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, MainSettingsFragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}