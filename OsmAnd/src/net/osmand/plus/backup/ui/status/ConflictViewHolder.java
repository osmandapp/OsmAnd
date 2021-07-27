package net.osmand.plus.backup.ui.status;

import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.Collections;

public class ConflictViewHolder extends ItemViewHolder {

	public ConflictViewHolder(View itemView) {
		super(itemView);
	}

	public void bindView(@NonNull Pair<LocalFile, RemoteFile> pair,
						 @Nullable BackupExportListener exportListener,
						 @Nullable ImportListener importListener, boolean nightMode) {
		OsmandApplication app = getApplication();
		SettingsItem item = pair.first.item;

		setupItemView(item, false);

		View localVersionButton = itemView.findViewById(R.id.local_version_button);
		localVersionButton.setOnClickListener(v -> app.getNetworkSettingsHelper().exportSettings(exportListener, item));
		View serverButton = itemView.findViewById(R.id.server_button);
		serverButton.setOnClickListener(v -> {
			SettingsItem settingsItem = pair.second.item;
			settingsItem.setShouldReplace(true);
			app.getNetworkSettingsHelper().importSettings(Collections.singletonList(settingsItem), "", 1, true, importListener);
		});
		AndroidUiHelper.updateVisibility(serverButton, true);
		AndroidUiHelper.updateVisibility(localVersionButton, true);
		UiUtilities.setupDialogButton(nightMode, localVersionButton, DialogButtonType.SECONDARY, R.string.upload_local_version);
		UiUtilities.setupDialogButton(nightMode, serverButton, DialogButtonType.SECONDARY, R.string.download_server_version);
		AndroidUtils.setBackground(app, localVersionButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
		AndroidUtils.setBackground(app, serverButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
	}
}
