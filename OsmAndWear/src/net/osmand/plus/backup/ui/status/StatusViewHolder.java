package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;
import static net.osmand.plus.backup.ui.BackupUiUtils.getLastBackupTimeDescription;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.SyncBackupTask;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class StatusViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final BackupHelper backupHelper;

	private final TextView title;
	private final ImageView icon;
	private final TextView description;
	private final ProgressBar progressBar;
	private final LinearLayout divider;

	private final boolean nightMode;

	public StatusViewHolder(@NonNull View itemView, boolean nightMode) {
		super(itemView);
		this.app = (OsmandApplication) itemView.getContext().getApplicationContext();
		this.backupHelper = app.getBackupHelper();
		this.nightMode = nightMode;

		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		description = itemView.findViewById(R.id.description);
		progressBar = itemView.findViewById(R.id.progress_bar);
		divider = itemView.findViewById(R.id.bottom_divider);
	}

	public void bindView() {
		boolean preparing = backupHelper.isBackupPreparing();
		String checking = app.getString(R.string.checking_progress);
		String backupTime = getLastBackupTimeDescription(app, app.getString(R.string.shared_string_never));
		description.setText(preparing ? checking : backupTime);
		progressBar.setIndeterminate(preparing);

		UiUtilities uiUtilities = app.getUIUtilities();
		SyncBackupTask exportTask = app.getNetworkSettingsHelper().getSyncTask(SYNC_ITEMS_KEY);
		if (exportTask != null) {
			int color = ColorUtilities.getActiveColorId(nightMode);
			icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_update, color));

			int progress = exportTask.getGeneralProgress();
			int maxProgress = exportTask.getMaxProgress();
			int percentage = maxProgress != 0 ? ProgressHelper.normalizeProgressPercent(progress * 100 / maxProgress) : 0;

			title.setText(app.getString(R.string.cloud_sync_progress, percentage + "%"));

			progressBar.setMax(maxProgress);
			progressBar.setProgress(progress);
		} else {
			PrepareBackupResult backup = backupHelper.getBackup();
			BackupStatus status = BackupStatus.getBackupStatus(app, backup);
			title.setText(status.statusTitleRes);
			icon.setImageDrawable(uiUtilities.getIcon(status.statusIconRes));
		}

		AndroidUiHelper.updateVisibility(divider, false);
		AndroidUiHelper.updateVisibility(progressBar, exportTask != null || preparing);
		AndroidUiHelper.updateVisibility(description, exportTask == null || preparing);
	}
}