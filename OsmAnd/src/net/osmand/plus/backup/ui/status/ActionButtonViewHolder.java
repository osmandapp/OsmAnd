package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.backup.NetworkSettingsHelper.BACKUP_ITEMS_KEY;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.List;

public class ActionButtonViewHolder extends RecyclerView.ViewHolder {

	private static final Log log = PlatformUtil.getLog(ActionButtonViewHolder.class);

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

		NetworkSettingsHelper settingsHelper = app.getNetworkSettingsHelper();
		if (settingsHelper.isBackupExporting()) {
			actionButton.setOnClickListener(v -> {
				settingsHelper.cancelImport();
				settingsHelper.cancelExport();
			});
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		} else if (status == BackupStatus.MAKE_BACKUP || status == BackupStatus.CONFLICTS) {
			actionButton.setOnClickListener(v -> {
				try {
					BackupInfo info = backup.getBackupInfo();
					List<SettingsItem> items = info.itemsToUpload;
					if (!items.isEmpty() || !Algorithms.isEmpty(info.filteredFilesToDelete)) {
						settingsHelper.exportSettings(BACKUP_ITEMS_KEY, items, info.itemsToDelete, exportListener);
					}
				} catch (IllegalArgumentException e) {
					log.error(e.getMessage(), e);
				}
			});
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.backup_now);
		} else if (status == BackupStatus.NO_INTERNET_CONNECTION || status == BackupStatus.ERROR) {
			actionButton.setOnClickListener(v -> app.getBackupHelper().prepareBackup());
			UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.SECONDARY, R.string.retry);
		} else if (status == BackupStatus.SUBSCRIPTION_EXPIRED) {
			actionButton.setOnClickListener(v -> {
				if (Version.isInAppPurchaseSupported()) {
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