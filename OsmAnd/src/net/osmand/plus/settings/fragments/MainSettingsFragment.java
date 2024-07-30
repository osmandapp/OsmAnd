package net.osmand.plus.settings.fragments;

import static net.osmand.plus.backup.ui.BackupUiUtils.getLastBackupTimeDescription;
import static net.osmand.plus.importfiles.ImportType.SETTINGS;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILES_LIST_UPDATED_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILE_KEY_ARG;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.ui.BackupAuthorizationFragment;
import net.osmand.plus.backup.ui.BackupCloudFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectBaseProfileBottomSheet;
import net.osmand.plus.profiles.SelectProfileBottomSheet.OnSelectProfileCallback;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.profileappearance.ProfileAppearanceFragment;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.settings.purchase.PurchasesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainSettingsFragment extends BaseSettingsFragment implements OnSelectProfileCallback {

	public static final String TAG = MainSettingsFragment.class.getName();

	private static final String BACKUP_AND_RESTORE = "backup_and_restore";
	private static final String CONFIGURE_PROFILE = "configure_profile";
	private static final String APP_PROFILES = "app_profiles";
	private static final String PURCHASES_SETTINGS = "purchases_settings";
	private static final String SELECTED_PROFILE = "selected_profile";
	private static final String CREATE_PROFILE = "create_profile";
	private static final String REORDER_PROFILES = "reorder_profiles";
	private static final String LOCAL_BACKUP = "local_backup";
	private static final String EXPORT_TO_FILE = "export_to_file";
	private static final String IMPORT_FROM_FILE = "import_from_file";

	private List<ApplicationMode> allAppModes;
	private Set<ApplicationMode> availableAppModes;

	@Override
	@ColorRes
	protected int getBackgroundColorRes() {
		return ColorUtilities.getActivityBgColorId(isNightMode());
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
		setupLocalBackup();
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		String key = preference.getKey();
		if (CONFIGURE_PROFILE.equals(key)) {
			View selectedProfile = holder.itemView.findViewById(R.id.selectable_list_item);
			if (selectedProfile != null) {
				int activeProfileColor = getSelectedAppMode().getProfileColor(isNightMode());
				Drawable backgroundDrawable = new ColorDrawable(ColorUtilities.getColorWithAlpha(activeProfileColor, 0.15f));
				AndroidUtils.setBackground(selectedProfile, backgroundDrawable);
			}
		} else if (LOCAL_BACKUP.equals(key)) {
			TextView title = holder.itemView.findViewById(android.R.id.title);
			title.setTextColor(ColorUtilities.getPrimaryTextColor(app, isNightMode()));
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
					BackupCloudFragment.showInstance(mapActivity.getSupportFragmentManager());
				} else {
					BackupAuthorizationFragment.showInstance(mapActivity.getSupportFragmentManager());
				}
			}
		} else if (EXPORT_TO_FILE.equals(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				ApplicationMode mode = getSelectedAppMode();
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				ExportSettingsFragment.showInstance(fragmentManager, mode, null, true);
				return true;
			}
		} else if (IMPORT_FROM_FILE.equals(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getImportHelper().chooseFileToImport(SETTINGS);
				return true;
			}
		}

		return super.onPreferenceClick(preference);
	}

	private void setupLocalBackup() {
		setupBackupToFilePref();
		setupRestoreFromFilePref();
		findPreference(LOCAL_BACKUP).setIconSpaceReserved(false);
	}

	private void setupBackupToFilePref() {
		Preference backupToFile = findPreference(EXPORT_TO_FILE);
		backupToFile.setIcon(getIcon(R.drawable.ic_action_save_to_file, getActiveColorRes()));
	}

	private void setupRestoreFromFilePref() {
		Preference restoreFromFile = findPreference(IMPORT_FROM_FILE);
		restoreFromFile.setIcon(getIcon(R.drawable.ic_action_read_from_file, getActiveColorRes()));
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

		if (app.getBackupHelper().isRegistered()) {
			String time = getLastBackupTimeDescription(app, "");
			if (!Algorithms.isEmpty(time)) {
				String summary = getString(R.string.last_sync);
				backupSettings.setSummary(getString(R.string.ltr_or_rtl_combine_via_colon, summary, time));
			}
		}
	}

	private void profileManagementPref() {
		int activeColorPrimaryResId = ColorUtilities.getActiveColorId(isNightMode());

		Preference createProfile = findPreference(CREATE_PROFILE);
		createProfile.setIcon(getIcon(R.drawable.ic_action_plus, activeColorPrimaryResId));

		Preference reorderProfiles = findPreference(REORDER_PROFILES);
		reorderProfiles.setIcon(getIcon(R.drawable.ic_action_edit_dark, activeColorPrimaryResId));
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
			ProfileAppearanceFragment.showInstance(activity, profileKey, imported);
		}
	}
}