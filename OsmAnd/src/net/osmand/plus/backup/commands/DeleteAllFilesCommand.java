package net.osmand.plus.backup.commands;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ServerError;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.backup.BackupHelper.LIST_FILES_URL;
import static net.osmand.plus.backup.BackupHelper.STATUS_EMPTY_RESPONSE_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_PARSE_JSON_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SUCCESS;

public class DeleteAllFilesCommand extends BaseDeleteFilesCommand {

	private final List<ExportSettingsType> types;

	public DeleteAllFilesCommand(@NonNull BackupHelper helper,
								 @NonNull List<ExportSettingsType> types) {
		super(helper, true);
		this.types = types;
	}

	public DeleteAllFilesCommand(@NonNull BackupHelper helper,
								 @NonNull List<ExportSettingsType> types,
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
		AndroidNetworkUtils.sendRequest(getApp(), LIST_FILES_URL, params, "Delete all files", false, false,
				getDeleteAllFilesListener());
		return null;
	}

	@Override
	protected void onProgressUpdate(Object... objects) {
		super.onProgressUpdate(objects);
		for (OnDeleteFilesListener listener : getListeners()) {
			Object obj = objects[0];
			if (obj instanceof Map) {
				listener.onFilesDeleteDone((Map) obj);
			} else if (obj instanceof Integer && objects.length == 2) {
				int status = (Integer) obj;
				String message = (String) objects[1];
				listener.onFilesDeleteError(status, message);
			}
		}
	}

	private OnRequestResultListener getDeleteAllFilesListener() {
		return (resultJson, error) -> {
			int status;
			String message;
			List<RemoteFile> remoteFiles = new ArrayList<>();
			if (!Algorithms.isEmpty(error)) {
				status = STATUS_SERVER_ERROR;
				message = "Download file list error: " + new ServerError(error);
			} else if (!Algorithms.isEmpty(resultJson)) {
				try {
					JSONObject result = new JSONObject(resultJson);
					JSONArray files = result.getJSONArray("allFiles");
					for (int i = 0; i < files.length(); i++) {
						remoteFiles.add(new RemoteFile(files.getJSONObject(i)));
					}
					status = STATUS_SUCCESS;
					message = "OK";
				} catch (JSONException | ParseException e) {
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
				for (RemoteFile file : remoteFiles) {
					ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForRemoteFile(file);
					if (types.contains(exportType)) {
						filesToDelete.add(file);
					}
				}
				if (!filesToDelete.isEmpty()) {
					deleteFiles(filesToDelete);
				} else {
					publishProgress(Collections.emptyMap());
				}
			}
		};
	}
}
