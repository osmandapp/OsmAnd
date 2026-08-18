package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum LoginDialogType {

	SIGN_IN(R.id.sign_in_container, R.string.user_login, R.string.osmand_cloud_login_descr,
			R.string.cloud_email_not_registered, SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED, SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED),
	SIGN_UP(R.id.sign_up_container, R.string.register_opr_create_new_account, R.string.osmand_cloud_create_account_descr,
			R.string.cloud_email_already_registered, SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED, SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED),
	VERIFY_EMAIL(R.id.verify_email_container, R.string.verify_email_address, R.string.verify_email_address_descr,
			-1, -1, -1),

	DELETE_ACCOUNT(R.id.delete_account_container, R.string.verify_account, R.string.verify_account_deletion_descr,
			-1, -1, SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED);

	@IdRes
	final int viewId;
	@StringRes
	final int titleId;
	@StringRes
	final int warningId;
	@StringRes
	final int descriptionId;
	final int warningErrorCode;
	final int permittedErrorCode;

	LoginDialogType(int viewId, int titleId, int descriptionId, int warningId, int warningErrorCode, int permittedErrorCode) {
		this.viewId = viewId;
		this.titleId = titleId;
		this.warningId = warningId;
		this.descriptionId = descriptionId;
		this.warningErrorCode = warningErrorCode;
		this.permittedErrorCode = permittedErrorCode;
	}
}
