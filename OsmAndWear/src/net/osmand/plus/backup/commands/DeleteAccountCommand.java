package net.osmand.plus.backup.commands;

import static net.osmand.plus.backup.BackupHelper.ACCOUNT_DELETE_URL;
import static net.osmand.plus.backup.BackupHelper.STATUS_EMPTY_RESPONSE_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SUCCESS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OperationLog;
import net.osmand.plus.backup.BackupCommand;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteAccountListener;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteAccountCommand extends BackupCommand {

	private final String email;
	private final String token;

	public DeleteAccountCommand(@NonNull BackupHelper helper, @NonNull String email, @NonNull String token) {
		super(helper);
		this.email = email;
		this.token = token;
	}

	@NonNull
	public List<OnDeleteAccountListener> getListeners() {
		return getHelper().getBackupListeners().getDeleteAccountListeners();
	}


	@Override
	protected Object doInBackground(Object... objects) {
		String operation = "Account Delete";
		OnRequestResultListener listener = getRequestListener();
		String params = getParams(operation, listener);
		if (!Algorithms.isEmpty(params)) {
			String body = getBody();
			String url = ACCOUNT_DELETE_URL + "?" + params;
			AndroidNetworkUtils.sendRequest(getApp(), url, body, operation, "application/json", true, true, listener);
		}
		return null;
	}

	@Nullable
	private String getParams(String operation, OnRequestResultListener listener) {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("deviceid", getHelper().getDeviceId());
		parameters.put("accessToken", getHelper().getAccessToken());

		return AndroidNetworkUtils.getParameters(getApp(), parameters, listener, operation, true);
	}

	@NonNull
	private String getBody() {
		JSONObject json = new JSONObject();
		try {
			json.put("username", email);
			json.put("password", JSONObject.NULL);
			json.put("token", token);
		} catch (JSONException e) {

		}
		return json.toString();
	}

	@NonNull
	private OnRequestResultListener getRequestListener() {
		OperationLog operationLog = createOperationLog("accountDelete");

		return (result, error, resultCode) -> {
			int status;
			String message = null;
			BackupError backupError = null;
			if (!Algorithms.isEmpty(error)) {
				backupError = new BackupError(error);
				message = "Account deletion error: " + backupError + "\nEmail=" + email + "\nDeviceId=" + getHelper().getDeviceId();
				status = STATUS_SERVER_ERROR;
			} else if (resultCode != null && resultCode == HttpURLConnection.HTTP_OK) {
				message = result;
				status = STATUS_SUCCESS;
			} else {
				message = "Account deletion error: empty response";
				status = STATUS_EMPTY_RESPONSE_ERROR;
			}
			publishProgress(status, message, backupError);
			operationLog.finishOperation(status + " " + message);
		};
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		for (OnDeleteAccountListener listener : getListeners()) {
			int status = (Integer) values[0];
			String message = (String) values[1];
			BackupError backupError = (BackupError) values[2];
			listener.onDeleteAccount(status, message, backupError);
		}
	}
}