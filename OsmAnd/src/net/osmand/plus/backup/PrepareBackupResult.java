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

	@Nullable
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

	@Nullable
	public String getError() {
		return error;
	}

	void setBackupInfo(@Nullable BackupInfo backupInfo) {
		this.backupInfo = backupInfo;
	}

	void setSettingsItems(@NonNull List<SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	void setRemoteFiles(@NonNull List<RemoteFile> remoteFiles) {
		Map<String, RemoteFile> remoteFilesMap = new HashMap<>();
		Map<String, RemoteFile> oldRemoteFiles = new HashMap<>();
		for (RemoteFile file : remoteFiles) {
			String typeNamePath = file.getTypeNamePath();
			if (!remoteFilesMap.containsKey(typeNamePath)) {
				remoteFilesMap.put(typeNamePath, file);
			} else if (!oldRemoteFiles.containsKey(typeNamePath) && !file.isInfoFile() && !file.isDeleted()) {
				oldRemoteFiles.put(typeNamePath, file);
			}
		}
		Map<String, RemoteFile> uniqueRemoteFiles = new HashMap<>();
		Map<String, RemoteFile> uniqueInfoRemoteFiles = new HashMap<>();
		Map<String, RemoteFile> deletedRemoteFiles = new HashMap<>();
		Set<String> uniqueFileIds = new TreeSet<>();
		for (Entry<String, RemoteFile> entry : remoteFilesMap.entrySet()) {
			String fileId = entry.getKey();
			RemoteFile file = entry.getValue();
			if (uniqueFileIds.add(fileId)) {
				if (file.isInfoFile()) {
					uniqueInfoRemoteFiles.put(fileId, file);
				} else if (file.isDeleted()) {
					deletedRemoteFiles.put(fileId, file);
				} else {
					uniqueRemoteFiles.put(fileId, file);
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

	public void setError(@Nullable String error) {
		this.error = error;
	}
}
