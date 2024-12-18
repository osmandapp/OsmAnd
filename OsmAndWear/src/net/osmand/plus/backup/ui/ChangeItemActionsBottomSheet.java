package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_DELETE;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_DOWNLOAD;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_UPLOAD;
import static net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType.UNIQUE;
import static net.osmand.plus.backup.ui.BackupUiUtils.generateTimeString;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_LOCAL;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_REMOTE;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.ExportBackupTask;
import net.osmand.plus.backup.ImportBackupTask;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType;
import net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType;
import net.osmand.plus.backup.ui.ChangesTabFragment.CloudChangeItem;
import net.osmand.plus.backup.ui.status.ItemViewHolder;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class ChangeItemActionsBottomSheet extends BottomSheetDialogFragment {

	public static final String TAG = ChangeItemActionsBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private NetworkSettingsHelper settingsHelper;

	public CloudChangeItem item;
	public RecentChangesType recentChangesType;
	private boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		settingsHelper = app.getNetworkSettingsHelper();
		nightMode = !app.getSettings().isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.change_item_bottom_sheet, container, false);

		TextView title = view.findViewById(R.id.title);
		title.setText(getTitleForOperation());

		setupHeaderItem(view);
		setupUploadAction(view);
		setupDownloadAction(view);

		return view;
	}

	private void setupHeaderItem(@NonNull View view) {
		View container = view.findViewById(R.id.item);
		ItemViewHolder itemViewHolder = new ItemViewHolder(container, nightMode);
		itemViewHolder.bindView(item, null, false);

		TextView descriptionView = container.findViewById(R.id.description);
		descriptionView.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon,
				app.getString(R.string.last_synchronized), BackupUiUtils.getTimeString(app, item.time)));
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.second_icon), false);
	}

	private void setupDownloadAction(@NonNull View view) {
		boolean deleteOperation = item.operation == SYNC_OPERATION_DELETE;
		boolean enabled = isRowEnabled(item.fileName) && (item.remoteFile != null || deleteOperation);

		String description;
		if (deleteOperation) {
			SettingsItemType type = item.remoteFile != null ? item.remoteFile.item.getType() : item.localFile.item.getType();
			boolean localChanges = recentChangesType == RECENT_CHANGES_LOCAL;
			if (localChanges) {
				String typeDescr = getDescriptionForItemType(type, item.fileName, getString(R.string.shared_string_uploaded));
				description = typeDescr + "\n" + getString(R.string.local_file_will_be_restored);
				deleteOperation = false;
			} else {
				description = generateTimeString(app, getString(R.string.shared_string_deleted), item.localFile.uploadTime);
			}
		} else if (item.remoteFile == null) {
			description = getString(R.string.shared_string_do_not_exist);
		} else {
			description = generateTimeString(app, getString(R.string.shared_string_uploaded), item.remoteFile.getUpdatetimems());
			if (recentChangesType == RECENT_CHANGES_LOCAL) {
				description = description + "\n" + getString(R.string.local_changes_will_be_dismissed);
			}
		}
		View downloadItem = view.findViewById(R.id.download_item);
		TextView titleTv = downloadItem.findViewById(R.id.title);
		TextView descriptionTv = downloadItem.findViewById(R.id.description);
		ImageView imageView = downloadItem.findViewById(R.id.icon);

		titleTv.setTextColor(enabled ? ColorUtilities.getActiveColor(app, nightMode) : ColorUtilities.getSecondaryTextColor(app, nightMode));
		titleTv.setText(R.string.download_cloud_version);
		descriptionTv.setText(description);
		imageView.setImageDrawable(getIcon(R.drawable.ic_action_cloud_download_outline, enabled ? ColorUtilities.getActiveColorId(nightMode) : ColorUtilities.getSecondaryIconColorId(nightMode)));
		SyncOperationType operationType = deleteOperation ? SYNC_OPERATION_DELETE : SYNC_OPERATION_DOWNLOAD;
		downloadItem.setOnClickListener(v -> {
			syncItem(operationType);
			dismiss();
		});
		downloadItem.setEnabled(enabled);
		if (enabled) {
			setupSelectableBackground(downloadItem);
		}
		AndroidUiHelper.updateVisibility(downloadItem.findViewById(R.id.second_icon), false);
		View divider = downloadItem.findViewById(R.id.bottom_divider);
		View bottomShadow = downloadItem.findViewById(R.id.bottom_shadow);
		AndroidUiHelper.updateVisibility(divider, false);
		AndroidUiHelper.updateVisibility(bottomShadow, false);
	}

	private void setupUploadAction(@NonNull View view) {
		boolean deleteOperation = item.operation == SYNC_OPERATION_DELETE;
		boolean enabled = isRowEnabled(item.fileName);
		String title = getString(deleteOperation || item.localFile == null ? R.string.delete_cloud_version : R.string.upload_local_version);
		String description;
		if (deleteOperation) {
			description = recentChangesType == RECENT_CHANGES_LOCAL ? getString(R.string.cloud_version_will_be_removed)
					: generateTimeString(app, getString(R.string.shared_string_modified), item.localFile.localModifiedTime);
		} else if (item.localFile == null) {
			description = getString(R.string.local_version_do_not_exist);
		} else {
			description = generateTimeString(app, getString(R.string.shared_string_modified), item.localFile.localModifiedTime);
			if (recentChangesType == RECENT_CHANGES_REMOTE) {
				description = description + "\n" + getString(R.string.cloud_changes_will_be_dismissed);
			}
		}
		View uploadItem = view.findViewById(R.id.upload_item);
		TextView titleTv = uploadItem.findViewById(R.id.title);
		TextView descriptionTv = uploadItem.findViewById(R.id.description);
		ImageView imageView = uploadItem.findViewById(R.id.icon);

		titleTv.setText(title);
		descriptionTv.setText(description);
		Drawable icon;
		if (item.localFile == null) {
			icon = getIcon(R.drawable.ic_action_cloud_delete, enabled ? R.color.color_osm_edit_delete : ColorUtilities.getSecondaryIconColorId(nightMode));
			titleTv.setTextColor(enabled ? ContextCompat.getColor(app, R.color.backup_warning) : ColorUtilities.getSecondaryTextColorId(nightMode));
		} else {
			icon = getIcon(R.drawable.ic_action_cloud_upload_outline, enabled ? ColorUtilities.getActiveColorId(nightMode) : ColorUtilities.getSecondaryIconColorId(nightMode));
			titleTv.setTextColor(enabled ? ColorUtilities.getActiveColor(app, nightMode) : ColorUtilities.getSecondaryTextColor(app, nightMode));
		}
		imageView.setImageDrawable(icon);
		uploadItem.setOnClickListener(v -> {
			SyncOperationType operationType = deleteOperation || item.localFile == null ? SYNC_OPERATION_DELETE : SYNC_OPERATION_UPLOAD;
			if (operationType == SYNC_OPERATION_DELETE) {
				AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(requireActivity(), nightMode));
				builder.setTitle(app.getString(R.string.shared_string_delete_file));
				builder.setMessage(getString(R.string.cloud_version_confirm_delete, item.title));
				builder.setNeutralButton(R.string.shared_string_cancel, null)
						.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
							syncItem(operationType);
							dismiss();
						});
				builder.show();
			} else {
				syncItem(operationType);
				dismiss();
			}
		});
		uploadItem.setEnabled(enabled);
		if (enabled) {
			setupSelectableBackground(uploadItem);
		}
		AndroidUiHelper.updateVisibility(uploadItem.findViewById(R.id.second_icon), false);
		View divider = uploadItem.findViewById(R.id.bottom_divider);
		View bottomShadow = uploadItem.findViewById(R.id.bottom_shadow);
		AndroidUiHelper.updateVisibility(divider, true);
		AndroidUiHelper.updateVisibility(bottomShadow, false);
	}

	private void setupSelectableBackground(@NonNull View view) {
		int color = ColorUtilities.getActiveColor(app, nightMode);
		View selectableView = view.findViewById(R.id.selectable_list_item);
		AndroidUtils.setBackground(selectableView, UiUtilities.getColoredSelectableDrawable(app, color, 0.3f));
	}

	private void syncItem(@NonNull SyncOperationType operation) {
		settingsHelper.syncSettingsItems(item.fileName, item.localFile, item.remoteFile, UNIQUE, operation);
	}

	private String getTitleForOperation() {
		switch (item.operation) {
			case SYNC_OPERATION_DOWNLOAD:
				return getString(item.localFile == null ? R.string.new_file : R.string.modified_file);
			case SYNC_OPERATION_UPLOAD:
				return getString(item.remoteFile == null ? R.string.new_file : R.string.modified_file);
			case SYNC_OPERATION_DELETE:
				return getString(R.string.deleted_file);
			default:
				return getString(R.string.cloud_conflict);
		}
	}

	private boolean isRowEnabled(@NonNull String fileName) {
		ImportBackupTask importTask = settingsHelper.getImportTask(fileName);
		ExportBackupTask exportTask = settingsHelper.getExportTask(fileName);
		return exportTask == null && importTask == null;
	}

	@Nullable
	private String getDescriptionForItemType(SettingsItemType type, String fileName, String summary) {
		UploadedFileInfo info = app.getBackupHelper().getDbHelper().getUploadedFileInfo(type.name(), fileName);
		return info != null ? generateTimeString(app, summary, info.getUploadTime()) : null;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull CloudChangeItem item,
	                                @NonNull ChangesTabFragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ChangeItemActionsBottomSheet fragment = new ChangeItemActionsBottomSheet();
			fragment.item = item;
			fragment.recentChangesType = target.getChangesTabType();
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
