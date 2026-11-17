package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_EMAIL_IS_INVALID;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_FILE_NOT_AVAILABLE;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_GZIP_ONLY_SUPPORTED_UPLOAD;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_NO_VALID_SUBSCRIPTION;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_PROVIDED_TOKEN_IS_NOT_VALID;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_SIZE_OF_SUPPORTED_BOX_IS_EXCEEDED;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_SUBSCRIPTION_WAS_EXPIRED_OR_NOT_PRESENT;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_SUBSCRIPTION_WAS_USED_FOR_ANOTHER_ACCOUNT;
import static net.osmand.plus.backup.BackupHelper.STATUS_SERVER_TEMPORALLY_UNAVAILABLE_ERROR;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_USER_IS_ALREADY_REGISTERED;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED;
import static net.osmand.plus.backup.BackupHelper.STATUS_NO_ORDER_ID_ERROR;

public class BackupError {

	private final String error;
	private String message;
	private int code;

	public BackupError(@NonNull String error) {
		this.error = error;
		parseError(error);
	}

	@NonNull
	public String getError() {
		return error;
	}

	@Nullable
	public String getMessage() {
		return message;
	}

	public int getCode() {
		return code;
	}

	private void parseError(@NonNull String error) {
		if (!Algorithms.isEmpty(error)) {
			try {
				JSONObject resultError = new JSONObject(error);
				if (resultError.has("error")) {
					JSONObject errorObj = resultError.getJSONObject("error");
					code = errorObj.getInt("errorCode");
					message = errorObj.getString("message");
				}
			} catch (JSONException e) {
				if (error.contains(" ") && Algorithms.parseIntSilently(error.split(" ")[0], 0) > 0) {
					code = Algorithms.parseIntSilently(error.split(" ")[0], 0);
					message = error;
				}
			}
		}
	}

	@NonNull
	public String getLocalizedError(@NonNull OsmandApplication app) {
		switch (code) {
			case SERVER_ERROR_CODE_EMAIL_IS_INVALID:
				return app.getString(R.string.osm_live_enter_email);
			case SERVER_ERROR_CODE_NO_VALID_SUBSCRIPTION:
				return app.getString(R.string.backup_error_no_valid_subscription);
			case SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED:
				return app.getString(R.string.backup_error_user_is_not_registered);
			case SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED:
				return app.getString(R.string.backup_error_token_is_not_valid_or_expired);
			case SERVER_ERROR_CODE_PROVIDED_TOKEN_IS_NOT_VALID:
				return app.getString(R.string.backup_error_token_is_not_valid);
			case SERVER_ERROR_CODE_FILE_NOT_AVAILABLE:
				return app.getString(R.string.backup_error_file_not_available);
			case SERVER_ERROR_CODE_GZIP_ONLY_SUPPORTED_UPLOAD:
				return app.getString(R.string.backup_error_gzip_only_supported_upload);
			case SERVER_ERROR_CODE_SIZE_OF_SUPPORTED_BOX_IS_EXCEEDED:
				if (!Algorithms.isEmpty(message)) {
					String prefix = "Maximum size of OsmAnd Cloud exceeded ";
					int indexStart = message.indexOf(prefix);
					int indexEnd = message.indexOf(".");
					if (indexStart != -1 && indexEnd != -1) {
						String size = message.substring(indexStart + prefix.length(), indexEnd);
						return app.getString(R.string.backup_error_size_is_exceeded, size);
					}
				}
				break;
			case SERVER_ERROR_CODE_SUBSCRIPTION_WAS_USED_FOR_ANOTHER_ACCOUNT:
				if (!Algorithms.isEmpty(message)) {
					String prefix = "user was already signed up as ";
					int index = message.indexOf(prefix);
					if (index != -1) {
						String login = message.substring(index + prefix.length());
						return app.getString(R.string.backup_error_subscription_was_used, login);
					}
				}
				break;
			case SERVER_ERROR_CODE_SUBSCRIPTION_WAS_EXPIRED_OR_NOT_PRESENT:
				return app.getString(R.string.backup_error_subscription_was_expired);
			case SERVER_ERROR_CODE_USER_IS_ALREADY_REGISTERED:
				return app.getString(R.string.backup_error_user_is_already_registered);
			case STATUS_SERVER_TEMPORALLY_UNAVAILABLE_ERROR:
				return app.getString(R.string.service_is_not_available_please_try_later);
			case STATUS_NO_ORDER_ID_ERROR:
				return app.getString(R.string.backup_error_no_subscription);
		}
		return message != null ? message : error;
	}

	@NonNull
	@Override
	public String toString() {
		return code + " (" + message + ")";
	}
}