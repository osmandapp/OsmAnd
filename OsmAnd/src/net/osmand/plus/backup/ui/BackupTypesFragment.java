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
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
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
		for (ExportType exportType : ExportType.values()) {
			boolean enabled = backupHelper.getBackupTypePref(exportType).get();
			boolean available = InAppPurchaseUtils.isExportTypeAvailable(app, exportType);
			boolean cloudRestore = requireMapActivity().getFragmentsHelper().isFirstScreenShowing();

			if (enabled && (available || cloudRestore)) {
				selectedItemsMap.put(exportType, getItemsForType(exportType));
			}
		}
		return selectedItemsMap;
	}

	@Override
	public void onCategorySelected(ExportCategory exportCategory, boolean selected) {
		super.onCategorySelected(exportCategory, selected);

		SettingsCategoryItems categoryItems = dataList.get(exportCategory);
		for (ExportType exportType : categoryItems.getTypes()) {
			backupHelper.getBackupTypePref(exportType).set(selected);
		}
	}

	@Override
	public void onTypeSelected(@NonNull ExportType exportType, boolean selected) {
		super.onTypeSelected(exportType, selected);
		backupHelper.getBackupTypePref(exportType).set(selected);
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