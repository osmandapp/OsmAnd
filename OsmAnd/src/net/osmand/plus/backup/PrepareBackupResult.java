package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class PrepareBackupResult {
	private BackupInfo backupInfo;
	private List<SettingsItem> settingsItems;
	private List<RemoteFile> remoteFiles;
	private List<RemoteFile> uniqueRemoteFiles;
	private List<RemoteFile> uniqueInfoRemoteFiles;
	private List<RemoteFile> deletedRemoteFiles;
	private List<RemoteFile> oldRemoteFiles;
	private List<LocalFile> localFiles;
	private String error;

	public enum RemoteFilesType {
		ALL,
		UNIQUE,
		UNIQUE_INFO,
		DELETED,
		OLD,
	}

	PrepareBackupResult() {
	}

	public BackupInfo getBackupInfo() {
		return backupInfo;
	}

	public List<SettingsItem> getSettingsItems() {
		return settingsItems;
	}

	public List<RemoteFile> getRemoteFiles() {
		return remoteFiles;
	}

	public List<RemoteFile> getRemoteFiles(@NonNull RemoteFilesType type) {
		switch (type) {
			case UNIQUE:
				return uniqueRemoteFiles;
			case UNIQUE_INFO:
				return uniqueInfoRemoteFiles;
			case DELETED:
				return deletedRemoteFiles;
			case OLD:
				return oldRemoteFiles;
			default:
				return this.remoteFiles;
		}
	}

	@Nullable
	public RemoteFile getRemoteFile(@NonNull String type, @NonNull String fileName) {
		if (!Algorithms.isEmpty(fileName) && !Algorithms.isEmpty(remoteFiles)) {
			for (RemoteFile remoteFile : remoteFiles) {
				if (remoteFile.getType().equals(type) && remoteFile.getName().equals(fileName)) {
					return remoteFile;
				}
			}
		}
		return null;
	}

	public List<LocalFile> getLocalFiles() {
		return localFiles;
	}

	public String getError() {
		return error;
	}

	void setBackupInfo(BackupInfo backupInfo) {
		this.backupInfo = backupInfo;
	}

	void setSettingsItems(List<SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	void setRemoteFiles(List<RemoteFile> remoteFiles) {
		List<RemoteFile> uniqueRemoteFiles = new ArrayList<>();
		List<RemoteFile> uniqueInfoRemoteFiles = new ArrayList<>();
		List<RemoteFile> deletedRemoteFiles = new ArrayList<>();
		Set<String> uniqueFileIds = new TreeSet<>();
		for (RemoteFile rf : remoteFiles) {
			String fileId = rf.getTypeNamePath();
			if (uniqueFileIds.add(fileId)) {
				if (rf.isInfoFile()) {
					uniqueInfoRemoteFiles.add(rf);
				} else if (rf.isDeleted()) {
					deletedRemoteFiles.add(rf);
				} else {
					uniqueRemoteFiles.add(rf);
				}
			}
		}
		List<RemoteFile> oldRemoteFiles = new ArrayList<>();
		for (RemoteFile rf : remoteFiles) {
			if (!rf.isInfoFile() && !rf.isDeleted() && !uniqueRemoteFiles.contains(rf)) {
				oldRemoteFiles.add(rf);
			}
		}
		this.uniqueRemoteFiles = uniqueRemoteFiles;
		this.uniqueInfoRemoteFiles = uniqueInfoRemoteFiles;
		this.deletedRemoteFiles = deletedRemoteFiles;
		this.oldRemoteFiles = oldRemoteFiles;
		this.remoteFiles = remoteFiles;
	}

	void setLocalFiles(List<LocalFile> localFiles) {
		this.localFiles = localFiles;
	}

	void setError(String error) {
		this.error = error;
	}
}
