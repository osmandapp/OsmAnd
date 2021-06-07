package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.backup.BackupHelper.OnUploadFileListener;
import net.osmand.plus.settings.backend.backup.Exporter;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BackupExporter extends Exporter {

	private final BackupHelper backupHelper;
	private final NetworkExportProgressListener listener;

	public interface NetworkExportProgressListener {
		void updateItemProgress(@NonNull String type, @NonNull String fileName, int value);

		void updateGeneralProgress(int uploadedItems, int uploadedKb);

		void networkExportDone(@NonNull Map<String, String> errors);
	}

	BackupExporter(@NonNull BackupHelper backupHelper, @Nullable NetworkExportProgressListener listener) {
		super(null);
		this.backupHelper = backupHelper;
		this.listener = listener;
	}

	@Override
	public void export() throws IOException {
		writeItems();
	}

	private void writeItems() throws IOException {
		Map<String, String> errors = new HashMap<>();
		OnUploadFileListener uploadFileListener = new OnUploadFileListener() {
			final int[] itemsProgress = {0};
			final int[] dataProgress = {0};

			@Override
			public void onFileUploadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork) {
				dataProgress[0] += deltaWork;
				if (listener != null) {
					listener.updateItemProgress(type, fileName, progress);
					listener.updateGeneralProgress(itemsProgress[0], dataProgress[0]);
				}
			}

			@Override
			public void onFileUploadDone(@NonNull String type, @NonNull String fileName, long uploadTime, @Nullable String error) {
				if (!Algorithms.isEmpty(error)) {
					errors.put(type + "/" + fileName, error);
				}
				itemsProgress[0] += 1;
				if (listener != null) {
					listener.updateGeneralProgress(itemsProgress[0], dataProgress[0]);
				}
			}
		};
		NetworkWriter networkWriter = new NetworkWriter(backupHelper, uploadFileListener);
		writeItems(networkWriter);
		if (!isCancelled()) {
			backupHelper.updateBackupUploadTime();
		}
		if (listener != null) {
			listener.networkExportDone(errors);
		}
	}
}
