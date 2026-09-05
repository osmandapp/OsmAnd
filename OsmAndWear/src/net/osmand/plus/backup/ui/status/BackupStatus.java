package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_NO_VALID_SUBSCRIPTION;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_SUBSCRIPTION_WAS_EXPIRED_OR_NOT_PRESENT;
import static net.osmand.plus.backup.BackupHelper.STATUS_NO_ORDER_ID_ERROR;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.util.Algorithms;

import java.net.HttpURLConnection;

public enum BackupStatus {
	BACKUP_COMPLETE(R.string.last_sync, R.drawable.ic_action_cloud_done_colored, -1, -1, -1, R.string.sync_now),
	MAKE_BACKUP(R.string.last_sync, R.drawable.ic_action_cloud_alert_colored, -1, -1, -1, R.string.sync_now),
	CONFLICTS(R.string.last_sync, R.drawable.ic_action_cloud_alert_colored, R.drawable.ic_action_alert, -1, -1, R.string.backup_view_conflicts),
	NO_INTERNET_CONNECTION(R.string.last_sync, R.drawable.ic_action_cloud_done_colored, R.drawable.ic_action_wifi_off, R.string.no_inet_connection, R.string.backup_no_internet_descr, R.string.retry),
	SUBSCRIPTION_EXPIRED(R.string.last_sync, R.drawable.ic_action_cloud_done_colored, R.drawable.ic_action_osmand_pro_logo_colored, R.string.backup_error_subscription_was_expired, R.string.backup_error_subscription_was_expired_descr, R.string.renew_subscription),
	ERROR(R.string.last_sync, R.drawable.ic_action_cloud_alert_colored, R.drawable.ic_action_alert, -1, -1, R.string.contact_support_retry);

	@StringRes
	public final int statusTitleRes;
	@DrawableRes
	public final int statusIconRes;
	@DrawableRes
	public final int warningIconRes;
	@StringRes
	public final int warningTitleRes;
	@StringRes
	public final int warningDescriptionRes;
	@StringRes
	public final int actionTitleRes;

	BackupStatus(int statusTitleRes, int statusIconRes, int warningIconRes, int warningTitleRes,
	             int warningDescriptionRes, int actionTitleRes) {
		this.statusTitleRes = statusTitleRes;
		this.statusIconRes = statusIconRes;
		this.warningIconRes = warningIconRes;
		this.warningTitleRes = warningTitleRes;
		this.warningDescriptionRes = warningDescriptionRes;
		this.actionTitleRes = actionTitleRes;
	}

	public static BackupStatus getBackupStatus(@NonNull OsmandApplication app, @NonNull PrepareBackupResult backup) {
		BackupInfo info = backup.getBackupInfo();

		if (!Algorithms.isEmpty(backup.getError())) {
			BackupError error = new BackupError(backup.getError());
			int errorCode = error.getCode();
			if (errorCode == SERVER_ERROR_CODE_SUBSCRIPTION_WAS_EXPIRED_OR_NOT_PRESENT
					|| errorCode == SERVER_ERROR_CODE_NO_VALID_SUBSCRIPTION
					|| errorCode == STATUS_NO_ORDER_ID_ERROR) {
				return SUBSCRIPTION_EXPIRED;
			} else if (errorCode == HttpURLConnection.HTTP_NOT_FOUND) {
				backup.setError(HttpURLConnection.HTTP_BAD_REQUEST + " " +
						app.getString(R.string.backup_error_failed_to_transfer_file));
				return ERROR;
			} else if (errorCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
				return ERROR;
			}
		}
		if (info != null) {
			if (!Algorithms.isEmpty(info.filteredFilesToMerge)) {
				return CONFLICTS;
			} else if (!Algorithms.isEmpty(info.itemsToUpload) || !Algorithms.isEmpty(info.itemsToDelete)
					|| !Algorithms.isEmpty(info.itemsToLocalDelete)) {
				return MAKE_BACKUP;
			}
		} else if (!app.getSettings().isInternetConnectionAvailable()) {
			return NO_INTERNET_CONNECTION;
		} else if (backup.getError() != null) {
			return ERROR;
		}
		return BACKUP_COMPLETE;
	}
}
