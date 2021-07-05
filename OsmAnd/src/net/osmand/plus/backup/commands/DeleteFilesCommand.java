package net.osmand.plus.backup.commands;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.settings.backend.ExportSettingsType;

import java.util.List;

public class DeleteFilesCommand extends BaseDeleteFilesCommand {

	private final List<RemoteFile> remoteFiles;

	public DeleteFilesCommand(@NonNull BackupHelper helper, @NonNull List<RemoteFile> remoteFiles, boolean byVersion) {
		super(helper, byVersion);
		this.remoteFiles = remoteFiles;
	}

	public DeleteFilesCommand(@NonNull BackupHelper helper, @NonNull List<RemoteFile> remoteFiles,
							  boolean byVersion, @Nullable OnDeleteFilesListener listener) {
		super(helper, byVersion, listener);
		this.remoteFiles = remoteFiles;
	}

	@Override
	protected Object doInBackground(Object... objects) {
		deleteFiles(remoteFiles);
		return null;
	}
}
