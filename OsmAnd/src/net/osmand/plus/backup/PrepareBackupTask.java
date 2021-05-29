package net.osmand.plus.backup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.BackupHelper.OnCollectLocalFilesListener;
import net.osmand.plus.backup.BackupHelper.OnDownloadFileListListener;
import net.osmand.plus.backup.BackupHelper.OnGenerateBackupInfoListener;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Stack;

public class PrepareBackupTask {

	private final OsmandApplication app;
	private final BackupHelper backupHelper;

	private final OnPrepareBackupListener listener;
	private final WeakReference<Context> contextRef;
	private ProgressImplementation progress;

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
		void onBackupPrepared(@Nullable BackupInfo backupInfo, @Nullable List<UserFile> userFiles,
							  @Nullable List<GpxFileInfo> fileInfos, @Nullable String error);
	}

	public PrepareBackupTask(@NonNull Context context, @Nullable OnPrepareBackupListener listener) {
		this.contextRef = new WeakReference<>(context);
		this.app = (OsmandApplication) context.getApplicationContext();
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
		onTasksInit();
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
		onTaskProgressUpdate("Collecting local info...");
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
		onTaskProgressUpdate("Downloading remote info...");
		try {
			backupHelper.downloadFileList(new OnDownloadFileListListener() {
				@Override
				public void onDownloadFileList(int status, @Nullable String message, @NonNull List<RemoteFile> remoteFiles) {
					if (status == BackupHelper.STATUS_SUCCESS) {
						PrepareBackupTask.this.remoteFiles = remoteFiles;
					} else {
						onError(!Algorithms.isEmpty(message) ? message : "Download file list error: " + status);
					}
					onTaskFinished(TaskType.COLLECT_REMOTE_FILES);
				}
			});
		} catch (UserNotRegisteredException e) {
			onError("User is not registered");
		}
	}

	private void doGenerateBackupInfo() {
		if (fileInfos == null || remoteFiles == null) {
			onTaskFinished(TaskType.GENERATE_BACKUP_INFO);
			return;
		}
		onTaskProgressUpdate("Generating backup info...");
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

	private void onTasksInit() {
		Context ctx = contextRef.get();
		if (ctx instanceof Activity && AndroidUtils.isActivityNotDestroyed((Activity) ctx)) {
			progress = ProgressImplementation.createProgressDialog(ctx,
					"Prepare backup", "Initializing...", ProgressDialog.STYLE_HORIZONTAL);
		}
	}

	private void onTaskProgressUpdate(String message) {
		Context ctx = contextRef.get();
		if (ctx instanceof Activity && AndroidUtils.isActivityNotDestroyed((Activity) ctx) && progress != null) {
			progress.startTask(message, -1);
		}
	}

	private void onError(@NonNull String message) {
		this.error = message;
		runningTasks.clear();
		onTasksDone();
	}

	private void onTasksDone() {
		if (listener != null) {
			listener.onBackupPrepared(result, userFiles, fileInfos, error);
		}
		Context ctx = contextRef.get();
		if (ctx instanceof Activity && AndroidUtils.isActivityNotDestroyed((Activity) ctx) && progress != null) {
			progress.finishTask();
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					try {
						if (progress.getDialog().isShowing()) {
							progress.getDialog().dismiss();
						}
					} catch (Exception e) {
						//ignored
					}
				}
			}, 300);
		}
	}
}
