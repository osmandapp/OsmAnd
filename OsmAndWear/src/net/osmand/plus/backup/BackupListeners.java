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
	private final List<OnSendCodeListener> sendCodeListeners = new ArrayList<>();
	private final List<OnCheckCodeListener> checkCodeListeners = new ArrayList<>();
	private final List<OnDeleteAccountListener> deleteAccountListeners = new ArrayList<>();

	public interface OnDeleteFilesListener {
		default void onFilesDeleteStarted(@NonNull List<RemoteFile> files) {
		}

		default void onFileDeleteProgress(@NonNull RemoteFile file, int progress) {
		}

		default void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {
		}

		default void onFilesDeleteError(int status, @NonNull String message) {
		}
	}

	public interface OnRegisterUserListener {
		void onRegisterUser(int status, @Nullable String message, @Nullable BackupError error);
	}

	public interface OnRegisterDeviceListener {
		void onRegisterDevice(int status, @Nullable String message, @Nullable BackupError error);
	}

	public interface OnUpdateSubscriptionListener {
		void onUpdateSubscription(int status, @Nullable String message, @Nullable String error);
	}

	public interface OnSendCodeListener {
		void onSendCode(int status, @Nullable String message, @Nullable BackupError error);
	}

	public interface OnCheckCodeListener {
		void onCheckCode(@NonNull String token, int status, @Nullable String message, @Nullable BackupError error);
	}

	public interface OnDeleteAccountListener {
		void onDeleteAccount(int status, @Nullable String message, @Nullable BackupError error);
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

		void onFileDownloadDone(@NonNull String type, @NonNull String fileName, @Nullable String error);

		boolean isDownloadCancelled();
	}

	@NonNull
	public List<OnDeleteFilesListener> getDeleteFilesListeners() {
		return deleteFilesListeners;
	}

	public void addDeleteFilesListener(@NonNull OnDeleteFilesListener listener) {
		if (!deleteFilesListeners.contains(listener)) {
			deleteFilesListeners.add(listener);
		}
	}

	public void removeDeleteFilesListener(@NonNull OnDeleteFilesListener listener) {
		deleteFilesListeners.remove(listener);
	}

	@NonNull
	public List<OnRegisterUserListener> getRegisterUserListeners() {
		return registerUserListeners;
	}

	public void addRegisterUserListener(@NonNull OnRegisterUserListener listener) {
		if (!registerUserListeners.contains(listener)) {
			registerUserListeners.add(listener);
		}
	}

	public void removeRegisterUserListener(@NonNull OnRegisterUserListener listener) {
		registerUserListeners.remove(listener);
	}

	@NonNull
	public List<OnRegisterDeviceListener> getRegisterDeviceListeners() {
		return registerDeviceListeners;
	}

	public void addRegisterDeviceListener(@NonNull OnRegisterDeviceListener listener) {
		if (!registerDeviceListeners.contains(listener)) {
			registerDeviceListeners.add(listener);
		}
	}

	public void removeRegisterDeviceListener(@NonNull OnRegisterDeviceListener listener) {
		registerDeviceListeners.remove(listener);
	}

	@NonNull
	public List<OnSendCodeListener> getSendCodeListeners() {
		return sendCodeListeners;
	}

	public void addSendCodeListener(@NonNull OnSendCodeListener listener) {
		if (!sendCodeListeners.contains(listener)) {
			sendCodeListeners.add(listener);
		}
	}

	public void removeSendCodeListener(@NonNull OnSendCodeListener listener) {
		sendCodeListeners.remove(listener);
	}

	@NonNull
	public List<OnCheckCodeListener> getCheckCodeListeners() {
		return checkCodeListeners;
	}

	public void addCheckCodeListener(@NonNull OnCheckCodeListener listener) {
		if (!checkCodeListeners.contains(listener)) {
			checkCodeListeners.add(listener);
		}
	}

	public void removeCheckCodeListener(@NonNull OnCheckCodeListener listener) {
		checkCodeListeners.remove(listener);
	}

	@NonNull
	public List<OnDeleteAccountListener> getDeleteAccountListeners() {
		return deleteAccountListeners;
	}

	public void addDeleteAccountListener(@NonNull OnDeleteAccountListener listener) {
		if (!deleteAccountListeners.contains(listener)) {
			deleteAccountListeners.add(listener);
		}
	}

	public void removeDeleteAccountListener(@NonNull OnDeleteAccountListener listener) {
		deleteAccountListeners.remove(listener);
	}
}
