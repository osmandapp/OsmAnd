package net.osmand.plus.download.ui;

import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.local.dialogs.LocalItemsAdapter;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeletedMapsFragment extends BaseFullScreenDialogFragment implements LocalItemsAdapter.LocalItemListener, RemoveDeletedMapsConfirmationDialog.ConfirmationDialogListener {

	private static final String TAG = DeletedMapsFragment.class.getSimpleName();
	public static final int CONFIRM_DELETE_ITEM = 1;
	public static final int CONFIRM_DELETE_ALL_ITEMS = 2;


	private RecyclerView listView;
	private LocalItemsAdapter listAdapter;
	private DownloadActivity activity;
	private Toolbar toolbar;
	private DownloadIndexesThread downloadThread;
	private LocalItem selectedItemToDelete;
	private List<Object> localItemsToDelete;
	private View divider;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		downloadThread = app.getDownloadThread();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.deprecated_maps_fragment, container, false);
		activity = (DownloadActivity) getActivity();

		toolbar = view.findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.unsupported_maps);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(activity)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());

		setupRecyclerView(view);

		view.findViewById(R.id.delete_all_button).setOnClickListener((v) -> onDeleteAll());
		divider = view.findViewById(R.id.bottom_divider);
		return view;
	}

	private void onDeleteAll() {
		askDeleteItems(null);
	}

	@Override
	public void onItemOptionsSelected(@NonNull BaseLocalItem item, @NonNull View view) {
		LocalItemsAdapter.LocalItemListener.super.onItemOptionsSelected(item, view);
		askDeleteItems((LocalItem) item);
	}

	@Override
	public void onItemSelected(@NonNull BaseLocalItem item) {
		LocalItemsAdapter.LocalItemListener.super.onItemSelected(item);
		askDeleteItems((LocalItem) item);
	}

	private void askDeleteItems(@Nullable LocalItem item) {
		selectedItemToDelete = item;
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			int itemsCount = item == null ? listAdapter.getItemCount() : 1;
			int requestCode = item == null ? CONFIRM_DELETE_ALL_ITEMS : CONFIRM_DELETE_ITEM;
			String title = app.getString(R.string.delete_all_items);
			String description = String.format(app.getString(R.string.deleted_maps_warning), itemsCount);
			RemoveDeletedMapsConfirmationDialog.showInstance(manager,
					this,
					title,
					description,
					requestCode);
		}
	}

	private void setupRecyclerView(@NonNull View view) {
		listAdapter = new LocalItemsAdapter(app, this, nightMode);
		listView = view.findViewById(android.R.id.list);
		listView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		listView.setAdapter(listAdapter);
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.removeType(Type.LANDSCAPE_SIDES);
		Dialog dialog = getDialog();
		boolean isInnerDialog = dialog != null && dialog.getWindow() != null;
		if (!isInnerDialog) {
			collection.removeType(Type.ROOT_INSET);
		} else {
			collection.add(InsetTarget.createHorizontalLandscape(R.id.sliding_tabs_container, R.id.freeVersionBanner, R.id.downloadProgressLayout, R.id.toolbar).build());
		}
		return collection;
	}

	@Override
	public void onResume() {
		super.onResume();
		reloadData();
	}

	private void reloadData() {
		DownloadResources indexes = downloadThread.getIndexes();
		List<IndexItem> deletedMaps = indexes.getDeletedItems();
		List<Object> localItemsToDelete = new ArrayList<>();

		for (IndexItem indexItem : deletedMaps) {
			File fileToDelete = indexItem.getTargetFile(app);
			LocalItemType type = LocalItemUtils.getItemType(app, fileToDelete);
			if (type != null) {
				LocalItem localItem = new LocalItem(fileToDelete, type);
				localItem.setDeleted(true);
				localItemsToDelete.add(localItem);
			}
		}
		this.localItemsToDelete = localItemsToDelete;
		listAdapter.setItems(localItemsToDelete);
		AndroidUiHelper.updateVisibility(divider, !localItemsToDelete.isEmpty());
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull Fragment target) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			DeletedMapsFragment fragment = new DeletedMapsFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void onActionConfirmed(int actionId) {
		if (actionId == CONFIRM_DELETE_ITEM) {
			deleteItems(Collections.singletonList(selectedItemToDelete));
		} else {
			deleteItems(localItemsToDelete);
		}
	}

	private void deleteItems(List<Object> items) {
		LocalOperationTask removeTask = new LocalOperationTask(app, DELETE_OPERATION, new LocalOperationTask.OperationListener() {
			@Override
			public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
				if (AndroidUtils.isActivityNotDestroyed(getActivity())) {
					reloadData();
					Fragment targetFragment = getTargetFragment();
					if (targetFragment instanceof DownloadIndexesThread.DownloadEvents downloadEvents) {
						downloadEvents.onUpdatedIndexesList();
					}
				}
			}
		});
		OsmAndTaskManager.executeTask(removeTask, items.toArray(new LocalItem[0]));
	}
}