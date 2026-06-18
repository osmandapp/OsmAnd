package net.osmand.plus.settings.mediastorage;

import static net.osmand.plus.settings.bottomsheets.ChangeMediaStorageBottomSheet.CHOSEN_MANUAL_URI;
import static net.osmand.plus.settings.bottomsheets.ChangeMediaStorageBottomSheet.CHOSEN_STORAGE_TYPE;
import static net.osmand.plus.settings.bottomsheets.ChangeMediaStorageBottomSheet.MOVE_MEDIA;
import static net.osmand.plus.settings.enums.MediaStorageType.CAMERA_FOLDER;
import static net.osmand.plus.settings.enums.MediaStorageType.MANUALLY_SPECIFIED;
import static net.osmand.plus.settings.enums.MediaStorageType.SHARED_STORAGE;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.bottomsheets.ChangeMediaStorageBottomSheet;
import net.osmand.plus.settings.datastorage.MoveFilesStopListener;
import net.osmand.plus.settings.mediastorage.task.CollectMediaFilesTask;
import net.osmand.plus.settings.mediastorage.task.CollectMediaFilesTask.MediaFilesCollection;
import net.osmand.plus.settings.mediastorage.task.MoveMediaFilesTask;
import net.osmand.plus.settings.mediastorage.task.ValidateMediaStorageTask;
import net.osmand.plus.settings.enums.MediaStorageType;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class MediaStorageFragment extends BaseSettingsFragment implements MoveFilesStopListener {

	private static final Log LOG = PlatformUtil.getLog(MediaStorageFragment.class);

	private static final int FOLDER_ACCESS_REQUEST = 1009;
	private static final int WRITE_EXTERNAL_STORAGE_REQUEST = 1010;

	private final List<CheckBoxPreference> storageTypePrefs = new ArrayList<>();

	private MoveMediaFilesTask moveFilesTask;
	private CollectMediaFilesTask collectFilesTask;
	private ValidateMediaStorageTask validateStorageTask;

	private MediaStorageType currentStorageType;
	private MediaStorageType pendingStorageType;

	@Override
	protected void setupPreferences() {
		PreferenceScreen screen = getPreferenceScreen();
		if (screen == null) {
			return;
		}
		storageTypePrefs.clear();
		for (MediaStorageType type : MediaStorageType.values()) {
			CheckBoxPreference preference = new CheckBoxPreference(requireContext());
			preference.setKey(type.name());
			preference.setTitle(type.getTitleId());
			preference.setSummary(type.getDescriptionId());
			preference.setLayoutResource(R.layout.media_storage_list_item);
			preference.setPersistent(false);
			screen.addPreference(preference);
			storageTypePrefs.add(preference);
		}
		currentStorageType = settings.MEDIA_STORAGE_TYPE.get();
		updateView();
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
		recyclerView.setItemAnimator(null);
		return recyclerView;
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (preference instanceof CheckBoxPreference) {
			TextView summary = holder.itemView.findViewById(R.id.summary);
			if (summary != null) {
				summary.setText(preference.getSummary());
			}
			View divider = holder.itemView.findViewById(R.id.divider);
			if (divider != null) {
				MediaStorageType[] types = MediaStorageType.values();
				boolean lastItem = types[types.length - 1].name().equals(preference.getKey());
				AndroidUiHelper.updateVisibility(divider, !lastItem);
			}
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (newValue instanceof Bundle resultData) {
			if (resultData.containsKey(ChangeMediaStorageBottomSheet.TAG)) {
				String typeName = resultData.getString(CHOSEN_STORAGE_TYPE);
				if (typeName != null) {
					applyNewStorageType(MediaStorageType.valueOf(typeName), resultData.getString(CHOSEN_MANUAL_URI), resultData.getBoolean(MOVE_MEDIA));
				}
			}
		} else {
			onStorageTypeSelected(preference.getKey());
		}
		return false;
	}

	private void onStorageTypeSelected(@Nullable String key) {
		if (key == null) {
			return;
		}
		MediaStorageType type = MediaStorageType.valueOf(key);
		if (type == MANUALLY_SPECIFIED) {
			openManualStoragePicker();
		} else if (type != currentStorageType) {
			if (requestLegacyPublicStoragePermissionIfNeeded(type)) {
				return;
			}
			showChangeMediaStorageBottomSheet(type, null);
		}
	}

	private boolean requestLegacyPublicStoragePermissionIfNeeded(@NonNull MediaStorageType type) {
		if (!requiresLegacyPublicStoragePermission(type) || AndroidUtils.hasPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			return false;
		}
		pendingStorageType = type;
		requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST);
		return true;
	}

	private boolean requiresLegacyPublicStoragePermission(@NonNull MediaStorageType type) {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && (type == SHARED_STORAGE || type == CAMERA_FOLDER);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST) {
			MediaStorageType storageType = pendingStorageType;
			pendingStorageType = null;
			if (storageType == null) {
				return;
			}
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				showChangeMediaStorageBottomSheet(storageType, null);
			} else {
				app.showShortToastMessage(R.string.missing_write_external_storage_permission);
			}
		}
	}

	private void openManualStoragePicker() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
				| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
		AndroidUtils.startActivityForResultIfSafe(this, intent, FOLDER_ACCESS_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == FOLDER_ACCESS_REQUEST) {
			if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
				return;
			}
			Uri treeUri = data.getData();
			if (!persistManualStoragePermission(data, treeUri)) {
				app.showShortToastMessage(R.string.folder_access_denied);
			} else {
				showChangeMediaStorageBottomSheet(MANUALLY_SPECIFIED, treeUri.toString());
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private boolean persistManualStoragePermission(@NonNull Intent data, @NonNull Uri treeUri) {
		int requiredFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
		return AndroidUtils.takePersistableUriPermission(app, treeUri, data.getFlags(), requiredFlags);
	}

	private void applyNewStorageType(@NonNull MediaStorageType storageType,
			@Nullable String manualUri, boolean moveMedia) {
		if (storageType == MANUALLY_SPECIFIED && Algorithms.isEmpty(manualUri)) {
			return;
		}
		if (isMediaMigrationInProgress()) {
			logDebug("Apply media storage skipped: media migration already in progress");
			app.showShortToastMessage(R.string.wait_current_task_finished);
			return;
		}
		MediaStorageLocation previousStorage = MediaStorageLocation.fromSettings(app);
		MediaStorageLocation newStorage = MediaStorageLocation.fromSelection(storageType, manualUri);
		String obsoleteManualUri = getObsoleteManualUri(previousStorage, newStorage);
		logDebug("Apply media storage requested: previous=" + previousStorage.getStorageType()
				+ ", new=" + newStorage.getStorageType() + ", moveMedia=" + moveMedia
				+ ", previousManualUri=" + previousStorage.getManualUri() + ", newManualUri=" + newStorage.getManualUri());
		stopValidateStorageTask();
		validateStorageTask = new ValidateMediaStorageTask(app, newStorage, writable -> {
			validateStorageTask = null;
			if (!isAdded()) {
				return true;
			}
			if (!writable) {
				logDebug("Apply media storage rejected: target is not writable, storage=" + newStorage.getStorageType());
				releaseRejectedManualStoragePermission(previousStorage, newStorage, manualUri);
				app.showShortToastMessage(R.string.media_storage_directory_not_writable);
				return true;
			}
			applyValidatedStorageType(storageType, manualUri, moveMedia, previousStorage, newStorage, obsoleteManualUri);
			return true;
		});
		OsmAndTaskManager.executeTask(validateStorageTask);
	}

	private void releaseRejectedManualStoragePermission(@NonNull MediaStorageLocation previousStorage, @NonNull MediaStorageLocation newStorage, @Nullable String manualUri) {
		if (newStorage.getStorageType() == MANUALLY_SPECIFIED && !previousStorage.hasSameStorage(newStorage)) {
			releaseManualStoragePermission(manualUri);
		}
	}

	private void applyValidatedStorageType(@NonNull MediaStorageType storageType, @Nullable String manualUri,
	                                       boolean moveMedia, @NonNull MediaStorageLocation previousStorage,
	                                       @NonNull MediaStorageLocation newStorage, @Nullable String obsoleteManualUri) {
		logDebug("Apply media storage validated: storage=" + storageType + ", moveMedia=" + moveMedia);
		saveStorageType(storageType, manualUri);
		if (moveMedia && !previousStorage.hasSameStorage(newStorage)) {
			collectAndMoveMediaFiles(previousStorage, newStorage, obsoleteManualUri);
		} else {
			logDebug("Apply media storage without migration");
			releaseManualStoragePermission(obsoleteManualUri);
		}
	}

	private void saveStorageType(@NonNull MediaStorageType storageType, @Nullable String manualUri) {
		if (storageType == MANUALLY_SPECIFIED) {
			settings.MEDIA_STORAGE_MANUAL_URI.set(manualUri);
		} else {
			settings.MEDIA_STORAGE_MANUAL_URI.set("");
		}
		settings.MEDIA_STORAGE_TYPE.set(storageType);
		currentStorageType = storageType;
		updateView();
	}

	@Nullable
	private String getObsoleteManualUri(@NonNull MediaStorageLocation previousStorage, @NonNull MediaStorageLocation newStorage) {
		if (previousStorage.getStorageType() != MANUALLY_SPECIFIED || previousStorage.hasSameStorage(newStorage)) {
			return null;
		}
		Uri manualTreeUri = previousStorage.getManualUri();
		return manualTreeUri == null ? null : manualTreeUri.toString();
	}

	private void releaseManualStoragePermission(@Nullable String manualUri) {
		if (!Algorithms.isEmpty(manualUri)) {
			int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
			AndroidUtils.releasePersistableUriPermission(app, Uri.parse(manualUri), flags);
		}
	}

	private void collectAndMoveMediaFiles(@NonNull MediaStorageLocation previous, @NonNull MediaStorageLocation newStorage, @Nullable String obsoleteManualUri) {
		logDebug("Collect media before migration: from=" + previous.getStorageType() + ", to=" + newStorage.getStorageType());
		stopCollectMediaFilesTask();
		stopMoveFilesTask();
		collectFilesTask = new CollectMediaFilesTask(app, previous, collection -> {
			collectFilesTask = null;
			moveMediaFiles(previous, newStorage, collection, obsoleteManualUri);
			return true;
		});
		OsmAndTaskManager.executeTask(collectFilesTask);
	}

	private void showChangeMediaStorageBottomSheet(@NonNull MediaStorageType storageType, @Nullable String manualUri) {
		ChangeMediaStorageBottomSheet.showInstance(getFragmentManager(), storageType.name(),
				currentStorageType, storageType, manualUri, this, false);
	}

	private void moveMediaFiles(@NonNull MediaStorageLocation from, @NonNull MediaStorageLocation to,
	                            @NonNull MediaFilesCollection collection, @Nullable String obsoleteManualUri) {
		List<MediaSource> sources = collection.sources();
		if (Algorithms.isEmpty(sources)) {
			logDebug("Move media skipped: no collected sources");
			releaseManualStoragePermission(obsoleteManualUri);
			return;
		}
		OsmandActionBarActivity activity = getActionBarActivity();
		logDebug("Move media task starting: from=" + from.getStorageType() + ", to=" + to.getStorageType()
				+ ", sources=" + sources.size() + ", size=" + collection.filesSize().first + ", hasActivity=" + (activity != null));
		stopMoveFilesTask();
		moveFilesTask = new MoveMediaFilesTask(app, activity, from, to, sources, collection.filesSize(), this, success -> {
			logDebug("Move media task callback: success=" + success + ", obsoleteManualUri=" + obsoleteManualUri);
			moveFilesTask = null;
			if (success) {
				releaseManualStoragePermission(obsoleteManualUri);
			}
			return false;
		});
		OsmAndTaskManager.executeTask(moveFilesTask);
	}

	private void updateView() {
		for (CheckBoxPreference preference : storageTypePrefs) {
			preference.setChecked(Algorithms.stringsEqual(currentStorageType.name(), preference.getKey()));
		}
	}

	private void stopMoveFilesTask() {
		if (moveFilesTask != null && moveFilesTask.getStatus() == AsyncTask.Status.RUNNING) {
			moveFilesTask.cancel(false);
		}
	}

	private void stopCollectMediaFilesTask() {
		if (collectFilesTask != null && collectFilesTask.getStatus() == AsyncTask.Status.RUNNING) {
			collectFilesTask.cancel(false);
		}
		collectFilesTask = null;
	}

	private void stopValidateStorageTask() {
		if (validateStorageTask != null && validateStorageTask.getStatus() == AsyncTask.Status.RUNNING) {
			validateStorageTask.cancel(false);
		}
		validateStorageTask = null;
	}

	@Override
	public void onStopTask() {
		stopValidateStorageTask();
		stopCollectMediaFilesTask();
		stopMoveFilesTask();
	}

	private boolean isMediaMigrationInProgress() {
		return collectFilesTask != null || moveFilesTask != null;
	}

	private static void logDebug(@NonNull String message) {
		if (PluginsHelper.isDevelopment()) {
			LOG.debug(message);
		}
	}
}