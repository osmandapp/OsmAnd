package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupListeners.OnCollectLocalFilesListener;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PrepareBackupTask {

	private final OsmandApplication app;
	private final BackupHelper backupHelper;

	private PrepareBackupResult backup;
	private final OnPrepareBackupListener listener;

	private List<TaskType> pendingTasks = new ArrayList<>();
	private List<TaskType> finishedTasks = new ArrayList<>();

	private enum TaskType {
		COLLECT_LOCAL_FILES(null),
		COLLECT_REMOTE_FILES(null),
		GENERATE_BACKUP_INFO(new TaskType[]{COLLECT_LOCAL_FILES, COLLECT_REMOTE_FILES});

		private final TaskType[] dependentTasks;

		TaskType(@Nullable TaskType[] dependentTasks) {
			this.dependentTasks = dependentTasks;
		}
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
		if (!pendingTasks.isEmpty()) {
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
		this.pendingTasks = new ArrayList<>(Arrays.asList(TaskType.values()));
		this.finishedTasks = new ArrayList<>();
	}

	private boolean runTasks() {
		if (pendingTasks.isEmpty()) {
			return false;
		} else {
			Iterator<TaskType> it = pendingTasks.iterator();
			while (it.hasNext()) {
				boolean shouldRun = true;
				TaskType taskType = it.next();
				if (taskType.dependentTasks != null) {
					for (TaskType dependentTask : taskType.dependentTasks) {
						if (!finishedTasks.contains(dependentTask)) {
							shouldRun = false;
							break;
						}
					}
				}
				if (shouldRun) {
					it.remove();
					runTask(taskType);
				}
			}
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
		finishedTasks.add(taskType);
		if (!runTasks()) {
			onTasksDone();
		}
	}

	private void doCollectLocalFiles() {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onStart(AppInitializer init) {
				}

				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					collectLocalFilesImpl();
				}
			});
		} else {
			collectLocalFilesImpl();
		}
	}

	private void collectLocalFilesImpl() {
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
		app.getNetworkSettingsHelper().collectSettings("", 0, false, (succeed, empty, items, remoteFiles) -> {
					if (succeed) {
						backup.setSettingsItems(items);
						backup.setRemoteFiles(remoteFiles);
					} else {
						onError("Download remote items error");
					}
					onTaskFinished(TaskType.COLLECT_REMOTE_FILES);
				}
		);
	}

	private void doGenerateBackupInfo() {
		if (backup.getLocalFiles() == null || backup.getRemoteFiles() == null) {
			onTaskFinished(TaskType.GENERATE_BACKUP_INFO);
			return;
		}
		backupHelper.generateBackupInfo(backup.getLocalFiles(), backup.getRemoteFiles(RemoteFilesType.UNIQUE),
				backup.getRemoteFiles(RemoteFilesType.DELETED), (backupInfo, error) -> {
					if (Algorithms.isEmpty(error)) {
						backup.setBackupInfo(backupInfo);
					} else {
						onError(error);
					}
					onTaskFinished(TaskType.GENERATE_BACKUP_INFO);
				});
	}

	private void onError(@NonNull String message) {
		backup.setError(message);
		pendingTasks.clear();
		onTasksDone();
	}

	private void onTasksDone() {
		backupHelper.setBackup(backup);
		if (listener != null) {
			listener.onBackupPrepared(backup);
		}
	}
}
