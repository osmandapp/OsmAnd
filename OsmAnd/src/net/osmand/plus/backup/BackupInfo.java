package net.osmand.plus.backup;

import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.ArrayList;
import java.util.Collections;
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
	public List<SettingsItem> itemsToLocalDelete;
	public List<LocalFile> filteredFilesToUpload;
	public List<RemoteFile> filteredFilesToDelete;
	public List<RemoteFile> filteredFilesToDownload;
	public List<Pair<LocalFile, RemoteFile>> filteredFilesToMerge;
	public List<LocalFile> filteredLocalFilesToDelete;

	void createItemCollections(@NonNull OsmandApplication app) {
		createFilteredFilesToDownload(app);
		createFilteredFilesToUpload(app);
		createItemsToUpload();
		createFilteredFilesToDelete(app);
		createItemsToDelete();
		createFilteredFilesToMerge(app);
		createFilteredLocalFilesToDelete();
		createLocalItemsToDelete();
	}

	private void createItemsToUpload() {
		Set<SettingsItem> items = new HashSet<>();
		for (LocalFile localFile : filteredFilesToUpload) {
			SettingsItem item = localFile.item;
			if (item != null) {
				items.add(item);
			}
		}
		itemsToUpload = getSortedItems(items);
	}

	private void createItemsToDelete() {
		Set<SettingsItem> items = new HashSet<>();
		for (RemoteFile remoteFile : filteredFilesToDelete) {
			SettingsItem item = remoteFile.item;
			if (item != null) {
				items.add(item);
			}
		}
		itemsToDelete = getSortedItems(items);
	}

	private void createLocalItemsToDelete() {
		Set<SettingsItem> items = new HashSet<>();
		for (LocalFile localFile : filteredLocalFilesToDelete) {
			SettingsItem item = localFile.item;
			if (item != null) {
				items.add(item);
			}
		}
		itemsToLocalDelete = getSortedItems(items);
	}

	@NonNull
	private List<SettingsItem> getSortedItems(@NonNull Set<SettingsItem> settingsItems) {
		List<SettingsItem> items = new ArrayList<>(settingsItems);
		Collections.sort(items, (o1, o2) -> -Long.compare(o1.getLastModifiedTime(), o2.getLastModifiedTime()));
		return items;
	}

	private void createFilteredFilesToDownload(@NonNull OsmandApplication app) {
		List<RemoteFile> files = new ArrayList<>();
		BackupHelper helper = app.getBackupHelper();
		for (RemoteFile remoteFile : filesToDownload) {
			ExportType exportType = ExportType.findBy(remoteFile);
			if (exportType != null && helper.getBackupTypePref(exportType).get()) {
				files.add(remoteFile);
			}
		}
		filteredFilesToDownload = files;
	}

	private void createFilteredFilesToUpload(@NonNull OsmandApplication app) {
		List<LocalFile> files = new ArrayList<>();
		BackupHelper helper = app.getBackupHelper();
		for (LocalFile localFile : filesToUpload) {
			ExportType type = ExportType.findBy(localFile.item);
			if (type != null
					&& helper.getBackupTypePref(type).get()
					&& InAppPurchaseUtils.isExportTypeAvailable(app, type)) {
				files.add(localFile);
			}
		}
		filteredFilesToUpload = files;
	}

	private void createFilteredFilesToDelete(@NonNull OsmandApplication app) {
		List<RemoteFile> files = new ArrayList<>();
		BackupHelper helper = app.getBackupHelper();
		for (RemoteFile remoteFile : filesToDelete) {
			ExportType exportType = ExportType.findBy(remoteFile);
			if (exportType != null && helper.getBackupTypePref(exportType).get()) {
				files.add(remoteFile);
			}
		}
		filteredFilesToDelete = files;
	}

	private void createFilteredLocalFilesToDelete() {
		List<LocalFile> files = new ArrayList<>();
		for (LocalFile localFile : localFilesToDelete) {
			ExportType exportType = ExportType.findBy(localFile.item);
			if (exportType != null && exportType.isEnabled()) {
				files.add(localFile);
			}
		}
		filteredLocalFilesToDelete = files;
	}

	private void createFilteredFilesToMerge(@NonNull OsmandApplication app) {
		List<Pair<LocalFile, RemoteFile>> files = new ArrayList<>();
		Set<SettingsItem> items = new HashSet<>();
		BackupHelper helper = app.getBackupHelper();
		for (Pair<LocalFile, RemoteFile> pair : filesToMerge) {
			SettingsItem item = pair.first.item;
			if (!items.contains(item)) {
				ExportType exportType = ExportType.findBy(pair.second);
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
