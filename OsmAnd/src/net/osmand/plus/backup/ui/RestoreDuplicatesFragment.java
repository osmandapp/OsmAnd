package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.RESTORE_ITEMS_KEY;
import static net.osmand.plus.settings.fragments.BaseSettingsListFragment.SETTINGS_LIST_TAG;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.backup.ImportBackupTask;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.fragments.FileImportDuplicatesFragment;
import net.osmand.plus.settings.fragments.ImportDuplicatesFragment;
import net.osmand.plus.settings.fragments.ImportSettingsFragment;

import org.apache.commons.logging.Log;

import java.util.List;

public class RestoreDuplicatesFragment extends ImportDuplicatesFragment {

	public static final String TAG = FileImportDuplicatesFragment.class.getSimpleName();
	public static final Log LOG = PlatformUtil.getLog(RestoreDuplicatesFragment.class);

	private NetworkSettingsHelper settingsHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settingsHelper = app.getNetworkSettingsHelper();
		ImportBackupTask importTask = settingsHelper.getImportTask(RESTORE_ITEMS_KEY);
		if (importTask != null) {
			if (settingsItems == null) {
				settingsItems = importTask.getSelectedItems();
			}
			if (duplicatesList == null) {
				duplicatesList = importTask.getDuplicates();
			}
			Fragment target = getTargetFragment();
			if (target instanceof RestoreSettingsFragment) {
				ImportListener importListener = ((RestoreSettingsFragment) target).getImportListener();
				importTask.setImportListener(importListener);
			}
		}
	}

	@Override
	protected ImportType getImportTaskType() {
		return settingsHelper.getImportTaskType(RESTORE_ITEMS_KEY);
	}

	@Override
	protected void importItems(boolean shouldReplace) {
		super.importItems(shouldReplace);
		if (settingsItems != null) {
			Fragment target = getTargetFragment();
			if (target instanceof ImportSettingsFragment) {
				try {
					ImportListener importListener = ((ImportSettingsFragment) target).getImportListener();
					settingsHelper.importSettings(RESTORE_ITEMS_KEY, settingsItems, false, importListener);
				} catch (IllegalArgumentException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
	}

	@Override
	protected void setupImportingUi() {
		super.setupImportingUi();
		toolbarLayout.setTitle(getString(R.string.shared_string_importing));
		description.setText(UiUtilities.createSpannableString(
				String.format(getString(R.string.importing_from), getString(R.string.osmand_cloud)),
				Typeface.BOLD, getString(R.string.osmand_cloud)
		));
	}

	public static void showInstance(@NonNull FragmentManager fm, List<? super Object> duplicatesList,
									List<SettingsItem> settingsItems, Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			RestoreDuplicatesFragment fragment = new RestoreDuplicatesFragment();
			fragment.setTargetFragment(target, 0);
			fragment.setDuplicatesList(duplicatesList);
			fragment.setSettingsItems(settingsItems);
			fm.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(SETTINGS_LIST_TAG)
					.commitAllowingStateLoss();
		}
	}
}