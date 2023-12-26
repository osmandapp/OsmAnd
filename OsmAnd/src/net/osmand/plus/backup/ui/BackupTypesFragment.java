package net.osmand.plus.backup.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.ExportType;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;

import org.apache.commons.logging.Log;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BackupTypesFragment extends BaseBackupTypesFragment {

	public static final String TAG = BackupTypesFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(BackupTypesFragment.class);

	@Override
	protected int getTitleId() {
		return R.string.backup_data;
	}

	@Override
	protected BackupClearType getClearType() {
		return BackupClearType.ALL;
	}

	@Override
	protected RemoteFilesType getRemoteFilesType() {
		return RemoteFilesType.UNIQUE;
	}

	@Override
	protected Map<ExportType, List<?>> getSelectedItems() {
		Map<ExportType, List<?>> selectedItemsMap = new EnumMap<>(ExportType.class);
		boolean available = InAppPurchaseUtils.isBackupAvailable(app);
		for (ExportType type : ExportType.values()) {
			if (backupHelper.getBackupTypePref(type).get() && (type.isAllowedInFreeVersion() || available)) {
				selectedItemsMap.put(type, getItemsForType(type));
			}
		}
		return selectedItemsMap;
	}

	@Override
	public void onCategorySelected(ExportCategory category, boolean selected) {
		super.onCategorySelected(category, selected);

		SettingsCategoryItems categoryItems = dataList.get(category);
		for (ExportType type : categoryItems.getTypes()) {
			backupHelper.getBackupTypePref(type).set(selected);
		}
	}

	@Override
	public void onTypeSelected(ExportType type, boolean selected) {
		super.onTypeSelected(type, selected);
		backupHelper.getBackupTypePref(type).set(selected);
	}

	@Override
	public void onClearTypesConfirmed(@NonNull List<ExportType> types) {
		try {
			updateProgressVisibility(true);
			backupHelper.deleteAllFiles(types);
		} catch (UserNotRegisteredException e) {
			updateProgressVisibility(false);
			log.error(e);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (manager.findFragmentByTag(TAG) == null) {
			BackupTypesFragment fragment = new BackupTypesFragment();
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}