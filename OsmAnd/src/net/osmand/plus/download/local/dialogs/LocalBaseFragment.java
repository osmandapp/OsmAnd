package net.osmand.plus.download.local.dialogs;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment.OnMapSourceUpdateListener;
import net.osmand.plus.utils.FileUtils.RenameCallback;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class LocalBaseFragment extends BaseOsmAndFragment implements OperationListener,
		DownloadEvents, OnMapSourceUpdateListener, RenameCallback {

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
