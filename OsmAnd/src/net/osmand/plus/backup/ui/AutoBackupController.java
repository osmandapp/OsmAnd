package net.osmand.plus.backup.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AutoBackupController extends SwitchBackupTypesController {

	private static final String PROCESS_ID = "select_auto_backup_types";

	public AutoBackupController(@NonNull OsmandApplication app) {
		super(app, BackupClearType.ALL, RemoteFilesType.ALL);
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Override
	public int getTitleId() {
		return R.string.auto_backup_title;
	}

	@Override
	@NonNull
	protected Map<ExportType, List<?>> collectSelectedItems() {
		Map<ExportType, List<?>> selectedItemsMap = new EnumMap<>(ExportType.class);
		for (ExportType exportType : ExportType.visibleValues()) {
			boolean enabled = backupHelper.getBackupTypePref(exportType, true).get();
			boolean available = InAppPurchaseUtils.isExportTypeAvailable(app, exportType);

			if (enabled && (available || cloudRestore)) {
				selectedItemsMap.put(exportType, getItemsForType(exportType));
			}
		}
		return selectedItemsMap;
	}

	@NonNull
	@Override
	protected Map<ExportCategory, SettingsCategoryItems> collectData() {
		Map<ExportType, List<?>> map = new EnumMap<>(ExportType.class);
		for (ExportType exportType : ExportType.visibleValues()) {
			map.put(exportType, Collections.emptyList());
		}
		return SettingsHelper.categorizeSettingsToOperate(map, true);
	}

	@Override
	protected void applyTypePreference(@NonNull ExportType exportType, boolean selected) {
		backupHelper.getBackupTypePref(exportType, true).set(selected);
	}

	@Override
	public void clearDataTypes(@NonNull List<ExportType> types) throws UserNotRegisteredException {
		backupHelper.deleteAllFiles(types);
	}

	public static void showScreen(@NonNull FragmentActivity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new AutoBackupController(app));

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		BackupTypesFragment.showInstance(fragmentManager, PROCESS_ID);
	}
}

