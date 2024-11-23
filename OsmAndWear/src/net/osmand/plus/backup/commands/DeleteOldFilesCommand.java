package net.osmand.plus.backup.commands;

import static net.osmand.plus.backup.BackupHelper.LIST_FILES_URL;
import static net.osmand.plus.backup.BackupHelper.STATUS_EMPTY_RESPONSE_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_PARSE_JSON_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SUCCESS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteOldFilesCommand extends BaseDeleteFilesCommand {

	private final List<ExportType> types;

	public DeleteOldFilesCommand(@NonNull BackupHelper helper,
								 @Nullable List<ExportType> types) {
		super(helper, true);
		this.types = types;
	}

	public DeleteOldFilesCommand(@NonNull BackupHelper helper,
								 @Nullable List<ExportType> types,
								 @Nullable OnDeleteFilesListener listener) {
		super(helper, true, listener);
		this.types = types;
	}

	@Override
	protected Void doInBackground(Object... objects) {
		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getHelper().getDeviceId());
		params.put("accessToken", getHelper().getAccessToken());
		params.put("allVersions", "true");
		AndroidNetworkUtils.sendRequest(getApp(), LIST_FILES_URL, params, "Delete old files", false, false,
				getDeleteOldFilesListener());
		return null;
	}

	@Override
	protected void onProgressUpdate(Object... objects) {
		super.onProgressUpdate(objects);
		for (OnDeleteFilesListener listener : getListeners()) {
			Object obj = objects[0];
			if (obj instanceof List) {
				listener.onFilesDeleteStarted((List) obj);
			} else if (obj instanceof Integer && objects.length == 2) {
				int status = (Integer) obj;
				String message = (String) objects[1];
				listener.onFilesDeleteError(status, message);
			}
		}
	}

	private OnRequestResultListener getDeleteOldFilesListener() {
		return (resultJson, error, resultCode) -> {
			int status;
			String message;
			List<RemoteFile> remoteFiles = new ArrayList<>();
			if (!Algorithms.isEmpty(error)) {
				status = STATUS_SERVER_ERROR;
				message = "Download file list error: " + new BackupError(error);
			} else if (!Algorithms.isEmpty(resultJson)) {
				try {
					JSONObject result = new JSONObject(resultJson);
					JSONArray allFiles = result.getJSONArray("allFiles");
					for (int i = 0; i < allFiles.length(); i++) {
						remoteFiles.add(new RemoteFile(allFiles.getJSONObject(i)));
					}
					JSONArray uniqueFiles = result.getJSONArray("uniqueFiles");
					for (int i = 0; i < uniqueFiles.length(); i++) {
						remoteFiles.remove(new RemoteFile(uniqueFiles.getJSONObject(i)));
					}
					status = STATUS_SUCCESS;
					message = "OK";
				} catch (JSONException e) {
					status = STATUS_PARSE_JSON_ERROR;
					message = "Download file list error: json parsing";
				}
			} else {
				status = STATUS_EMPTY_RESPONSE_ERROR;
				message = "Download file list error: empty response";
			}
			if (status != STATUS_SUCCESS) {
				publishProgress(status, message);
			} else {
				List<RemoteFile> filesToDelete = new ArrayList<>();
				if (types != null) {
					for (RemoteFile file : remoteFiles) {
						ExportType exportType = ExportType.findBy(file);
						if (types.contains(exportType)) {
							filesToDelete.add(file);
						}
					}
				} else {
					filesToDelete.addAll(remoteFiles);
				}
				if (!filesToDelete.isEmpty()) {
					publishProgress(filesToDelete);
					deleteFiles(filesToDelete);
				}
			}
		};
	}
}
