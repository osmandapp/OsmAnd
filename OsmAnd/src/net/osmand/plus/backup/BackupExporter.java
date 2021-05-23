package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.backup.BackupHelper.OnUploadFileListener;
import net.osmand.plus.settings.backend.backup.Exporter;
import net.osmand.plus.settings.backend.backup.JsonSettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ExportProgressListener;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class BackupExporter extends Exporter {

	private final BackupHelper backupHelper;

	BackupExporter(@NonNull BackupHelper backupHelper, @Nullable ExportProgressListener progressListener) {
		super(progressListener);
		this.backupHelper = backupHelper;
	}

	@Override
	public void export() throws JSONException, IOException {
		writeItems();
	}

	private void writeItems() throws JSONException, IOException {
		List<SettingsItemWriter<? extends SettingsItem>> itemWriters = getItemWriters();
		final int itemWritersCount = itemWriters.size();
		OnUploadFileListener uploadFileListener = new OnUploadFileListener() {

			int processed = 0;

			@Override
			public void onFileUploadProgress(@NonNull String fileName, int progress) {
				if (getProgressListener() != null) {
					getProgressListener().updateProgress(progress);
				}
			}

			@Override
			public void onFileUploadDone(@NonNull String fileName, long uploadTime, @Nullable String error) {
				processed++;
				if (!Algorithms.isEmpty(error)) {
					setCancelled(true);
				} else {
					backupHelper.updateFileUploadTime(fileName, uploadTime);
				}
				if (processed == itemWritersCount && !isCancelled()) {
					backupHelper.updateBackupUploadTime();
				}
			}
		};
		NetworkWriter networkWriter = new NetworkWriter(backupHelper, uploadFileListener);
		JSONObject backupJson = createItemsJson();
		JsonSettingsItem jsonItem = new JsonSettingsItem(backupHelper.getApp(), "backup", backupJson);
		networkWriter.write(jsonItem.getWriter());
		writeItems(networkWriter, itemWriters);
	}
}
