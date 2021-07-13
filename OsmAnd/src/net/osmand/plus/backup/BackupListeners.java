package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BackupListeners {

	private final List<OnDeleteFilesListener> deleteFilesListeners = new ArrayList<>();
	private final List<OnRegisterUserListener> registerUserListeners = new ArrayList<>();
	private final List<OnRegisterDeviceListener> registerDeviceListeners = new ArrayList<>();

	public interface OnDeleteFilesListener {
		void onFilesDeleteStarted(@NonNull List<RemoteFile> files);

		void onFileDeleteProgress(@NonNull RemoteFile file, int progress);

		void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors);

		void onFilesDeleteError(int status, @NonNull String message);
	}

	public interface OnRegisterUserListener {
		void onRegisterUser(int status, @Nullable String message, @Nullable ServerError error);
	}

	public interface OnRegisterDeviceListener {
		void onRegisterDevice(int status, @Nullable String message, @Nullable ServerError error);
	}

	public interface OnUpdateOrderIdListener {
		void onUpdateOrderId(int status, @Nullable String message, @Nullable String error);
	}

	public interface OnDownloadFileListListener {
		void onDownloadFileList(int status, @Nullable String message, @NonNull List<RemoteFile> remoteFiles);
	}

	public interface OnCollectLocalFilesListener {
		void onFileCollected(@NonNull LocalFile localFile);

		void onFilesCollected(@NonNull List<LocalFile> localFiles);
	}

	public interface OnGenerateBackupInfoListener {
		void onBackupInfoGenerated(@Nullable BackupInfo backupInfo, @Nullable String error);
	}

	public interface OnUploadFileListener {
		void onFileUploadStarted(@NonNull String type, @NonNull String fileName, int work);

		void onFileUploadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork);

		void onFileUploadDone(@NonNull String type, @NonNull String fileName, long uploadTime, @Nullable String error);

		boolean isUploadCancelled();
	}

	public interface OnDownloadFileListener {
		void onFileDownloadStarted(@NonNull String type, @NonNull String fileName, int work);

		void onFileDownloadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork);

		void onFileDownloadDone(@NonNull String type, @NonNull String fileName, long uploadTime, @Nullable String error);

		boolean isDownloadCancelled();
	}

	public List<OnDeleteFilesListener> getDeleteFilesListeners() {
		return deleteFilesListeners;
	}

	public void addDeleteFilesListener(@NonNull OnDeleteFilesListener listener) {
		deleteFilesListeners.add(listener);
	}

	public void removeDeleteFilesListener(@NonNull OnDeleteFilesListener listener) {
		deleteFilesListeners.remove(listener);
	}

	public List<OnRegisterUserListener> getRegisterUserListeners() {
		return registerUserListeners;
	}

	public void addRegisterUserListener(@NonNull OnRegisterUserListener listener) {
		registerUserListeners.add(listener);
	}

	public void removeRegisterUserListener(@NonNull OnRegisterUserListener listener) {
		registerUserListeners.remove(listener);
	}

	public List<OnRegisterDeviceListener> getRegisterDeviceListeners() {
		return registerDeviceListeners;
	}

	public void addRegisterDeviceListener(@NonNull OnRegisterDeviceListener listener) {
		registerDeviceListeners.add(listener);
	}

	public void removeRegisterDeviceListener(@NonNull OnRegisterDeviceListener listener) {
		registerDeviceListeners.remove(listener);
	}
}
