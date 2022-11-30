package net.osmand.plus.backup.ui.status;

import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

public class SyncStatusViewHolder extends RecyclerView.ViewHolder {
	private final OsmandApplication app;

	public SyncStatusViewHolder(@NonNull View itemView) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
	}

	public void bindView() {
		setupSyncHeader();
	}

	private void setupSyncHeader() {
		TextViewEx title = itemView.findViewById(R.id.sync_title);
		TextViewEx lastSync = itemView.findViewById(R.id.last_sync);
		TextViewEx changes = itemView.findViewById(R.id.changes);
		AppCompatImageView headerIcon = itemView.findViewById(R.id.sync_header_icon);
		ProgressBar progressBar = itemView.findViewById(R.id.progress_bar);

		if (true) {
			String backupTime = "";
			if (Algorithms.isEmpty(backupTime)) {
				lastSync.setText(R.string.shared_string_never);
			} else {
				lastSync.setText(backupTime);
			}

			int changesCount = 0;
			String changesCountString = app.getString(R.string.changes, String.valueOf(changesCount));
			if (changesCount > 0) {
				changes.setText(changesCountString);
				headerIcon.setImageResource(R.drawable.ic_action_cloud_alert);
			} else {
				changes.setText(R.string.no_new_changes);
				headerIcon.setImageResource(R.drawable.ic_action_cloud_done);
			}

			AndroidUiHelper.updateVisibility(progressBar, false);
		} else {
			AndroidUiHelper.updateVisibility(progressBar, true);
		}
	}
}