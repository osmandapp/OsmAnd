package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.backup.BackupHelper.OnUploadFileListener;
import net.osmand.plus.settings.backend.backup.Exporter;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ExportProgressListener;
import net.osmand.util.Algorithms;

import java.io.IOException;

public class BackupExporter extends Exporter {

	private final BackupHelper backupHelper;

	BackupExporter(@NonNull BackupHelper backupHelper, @Nullable ExportProgressListener progressListener) {
		super(progressListener);
		this.backupHelper = backupHelper;
	}

	@Override
	public void export() throws IOException {
		writeItems();
	}

	private void writeItems() throws IOException {
		OnUploadFileListener uploadFileListener = new OnUploadFileListener() {

			@Override
			public void onFileUploadProgress(@NonNull String fileName, int progress) {
				ExportProgressListener progressListener = getProgressListener();
				if (progressListener != null) {
					progressListener.updateProgress(progress);
				}
			}

			@Override
			public void onFileUploadDone(@NonNull String fileName, long uploadTime, @Nullable String error) {
				if (!Algorithms.isEmpty(error)) {
					setCancelled(true);
				} else {
					backupHelper.updateFileUploadTime(fileName, uploadTime);
				}
			}
		};
		NetworkWriter networkWriter = new NetworkWriter(backupHelper, uploadFileListener);
		writeItems(networkWriter);
		if (!isCancelled()) {
			backupHelper.updateBackupUploadTime();
		}
	}
}
