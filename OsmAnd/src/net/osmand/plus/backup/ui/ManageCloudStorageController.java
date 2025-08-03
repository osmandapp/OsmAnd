package net.osmand.plus.backup.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;

import java.util.List;

public class ManageCloudStorageController extends BaseBackupTypesController {

	private static final String PROCESS_ID = "manage_cloud_storage";

	public ManageCloudStorageController(@NonNull OsmandApplication app) {
		super(app, BackupClearType.ALL, RemoteFilesType.UNIQUE);
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@NonNull
	@Override
	public BackupTypesAdapter createUiAdapter(@NonNull Context context) {
		return new ManageCloudStorageAdapter(context, this);
	}

	@Override
	public int getTitleId() {
		return R.string.manage_storage;
	}

	@Override
	public void onTypeSelected(@NonNull ExportType exportType, boolean selected) {
		showConfirmClearDialog(null, exportType);
	}

	@Override
	public void onCategorySelected(ExportCategory exportCategory, boolean selected) {
		showConfirmClearDialog(exportCategory, null);
	}

	private void showConfirmClearDialog(@Nullable ExportCategory category,
	                                    @Nullable ExportType exportType) {
		// TODO: implement
		app.showShortToastMessage((category != null ? category.name() : exportType.name()) + " deleted");
	}

	@Override
	public void onClearTypesConfirmed(@NonNull List<ExportType> types) {

	}

	@Nullable
	public static ManageCloudStorageController getExistedInstance(@NonNull OsmandApplication app) {
		return (ManageCloudStorageController) app.getDialogManager().findController(PROCESS_ID);
	}

	public static void showScreen(@NonNull FragmentActivity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new ManageCloudStorageController(app));

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		BackupTypesFragment.showInstance(fragmentManager, PROCESS_ID);
	}
}
