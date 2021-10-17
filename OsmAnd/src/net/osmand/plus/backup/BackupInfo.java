package net.osmand.plus.backup;

import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BackupInfo {
	public List<RemoteFile> filesToDownload = new ArrayList<>();
	public List<LocalFile> filesToUpload = new ArrayList<>();
	public List<RemoteFile> filesToDelete = new ArrayList<>();
	public List<LocalFile> localFilesToDelete = new ArrayList<>();
	public List<Pair<LocalFile, RemoteFile>> filesToMerge = new ArrayList<>();

	public List<SettingsItem> itemsToUpload;
	public List<SettingsItem> itemsToDelete;
	public List<LocalFile> filteredFilesToUpload;
	public List<RemoteFile> filteredFilesToDelete;
	public List<Pair<LocalFile, RemoteFile>> filteredFilesToMerge;

	void createItemCollections(@NonNull OsmandApplication app) {
		createFilteredFilesToUpload(app);
		createItemsToUpload();
		createFilteredFilesToDelete(app);
		createItemsToDelete();
		createFilteredFilesToMerge(app);
	}

	private void createItemsToUpload() {
		Set<SettingsItem> items = new HashSet<>();
		for (LocalFile localFile : filteredFilesToUpload) {
			SettingsItem item = localFile.item;
			if (item != null) {
				items.add(item);
			}
		}
		itemsToUpload = new ArrayList<>(items);
	}

	private void createItemsToDelete() {
		Set<SettingsItem> items = new HashSet<>();
		for (RemoteFile remoteFile : filteredFilesToDelete) {
			SettingsItem item = remoteFile.item;
			if (item != null) {
				items.add(item);
			}
		}
		itemsToDelete = new ArrayList<>(items);
	}

	private void createFilteredFilesToUpload(@NonNull OsmandApplication app) {
		List<LocalFile> files = new ArrayList<>();
		BackupHelper helper = app.getBackupHelper();
		for (LocalFile localFile : filesToUpload) {
			ExportSettingsType type = localFile.item != null ?
					ExportSettingsType.getExportSettingsTypeForItem(localFile.item) : null;
			if (type != null && helper.getBackupTypePref(type).get()) {
				files.add(localFile);
			}
		}
		filteredFilesToUpload = files;
	}

	private void createFilteredFilesToDelete(@NonNull OsmandApplication app) {
		List<RemoteFile> files = new ArrayList<>();
		BackupHelper helper = app.getBackupHelper();
		for (RemoteFile remoteFile : filesToDelete) {
			ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForRemoteFile(remoteFile);
			if (exportType != null && helper.getBackupTypePref(exportType).get()) {
				files.add(remoteFile);
			}
		}
		filteredFilesToDelete = files;
	}

	private void createFilteredFilesToMerge(@NonNull OsmandApplication app) {
		List<Pair<LocalFile, RemoteFile>> files = new ArrayList<>();
		Set<SettingsItem> items = new HashSet<>();
		BackupHelper helper = app.getBackupHelper();
		for (Pair<LocalFile, RemoteFile> pair : filesToMerge) {
			SettingsItem item = pair.first.item;
			if (!items.contains(item)) {
				ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForRemoteFile(pair.second);
				if (exportType != null && helper.getBackupTypePref(exportType).get()) {
					files.add(pair);
					items.add(item);
				}
			}
		}
		filteredFilesToMerge = files;
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
