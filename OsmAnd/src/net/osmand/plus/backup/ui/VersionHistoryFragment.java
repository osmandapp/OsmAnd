package net.osmand.plus.backup.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;

import org.apache.commons.logging.Log;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class VersionHistoryFragment extends BaseBackupTypesFragment {

	public static final String TAG = VersionHistoryFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(VersionHistoryFragment.class);

	@Override
	protected int getTitleId() {
		return R.string.backup_version_history;
	}

	@Override
	protected BackupClearType getClearType() {
		return BackupClearType.HISTORY;
	}

	@Override
	protected RemoteFilesType getRemoteFilesType() {
		return RemoteFilesType.OLD;
	}

	@Override
	protected Map<ExportSettingsType, List<?>> getSelectedItems() {
		Map<ExportSettingsType, List<?>> selectedItemsMap = new EnumMap<>(ExportSettingsType.class);
		for (ExportSettingsType type : ExportSettingsType.values()) {
			if (backupHelper.getVersionHistoryTypePref(type).get()) {
				selectedItemsMap.put(type, getItemsForType(type));
			}
		}
		return selectedItemsMap;
	}

	@Override
	public void onCategorySelected(ExportSettingsCategory category, boolean selected) {
		super.onCategorySelected(category, selected);

		SettingsCategoryItems categoryItems = dataList.get(category);
		for (ExportSettingsType type : categoryItems.getTypes()) {
			backupHelper.getVersionHistoryTypePref(type).set(selected);
		}
	}

	@Override
	public void onTypeSelected(ExportSettingsType type, boolean selected) {
		super.onTypeSelected(type, selected);
		backupHelper.getVersionHistoryTypePref(type).set(selected);
	}

	@Override
	public void onClearTypesConfirmed(@NonNull List<ExportSettingsType> types) {
		try {
			updateProgressVisibility(true);
			backupHelper.deleteOldFiles(types);
		} catch (UserNotRegisteredException e) {
			updateProgressVisibility(false);
			log.error(e);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (manager.findFragmentByTag(TAG) == null) {
			VersionHistoryFragment fragment = new VersionHistoryFragment();
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
