package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupListeners.OnCollectLocalFilesListener;
import net.osmand.plus.backup.BackupListeners.OnGenerateBackupInfoListener;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupCollectListener;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Stack;

public class PrepareBackupTask {

	private final OsmandApplication app;
	private final BackupHelper backupHelper;

	private PrepareBackupResult backup;
	private final OnPrepareBackupListener listener;

	private Stack<TaskType> runningTasks = new Stack<>();

	private enum TaskType {
		COLLECT_LOCAL_FILES,
		COLLECT_REMOTE_FILES,
		GENERATE_BACKUP_INFO
	}

	public interface OnPrepareBackupListener {
		void onBackupPreparing();
		void onBackupPrepared(@Nullable PrepareBackupResult backupResult);
	}

	public PrepareBackupTask(@NonNull OsmandApplication app, @Nullable OnPrepareBackupListener listener) {
		this.app = app;
		this.backupHelper = app.getBackupHelper();
		this.listener = listener;
	}

	@Nullable
	public PrepareBackupResult getResult() {
		return backup;
	}

	public boolean prepare() {
		if (!runningTasks.empty()) {
			return false;
		}
		if (listener != null) {
			listener.onBackupPreparing();
		}
		initTasks();
		return runTasks();
	}

	private void initTasks() {
		backup = new PrepareBackupResult();
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
			public void onFileCollected(@NonNull LocalFile localFile) {
			}

			@Override
			public void onFilesCollected(@NonNull List<LocalFile> localFiles) {
				backup.setLocalFiles(localFiles);
				onTaskFinished(TaskType.COLLECT_LOCAL_FILES);
			}
		});
	}

	private void doCollectRemoteFiles() {
		app.getNetworkSettingsHelper().collectSettings("", 0, false, new BackupCollectListener() {
					@Override
					public void onBackupCollectFinished(boolean succeed, boolean empty,
														@NonNull List<SettingsItem> items,
														@NonNull List<RemoteFile> remoteFiles) {
						if (succeed) {
							backup.setSettingsItems(items);
							backup.setRemoteFiles(remoteFiles);
						} else {
							onError("Download remote items error");
						}
						onTaskFinished(TaskType.COLLECT_REMOTE_FILES);
					}
				}
		);
	}

	private void doGenerateBackupInfo() {
		if (backup.getLocalFiles() == null || backup.getRemoteFiles() == null) {
			onTaskFinished(TaskType.GENERATE_BACKUP_INFO);
			return;
		}
		backupHelper.generateBackupInfo(backup.getLocalFiles(), backup.getRemoteFiles(RemoteFilesType.UNIQUE),
				backup.getRemoteFiles(RemoteFilesType.DELETED), new OnGenerateBackupInfoListener() {
					@Override
					public void onBackupInfoGenerated(@Nullable BackupInfo backupInfo, @Nullable String error) {
						if (Algorithms.isEmpty(error)) {
							backup.setBackupInfo(backupInfo);
						} else {
							onError(error);
						}
						onTaskFinished(TaskType.GENERATE_BACKUP_INFO);
					}
				});
	}

	private void onError(@NonNull String message) {
		backup.setError(message);
		runningTasks.clear();
		onTasksDone();
	}

	private void onTasksDone() {
		backupHelper.setBackup(backup);
		if (listener != null) {
			listener.onBackupPrepared(backup);
		}
	}
}
