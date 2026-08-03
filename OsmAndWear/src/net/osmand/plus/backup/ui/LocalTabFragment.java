package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_DELETE;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_UPLOAD;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_LOCAL;

import androidx.annotation.NonNull;

import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalTabFragment extends ChangesTabFragment {

	@NonNull
	@Override
	public RecentChangesType getChangesTabType() {
		return RECENT_CHANGES_LOCAL;
	}

	@NonNull
	@Override
	public List<CloudChangeItem> generateData() {
		List<CloudChangeItem> changeItems = new ArrayList<>();

		PrepareBackupResult backup = backupHelper.getBackup();
		BackupInfo info = backup.getBackupInfo();
		if (info == null) {
			return changeItems;
		}
		Map<String, FileInfo> filesByName = new HashMap<>();
		List<LocalFile> localFiles = info.filteredFilesToUpload;
		for (LocalFile localFile : localFiles) {
			String key = localFile.getTypeFileName();
			FileInfo fileInfo = new FileInfo();
			fileInfo.localFile = localFile;
			filesByName.put(key, fileInfo);
		}
		List<RemoteFile> deletedFiles = info.filteredFilesToDelete;
		for (RemoteFile deletedFile : deletedFiles) {
			String key = deletedFile.getTypeNamePath();
			FileInfo fileInfo = new FileInfo();
			fileInfo.remoteFile = deletedFile;
			fileInfo.deleted = true;
			filesByName.put(key, fileInfo);
		}
		if (filesByName.size() > 0) {
			Map<RemoteFile, SettingsItem> downloadItems = BackupUtils.getItemsMapForRestore(info, backup.getSettingsItems());
			for (Map.Entry<RemoteFile, SettingsItem> entry : downloadItems.entrySet()) {
				RemoteFile remoteFile = entry.getKey();
				String key = remoteFile.getTypeNamePath();
				FileInfo fileInfo = filesByName.get(key);
				if (fileInfo != null && fileInfo.remoteFile == null) {
					fileInfo.remoteFile = remoteFile;
				}
			}
			for (Map.Entry<String, FileInfo> entry : filesByName.entrySet()) {
				String key = entry.getKey();
				FileInfo fileInfo = entry.getValue();
				if (fileInfo.remoteFile == null) {
					RemoteFile remoteFile = backup.getRemoteFiles().get(key);
					if (remoteFile != null) {
						fileInfo.remoteFile = remoteFile;
					}
				}
			}
		}
		for (Map.Entry<String, FileInfo> entry : filesByName.entrySet()) {
			String key = entry.getKey();
			FileInfo fileInfo = entry.getValue();

			SyncOperationType operation;
			if (fileInfo.deleted) {
				operation = SYNC_OPERATION_DELETE;
			} else {
				operation = SYNC_OPERATION_UPLOAD;
			}
			CloudChangeItem changeItem = createChangeItem(operation, fileInfo.localFile, fileInfo.remoteFile);
			if (changeItem != null) {
				// FIXME
				changeItems.add(changeItem);
			}
		}
		return changeItems;
	}
}
