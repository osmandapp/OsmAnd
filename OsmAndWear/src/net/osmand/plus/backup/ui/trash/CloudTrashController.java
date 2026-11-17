package net.osmand.plus.backup.ui.trash;

import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_DOWNLOAD;
import static net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType.DELETED;
import static net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType.OLD;
import static net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType.UNIQUE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.importfiles.ui.FileExistBottomSheet;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CloudTrashController {

	private static final Log log = PlatformUtil.getLog(CloudTrashController.class);

	public static final int CONFIRM_EMPTY_TRASH_ID = 1;
	public static final int DAYS_FOR_TRASH_CLEARING = 30;
	private final DateFormat GROUP_DATE_FORMAT = new SimpleDateFormat("LLLL yyyy", Locale.getDefault());

	private final OsmandApplication app;
	private final BackupHelper backupHelper;
	private final NetworkSettingsHelper settingsHelper;
	private final CloudTrashFragment fragment;

	public CloudTrashController(@NonNull OsmandApplication app, @NonNull CloudTrashFragment fragment) {
		this.app = app;
		this.fragment = fragment;
		this.backupHelper = app.getBackupHelper();
		this.settingsHelper = app.getNetworkSettingsHelper();
	}

	@NonNull
	public Map<String, TrashGroup> collectTrashGroups() {
		Map<String, TrashGroup> groups = new LinkedHashMap<>();

		List<TrashItem> items = collectTrashItems();
		if (!Algorithms.isEmpty(items)) {
			Collections.sort(items, (i1, i2) -> -Long.compare(i1.getTime(), i2.getTime()));

			for (TrashItem item : items) {
				long time = item.getTime();
				String name = Algorithms.capitalizeFirstLetter(GROUP_DATE_FORMAT.format(time));

				TrashGroup group = groups.get(name);
				if (group == null) {
					group = new TrashGroup(name);
					groups.put(name, group);
				}
				group.addItem(item);
			}
		}
		return groups;
	}

	@NonNull
	public List<TrashItem> collectTrashItems() {
		List<TrashItem> items = new ArrayList<>();
		PrepareBackupResult backup = backupHelper.getBackup();
		Map<String, RemoteFile> oldFiles = backup.getRemoteFiles(OLD);
		Map<String, RemoteFile> deletedFiles = backup.getRemoteFiles(DELETED);
		if (deletedFiles != null && oldFiles != null) {
			for (Map.Entry<String, RemoteFile> entry : deletedFiles.entrySet()) {
				RemoteFile oldFile = oldFiles.get(entry.getKey());
				if (oldFile != null) {
					items.add(new TrashItem(oldFile, entry.getValue()));
				}
			}
		}
		return items;
	}

	public void showClearConfirmationDialog() {
		FragmentManager manager = fragment.getFragmentManager();
		if (manager != null) {
			String title = app.getString(R.string.delete_all_items);
			String description = app.getString(R.string.cloud_trash_clear_confirmation);
			ConfirmationBottomSheet.showConfirmDeleteDialog(manager, fragment, title, description, CONFIRM_EMPTY_TRASH_ID);
		}
	}

	public void showItemMenu(@NonNull TrashItem item) {
		FragmentActivity activity = fragment.getActivity();
		if (activity != null) {
			CloudTrashItemMenuController.showDialog(activity, this, item);
		}
	}

	public void deleteItem(@NonNull TrashItem item) {
		try {
			List<RemoteFile> files = item.getRemoteFiles();
			String message = app.getString(R.string.shared_string_is_deleted, item.getName(app));
			backupHelper.deleteFiles(files, true, new TrashDeletionListener(message));
		} catch (UserNotRegisteredException e) {
			log.error(e);
		}
	}

	public void downloadItem(@NonNull TrashItem item) {
		SettingsItem settingsItem = item.getSettingsItem();
		if (settingsItem == null) {
			log.error("Failed to download item: " + item.oldFile.getName() + ", SettingsItem is null");
			return;
		}
		LocalFile localFile = backupHelper.getBackup().getLocalFiles().get(item.oldFile.getTypeNamePath());
		if (localFile != null) {
			FragmentManager manager = fragment.getFragmentManager();
			if (manager != null) {
				FileExistBottomSheet.showInstance(manager, localFile.getFileName(), overwrite -> downloadItem(item, overwrite));
			}
		} else {
			downloadItem(item, true);
		}
	}

	public void downloadItem(@NonNull TrashItem item, boolean shouldReplace) {
		boolean localDeletion = item.isLocalDeletion();
		RemoteFilesType filesType = localDeletion ? UNIQUE : OLD;
		settingsHelper.syncSettingsItems(item.oldFile.getName(), null, item.oldFile,
				filesType, SYNC_OPERATION_DOWNLOAD, shouldReplace, !localDeletion);
	}

	public void restoreItem(@NonNull TrashItem item) {
		if (item.getSettingsItem() == null) {
			log.error("Failed to restore item: " + item.oldFile.getName() + ", SettingsItem is null");
			return;
		}
		try {
			List<RemoteFile> files = Collections.singletonList(item.deletedFile);
			String message = app.getString(R.string.cloud_item_restored, item.getName(app));
			backupHelper.deleteFiles(files, true, new TrashDeletionListener(message));
		} catch (UserNotRegisteredException e) {
			log.error(e);
		}
	}

	public void clearTrash() {
		List<RemoteFile> files = new ArrayList<>();
		for (TrashItem item : collectTrashItems()) {
			files.addAll(item.getRemoteFiles());
		}
		try {
			String message = app.getString(R.string.trash_is_empty);
			backupHelper.deleteFiles(files, true, new TrashDeletionListener(message));
		} catch (UserNotRegisteredException e) {
			log.error(e);
		}
	}

	private class TrashDeletionListener implements OnDeleteFilesListener {

		private String snackbarMessage;

		public TrashDeletionListener(@Nullable String snackbarMessage) {
			this.snackbarMessage = snackbarMessage;
		}

		@Override
		public void onFilesDeleteStarted(@NonNull List<RemoteFile> files) {
			fragment.updateProgressVisibility(true);
		}

		@Override
		public void onFileDeleteProgress(@NonNull RemoteFile file, int progress) {
			fragment.updateProgressVisibility(true);
		}

		@Override
		public void onFilesDeleteError(int status, @NonNull String message) {
			fragment.updateProgressVisibility(false);
			app.showToastMessage(new BackupError(message).getLocalizedError(app));
			backupHelper.prepareBackup();
		}

		@Override
		public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {
			fragment.updateProgressVisibility(false);
			if (Algorithms.isEmpty(errors)) {
				if (!Algorithms.isEmpty(snackbarMessage)) {
					fragment.showSnackbar(snackbarMessage);
				}
			} else {
				String message = AndroidUtils.formatWarnings(errors.values()).toString();
				app.showToastMessage(new BackupError(message).getLocalizedError(app));
			}
			backupHelper.prepareBackup();
		}
	}
}