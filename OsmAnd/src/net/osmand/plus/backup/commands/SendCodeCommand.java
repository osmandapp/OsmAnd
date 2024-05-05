package net.osmand.plus.backup.commands;

import static net.osmand.plus.backup.BackupHelper.SEND_CODE_URL;
import static net.osmand.plus.backup.BackupHelper.STATUS_EMPTY_RESPONSE_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_PARSE_JSON_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SUCCESS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OperationLog;
import net.osmand.plus.backup.BackupCommand;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnSendCodeListener;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendCodeCommand extends BackupCommand {

	private final String email;
	private final String action;

	public SendCodeCommand(@NonNull BackupHelper helper, @NonNull String email, @NonNull String action) {
		super(helper);
		this.email = email;
		this.action = action;
	}

	@NonNull
	public List<OnSendCodeListener> getListeners() {
		return getHelper().getBackupListeners().getSendCodeListeners();
	}


	@Override
	protected Object doInBackground(Object... objects) {
		String operation = "Send Code";
		OnRequestResultListener listener = getRequestListener();
		String params = getParams(operation, listener);
		if (!Algorithms.isEmpty(params)) {
			String body = getBody();
			String url = SEND_CODE_URL + "?" + params;
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
			json.putOpt("email", email);
			json.putOpt("action", action);
			json.putOpt("lang", getApp().getLocaleHelper().getLanguage());
		} catch (JSONException e) {

		}
		return json.toString();
	}

	@NonNull
	private OnRequestResultListener getRequestListener() {
		OperationLog operationLog = createOperationLog("sendCode");

		return (result, error, resultCode) -> {
			int status;
			String message = null;
			BackupError backupError = null;
			if (!Algorithms.isEmpty(error)) {
				backupError = new BackupError(error);
				message = "Send code error: " + backupError + "\nEmail=" + email + "\nDeviceId=" + getHelper().getDeviceId();
				status = STATUS_SERVER_ERROR;
			} else if (!Algorithms.isEmpty(result)) {
				try {
					JSONObject resultJson = new JSONObject(result);
					if (resultJson.has("status") && "ok".equals(resultJson.getString("status"))) {
						message = "Code have been sent successfully. Please check for email with activation code.";
						status = STATUS_SUCCESS;
					} else {
						message = "Send code error: unknown";
						status = STATUS_SERVER_ERROR;
					}
				} catch (JSONException e) {
					message = "Send code error: json parsing";
					status = STATUS_PARSE_JSON_ERROR;
				}
			} else {
				message = "Send code error: empty response";
				status = STATUS_EMPTY_RESPONSE_ERROR;
			}
			publishProgress(status, message, backupError);
			operationLog.finishOperation(status + " " + message);
		};
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		for (OnSendCodeListener listener : getListeners()) {
			int status = (Integer) values[0];
			String message = (String) values[1];
			BackupError backupError = (BackupError) values[2];
			listener.onSendCode(status, message, backupError);
		}
	}
}
