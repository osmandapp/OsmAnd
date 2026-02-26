package net.osmand.plus.backup.commands;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.Request;
import net.osmand.plus.utils.AndroidNetworkUtils.RequestResponse;
import net.osmand.OperationLog;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupCommand;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.ThreadPoolTaskExecutor;
import net.osmand.plus.backup.ThreadPoolTaskExecutor.OnThreadPoolTaskExecutorListener;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.backup.BackupHelper.DELETE_FILE_URL;
import static net.osmand.plus.backup.BackupHelper.DELETE_FILE_VERSION_URL;

public abstract class BaseDeleteFilesCommand extends BackupCommand {

	private final boolean byVersion;
	private OnDeleteFilesListener listener;
	private List<DeleteRemoteFileTask> tasks = new ArrayList<>();
	private final Set<Object> itemsProgress = new HashSet<>();

	public BaseDeleteFilesCommand(@NonNull BackupHelper helper, boolean byVersion) {
		super(helper);
		this.byVersion = byVersion;
	}

	public BaseDeleteFilesCommand(@NonNull BackupHelper helper, boolean byVersion,
								  @Nullable OnDeleteFilesListener listener) {
		super(helper);
		this.byVersion = byVersion;
		this.listener = listener;
	}

	public List<OnDeleteFilesListener> getListeners() {
		return getHelper().getBackupListeners().getDeleteFilesListeners();
	}

	protected void deleteFiles(@NonNull List<RemoteFile> remoteFiles) {
		Map<String, String> commonParameters = new HashMap<>();
		commonParameters.put("deviceid", getHelper().getDeviceId());
		commonParameters.put("accessToken", getHelper().getAccessToken());

		List<DeleteRemoteFileTask> tasks = new ArrayList<>();
		for (RemoteFile remoteFile : remoteFiles) {
			Map<String, String> parameters = new HashMap<>(commonParameters);
			parameters.put("name", remoteFile.getName());
			parameters.put("type", remoteFile.getType());
			if (byVersion) {
				parameters.put("updatetime", String.valueOf(remoteFile.getUpdatetimems()));
			}
			Request r = new Request(byVersion ? DELETE_FILE_VERSION_URL : DELETE_FILE_URL, parameters, null, false, true);
			tasks.add(new DeleteRemoteFileTask(getApp(), r, remoteFile, byVersion));
		}
		ThreadPoolTaskExecutor<DeleteRemoteFileTask> executor =
				new ThreadPoolTaskExecutor<>(new OnThreadPoolTaskExecutorListener<DeleteRemoteFileTask>() {

					@Override
					public void onTaskStarted(@NonNull DeleteRemoteFileTask task) {
					}

					@Override
					public void onTaskFinished(@NonNull DeleteRemoteFileTask task) {
						publishProgress(task);
					}

					@Override
					public void onTasksFinished(@NonNull List<DeleteRemoteFileTask> tasks) {
						BaseDeleteFilesCommand.this.tasks = tasks;
					}
				});
		executor.run(tasks);
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			getListeners().add(listener);
		}
	}

	@Override
	protected void onProgressUpdate(Object... objects) {
		for (OnDeleteFilesListener listener : getListeners()) {
			Object obj = objects[0];
			if (obj instanceof DeleteRemoteFileTask) {
				RemoteFile remoteFile = ((DeleteRemoteFileTask) obj).remoteFile;
				itemsProgress.add(remoteFile);
				listener.onFileDeleteProgress(remoteFile, itemsProgress.size());
			}
		}
	}

	@Override
	protected void onPostExecute(Object o) {
		List<OnDeleteFilesListener> listeners = getListeners();
		if (!listeners.isEmpty()) {
			Map<RemoteFile, String> errors = new HashMap<>();
			for (DeleteRemoteFileTask task : tasks) {
				boolean success;
				String message = null;
				RequestResponse response = task.response;
				if (response != null) {
					String errorStr = response.getError();
					if (!Algorithms.isEmpty(errorStr)) {
						message = new BackupError(errorStr).toString();
						success = false;
					} else {
						String responseStr = response.getResponse();
						try {
							JSONObject result = new JSONObject(responseStr);
							if (result.has("status") && "ok".equals(result.getString("status"))) {
								success = true;
							} else {
								message = "Unknown error";
								success = false;
							}
						} catch (JSONException e) {
							message = "Json parsing error";
							success = false;
						}
					}
					if (!success) {
						errors.put(task.remoteFile, message);
					}
				}
			}
			for (OnDeleteFilesListener listener : listeners) {
				listener.onFilesDeleteDone(errors);
			}
		}
		if (listener != null) {
			getListeners().remove(listener);
		}
	}

	private static class DeleteRemoteFileTask extends ThreadPoolTaskExecutor.Task {

		private final OsmandApplication app;
		private final Request request;
		private final RemoteFile remoteFile;
		private final boolean byVersion;
		private RequestResponse response;

		public DeleteRemoteFileTask(@NonNull OsmandApplication app, @NonNull Request request,
									@NonNull RemoteFile remoteFile, boolean byVersion) {
			this.app = app;
			this.request = request;
			this.remoteFile = remoteFile;
			this.byVersion = byVersion;
		}

		@Override
		public Void call() {
			OperationLog operationLog = new OperationLog("deleteFile", BackupHelper.DEBUG);
			AndroidNetworkUtils.sendRequest(app, request, (result, error, resultCode) ->
					response = new RequestResponse(request, result, error));
			if (response.getError() == null && !byVersion) {
				app.getBackupHelper().getDbHelper().removeUploadedFileInfo(
						new UploadedFileInfo(remoteFile.getType(), remoteFile.getName()));
			}
			operationLog.finishOperation(remoteFile.getName());
			return null;
		}
	}
}
