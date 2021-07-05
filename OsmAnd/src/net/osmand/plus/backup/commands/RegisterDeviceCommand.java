package net.osmand.plus.backup.commands;

import androidx.annotation.NonNull;

import net.osmand.AndroidNetworkUtils;
import net.osmand.OperationLog;
import net.osmand.plus.backup.BackupCommand;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnRegisterDeviceListener;
import net.osmand.plus.backup.ServerError;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.backup.BackupHelper.DEVICE_REGISTER_URL;
import static net.osmand.plus.backup.BackupHelper.STATUS_EMPTY_RESPONSE_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_PARSE_JSON_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_ERROR;
import static net.osmand.plus.backup.BackupHelper.STATUS_SUCCESS;

public class RegisterDeviceCommand extends BackupCommand {

	private final String token;

	public RegisterDeviceCommand(@NonNull BackupHelper helper, @NonNull String token) {
		super(helper);
		this.token = token;
	}

	public List<OnRegisterDeviceListener> getListeners() {
		return getHelper().getBackupListeners().getRegisterDeviceListeners();
	}

	@Override
	protected Object doInBackground(Object... objects) {
		Map<String, String> params = new HashMap<>();
		BackupHelper helper = getHelper();
		params.put("email", helper.getEmail());
		String orderId = helper.getOrderId();
		if (orderId != null) {
			params.put("orderid", orderId);
		}
		String androidId = helper.getAndroidId();
		if (!Algorithms.isEmpty(androidId)) {
			params.put("deviceid", androidId);
		}
		params.put("token", token);
		OperationLog operationLog = createOperationLog("registerDevice");
		AndroidNetworkUtils.sendRequest(getApp(), DEVICE_REGISTER_URL, params, "Register device", false, true, (resultJson, error) -> {
			int status;
			String message;
			ServerError serverError = null;
			if (!Algorithms.isEmpty(error)) {
				serverError = new ServerError(error);
				message = "Device registration error: " + serverError;
				status = STATUS_SERVER_ERROR;
			} else if (!Algorithms.isEmpty(resultJson)) {
				OsmandSettings settings = getApp().getSettings();
				try {
					JSONObject result = new JSONObject(resultJson);
					settings.BACKUP_DEVICE_ID.set(result.getString("id"));
					settings.BACKUP_USER_ID.set(result.getString("userid"));
					settings.BACKUP_NATIVE_DEVICE_ID.set(result.getString("deviceid"));
					settings.BACKUP_ACCESS_TOKEN.set(result.getString("accesstoken"));
					settings.BACKUP_ACCESS_TOKEN_UPDATE_TIME.set(result.getString("udpatetime"));

					message = "Device have been registered successfully";
					status = STATUS_SUCCESS;
				} catch (JSONException e) {
					message = "Device registration error: json parsing";
					status = STATUS_PARSE_JSON_ERROR;
				}
			} else {
				message = "Device registration error: empty response";
				status = STATUS_EMPTY_RESPONSE_ERROR;
			}
			publishProgress(status, message, serverError);
			operationLog.finishOperation(status + " " + message);
		});
		return null;
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		for (OnRegisterDeviceListener listener : getListeners()) {
			int status = (Integer) values[0];
			String message = (String) values[1];
			ServerError serverError = (ServerError) values[2];
			listener.onRegisterDevice(status, message, serverError);
		}
	}
}
