package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.CLEAR_TILES_OPERATION;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.local.dialogs.DeleteConfirmationDialogController.ConfirmDeletionListener;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment.OnMapSourceUpdateListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class LocalBaseFragment extends BaseFullScreenFragment implements OperationListener,
		ConfirmDeletionListener, DownloadEvents, OnMapSourceUpdateListener, RenameCallback {

	private final Map<String, IndexItem> itemsToUpdate = new HashMap<>();

	@NonNull
	public Map<String, IndexItem> getItemsToUpdate() {
		return itemsToUpdate;
	}

	@Nullable
	public abstract Map<CategoryType, LocalCategory> getCategories();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		DeleteConfirmationDialogController.askUpdateListener(app, this);
	}

	@Nullable
	@Override
	public Set<InsetSide> getRootInsetSides() {
		return null;
	}

	@Override
	public void onResume() {
		super.onResume();
		reloadItemsToUpdate();
	}

	protected void reloadItemsToUpdate() {
		itemsToUpdate.clear();
		for (IndexItem item : app.getDownloadThread().getIndexes().getItemsToUpdate()) {
			itemsToUpdate.put(item.getTargetFileName(), item);
		}
	}

	@Override
	public void onMapSourceUpdated() {
		reloadLocalIndexes();
	}

	@Override
	public void fileRenamed(@NonNull File src, @NonNull File dest) {
		reloadLocalIndexes();
	}

	private void reloadLocalIndexes() {
		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			activity.reloadLocalIndexes();
		}
	}

	protected void updateProgressVisibility(boolean visible) {
		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	@Override
	public void onDeletionConfirmed(@NonNull BaseLocalItem item) {
		performOperation(DELETE_OPERATION, item);
	}

	public void performOperation(@NonNull OperationType type, @NonNull BaseLocalItem... items) {
		LocalOperationTask task = new LocalOperationTask(app, type, this);
		OsmAndTaskManager.executeTask(task, items);
	}

	@Override
	public void onOperationStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
		updateProgressVisibility(false);

		if (!Algorithms.isEmpty(result)) {
			app.showToastMessage(result);
		}
		DownloadActivity activity = getDownloadActivity();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			if (CollectionUtils.equalsToAny(type, RESTORE_OPERATION, BACKUP_OPERATION, CLEAR_TILES_OPERATION)) {
				activity.reloadLocalIndexes();
			} else {
				activity.onUpdatedIndexesList();
			}
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		reloadItemsToUpdate();
	}

	@Override
	public void downloadHasFinished() {
		reloadItemsToUpdate();
	}

	@Nullable
	protected DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	@NonNull
	protected DownloadActivity requireDownloadActivity() {
		return (DownloadActivity) requireActivity();
	}
}
