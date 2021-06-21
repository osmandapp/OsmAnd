package net.osmand.plus.backup;

import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.ArrayList;
import java.util.List;

public class BackupInfo {
	public List<RemoteFile> filesToDownload = new ArrayList<>();
	public List<LocalFile> filesToUpload = new ArrayList<>();
	public List<RemoteFile> filesToDelete = new ArrayList<>();
	public List<LocalFile> localFilesToDelete = new ArrayList<>();
	public List<Pair<LocalFile, RemoteFile>> filesToMerge = new ArrayList<>();

	public List<SettingsItem> getItemsToUpload(@NonNull OsmandApplication app) {
		List<SettingsItem> items = new ArrayList<>();
		for (LocalFile localFile : getFilteredFilesToUpload(app)) {
			SettingsItem item = localFile.item;
			if (item != null && !items.contains(item)) {
				items.add(item);
			}
		}
		return items;
	}

	public List<SettingsItem> getItemsToDelete(@NonNull OsmandApplication app) {
		List<SettingsItem> items = new ArrayList<>();
		for (RemoteFile remoteFile : getFilteredFilesToDelete(app)) {
			SettingsItem item = remoteFile.item;
			if (item != null && !items.contains(item)) {
				items.add(item);
			}
		}
		return items;
	}

	public List<LocalFile> getFilteredFilesToUpload(@NonNull OsmandApplication app) {
		List<LocalFile> files = new ArrayList<>();
		BackupHelper helper = app.getBackupHelper();
		for (LocalFile localFile : filesToUpload) {
			ExportSettingsType type = localFile.item != null ?
					ExportSettingsType.getExportSettingsTypeForItem(localFile.item) : null;
			if (type != null && helper.getBackupTypePref(type).get()) {
				files.add(localFile);
			}
		}
		return files;
	}

	public List<RemoteFile> getFilteredFilesToDelete(@NonNull OsmandApplication app) {
		List<RemoteFile> files = new ArrayList<>();
		BackupHelper helper = app.getBackupHelper();
		for (RemoteFile remoteFile : filesToDelete) {
			ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForRemoteFile(remoteFile);
			if (exportType != null && helper.getBackupTypePref(exportType).get()) {
				files.add(remoteFile);
			}
		}
		return files;
	}

	public List<Pair<LocalFile, RemoteFile>> getFilteredFilesToMerge(@NonNull OsmandApplication app) {
		List<Pair<LocalFile, RemoteFile>> files = new ArrayList<>();
		BackupHelper helper = app.getBackupHelper();
		for (Pair<LocalFile, RemoteFile> pair : filesToMerge) {
			ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForRemoteFile(pair.second);
			if (exportType != null && helper.getBackupTypePref(exportType).get()) {
				files.add(pair);
			}
		}
		return files;
	}

	@NonNull
	@Override
	public String toString() {
		return "BackupInfo {" +
				" filesToDownload=" + filesToDownload.size() +
				", filesToUpload=" + filesToUpload.size() +
				", filesToDelete=" + filesToDelete.size() +
				", localFilesToDelete=" + localFilesToDelete.size() +
				", filesToMerge=" + filesToMerge.size() +
				" }";
	}
}
