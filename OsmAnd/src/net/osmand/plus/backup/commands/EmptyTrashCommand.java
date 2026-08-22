package net.osmand.plus.backup.commands;

import static net.osmand.plus.backup.BackupHelper.EMPTY_TRASH_URL;
import static net.osmand.plus.backup.BackupHelper.STATUS_EMPTY_RESPONSE_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_PARSE_JSON_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_ERROR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OperationLog;
import net.osmand.plus.backup.BackupCommand;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmptyTrashCommand extends BackupCommand {

	private static final String CONTENT_TYPE_JSON = "application/json";

	private final List<RemoteFile> deletedFiles;
	private final OnDeleteFilesListener listener;

	private int errorStatus = STATUS_SERVER_ERROR;
	private String errorMessage;

	public EmptyTrashCommand(@NonNull BackupHelper helper, @NonNull List<RemoteFile> deletedFiles,
	                         @Nullable OnDeleteFilesListener listener) {
		super(helper);
		this.deletedFiles = deletedFiles;
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
		try {
			JSONArray filesJson = new JSONArray();
			for (RemoteFile file : deletedFiles) {
				JSONObject fileJson = new JSONObject();
				fileJson.put("name", file.getName());
				fileJson.put("type", file.getType());
				fileJson.put("updatetime", file.getUpdatetimems());
				filesJson.put(fileJson);
			}

			Map<String, String> parameters = new HashMap<>();
			parameters.put("deviceid", getHelper().getDeviceId());
			parameters.put("accessToken", getHelper().getAccessToken());
			String query = AndroidNetworkUtils.getParameters(getApp(), parameters, null, null, false);
			if (Algorithms.isEmpty(query)) {
				errorStatus = STATUS_PARSE_JSON_ERROR;
				errorMessage = "Failed to encode empty trash request parameters";
			} else {
				String url = EMPTY_TRASH_URL + "?" + query;
				AndroidNetworkUtils.sendRequest(getApp(), url, filesJson.toString(), null, CONTENT_TYPE_JSON,
						false, true, (result, error, resultCode) -> processResponse(result, error));
			}
		} catch (JSONException e) {
			errorStatus = STATUS_PARSE_JSON_ERROR;
			errorMessage = "Failed to create empty trash request";
		}
		operationLog.finishOperation("Files: " + deletedFiles.size());
		return null;
	}

	private void processResponse(@Nullable String result, @Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			errorStatus = STATUS_SERVER_ERROR;
			errorMessage = error;
		} else if (Algorithms.isEmpty(result)) {
			errorStatus = STATUS_EMPTY_RESPONSE_ERROR;
			errorMessage = "Empty trash error: empty response";
		} else {
			try {
				JSONObject json = new JSONObject(result);
				if (!"ok".equals(json.optString("status"))) {
					errorStatus = STATUS_SERVER_ERROR;
					errorMessage = "Empty trash error: unknown response";
				}
			} catch (JSONException e) {
				errorStatus = STATUS_PARSE_JSON_ERROR;
				errorMessage = "Empty trash error: JSON parsing failed";
			}
		}
	}

	@Override
	protected void onPostExecute(Object o) {
		List<OnDeleteFilesListener> listeners = getListeners();
		if (errorMessage == null) {
			for (OnDeleteFilesListener deleteListener : listeners) {
				deleteListener.onFilesDeleteDone(Collections.emptyMap());
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
