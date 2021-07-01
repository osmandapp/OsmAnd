package net.osmand.plus.backup.commands;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.RequestResponse;
import net.osmand.OperationLog;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupCommand;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ServerError;
import net.osmand.plus.backup.ThreadPoolTaskExecutor;
import net.osmand.plus.backup.ThreadPoolTaskExecutor.OnThreadPoolTaskExecutorListener;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.backup.BackupHelper.DELETE_FILE_URL;
import static net.osmand.plus.backup.BackupHelper.DELETE_FILE_VERSION_URL;
import static net.osmand.plus.backup.BackupHelper.THREAD_POOL_SIZE;

public abstract class BaseDeleteFilesCommand extends BackupCommand {

	private final boolean byVersion;
	private OnDeleteFilesListener listener;
	private List<SendRequestTask> tasks = new ArrayList<>();

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

		List<SendRequestTask> tasks = new ArrayList<>();
		for (RemoteFile remoteFile : remoteFiles) {
			Map<String, String> parameters = new HashMap<>(commonParameters);
			parameters.put("name", remoteFile.getName());
			parameters.put("type", remoteFile.getType());
			if (byVersion) {
				parameters.put("updatetime", String.valueOf(remoteFile.getUpdatetimems()));
			}
			AndroidNetworkUtils.Request r = new AndroidNetworkUtils.Request(byVersion ? DELETE_FILE_VERSION_URL : DELETE_FILE_URL, parameters, null, false, true);
			tasks.add(new SendRequestTask(getApp(), r, remoteFile));
		}
		ThreadPoolTaskExecutor<SendRequestTask> executor =
				new ThreadPoolTaskExecutor<>(THREAD_POOL_SIZE, new OnThreadPoolTaskExecutorListener<SendRequestTask>() {

					@Override
					public void onTaskStarted(@NonNull SendRequestTask task) {
					}

					@Override
					public void onTaskFinished(@NonNull SendRequestTask task) {
						publishProgress(task);
					}

					@Override
					public void onTasksFinished(@NonNull List<SendRequestTask> tasks) {
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
			if (obj instanceof SendRequestTask) {
				RemoteFile remoteFile = ((SendRequestTask) obj).remoteFile;
				listener.onFileDeleteProgress(remoteFile);
			}
		}
	}

	@Override
	protected void onPostExecute(Object o) {
		List<OnDeleteFilesListener> listeners = getListeners();
		if (!listeners.isEmpty()) {
			Map<RemoteFile, String> errors = new HashMap<>();
			for (SendRequestTask task : tasks) {
				boolean success;
				String message = null;
				RequestResponse response = task.response;
				if (response != null) {
					String errorStr = response.getError();
					if (!Algorithms.isEmpty(errorStr)) {
						message = new ServerError(errorStr).toString();
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

	private static class SendRequestTask extends ThreadPoolTaskExecutor.Task {

		private final OsmandApplication app;
		private final AndroidNetworkUtils.Request request;
		private final RemoteFile remoteFile;
		private RequestResponse response;

		public SendRequestTask(@NonNull OsmandApplication app, @NonNull AndroidNetworkUtils.Request request,
							   @NonNull RemoteFile remoteFile) {
			this.app = app;
			this.request = request;
			this.remoteFile = remoteFile;
		}

		@Override
		public Void call() throws Exception {
			OperationLog operationLog = new OperationLog("deleteFile", BackupHelper.DEBUG);
			AndroidNetworkUtils.sendRequest(app, request, (result, error) ->
					response = new RequestResponse(request, result, error));
			operationLog.finishOperation(remoteFile.getName());
			return null;
		}
	}
}
