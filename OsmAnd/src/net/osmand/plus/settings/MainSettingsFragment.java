package net.osmand.plus.settings;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.profiles.ProfileDataObject;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SelectProfileListener;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_BASE_APP_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.getBaseProfiles;

public class MainSettingsFragment extends BaseSettingsFragment {

	public static final String TAG = MainSettingsFragment.class.getName();

	private static final String CONFIGURE_PROFILE = "configure_profile";
	private static final String APP_PROFILES = "app_profiles";
	private static final String SELECTED_PROFILE = "selected_profile";
	private static final String CREATE_PROFILE = "create_profile";
	private static final String IMPORT_PROFILE = "import_profile";
	private static final String REORDER_PROFILES = "reorder_profiles";

	private List<ApplicationMode> allAppModes;
	private Set<ApplicationMode> availableAppModes;
	private SelectProfileListener selectProfileListener = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	@ColorRes
	protected int getBackgroundColorRes() {
		return isNightMode() ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	@Override
	protected void setupPreferences() {
		allAppModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		allAppModes.remove(ApplicationMode.DEFAULT);
		availableAppModes = new LinkedHashSet<>(ApplicationMode.values(getMyApplication()));
		availableAppModes.remove(ApplicationMode.DEFAULT);
		Preference globalSettings = findPreference("global_settings");
		globalSettings.setIcon(getContentIcon(R.drawable.ic_action_settings));
		PreferenceCategory selectedProfile = (PreferenceCategory) findPreference(SELECTED_PROFILE);
		selectedProfile.setIconSpaceReserved(false);
		setupConfigureProfilePref();
		PreferenceCategory appProfiles = (PreferenceCategory) findPreference(APP_PROFILES);
		appProfiles.setIconSpaceReserved(false);
		setupAppProfiles(appProfiles);
		profileManagementPref();
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		String key = preference.getKey();
		if (CONFIGURE_PROFILE.equals(key)) {
			View selectedProfile = holder.itemView.findViewById(R.id.selectable_list_item);
			if (selectedProfile != null) {
				int activeProfileColor = getActiveProfileColor();
				Drawable backgroundDrawable = new ColorDrawable(UiUtilities.getColorWithAlpha(activeProfileColor, 0.15f));
				AndroidUtils.setBackground(selectedProfile, backgroundDrawable);
			}
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		ApplicationMode applicationMode = ApplicationMode.valueOfStringKey(preference.getKey(), null);
		if (applicationMode != null) {
			if (newValue instanceof Boolean) {
				boolean isChecked = (Boolean) newValue;
				onProfileSelected(applicationMode, isChecked);
				preference.setIcon(getAppProfilesIcon(applicationMode, isChecked));
			}
		}
		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (preference.getParent() != null && APP_PROFILES.equals(preference.getParent().getKey())) {
			BaseSettingsFragment.showInstance(getActivity(), SettingsScreenType.CONFIGURE_PROFILE,
					ApplicationMode.valueOfStringKey(prefId, null));
			return true;
		} else if (CREATE_PROFILE.equals(prefId)) {
			final SelectProfileBottomSheetDialogFragment dialog = new SelectProfileBottomSheetDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putString(DIALOG_TYPE, TYPE_BASE_APP_PROFILE);
			dialog.setArguments(bundle);
			if (getActivity() != null) {
				getActivity().getSupportFragmentManager().beginTransaction()
						.add(dialog, "select_base_profile").commitAllowingStateLoss();
			}
		}
		return super.onPreferenceClick(preference);
	}

	private void setupConfigureProfilePref() {
		ApplicationMode selectedMode = app.getSettings().APPLICATION_MODE.get();
		String title = selectedMode.toHumanString(getContext());
		String profileType = getAppModeDescription(getContext(), selectedMode);
		int iconRes = selectedMode.getIconRes();
		Preference configureProfile = findPreference(CONFIGURE_PROFILE);
		configureProfile.setIcon(getPaintedIcon(iconRes, getActiveProfileColor()));
		configureProfile.setTitle(title);
		configureProfile.setSummary(profileType);
	}

	private void profileManagementPref() {
		Preference createProfile = findPreference(CREATE_PROFILE);
		createProfile.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_plus,
				isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
		Preference importProfile = findPreference(IMPORT_PROFILE);
//		importProfile.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_import,
//				isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
		Preference reorderProfiles = findPreference(REORDER_PROFILES);
		reorderProfiles.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_edit_dark,
				isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
	}

	private void setupAppProfiles(PreferenceCategory preferenceCategory) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		for (ApplicationMode applicationMode : allAppModes) {
			boolean isAppProfileEnabled = availableAppModes.contains(applicationMode);
			SwitchPreferenceEx pref = new SwitchPreferenceEx(app);
			pref.setPersistent(false);
			pref.setKey(applicationMode.getStringKey());
			pref.setIcon(getAppProfilesIcon(applicationMode, isAppProfileEnabled));
			pref.setTitle(applicationMode.toHumanString(getContext()));
			pref.setSummary(getAppModeDescription(getContext(), applicationMode));
			pref.setChecked(isAppProfileEnabled);
			pref.setLayoutResource(R.layout.preference_with_descr_dialog_and_switch);
			pref.setFragment(ConfigureProfileFragment.class.getName());
			preferenceCategory.addPreference(pref);
		}
	}

	public void onProfileSelected(ApplicationMode item, boolean isChecked) {
		if (isChecked) {
			availableAppModes.add(item);
		} else {
			availableAppModes.remove(item);
		}
		ApplicationMode.changeProfileAvailability(item, isChecked, getMyApplication());
	}

	private Drawable getAppProfilesIcon(ApplicationMode applicationMode, boolean appProfileEnabled) {
		int iconResId = applicationMode.getIconRes();
		return appProfileEnabled ? app.getUIUtilities().getIcon(applicationMode.getIconRes(), applicationMode.getIconColorInfo().getColor(isNightMode()))
				: getIcon(iconResId, isNightMode() ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light);
	}

	public SelectProfileListener getParentProfileListener() {
		if (selectProfileListener == null) {
			selectProfileListener = new SelectProfileListener() {
				@Override
				public void onSelectedType(int pos, String stringRes) {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager fragmentManager = activity.getSupportFragmentManager();
						if (fragmentManager != null) {
							ProfileDataObject profileDataObject = getBaseProfiles(app).get(pos);
							ProfileAppearanceFragment.showInstance(activity, SettingsScreenType.PROFILE_APPEARANCE,
									profileDataObject.getStringKey());
						}
					}
				}
			};
		}
		return selectProfileListener;
	}
}