package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class PrepareBackupResult {
	private BackupInfo backupInfo;
	private List<SettingsItem> settingsItems;
	private Map<String, RemoteFile> remoteFiles;
	private Map<String, RemoteFile> uniqueRemoteFiles;
	private Map<String, RemoteFile> uniqueInfoRemoteFiles;
	private Map<String, RemoteFile> deletedRemoteFiles;
	private Map<String, RemoteFile> oldRemoteFiles;
	private Map<String, LocalFile> localFiles;
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

	public Map<String, RemoteFile> getRemoteFiles() {
		return remoteFiles;
	}

	public Map<String, RemoteFile> getRemoteFiles(@NonNull RemoteFilesType type) {
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
		String typeWithName;
		if (!Algorithms.isEmpty(fileName)) {
			typeWithName = type + (fileName.charAt(0) == '/' ? fileName : "/" + fileName);
		} else {
			typeWithName = type;
		}
		return remoteFiles.get(typeWithName);
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
		Map<String, RemoteFile> remoteFilesMap = new HashMap<>();
		Map<String, RemoteFile> oldRemoteFiles = new HashMap<>();
		for (RemoteFile rf : remoteFiles) {
			String typeNamePath = rf.getTypeNamePath();
			if (!remoteFilesMap.containsKey(typeNamePath)) {
				remoteFilesMap.put(typeNamePath, rf);
			} else if (!rf.isInfoFile() && !rf.isDeleted()) {
				oldRemoteFiles.put(typeNamePath, rf);
			}
		}
		Map<String, RemoteFile> uniqueRemoteFiles = new HashMap<>();
		Map<String, RemoteFile> uniqueInfoRemoteFiles = new HashMap<>();
		Map<String, RemoteFile> deletedRemoteFiles = new HashMap<>();
		Set<String> uniqueFileIds = new TreeSet<>();
		for (Entry<String, RemoteFile> rfEntry : remoteFilesMap.entrySet()) {
			String fileId = rfEntry.getKey();
			RemoteFile rf = rfEntry.getValue();
			if (uniqueFileIds.add(fileId)) {
				if (rf.isInfoFile()) {
					uniqueInfoRemoteFiles.put(fileId, rf);
				} else if (rf.isDeleted()) {
					deletedRemoteFiles.put(fileId, rf);
				} else {
					uniqueRemoteFiles.put(fileId, rf);
				}
			}
		}
		this.uniqueRemoteFiles = uniqueRemoteFiles;
		this.uniqueInfoRemoteFiles = uniqueInfoRemoteFiles;
		this.deletedRemoteFiles = deletedRemoteFiles;
		this.oldRemoteFiles = oldRemoteFiles;
		this.remoteFiles = remoteFilesMap;
	}

	public Map<String, LocalFile> getLocalFiles() {
		return localFiles;
	}

	void setLocalFiles(@NonNull List<LocalFile> localFiles) {
		Map<String, LocalFile> localFileMap = new HashMap<>();
		for (LocalFile localFile : localFiles) {
			localFileMap.put(localFile.getTypeFileName(), localFile);
		}
		this.localFiles = localFileMap;
	}

	void setError(String error) {
		this.error = error;
	}
}
