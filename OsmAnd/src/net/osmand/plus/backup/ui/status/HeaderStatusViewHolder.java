package net.osmand.plus.backup.ui.status;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.backup.ExportBackupTask;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.fragments.MainSettingsFragment;
import net.osmand.util.Algorithms;

import static net.osmand.plus.activities.OsmandBaseExpandableListAdapter.adjustIndicator;

public class HeaderStatusViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final ProgressBar progressBar;
	private final ImageView icon;
	private final TextView description;

	public HeaderStatusViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		description = itemView.findViewById(R.id.description);
		progressBar = itemView.findViewById(R.id.progress_bar);
	}

	public void bindView(@NonNull BackupStatusAdapter adapter, @NonNull BackupStatus status, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) itemView.getContext().getApplicationContext();
		NetworkSettingsHelper settingsHelper = app.getNetworkSettingsHelper();

		ExportBackupTask exportTask = settingsHelper.getExportTask();
		if (exportTask != null) {
			title.setText(R.string.uploading);
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_cloud_upload));

			int progress = exportTask.getGeneralProgress();
			int maxProgress = (int) exportTask.getMaxProgress();
			int percentage = maxProgress != 0 ? BasicProgressAsyncTask.normalizeProgress(progress * 100 / maxProgress) : 0;

			String uploading = app.getString(R.string.local_openstreetmap_uploading);
			title.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, uploading, percentage + "%"));

			progressBar.setMax(maxProgress);
			progressBar.setProgress(progress);

			AndroidUiHelper.updateVisibility(description, false);
			AndroidUiHelper.updateVisibility(progressBar, true);
		} else {
			description.setText(status.statusTitleRes);
			icon.setImageDrawable(getContentIcon(status.statusIconRes));

			String backupTime = MainSettingsFragment.getLastBackupTimeDescription(app);
			if (Algorithms.isEmpty(backupTime)) {
				title.setText(R.string.shared_string_never);
			} else {
				title.setText(backupTime);
			}
			AndroidUiHelper.updateVisibility(description, true);
			AndroidUiHelper.updateVisibility(progressBar, false);
		}
		itemView.setOnClickListener(v -> {
			adapter.toggleUploadItemsVisibility();
			adjustIndicator(app, itemView, adapter.isUploadItemsVisible(), nightMode);
		});
		adjustIndicator(app, itemView, adapter.isUploadItemsVisible(), nightMode);
		setupSelectableBackground(itemView.findViewById(R.id.header_container));
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.explicit_indicator), status != BackupStatus.BACKUP_COMPLETE);
	}

	private void setupSelectableBackground(@NonNull View view) {
		int color = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.active_color_basic);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(view.getContext(), color, 0.3f);
		AndroidUtils.setBackground(view, drawable);
	}

	private Drawable getContentIcon(@DrawableRes int icon) {
		OsmandApplication app = (OsmandApplication) itemView.getContext().getApplicationContext();
		return app.getUIUtilities().getIcon(icon, R.color.description_font_and_bottom_sheet_icons);
	}
}
