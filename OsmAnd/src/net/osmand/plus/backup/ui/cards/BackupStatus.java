package net.osmand.plus.backup.ui.cards;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

enum BackupStatus {
	BACKUP_COMPLETE(R.string.backup_complete, R.drawable.ic_action_cloud_done, -1, -1, -1, R.string.backup_now),
	MAKE_BACKUP(R.string.last_backup, R.drawable.ic_action_cloud, R.drawable.ic_action_alert_circle, R.string.make_backup, R.string.make_backup_descr, R.string.backup_now),
	CONFLICTS(R.string.last_backup, R.drawable.ic_action_cloud_alert, R.drawable.ic_action_alert, R.string.backup_conflicts, R.string.backup_confilcts_descr, R.string.backup_view_conflicts),
	NO_INTERNET_CONNECTION(R.string.last_backup, R.drawable.ic_action_cloud_alert, R.drawable.ic_action_wifi_off, R.string.no_inet_connection, R.string.backup_no_internet_descr, R.string.retry),
	ERROR(R.string.last_backup, R.drawable.ic_action_cloud_alert, R.drawable.ic_action_alert, -1, -1, R.string.retry);

	@StringRes
	public int statusTitleRes;
	@DrawableRes
	public int statusIconRes;
	@DrawableRes
	public int warningIconRes;
	@StringRes
	public int warningTitleRes;
	@StringRes
	public int warningDescriptionRes;
	@StringRes
	public int actionTitleRes;

	BackupStatus(int statusTitleRes, int statusIconRes, int warningIconRes, int warningTitleRes,
				 int warningDescriptionRes, int actionTitleRes) {
		this.statusTitleRes = statusTitleRes;
		this.statusIconRes = statusIconRes;
		this.warningIconRes = warningIconRes;
		this.warningTitleRes = warningTitleRes;
		this.warningDescriptionRes = warningDescriptionRes;
		this.actionTitleRes = actionTitleRes;
	}
}
