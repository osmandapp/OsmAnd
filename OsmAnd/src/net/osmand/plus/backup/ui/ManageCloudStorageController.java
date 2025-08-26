package net.osmand.plus.backup.ui;

import static android.graphics.Typeface.BOLD;
import static net.osmand.plus.utils.UiUtilities.createSpannableString;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.OnCompleteCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

import java.util.ArrayList;
import java.util.Collections;
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
		String name = getString(exportType.getTitleId());
		List<ExportType> typesToDelete = Collections.singletonList(exportType);
		showConfirmDeleteDataDialog(name, () -> onClearTypesConfirmed(typesToDelete));
	}

	@Override
	public void onCategorySelected(ExportCategory exportCategory, boolean selected) {
		String name = getString(exportCategory.getTitleId());
		List<ExportType> typesToDelete = new ArrayList<>(getCategoryItems(exportCategory).getTypes());
		showConfirmDeleteDataDialog(name, () -> onClearTypesConfirmed(typesToDelete));
	}

	private void showConfirmDeleteDataDialog(@NonNull String name,
	                                         @NonNull OnCompleteCallback callback) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			int warningColor = ColorUtilities.getColor(app, R.color.deletion_color_warning);
			int textColor = ColorUtilities.getSecondaryTextColor(activity, isNightMode());

			AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
					.setTitle(getString(R.string.delete_data))
					.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_delete, ((dialog, which) -> callback.onComplete()))
					.setPositiveButtonTextColor(warningColor);

			String description = getString(R.string.manage_storage_delete_data_dialog_summary, name);
			SpannableString spannable = createSpannableString(description, BOLD, name);
			UiUtilities.setSpan(spannable, new ForegroundColorSpan(textColor), description, description);
			CustomAlert.showSimpleMessage(dialogData, spannable);
		}
	}

	@Override
	public void onClearTypesConfirmed(@NonNull List<ExportType> types) {
		try {
			screen.updateProgressVisibility(true);
			List<RemoteFile> remoteFiles = collectRemoteFilesForTypes(types);
			backupHelper.deleteFiles(remoteFiles, false, null);
		} catch (UserNotRegisteredException e) {
			screen.updateProgressVisibility(false);
			LOG.error(e);
		}
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
