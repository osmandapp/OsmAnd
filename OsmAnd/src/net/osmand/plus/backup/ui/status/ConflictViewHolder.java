package net.osmand.plus.backup.ui.status;

import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.ExportBackupTask;
import net.osmand.plus.backup.ImportBackupTask;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import org.apache.commons.logging.Log;

import java.util.Collections;

public class ConflictViewHolder extends ItemViewHolder {

	private static final Log log = PlatformUtil.getLog(ConflictViewHolder.class);

	private final View serverButton;
	private final View localVersionButton;

	public ConflictViewHolder(View itemView) {
		super(itemView);
		serverButton = itemView.findViewById(R.id.server_button);
		localVersionButton = itemView.findViewById(R.id.local_version_button);
	}

	public void bindView(@NonNull Pair<LocalFile, RemoteFile> pair,
						 @Nullable BackupExportListener exportListener,
						 @Nullable ImportListener importListener, boolean nightMode) {
		OsmandApplication app = getApplication();
		NetworkSettingsHelper settingsHelper = app.getNetworkSettingsHelper();
		SettingsItem item = pair.first.item;

		String fileName = BackupHelper.getItemFileName(item);
		setupItemView(fileName, item, false);
		updateButtonsState(settingsHelper, fileName);

		localVersionButton.setOnClickListener(v -> {
			try {
				settingsHelper.exportSettings(fileName, exportListener, item);
			} catch (IllegalArgumentException e) {
				log.error(e.getMessage(), e);
			}
			updateButtonsState(settingsHelper, fileName);
		});
		serverButton.setOnClickListener(v -> {
			try {
				SettingsItem settingsItem = pair.second.item;
				settingsItem.setShouldReplace(true);
				settingsHelper.importSettings(fileName, Collections.singletonList(settingsItem), true, importListener);
			} catch (IllegalArgumentException e) {
				log.error(e.getMessage(), e);
			}
			updateButtonsState(settingsHelper, fileName);
		});
		AndroidUiHelper.updateVisibility(serverButton, true);
		AndroidUiHelper.updateVisibility(localVersionButton, true);
		UiUtilities.setupDialogButton(nightMode, localVersionButton, DialogButtonType.SECONDARY, R.string.upload_local_version);
		UiUtilities.setupDialogButton(nightMode, serverButton, DialogButtonType.SECONDARY, R.string.download_server_version);
		AndroidUtils.setBackground(app, localVersionButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
		AndroidUtils.setBackground(app, serverButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
	}

	private void updateButtonsState(@NonNull NetworkSettingsHelper helper, @NonNull String fileName) {
		ImportBackupTask importTask = helper.getImportTask(fileName);
		ExportBackupTask exportTask = helper.getExportTask(fileName);
		boolean enabled = exportTask == null && importTask == null;

		serverButton.setEnabled(enabled);
		localVersionButton.setEnabled(enabled);
	}
}
