package net.osmand.plus.backup.commands;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.OperationLog;
import net.osmand.plus.backup.BackupCommand;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnRegisterUserListener;
import net.osmand.plus.backup.BackupError;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.backup.BackupHelper.STATUS_EMPTY_RESPONSE_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_PARSE_JSON_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SUCCESS;
import static net.osmand.plus.backup.BackupHelper.USER_REGISTER_URL;

public class RegisterUserCommand extends BackupCommand {

	private final boolean login;
	private final String email;
	private final String promoCode;

	public RegisterUserCommand(@NonNull BackupHelper helper, boolean login,
							   @NonNull String email, @Nullable String promoCode) {
		super(helper);
		this.login = login;
		this.email = email;
		this.promoCode = promoCode;
	}

	public List<OnRegisterUserListener> getListeners() {
		return getHelper().getBackupListeners().getRegisterUserListeners();
	}

	@Override
	protected Object doInBackground(Object... objects) {
		Map<String, String> params = new HashMap<>();
		params.put("email", email);
		params.put("login", String.valueOf(login));
		params.put("lang", getApp().getLocaleHelper().getLanguage());
		String orderId = Algorithms.isEmpty(promoCode) ? getHelper().getOrderId() : promoCode;
		if (!Algorithms.isEmpty(orderId)) {
			params.put("orderid", orderId);
		}
		String deviceId = getApp().getUserAndroidId();
		params.put("deviceid", deviceId);
		OperationLog operationLog = createOperationLog("registerUser");
		AndroidNetworkUtils.sendRequest(getApp(), USER_REGISTER_URL, params, "Register user", false, true, (resultJson, error, resultCode) -> {
			int status;
			String message;
			BackupError backupError = null;
			if (!Algorithms.isEmpty(error)) {
				backupError = new BackupError(error);
				message = "User registration error: " + backupError + "\nEmail=" + email + "\nOrderId=" + orderId + "\nDeviceId=" + deviceId;
				status = STATUS_SERVER_ERROR;
			} else if (!Algorithms.isEmpty(resultJson)) {
				try {
					JSONObject result = new JSONObject(resultJson);
					if (result.has("status") && "ok".equals(result.getString("status"))) {
						message = "You have been registered successfully. Please check for email with activation code.";
						status = STATUS_SUCCESS;
					} else {
						message = "User registration error: unknown";
						status = STATUS_SERVER_ERROR;
					}
				} catch (JSONException e) {
					message = "User registration error: json parsing";
					status = STATUS_PARSE_JSON_ERROR;
				}
			} else {
				message = "User registration error: empty response";
				status = STATUS_EMPTY_RESPONSE_ERROR;
			}
			publishProgress(status, message, backupError);
			operationLog.finishOperation(status + " " + message);
		});
		return null;
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		for (OnRegisterUserListener listener : getListeners()) {
			int status = (Integer) values[0];
			String message = (String) values[1];
			BackupError backupError = (BackupError) values[2];
			listener.onRegisterUser(status, message, backupError);
		}
	}
}
