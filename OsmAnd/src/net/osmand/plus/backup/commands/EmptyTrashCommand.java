package net.osmand.plus.backup.commands;

import static net.osmand.plus.backup.BackupHelper.EMPTY_TRASH_URL;
import static net.osmand.plus.backup.BackupHelper.STATUS_PARSE_JSON_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_ERROR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OperationLog;
import net.osmand.plus.backup.BackupCommand;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EmptyTrashCommand extends BackupCommand {

	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final int CHUNK_SIZE = 50;

	private static final String RESULT_DELETED = "deleted";
	private static final String RESULT_ALREADY_MISSING = "already_missing";
	private static final String RESULT_SKIPPED_NOT_TRASH = "skipped_not_trash";
	private static final String RESULT_FAILED = "failed";

	private final List<RemoteFile> deletedFiles;
	private final OnDeleteFilesListener listener;
	private final Map<RemoteFile, String> errors = new LinkedHashMap<>();

	private int errorStatus = STATUS_SERVER_ERROR;
	private String errorMessage;
	private int processedFiles;

	public EmptyTrashCommand(@NonNull BackupHelper helper, @NonNull List<RemoteFile> deletedFiles,
	                         @Nullable OnDeleteFilesListener listener) {
		super(helper);
		this.deletedFiles = new ArrayList<>(deletedFiles);
		this.listener = listener;
	}

	@NonNull
	private List<OnDeleteFilesListener> getListeners() {
		return getHelper().getBackupListeners().getDeleteFilesListeners();
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			getHelper().getBackupListeners().addDeleteFilesListener(listener);
		}
		for (OnDeleteFilesListener deleteListener : getListeners()) {
			deleteListener.onFilesDeleteStarted(deletedFiles);
		}
	}

	@Override
	protected Object doInBackground(Object... objects) {
		OperationLog operationLog = createOperationLog("emptyTrash");
		operationLog.startOperation();

		Map<String, String> parameters = new HashMap<>();
		parameters.put("deviceid", getHelper().getDeviceId());
		parameters.put("accessToken", getHelper().getAccessToken());
		String query = AndroidNetworkUtils.getParameters(getApp(), parameters, null, null, false);
		if (Algorithms.isEmpty(query)) {
			errorStatus = STATUS_PARSE_JSON_ERROR;
			errorMessage = "Failed to encode empty trash request parameters";
		} else {
			String url = EMPTY_TRASH_URL + "?" + query;
			for (int start = 0; start < deletedFiles.size(); start += CHUNK_SIZE) {
				int end = Math.min(start + CHUNK_SIZE, deletedFiles.size());
				List<RemoteFile> chunk = deletedFiles.subList(start, end);
				try {
					JSONArray filesJson = createFilesJson(chunk);
					String[] requestError = new String[1];
					AndroidNetworkUtils.sendRequest(getApp(), url, filesJson.toString(), null, CONTENT_TYPE_JSON,
							false, true, (result, error, resultCode) ->
									requestError[0] = processResponse(chunk, result, error));
					if (!Algorithms.isEmpty(requestError[0])) {
						addChunkError(deletedFiles.subList(end, deletedFiles.size()), requestError[0]);
						for (RemoteFile file : chunk) {
							publishProgress(file);
						}
						break;
					}
				} catch (JSONException e) {
					addChunkError(chunk, "Failed to create empty Trash request");
				}
				for (RemoteFile file : chunk) {
					publishProgress(file);
				}
			}
		}
		operationLog.finishOperation("Files: " + deletedFiles.size());
		return null;
	}

	@NonNull
	private JSONArray createFilesJson(@NonNull List<RemoteFile> files) throws JSONException {
		JSONArray filesJson = new JSONArray();
		for (RemoteFile file : files) {
			JSONObject fileJson = new JSONObject();
			fileJson.put("name", file.getName());
			fileJson.put("type", file.getType());
			fileJson.put("updatetime", file.getUpdatetimems());
			filesJson.put(fileJson);
		}
		return filesJson;
	}

	@Nullable
	private String processResponse(@NonNull List<RemoteFile> chunk, @Nullable String result,
	                               @Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			String message = new BackupError(error).getLocalizedError(getApp());
			addChunkError(chunk, message);
			return message;
		} else if (Algorithms.isEmpty(result)) {
			String message = "Empty Trash error: empty response";
			addChunkError(chunk, message);
			return message;
		} else {
			try {
				JSONObject json = new JSONObject(result);
				if (!"ok".equals(json.optString("status"))) {
					String message = json.optString("message");
					if (Algorithms.isEmpty(message)) {
						message = "Empty Trash error: unknown response";
					}
					addChunkError(chunk, message);
					return message;
				} else if (json.has("results")) {
					JSONArray results = json.optJSONArray("results");
					if (results == null) {
						String message = "Empty Trash error: invalid results";
						addChunkError(chunk, message);
						return message;
					}
					processResults(chunk, results);
				}
			} catch (JSONException e) {
				String message = "Empty Trash error: JSON parsing failed";
				addChunkError(chunk, message);
				return message;
			}
		}
		return null;
	}

	private void processResults(@NonNull List<RemoteFile> chunk, @NonNull JSONArray results) {
		Set<Integer> processedIndexes = new HashSet<>();
		for (int i = 0; i < results.length(); i++) {
			JSONObject result = results.optJSONObject(i);
			int fileIndex = findFileIndex(chunk, result, processedIndexes);
			if (fileIndex < 0) {
				continue;
			}
			processedIndexes.add(fileIndex);
			RemoteFile file = chunk.get(fileIndex);
			String status = result.optString("status");
			if (RESULT_DELETED.equals(status) || RESULT_ALREADY_MISSING.equals(status)) {
				continue;
			}
			String message = result.optString("message");
			if (Algorithms.isEmpty(message)) {
				if (RESULT_SKIPPED_NOT_TRASH.equals(status)) {
					message = "File is no longer in Trash";
				} else if (RESULT_FAILED.equals(status)) {
					message = "Failed to delete file from Trash";
				} else {
					message = "Unknown empty Trash result: " + status;
				}
			}
			errors.put(file, formatFileError(file, message));
		}
		for (int i = 0; i < chunk.size(); i++) {
			if (!processedIndexes.contains(i)) {
				RemoteFile file = chunk.get(i);
				errors.put(file, formatFileError(file, "Empty Trash result is missing"));
			}
		}
	}

	private int findFileIndex(@NonNull List<RemoteFile> chunk, @Nullable JSONObject result,
	                          @NonNull Set<Integer> processedIndexes) {
		if (result == null || !result.has("name") || !result.has("type") || !result.has("updatetime")) {
			return -1;
		}
		String name = result.optString("name", null);
		String type = result.optString("type", null);
		long updateTime = result.optLong("updatetime", Long.MIN_VALUE);
		for (int i = 0; i < chunk.size(); i++) {
			RemoteFile file = chunk.get(i);
			if (!processedIndexes.contains(i)
					&& Algorithms.stringsEqual(file.getName(), name)
					&& Algorithms.stringsEqual(file.getType(), type)
					&& file.getUpdatetimems() == updateTime) {
				return i;
			}
		}
		return -1;
	}

	private void addChunkError(@NonNull List<RemoteFile> chunk, @NonNull String message) {
		for (RemoteFile file : chunk) {
			errors.put(file, formatFileError(file, message));
		}
	}

	@NonNull
	private String formatFileError(@NonNull RemoteFile file, @NonNull String message) {
		return file.getTypeNamePath() + ": " + message;
	}

	@Override
	protected void onProgressUpdate(Object... objects) {
		if (objects.length > 0 && objects[0] instanceof RemoteFile) {
			RemoteFile file = (RemoteFile) objects[0];
			processedFiles++;
			for (OnDeleteFilesListener deleteListener : getListeners()) {
				deleteListener.onFileDeleteProgress(file, processedFiles);
			}
		}
	}

	@Override
	protected void onPostExecute(Object o) {
		List<OnDeleteFilesListener> listeners = getListeners();
		if (errorMessage == null) {
			for (OnDeleteFilesListener deleteListener : listeners) {
				deleteListener.onFilesDeleteDone(Collections.unmodifiableMap(errors));
			}
		} else {
			for (OnDeleteFilesListener deleteListener : listeners) {
				deleteListener.onFilesDeleteError(errorStatus, errorMessage);
			}
		}
		if (listener != null) {
			getHelper().getBackupListeners().removeDeleteFilesListener(listener);
		}
	}
}
