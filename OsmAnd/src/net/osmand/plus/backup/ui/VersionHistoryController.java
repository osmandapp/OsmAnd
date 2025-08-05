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
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class VersionHistoryController extends SwitchBackupTypesController {

	private static final String PROCESS_ID = "select_types_to_manage_version_history";

	public VersionHistoryController(@NonNull OsmandApplication app) {
		super(app, BackupClearType.HISTORY, RemoteFilesType.OLD);
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Override
	public int getTitleId() {
		return R.string.backup_version_history;
	}

	@Override
	@NonNull
	protected Map<ExportType, List<?>> collectSelectedItems() {
		Map<ExportType, List<?>> selectedItemsMap = new EnumMap<>(ExportType.class);
		for (ExportType exportType : ExportType.values()) {
			if (backupHelper.getVersionHistoryTypePref(exportType).get()) {
				selectedItemsMap.put(exportType, getItemsForType(exportType));
			}
		}
		return selectedItemsMap;
	}

	@Override
	protected void applyTypePreference(@NonNull ExportType exportType, boolean selected) {
		backupHelper.getVersionHistoryTypePref(exportType).set(selected);
	}

	@Override
	protected boolean shouldProposeToClearData() {
		return true;
	}

	@Override
	public void clearDataTypes(@NonNull List<ExportType> types) throws UserNotRegisteredException {
		backupHelper.deleteOldFiles(types);
	}

	public static void showScreen(@NonNull FragmentActivity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new VersionHistoryController(app));

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		BackupTypesFragment.showInstance(fragmentManager, PROCESS_ID);
	}
}
