package net.osmand.plus.settings.fragments;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.ui.BackupAndRestoreFragment;
import net.osmand.plus.backup.ui.BackupAuthorizationFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectBaseProfileBottomSheet;
import net.osmand.plus.profiles.SelectProfileBottomSheet.OnSelectProfileCallback;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.importfiles.ImportHelper.ImportType.SETTINGS;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILES_LIST_UPDATED_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILE_KEY_ARG;

public class MainSettingsFragment extends BaseSettingsFragment implements OnSelectProfileCallback {

	public static final String TAG = MainSettingsFragment.class.getName();

	private static final int MIN_DURATION_FOR_DATE_FORMAT = 48 * 60 * 60;

	private static final String BACKUP_AND_RESTORE = "backup_and_restore";
	private static final String CONFIGURE_PROFILE = "configure_profile";
	private static final String APP_PROFILES = "app_profiles";
	private static final String PURCHASES_SETTINGS = "purchases_settings";
	private static final String SELECTED_PROFILE = "selected_profile";
	private static final String CREATE_PROFILE = "create_profile";
	private static final String IMPORT_PROFILE = "import_profile";
	private static final String REORDER_PROFILES = "reorder_profiles";

	private List<ApplicationMode> allAppModes;
	private Set<ApplicationMode> availableAppModes;

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
		availableAppModes = new LinkedHashSet<>(ApplicationMode.values(getMyApplication()));
		Preference globalSettings = findPreference("global_settings");
		globalSettings.setIcon(getContentIcon(R.drawable.ic_action_settings));
		setupBackupAndRestorePref();
		Preference purchasesSettings = findPreference(PURCHASES_SETTINGS);
		purchasesSettings.setIcon(getContentIcon(R.drawable.ic_action_purchases));
		PreferenceCategory selectedProfile = findPreference(SELECTED_PROFILE);
		selectedProfile.setIconSpaceReserved(false);
		setupConfigureProfilePref();
		PreferenceCategory appProfiles = findPreference(APP_PROFILES);
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
				int activeProfileColor = getSelectedAppMode().getProfileColor(isNightMode());
				Drawable backgroundDrawable = new ColorDrawable(UiUtilities.getColorWithAlpha(activeProfileColor, 0.15f));
				AndroidUtils.setBackground(selectedProfile, backgroundDrawable);
			}
		}
		boolean visible = !ApplicationMode.DEFAULT.getStringKey().equals(key);
		AndroidUiHelper.updateVisibility(holder.findViewById(R.id.switchWidget), visible);
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
			if (getActivity() != null) {
				SelectBaseProfileBottomSheet.showInstance(
						getActivity(), this, getSelectedAppMode(), null, false);
			}
		} else if (IMPORT_PROFILE.equals(prefId)) {
			final MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getImportHelper().chooseFileToImport(SETTINGS, new CallbackWithObject<List<SettingsItem>>() {

					@Override
					public boolean processResult(List<SettingsItem> result) {
						for (SettingsItem item : result) {
							if (item.getType() == SettingsItemType.PROFILE) {
								ConfigureProfileFragment.showInstance(mapActivity, SettingsScreenType.CONFIGURE_PROFILE,
										ApplicationMode.valueOfStringKey(item.getName(), null));
								break;
							}
						}
						return false;
					}

				});
			}
		} else if (PURCHASES_SETTINGS.equals(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				PurchasesFragment.showInstance(fragmentManager);
			}
		} else if (BACKUP_AND_RESTORE.equals(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				if (app.getBackupHelper().isRegistered()) {
					BackupAndRestoreFragment.showInstance(mapActivity.getSupportFragmentManager());
				} else {
					BackupAuthorizationFragment.showInstance(mapActivity.getSupportFragmentManager());
				}
			}
		}

		return super.onPreferenceClick(preference);
	}

	private void setupConfigureProfilePref() {
		ApplicationMode selectedMode = app.getSettings().APPLICATION_MODE.get();
		String title = selectedMode.toHumanString();
		String profileType = ProfileDataUtils.getAppModeDescription(getContext(), selectedMode);
		Preference configureProfile = findPreference(CONFIGURE_PROFILE);
		configureProfile.setIcon(getAppProfilesIcon(selectedMode, true));
		configureProfile.setTitle(title);
		configureProfile.setSummary(profileType);
	}

	private void setupBackupAndRestorePref() {
		Preference backupSettings = findPreference(BACKUP_AND_RESTORE);
		backupSettings.setIcon(getContentIcon(R.drawable.ic_action_cloud_upload));

		String time = getLastBackupTimeDescription(app);
		if (!Algorithms.isEmpty(time)) {
			String summary = getString(R.string.last_backup);
			backupSettings.setSummary(getString(R.string.ltr_or_rtl_combine_via_colon, summary, time));
		}
	}

	public static String getLastBackupTimeDescription(OsmandApplication app) {
		long lastUploadedTime = app.getSettings().BACKUP_LAST_UPLOADED_TIME.get();
		return getLastBackupTimeDescription(app, lastUploadedTime, "");
	}

	public static String getLastBackupTimeDescription(OsmandApplication app, long lastUploadedTimems, String def) {
		if (lastUploadedTimems > 0) {
			long duration = (System.currentTimeMillis() - lastUploadedTimems) / 1000;
			if (duration > MIN_DURATION_FOR_DATE_FORMAT) {
				return OsmAndFormatter.getFormattedDate(app, lastUploadedTimems);
			} else {
				return app.getString(R.string.duration_ago, OsmAndFormatter.getFormattedDuration((int) duration, app));
			}
		}
		return def;
	}

	private void profileManagementPref() {
		int activeColorPrimaryResId = isNightMode() ? R.color.active_color_primary_dark
				: R.color.active_color_primary_light;

		Preference createProfile = findPreference(CREATE_PROFILE);
		createProfile.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_plus, activeColorPrimaryResId));

		Preference importProfile = findPreference(IMPORT_PROFILE);
		importProfile.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_import, activeColorPrimaryResId));

		Preference reorderProfiles = findPreference(REORDER_PROFILES);
		reorderProfiles.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_edit_dark, activeColorPrimaryResId));
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
			preferenceCategory.addPreference(pref);

			pref.setIcon(getAppProfilesIcon(applicationMode, isAppProfileEnabled));
			pref.setTitle(applicationMode.toHumanString());
			pref.setSummary(ProfileDataUtils.getAppModeDescription(getContext(), applicationMode));
			pref.setChecked(isAppProfileEnabled);
			pref.setLayoutResource(R.layout.preference_with_descr_dialog_and_switch);
			pref.setFragment(ConfigureProfileFragment.class.getName());
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
		return appProfileEnabled ? app.getUIUtilities().getPaintedIcon(applicationMode.getIconRes(), applicationMode.getProfileColor(isNightMode()))
				: getIcon(iconResId, isNightMode() ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light);
	}

	@Override
	public void onPause() {
		updateRouteInfoMenu();
		super.onPause();
	}

	@Override
	public void onProfileSelected(Bundle args) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			String profileKey = args.getString(PROFILE_KEY_ARG);
			boolean imported = args.getBoolean(PROFILES_LIST_UPDATED_ARG);
			ProfileAppearanceFragment.showInstance(activity, SettingsScreenType.PROFILE_APPEARANCE,
					profileKey, imported);
		}
	}
}