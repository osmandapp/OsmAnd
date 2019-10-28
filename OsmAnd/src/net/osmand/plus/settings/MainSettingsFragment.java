package net.osmand.plus.settings;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.profiles.SettingsProfileFragment;

public class MainSettingsFragment extends BaseSettingsFragment {

	public static final String TAG = MainSettingsFragment.class.getSimpleName();

	private static final String CONFIGURE_PROFILE = "configure_profile";

	@Override
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

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		String key = preference.getKey();
		if (CONFIGURE_PROFILE.equals(key)) {
			View iconContainer = holder.itemView.findViewById(R.id.icon_container);
			if (iconContainer != null) {
				int profileColor = getActiveProfileColor();
				int bgColor = UiUtilities.getColorWithAlpha(profileColor, 0.1f);
				Drawable backgroundDrawable;

				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
					int selectedColor = UiUtilities.getColorWithAlpha(profileColor, 0.3f);
					Drawable background = getPaintedIcon(R.drawable.circle_background_light, bgColor);
					Drawable ripple = getPaintedIcon(R.drawable.ripple_circle, selectedColor);
					backgroundDrawable = new LayerDrawable(new Drawable[] {background, ripple});
				} else {
					backgroundDrawable = getPaintedIcon(R.drawable.circle_background_light, bgColor);
				}
				AndroidUtils.setBackground(iconContainer, backgroundDrawable);
			}
		}
	}

	private void setupManageProfilesPref() {
		Preference manageProfiles = findPreference("manage_profiles");
		manageProfiles.setIcon(getContentIcon(R.drawable.ic_action_manage_profiles));
		manageProfiles.setFragment(SettingsProfileFragment.class.getName());
	}

	private void setupConfigureProfilePref() {
		ApplicationMode selectedMode = getSelectedAppMode();

		String title = selectedMode.toHumanString(getContext());
		String profileType = getAppModeDescription(getContext(), selectedMode);
		int iconRes = selectedMode.getIconRes();

		Preference configureProfile = findPreference(CONFIGURE_PROFILE);
		configureProfile.setIcon(getPaintedIcon(iconRes, getActiveProfileColor()));
		configureProfile.setTitle(title);
		configureProfile.setSummary(profileType);
	}
}