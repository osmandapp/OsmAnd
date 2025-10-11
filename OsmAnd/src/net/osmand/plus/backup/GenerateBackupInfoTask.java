package net.osmand.plus.backup;

import static net.osmand.plus.backup.BackupHelper.DEBUG;

import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.OperationLog;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupListeners.OnGenerateBackupInfoListener;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.CollectionSettingsItem;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenerateBackupInfoTask extends AsyncTask<Void, Void, BackupInfo> {

	private static final Log LOG = PlatformUtil.getLog(GenerateBackupInfoTask.class);

	private final OperationLog operationLog = new OperationLog("generateBackupInfo", DEBUG, 200);

	private final OsmandApplication app;
	private final BackupHelper backupHelper;

	private final Map<String, LocalFile> localFiles;
	private final Map<String, RemoteFile> uniqueRemoteFiles;
	private final Map<String, RemoteFile> deletedRemoteFiles;
	private final OnGenerateBackupInfoListener listener;
	private final boolean autoSync;

	protected GenerateBackupInfoTask(@NonNull OsmandApplication app,
			@NonNull Map<String, LocalFile> localFiles,
			@NonNull Map<String, RemoteFile> uniqueRemoteFiles,
			@NonNull Map<String, RemoteFile> deletedRemoteFiles,
			@NonNull boolean autoSync,
			@Nullable OnGenerateBackupInfoListener listener) {
		this.app = app;
		this.backupHelper = app.getBackupHelper();
		this.localFiles = localFiles;
		this.uniqueRemoteFiles = uniqueRemoteFiles;
		this.deletedRemoteFiles = deletedRemoteFiles;
		this.listener = listener;
		this.autoSync = autoSync;
		operationLog.startOperation();
	}

	@Override
	protected BackupInfo doInBackground(Void... voids) {
		BackupInfo info = new BackupInfo(autoSync);
				/*
				operationLog.log("=== localFiles ===");
				for (LocalFile localFile : localFiles.values()) {
					operationLog.log(localFile.toString());
				}
				operationLog.log("=== localFiles ===");
				operationLog.log("=== uniqueRemoteFiles ===");
				for (RemoteFile remoteFile : uniqueRemoteFiles.values()) {
					operationLog.log(remoteFile.toString());
				}
				operationLog.log("=== uniqueRemoteFiles ===");
				operationLog.log("=== deletedRemoteFiles ===");
				for (RemoteFile remoteFile : deletedRemoteFiles.values()) {
					operationLog.log(remoteFile.toString());
				}
				operationLog.log("=== deletedRemoteFiles ===");
				*/
		List<RemoteFile> remoteFiles = new ArrayList<>(uniqueRemoteFiles.values());
		remoteFiles.addAll(deletedRemoteFiles.values());
		for (RemoteFile remoteFile : remoteFiles) {
			ExportType exportType = ExportType.findBy(remoteFile);
			if (exportType == null || !exportType.isEnabled() || remoteFile.isRecordedVoiceFile()) {
				continue;
			}
			LocalFile localFile = localFiles.get(remoteFile.getTypeNamePath());
			if (localFile != null) {
				boolean fileChangedLocally = localFile.localModifiedTime > localFile.uploadTime;
				boolean fileChangedRemotely = remoteFile.getUpdatetimems() > localFile.uploadTime;
				if (fileChangedRemotely && fileChangedLocally) {
					info.filesToMerge.add(new Pair<>(localFile, remoteFile));
				} else if (fileChangedLocally) {
					info.filesToUpload.add(localFile);
				} else if (fileChangedRemotely) {
					if (remoteFile.isDeleted()) {
						info.localFilesToDelete.add(localFile);
					} else {
						info.filesToDownload.add(remoteFile);
					}
				}
				if (PluginsHelper.isDevelopment()) {
					if (fileChangedRemotely || fileChangedLocally) {
						LOG.debug("file to backup " + localFile + " " + remoteFile
								+ " fileChangedRemotely " + fileChangedRemotely + " fileChangedLocally " + fileChangedLocally);
					}
				}
			} else if (!remoteFile.isDeleted()) {
				UploadedFileInfo fileInfo = backupHelper.getUploadedFileInfo(remoteFile.getType(), remoteFile.getName());
				// suggest to remove only if file exists in db
				if (fileInfo != null && fileInfo.getUploadTime() >= remoteFile.getUpdatetimems()) {
					// conflicts not supported yet
					// info.filesToMerge.add(new Pair<>(null, remoteFile));
					info.filesToDelete.add(remoteFile);
				} else {
					info.filesToDownload.add(remoteFile);
				}
			}
		}
		for (LocalFile localFile : localFiles.values()) {
			ExportType exportType = ExportType.findBy(localFile.item);
			if (exportType == null || !exportType.isEnabled()) {
				continue;
			}
			boolean hasRemoteFile = uniqueRemoteFiles.containsKey(localFile.getTypeFileName());
			boolean toDelete = info.localFilesToDelete.contains(localFile);
			if (!hasRemoteFile && !toDelete) {
				boolean isEmpty = localFile.item instanceof CollectionSettingsItem<?> && ((CollectionSettingsItem<?>) localFile.item).isEmpty();
				if (!isEmpty) {
					info.filesToUpload.add(localFile);
				}
			}
		}
		info.createItemCollections(app);

		operationLog.log("=== filesToUpload ===");
		for (LocalFile localFile : info.filesToUpload) {
			operationLog.log(localFile.toString());
		}
		operationLog.log("=== filesToUpload ===");
		operationLog.log("=== filesToDownload ===");
		for (RemoteFile remoteFile : info.filesToDownload) {
			LocalFile localFile = localFiles.get(remoteFile.getTypeNamePath());
			if (localFile != null) {
				operationLog.log(remoteFile + " localUploadTime=" + localFile.uploadTime);
			} else {
				operationLog.log(remoteFile.toString());
			}
		}
		operationLog.log("=== filesToDownload ===");
		operationLog.log("=== filesToDelete ===");
		for (RemoteFile remoteFile : info.filesToDelete) {
			operationLog.log(remoteFile.toString());
		}
		operationLog.log("=== filesToDelete ===");
		operationLog.log("=== localFilesToDelete ===");
		for (LocalFile localFile : info.localFilesToDelete) {
			operationLog.log(localFile.toString());
		}
		operationLog.log("=== localFilesToDelete ===");
		operationLog.log("=== filesToMerge ===");
		for (Pair<LocalFile, RemoteFile> filePair : info.filesToMerge) {
			operationLog.log("LOCAL=" + filePair.first.toString() + " REMOTE=" + filePair.second.toString());
		}
		operationLog.log("=== filesToMerge ===");
		return info;
	}

	@Override
	protected void onPostExecute(BackupInfo backupInfo) {
		operationLog.finishOperation(backupInfo.toString());
		if (listener != null) {
			listener.onBackupInfoGenerated(backupInfo, null);
		}
	}
}
