package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.BackupHelper.CollectType;
import net.osmand.plus.backup.BackupHelper.OnCollectLocalFilesListener;
import net.osmand.plus.backup.BackupHelper.OnGenerateBackupInfoListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class PrepareBackupTask {

	private final OsmandApplication app;
	private final BackupHelper backupHelper;

	private PrepareBackupResult result;
	private final OnPrepareBackupListener listener;

	private Stack<TaskType> runningTasks = new Stack<>();

	private enum TaskType {
		COLLECT_LOCAL_FILES,
		COLLECT_REMOTE_FILES,
		GENERATE_BACKUP_INFO
	}

	public static class PrepareBackupResult {
		private BackupInfo backupInfo;
		private List<SettingsItem> settingsItems;
		private List<RemoteFile> remoteFiles;
		private List<RemoteFile> allRemoteFiles;
		private List<LocalFile> fileInfos;
		private String error;

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

		public List<RemoteFile> getAllRemoteFiles() {
			return allRemoteFiles;
		}

		public List<LocalFile> getFileInfos() {
			return fileInfos;
		}

		public String getError() {
			return error;
		}
	}

	public interface OnPrepareBackupListener {
		void onBackupPrepared(@Nullable PrepareBackupResult backupResult);
	}

	public PrepareBackupTask(@NonNull OsmandApplication app, @Nullable OnPrepareBackupListener listener) {
		this.app = app;
		this.backupHelper = app.getBackupHelper();
		this.listener = listener;
	}

	@Nullable
	public PrepareBackupResult getResult() {
		return result;
	}

	public boolean prepare() {
		if (!runningTasks.empty()) {
			return false;
		}
		initTasks();
		return runTasks();
	}

	private void initTasks() {
		result = new PrepareBackupResult();
		Stack<TaskType> tasks = new Stack<>();
		TaskType[] types = TaskType.values();
		for (int i = types.length - 1; i >= 0; i--) {
			tasks.push(types[i]);
		}
		this.runningTasks = tasks;
	}

	private boolean runTasks() {
		if (runningTasks.empty()) {
			return false;
		} else {
			TaskType taskType = runningTasks.pop();
			runTask(taskType);
			return true;
		}
	}

	private void runTask(@NonNull TaskType taskType) {
		switch (taskType) {
			case COLLECT_LOCAL_FILES:
				doCollectLocalFiles();
				break;
			case COLLECT_REMOTE_FILES:
				doCollectRemoteFiles();
				break;
			case GENERATE_BACKUP_INFO:
				doGenerateBackupInfo();
				break;
		}
	}

	private void onTaskFinished(@NonNull TaskType taskType) {
		if (!runTasks()) {
			onTasksDone();
		}
	}

	private void doCollectLocalFiles() {
		backupHelper.collectLocalFiles(new OnCollectLocalFilesListener() {
			@Override
			public void onFileCollected(@NonNull LocalFile fileInfo) {
			}

			@Override
			public void onFilesCollected(@NonNull List<LocalFile> fileInfos) {
				PrepareBackupTask.this.result.fileInfos = fileInfos;
				onTaskFinished(TaskType.COLLECT_LOCAL_FILES);
			}
		});
	}

	private void doCollectRemoteFiles() {
		app.getNetworkSettingsHelper().collectSettings("", 0, CollectType.COLLECT_UNIQUE, new NetworkSettingsHelper.BackupCollectListener() {
					@Override
					public void onBackupCollectFinished(boolean succeed, boolean empty,
														@NonNull List<SettingsItem> items,
														@NonNull List<RemoteFile> remoteFiles) {
						if (succeed) {
							List<RemoteFile> originalRemoteFiles = new ArrayList<>();
							for (RemoteFile remoteFile : remoteFiles) {
								if (!remoteFile.getName().endsWith(BackupHelper.INFO_EXT)) {
									originalRemoteFiles.add(remoteFile);
								}
							}
							PrepareBackupTask.this.result.settingsItems = items;
							PrepareBackupTask.this.result.remoteFiles = originalRemoteFiles;
							PrepareBackupTask.this.result.allRemoteFiles = remoteFiles;
						} else {
							onError("Download remote items error");
						}
						onTaskFinished(TaskType.COLLECT_REMOTE_FILES);
					}
				}
		);
	}

	private void doGenerateBackupInfo() {
		if (result.fileInfos == null || result.remoteFiles == null) {
			onTaskFinished(TaskType.GENERATE_BACKUP_INFO);
			return;
		}
		backupHelper.generateBackupInfo(result.fileInfos, result.remoteFiles, new OnGenerateBackupInfoListener() {
			@Override
			public void onBackupInfoGenerated(@Nullable BackupInfo backupInfo, @Nullable String error) {
				if (Algorithms.isEmpty(error)) {
					PrepareBackupTask.this.result.backupInfo = backupInfo;
				} else {
					onError(error);
				}
				onTaskFinished(TaskType.GENERATE_BACKUP_INFO);
			}
		});
	}

	private void onError(@NonNull String message) {
		result.error = message;
		runningTasks.clear();
		onTasksDone();
	}

	private void onTasksDone() {
		backupHelper.setBackup(result);
		if (listener != null) {
			listener.onBackupPrepared(result);
		}
	}
}
