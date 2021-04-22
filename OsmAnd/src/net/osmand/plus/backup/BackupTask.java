package net.osmand.plus.backup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.BackupHelper.OnDeleteFilesListener;
import net.osmand.plus.backup.BackupHelper.OnDownloadFileListener;
import net.osmand.plus.backup.BackupHelper.OnUploadFilesListener;
import net.osmand.plus.importfiles.FavoritesImportTask;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.TEMP_DIR;

public class BackupTask {

	private final OsmandApplication app;
	private final BackupHelper backupHelper;

	private final OnBackupListener listener;
	private final WeakReference<Context> contextRef;
	private ProgressImplementation progress;

	private final BackupInfo backupInfo;
	private Map<File, String> uploadErrors;
	private Map<File, String> downloadErrors;
	private Map<UserFile, String> deleteErrors;
	private String error;

	private final TaskType[] backupTasks = {TaskType.UPLOAD_FILES, TaskType.DELETE_FILES};
	private final TaskType[] restoreTasks = {TaskType.DOWNLOAD_FILES};

	private Stack<TaskType> runningTasks = new Stack<>();

	private enum TaskType {
		UPLOAD_FILES,
		DOWNLOAD_FILES,
		DELETE_FILES
	}

	public interface OnBackupListener {
		void onBackupDone(@Nullable Map<File, String> uploadErrors,
						  @Nullable Map<File, String> downloadErrors,
						  @Nullable Map<UserFile, String> deleteErrors, @Nullable String error);
	}

	public BackupTask(@NonNull BackupInfo backupInfo, @NonNull Context context, @Nullable OnBackupListener listener) {
		this.contextRef = new WeakReference<>(context);
		this.app = (OsmandApplication) context.getApplicationContext();
		this.backupHelper = app.getBackupHelper();
		this.backupInfo = backupInfo;
		this.listener = listener;
	}

	public BackupInfo getBackupInfo() {
		return backupInfo;
	}

	public Map<File, String> getUploadErrors() {
		return uploadErrors;
	}

	public Map<File, String> getDownloadErrors() {
		return downloadErrors;
	}

	public Map<UserFile, String> getDeleteErrors() {
		return deleteErrors;
	}

	public String getError() {
		return error;
	}

	public boolean runBackup() {
		if (!runningTasks.empty()) {
			return false;
		}
		initBackupTasks();
		return runTasks();
	}

	public boolean runRestore() {
		if (!runningTasks.empty()) {
			return false;
		}
		initRestoreTasks();
		return runTasks();
	}

	private void initBackupTasks() {
		initData();
		Stack<TaskType> tasks = new Stack<>();
		for (int i = backupTasks.length - 1; i >= 0; i--) {
			tasks.push(backupTasks[i]);
		}
		this.runningTasks = tasks;
		onBackupTasksInit();
	}

	private void initRestoreTasks() {
		initData();
		Stack<TaskType> tasks = new Stack<>();
		for (int i = restoreTasks.length - 1; i >= 0; i--) {
			tasks.push(restoreTasks[i]);
		}
		this.runningTasks = tasks;
		onRestoreTasksInit();
	}

	private void initData() {
		uploadErrors = null;
		downloadErrors = null;
		deleteErrors = null;
		error = null;
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
			case UPLOAD_FILES:
				doUploadFiles();
				break;
			case DOWNLOAD_FILES:
				doDownloadFiles();
				break;
			case DELETE_FILES:
				doDeleteFiles();
				break;
		}
	}

	private void onTaskFinished(@NonNull TaskType taskType) {
		if (!runTasks()) {
			onTasksDone();
		}
	}

	private void doUploadFiles() {
		if (Algorithms.isEmpty(backupInfo.filesToUpload)) {
			onTaskFinished(TaskType.UPLOAD_FILES);
			return;
		}
		onTaskProgressUpdate("Upload files...");
		try {
			backupHelper.uploadFiles(backupInfo.filesToUpload, new OnUploadFilesListener() {
				@Override
				public void onFileUploadProgress(@NonNull File file, int progress) {
					if (progress == 0) {
						onTaskProgressUpdate(file.getName(), (int) (file.length() / 1024));
					} else {
						onTaskProgressUpdate(progress);
					}
				}

				@Override
				public void onFilesUploadDone(@NonNull Map<File, String> errors) {
					uploadErrors = errors;
					onTaskFinished(TaskType.UPLOAD_FILES);
				}
			});
		} catch (UserNotRegisteredException e) {
			onError("User is not registered");
		}
	}

	private void doDownloadFiles() {
		if (Algorithms.isEmpty(backupInfo.filesToDownload)) {
			onTaskFinished(TaskType.DOWNLOAD_FILES);
			return;
		}
		onTaskProgressUpdate("Download files...");
		File favoritesFile = app.getFavorites().getExternalFile();
		String favoritesFileName = favoritesFile.getName();
		File tempFavoritesFile = null;
		final Map<File, UserFile> filesMap = new HashMap<>();
		for (UserFile userFile : backupInfo.filesToDownload) {
			File file;
			String fileName = userFile.getName();
			if (favoritesFileName.equals(fileName)) {
				file = new File(app.getAppPath(TEMP_DIR), fileName);
				tempFavoritesFile = file;
			} else {
				file = new File(app.getAppPath(GPX_INDEX_DIR), fileName);
			}
			filesMap.put(file, userFile);
		}
		final File finalTempFavoritesFile = tempFavoritesFile;
		try {
			backupHelper.downloadFiles(filesMap, new OnDownloadFileListener() {
				@Override
				public void onFileDownloadProgress(@NonNull UserFile userFile, int progress) {
					if (progress == 0) {
						onTaskProgressUpdate(new File(userFile.getName()).getName(), userFile.getFilesize() / 1024);
					} else {
						onTaskProgressUpdate(progress);
					}
				}

				@Override
				public void onFileDownloadedAsync(@NonNull File file) {
					UserFile userFile = filesMap.get(file);
					long userFileTime = userFile.getClienttimems();
					if (file.equals(finalTempFavoritesFile)) {
						GPXFile gpxFile = GPXUtilities.loadGPXFile(finalTempFavoritesFile);
						FavoritesImportTask.mergeFavorites(app, gpxFile, "", false);
						finalTempFavoritesFile.delete();
						app.getFavorites().getExternalFile().setLastModified(userFileTime);
					} else {
						file.setLastModified(userFileTime);
						GpxDataItem item = new GpxDataItem(file, userFileTime);
						app.getGpxDbHelper().add(item);
					}
				}

				@Override
				public void onFilesDownloadDone(@NonNull Map<File, String> errors) {
					downloadErrors = errors;
					onTaskFinished(TaskType.DOWNLOAD_FILES);
				}
			});
		} catch (UserNotRegisteredException e) {
			onError("User is not registered");
		}
	}

	private void doDeleteFiles() {
		if (Algorithms.isEmpty(backupInfo.filesToDelete)) {
			onTaskFinished(TaskType.DELETE_FILES);
			return;
		}
		onTaskProgressUpdate("Delete files...");
		try {
			backupHelper.deleteFiles(backupInfo.filesToDelete, new OnDeleteFilesListener() {
				@Override
				public void onFileDeleteProgress(@NonNull UserFile userFile) {
					onTaskProgressUpdate(userFile.getName());
				}

				@Override
				public void onFilesDeleteDone(@NonNull Map<UserFile, String> errors) {
					deleteErrors = errors;
					onTaskFinished(TaskType.DELETE_FILES);
				}
			});
		} catch (UserNotRegisteredException e) {
			onError("User is not registered");
		}
	}

	private void onBackupTasksInit() {
		Context ctx = contextRef.get();
		if (ctx instanceof Activity && AndroidUtils.isActivityNotDestroyed((Activity) ctx)) {
			progress = ProgressImplementation.createProgressDialog(ctx,
					"Backup data", "Initializing...", ProgressDialog.STYLE_HORIZONTAL);
		}
	}

	private void onRestoreTasksInit() {
		Context ctx = contextRef.get();
		if (ctx instanceof Activity && AndroidUtils.isActivityNotDestroyed((Activity) ctx)) {
			progress = ProgressImplementation.createProgressDialog(ctx,
					"Restore data", "Initializing...", ProgressDialog.STYLE_HORIZONTAL);
		}
	}

	private void onTaskProgressUpdate(Object... objects) {
		Context ctx = contextRef.get();
		if (ctx instanceof Activity && AndroidUtils.isActivityNotDestroyed((Activity) ctx) && progress != null) {
			if (objects != null) {
				if (objects.length == 1) {
					if (objects[0] instanceof String) {
						progress.startTask((String) objects[0], -1);
					} else if (objects[0] instanceof Integer) {
						int progressValue = (Integer) objects[0];
						if (progressValue < Integer.MAX_VALUE) {
							progress.progress(progressValue);
						} else {
							progress.finishTask();
						}
					}
				} else if (objects.length == 2) {
					progress.startTask((String) objects[0], (Integer) objects[1]);
				}
			}
		}
	}

	private void onError(@NonNull String message) {
		this.error = message;
		runningTasks.clear();
		onTasksDone();
	}

	private void onTasksDone() {
		if (listener != null) {
			listener.onBackupDone(uploadErrors, downloadErrors, deleteErrors, error);
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
