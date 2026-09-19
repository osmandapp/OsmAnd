package net.osmand.plus.backup.commands;

import static net.osmand.plus.backup.BackupHelper.CHECK_CODE_URL;
import static net.osmand.plus.backup.BackupHelper.STATUS_EMPTY_RESPONSE_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SUCCESS;

import androidx.annotation.NonNull;

import net.osmand.OperationLog;
import net.osmand.plus.backup.BackupCommand;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnCheckCodeListener;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.List;

public class CheckCodeCommand extends BackupCommand {

	private final String email;
	private final String token;

	public CheckCodeCommand(@NonNull BackupHelper helper, @NonNull String email, @NonNull String token) {
		super(helper);
		this.email = email;
		this.token = token;
	}

	@NonNull
	public List<OnCheckCodeListener> getListeners() {
		return getHelper().getBackupListeners().getCheckCodeListeners();
	}


	@Override
	protected Object doInBackground(Object... objects) {
		String body = getBody();
		OnRequestResultListener listener = getRequestListener();
		AndroidNetworkUtils.sendRequest(getApp(), CHECK_CODE_URL, body, "Check code",
				"application/json", true, true, listener);
		return null;
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
		OperationLog operationLog = createOperationLog("checkCode");

		return (result, error, resultCode) -> {
			int status;
			String message = null;
			BackupError backupError = null;
			if (!Algorithms.isEmpty(error)) {
				backupError = new BackupError(error);
				message = "Check code error: " + backupError + "\nEmail=" + email + "\nDeviceId=" + getHelper().getDeviceId();
				status = STATUS_SERVER_ERROR;
			} else if (resultCode != null && resultCode == HttpURLConnection.HTTP_OK) {
				message = result;
				status = STATUS_SUCCESS;
			} else {
				message = "Check code error: empty response";
				status = STATUS_EMPTY_RESPONSE_ERROR;
			}
			publishProgress(status, message, backupError);
			operationLog.finishOperation(status + " " + message);
		};
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		for (OnCheckCodeListener listener : getListeners()) {
			int status = (Integer) values[0];
			String message = (String) values[1];
			BackupError backupError = (BackupError) values[2];
			listener.onCheckCode(token, status, message, backupError);
		}
	}
}