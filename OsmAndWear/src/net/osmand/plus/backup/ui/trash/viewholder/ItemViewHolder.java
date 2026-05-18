package net.osmand.plus.backup.ui.trash.viewholder;

import static net.osmand.plus.backup.NetworkSettingsHelper.BACKUP_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.RESTORE_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.ExportBackupTask;
import net.osmand.plus.backup.ExportBackupTask.ItemProgressInfo;
import net.osmand.plus.backup.ImportBackupTask;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.SyncBackupTask;
import net.osmand.plus.backup.ui.trash.CloudTrashController;
import net.osmand.plus.backup.ui.trash.TrashItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class ItemViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final CloudTrashController controller;
	private final NetworkSettingsHelper settingsHelper;

	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final ImageView cloudIcon;
	private final ImageView secondIcon;
	private final ProgressBar progressBar;
	private final View bottomShadow;
	private final View bottomDivider;

	public ItemViewHolder(@NonNull View itemView, @NonNull CloudTrashController controller, boolean nightMode) {
		super(itemView);
		this.controller = controller;
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();
		settingsHelper = app.getNetworkSettingsHelper();

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		icon = itemView.findViewById(R.id.icon);
		cloudIcon = itemView.findViewById(R.id.cloud_icon);
		secondIcon = itemView.findViewById(R.id.second_icon);
		progressBar = itemView.findViewById(R.id.progress_bar);
		bottomShadow = itemView.findViewById(R.id.bottom_shadow);
		bottomDivider = itemView.findViewById(R.id.bottom_divider);

		setupSelectableBackground(nightMode);
	}

	public void bindView(@NonNull TrashItem item, boolean lastItem, boolean hideDivider) {
		title.setText(item.getName(app));
		description.setText(item.getDescription(app));

		int iconId = item.getIconId();
		icon.setImageDrawable(iconId != -1 ? uiUtilities.getIcon(iconId) : null);

		setupProgress(item);
		itemView.setEnabled(!isSyncing(item));
		itemView.setOnClickListener(v -> controller.showItemMenu(item));
		secondIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_cloud_done));

		AndroidUiHelper.updateVisibility(secondIcon, item.synced);
		AndroidUiHelper.updateVisibility(cloudIcon, !item.isLocalDeletion());
		AndroidUiHelper.updateVisibility(bottomShadow, lastItem);
		AndroidUiHelper.updateVisibility(bottomDivider, !lastItem && !hideDivider);
	}

	private void setupProgress(@NonNull TrashItem item) {
		ItemProgressInfo progressInfo = getItemProgressInfo(item);
		if (progressInfo != null) {
			progressBar.setMax(progressInfo.getWork());
			progressBar.setProgress(progressInfo.getValue());
		}
		boolean syncing = isSyncing(item);
		AndroidUiHelper.updateVisibility(progressBar, !item.synced && syncing);
	}

	@Nullable
	private ItemProgressInfo getItemProgressInfo(@NonNull TrashItem item) {
		String key = item.oldFile.getName();
		ImportBackupTask importTask = settingsHelper.getImportTask(key);
		if (importTask == null) {
			importTask = settingsHelper.getImportTask(RESTORE_ITEMS_KEY);
		}
		if (importTask != null) {
			return importTask.getItemProgressInfo(item.oldFile.getType(), key);
		}
		ExportBackupTask exportTask = settingsHelper.getExportTask(key);
		if (exportTask == null) {
			exportTask = settingsHelper.getExportTask(BACKUP_ITEMS_KEY);
		}
		if (exportTask != null) {
			return exportTask.getItemProgressInfo(item.oldFile.getType(), key);
		}
		return null;
	}

	private boolean isSyncing(@NonNull TrashItem item) {
		SyncBackupTask syncTask = settingsHelper.getSyncTask(item.oldFile.getName());
		if (syncTask == null) {
			syncTask = settingsHelper.getSyncTask(SYNC_ITEMS_KEY);
		}
		return syncTask != null;
	}

	private void setupSelectableBackground(boolean nightMode) {
		int color = ColorUtilities.getActiveColor(app, nightMode);
		View view = itemView.findViewById(R.id.selectable_list_item);
		AndroidUtils.setBackground(view, UiUtilities.getColoredSelectableDrawable(app, color, 0.3f));
	}
}