package net.osmand.plus.backup.ui.status;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.List;

public class ActionButtonViewHolder extends RecyclerView.ViewHolder {

	private final View divider;
	private final View actionButton;

	public ActionButtonViewHolder(@NonNull View itemView) {
		super(itemView);
		divider = itemView.findViewById(R.id.divider);
		actionButton = itemView.findViewById(R.id.action_button);
	}

	public void bindView(@NonNull MapActivity mapActivity, @NonNull PrepareBackupResult backup,
						 @Nullable BackupExportListener exportListener, boolean uploadItemsVisible, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) itemView.getContext().getApplicationContext();
		BackupStatus status = BackupStatus.getBackupStatus(app, backup);

		if (app.getNetworkSettingsHelper().isBackupExporting()) {
			actionButton.setOnClickListener(v -> app.getNetworkSettingsHelper().cancelExport());
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		} else if (status == BackupStatus.MAKE_BACKUP || status == BackupStatus.CONFLICTS) {
			actionButton.setOnClickListener(v -> {
				BackupInfo info = backup.getBackupInfo();
				List<SettingsItem> items = info.itemsToUpload;
				if (!items.isEmpty() || !Algorithms.isEmpty(info.filteredFilesToDelete)) {
					app.getNetworkSettingsHelper().exportSettings(items, info.itemsToDelete, exportListener);
				}
			});
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.backup_now);
		} else if (status == BackupStatus.NO_INTERNET_CONNECTION || status == BackupStatus.ERROR) {
			actionButton.setOnClickListener(v -> app.getBackupHelper().prepareBackup());
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.retry);
		} else if (status == BackupStatus.SUBSCRIPTION_EXPIRED) {
			actionButton.setOnClickListener(v -> {
				if (Version.isGooglePlayEnabled()) {
					OsmAndProPlanFragment.showInstance(mapActivity);
				} else {
					PromoCodeBottomSheet.showInstance(mapActivity.getSupportFragmentManager());
				}
			});
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.renew_subscription);
		}
		AndroidUiHelper.updateVisibility(divider, uploadItemsVisible);
		AndroidUtils.setBackground(app, actionButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
	}
}