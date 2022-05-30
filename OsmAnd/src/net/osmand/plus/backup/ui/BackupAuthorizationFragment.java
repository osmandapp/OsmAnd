package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.ExportSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;

import static net.osmand.plus.importfiles.ImportHelper.ImportType.SETTINGS;
import static net.osmand.plus.utils.UiUtilities.setupDialogButton;

public class BackupAuthorizationFragment extends BaseSettingsFragment implements InAppPurchaseListener {

	private static final String AUTHORIZE = "authorize";
	private static final String LOCAL_BACKUP = "local_backup";
	private static final String BACKUP_TO_FILE = "backup_to_file";
	private static final String RESTORE_FROM_FILE = "restore_from_file";
	private static final String LOCAL_BACKUP_DIVIDER = "local_backup_divider";

	private boolean localBackupVisible = true;

	@Override
	@ColorRes
	protected int getBackgroundColorRes() {
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), isNightMode());
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);
		View subtitle = view.findViewById(R.id.toolbar_subtitle);
		AndroidUiHelper.updateVisibility(subtitle, false);
		((AppBarLayout) view.findViewById(R.id.appbar)).setOutlineProvider(null);
	}

	@Override
	protected void setupPreferences() {
		Preference localBackup = findPreference(LOCAL_BACKUP);
		localBackup.setIconSpaceReserved(false);

		setupBackupToFilePref();
		setupRestoreFromFilePref();
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		String prefId = preference.getKey();
		if (LOCAL_BACKUP.equals(prefId)) {
			bindLocalBackupPref(holder);
		} else if (AUTHORIZE.equals(prefId)) {
			bindAuthorizePref(holder);
		}
		super.onBindPreferenceViewHolder(preference, holder);
	}

	private void setupBackupToFilePref() {
		Preference backupToFile = findPreference(BACKUP_TO_FILE);
		backupToFile.setIcon(getIcon(R.drawable.ic_action_save_to_file, getActiveColorRes()));
	}

	private void setupRestoreFromFilePref() {
		Preference restoreFromFile = findPreference(RESTORE_FROM_FILE);
		restoreFromFile.setIcon(getIcon(R.drawable.ic_action_read_from_file, getActiveColorRes()));
	}

	private void bindLocalBackupPref(PreferenceViewHolder holder) {
		ImageView indicator = holder.itemView.findViewById(R.id.icon_logout);
		if (!localBackupVisible) {
			indicator.setImageDrawable(getContentIcon(R.drawable.ic_action_arrow_down));
			indicator.setContentDescription(getString(R.string.access_collapsed_list));
		} else {
			indicator.setImageDrawable(getContentIcon(R.drawable.ic_action_arrow_up));
			indicator.setContentDescription(getString(R.string.access_expanded_list));
		}
	}

	private void bindAuthorizePref(PreferenceViewHolder holder) {
		View signUpButton = holder.itemView.findViewById(R.id.sign_up_button);
		View signInButton = holder.itemView.findViewById(R.id.sign_in_button);

		boolean subscribed = InAppPurchaseHelper.isOsmAndProAvailable(app);
		if (subscribed) {
			setupAuthorizeButton(signUpButton, DialogButtonType.PRIMARY, R.string.register_opr_create_new_account, true);
		} else {
			signUpButton.setOnClickListener(v -> {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.OSMAND_CLOUD);
				}
			});
			setupDialogButton(isNightMode(), signUpButton, DialogButtonType.PRIMARY, R.string.shared_string_get);
		}
		setupAuthorizeButton(signInButton, DialogButtonType.SECONDARY, R.string.register_opr_have_account, false);
	}

	private void setupAuthorizeButton(View view, DialogButtonType buttonType, @StringRes int textId, final boolean signUp) {
		setupDialogButton(isNightMode(), view, buttonType, textId);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					AuthorizeFragment.showInstance(mapActivity.getSupportFragmentManager(), signUp);
				}
			}
		});
	}

	private void toggleLocalPrefsVisibility() {
		localBackupVisible = !localBackupVisible;
		findPreference(BACKUP_TO_FILE).setVisible(localBackupVisible);
		findPreference(RESTORE_FROM_FILE).setVisible(localBackupVisible);
		findPreference(LOCAL_BACKUP_DIVIDER).setVisible(localBackupVisible);
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);
		return recyclerView;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (BACKUP_TO_FILE.equals(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				ApplicationMode mode = getSelectedAppMode();
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				ExportSettingsFragment.showInstance(fragmentManager, mode, null, true);
				return true;
			}
		} else if (RESTORE_FROM_FILE.equals(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getImportHelper().chooseFileToImport(SETTINGS, null);
				return true;
			}
		} else if (LOCAL_BACKUP.equals(prefId)) {
			toggleLocalPrefsVisibility();
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		String tag = SettingsScreenType.BACKUP_AUTHORIZATION.fragmentName;
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, tag)) {
			Fragment fragment = new BackupAuthorizationFragment();
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, tag)
					.addToBackStack(SettingsScreenType.BACKUP_AUTHORIZATION.name())
					.commitAllowingStateLoss();
		}
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {

	}

	@Override
	public void onGetItems() {

	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		updatePreference(findPreference(AUTHORIZE));
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {

	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {

	}
}