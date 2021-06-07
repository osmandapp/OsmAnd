package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.BackupHelper.CollectType;
import net.osmand.plus.backup.BackupHelper.OnCollectLocalFilesListener;
import net.osmand.plus.backup.BackupHelper.OnGenerateBackupInfoListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.CollectListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class PrepareBackupTask {

	private final OsmandApplication app;
	private final BackupHelper backupHelper;

	private final OnPrepareBackupListener listener;

	private BackupInfo result;
	private List<RemoteFile> remoteFiles;
	private List<LocalFile> fileInfos;
	private String error;

	private Stack<TaskType> runningTasks = new Stack<>();

	private enum TaskType {
		COLLECT_LOCAL_FILES,
		COLLECT_REMOTE_FILES,
		GENERATE_BACKUP_INFO
	}

	public interface OnPrepareBackupListener {
		void onBackupPrepared(@Nullable BackupInfo backupInfo, @Nullable String error);
	}

	public PrepareBackupTask(@NonNull OsmandApplication app, @Nullable OnPrepareBackupListener listener) {
		this.app = app;
		this.backupHelper = app.getBackupHelper();
		this.listener = listener;
	}

	public BackupInfo getResult() {
		return result;
	}

	public String getError() {
		return error;
	}

	public boolean prepare() {
		if (!runningTasks.empty()) {
			return false;
		}
		initTasks();
		return runTasks();
	}

	private void initTasks() {
		result = null;
		remoteFiles = null;
		fileInfos = null;
		error = null;
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
				PrepareBackupTask.this.fileInfos = fileInfos;
				onTaskFinished(TaskType.COLLECT_LOCAL_FILES);
			}
		});
	}

	private void doCollectRemoteFiles() {
		app.getNetworkSettingsHelper().collectSettings("", 0, CollectType.COLLECT_UNIQUE, new CollectListener() {
			@Override
			public void onCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items) {
				if (succeed) {
					List<RemoteFile> remoteFiles = new ArrayList<>();
					for (RemoteFile remoteFile : app.getBackupHelper().getRemoteFiles()) {
						if (!remoteFile.getName().endsWith(BackupHelper.INFO_EXT)) {
							remoteFiles.add(remoteFile);
						}
					}
					PrepareBackupTask.this.remoteFiles = remoteFiles;
				} else {
					onError("Download remote items error");
				}
				onTaskFinished(TaskType.COLLECT_REMOTE_FILES);
			}
		});
	}

	private void doGenerateBackupInfo() {
		if (fileInfos == null || remoteFiles == null) {
			onTaskFinished(TaskType.GENERATE_BACKUP_INFO);
			return;
		}
		backupHelper.generateBackupInfo(fileInfos, remoteFiles, new OnGenerateBackupInfoListener() {
			@Override
			public void onBackupInfoGenerated(@Nullable BackupInfo backupInfo, @Nullable String error) {
				if (Algorithms.isEmpty(error)) {
					PrepareBackupTask.this.result = backupInfo;
				} else {
					onError(error);
				}
				onTaskFinished(TaskType.GENERATE_BACKUP_INFO);
			}
		});
	}

	private void onError(@NonNull String message) {
		this.error = message;
		runningTasks.clear();
		onTasksDone();
	}

	private void onTasksDone() {
		if (listener != null) {
			listener.onBackupPrepared(result, error);
		}
	}
}
