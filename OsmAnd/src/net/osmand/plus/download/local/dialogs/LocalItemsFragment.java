package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.DownloadActivity.LOCAL_TAB_NUMBER;
import static net.osmand.plus.download.local.ItemType.REGULAR_MAPS;
import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.CLEAR_TILES_OPERATION;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.utils.ColorUtilities.getAppBarColorId;
import static net.osmand.plus.utils.ColorUtilities.getToolbarActiveColorId;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.ItemType;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemsHolder;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.local.dialogs.DeleteConfirmationBottomSheet.ConfirmDeletionListener;
import net.osmand.plus.download.local.dialogs.LocalItemsAdapter.LocalItemListener;
import net.osmand.plus.download.local.dialogs.MemoryInfo.MemoryItem;
import net.osmand.plus.download.local.dialogs.SortMapsBottomSheet.MapsSortModeListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment.OnMapSourceUpdateListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.settings.enums.MapsSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalItemsFragment extends LocalBaseFragment implements LocalItemListener, DownloadEvents,
		ConfirmDeletionListener, OperationListener, MapsSortModeListener, OnMapSourceUpdateListener {

	private static final String TAG = LocalItemsFragment.class.getSimpleName();

	private LocalGroup group;
	private ImportHelper importHelper;
	private OptionsMenuHelper optionsHelper;
	private ItemsSelectionHelper<LocalItem> selectionHelper = new ItemsSelectionHelper<>();

	private LocalItemsAdapter adapter;
	private boolean selectionMode;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		if (selectionMode) {
			return ColorUtilities.getStatusBarActiveColorId(nightMode);
		} else {
			return ColorUtilities.getStatusBarColorId(nightMode);
		}
	}

	@Nullable
	@Override
	public LocalItemsHolder getItemsHolder() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof LocalCategoriesFragment) {
			return ((LocalCategoriesFragment) fragment).getItemsHolder();
		}
		return null;
	}

	@NonNull
	public ItemsSelectionHelper<LocalItem> getSelectionHelper() {
		return selectionHelper;
	}

	public boolean isSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		updateContent();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DownloadActivity activity = requireDownloadActivity();
		importHelper = new ImportHelper(activity);
		optionsHelper = new OptionsMenuHelper(activity);

		if (savedInstanceState == null) {
			selectionHelper.setAllItems(group.getItems());
		}
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (selectionMode) {
					selectionMode = false;
					updateContent();
				} else {
					FragmentManager manager = activity.getSupportFragmentManager();
					if (!manager.isStateSaved()) {
						manager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
					}
				}
			}
		});
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.recyclerview_fragment, container, false);

		DownloadActivity activity = requireDownloadActivity();
		activity.getAccessibilityAssistant().registerPage(view, LOCAL_TAB_NUMBER);

		setupRecyclerView(view);
		updateAdapter();

		return view;
	}

	private void setupRecyclerView(@NonNull View view) {
		adapter = new LocalItemsAdapter(view.getContext(), this, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(adapter);
	}

	private void updateContent() {
		updateToolbar();
		updateAdapter();
	}

	private void updateAdapter() {
		List<Object> items = new ArrayList<>(getSortedItems());
		if (!selectionMode) {
			addMemoryInfo(items);
		}
		adapter.setSelectionMode(selectionMode);
		adapter.setItems(items);
	}

	@NonNull
	private List<LocalItem> getSortedItems() {
		List<LocalItem> items = new ArrayList<>(group.getItems());
		if (group.getType() == REGULAR_MAPS) {
			MapsSortMode sortMode = settings.LOCAL_MAPS_SORT_MODE.get();
			Collections.sort(items, new MapsComparator(sortMode));
		} else {
			Collator collator = OsmAndCollator.primaryCollator();
			Collections.sort(items, (o1, o2) -> collator.compare(o1.getName(), o2.getName()));
		}
		return items;
	}

	private void addMemoryInfo(@NonNull List<Object> items) {
		LocalItemsHolder itemsHolder = getItemsHolder();
		if (itemsHolder != null) {
			ItemType itemType = group.getType();
			CategoryType categoryType = itemType.getCategoryType();
			LocalCategory category = itemsHolder.getCategory(categoryType);
			if (category != null) {
				List<MemoryItem> memoryItems = new ArrayList<>();

				long size = group.getSize();
				String title = getString(itemType.getTitleId());
				String text = getString(R.string.ltr_or_rtl_combine_via_dash, title, AndroidUtils.formatSize(app, size));
				memoryItems.add(new MemoryItem(text, size, ColorUtilities.getActiveColorId(nightMode)));

				title = getString(categoryType.getTitleId());
				text = getString(R.string.ltr_or_rtl_combine_via_dash, title, AndroidUtils.formatSize(app, category.getSize()));
				memoryItems.add(new MemoryItem(text, category.getSize() - group.getSize(), categoryType.getColorId()));

				items.add(0, new MemoryInfo(memoryItems));
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		optionsHelper.onCreateOptionsMenu(menu, group.getType(), this);
	}

	@Override
	public void onResume() {
		super.onResume();

		updateToolbar();
		optionsHelper.reloadItemsToUpdate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		updateToolbar();
		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			activity.updateToolbar();
		}
	}

	private void updateToolbar() {
		DownloadActivity activity = getDownloadActivity();
		ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
		if (actionBar != null) {
			updateStatusBar(activity);
			activity.invalidateOptionsMenu();
			actionBar.setTitle(selectionMode ? getString(R.string.shared_string_select) : group.getName(app));

			int colorId = selectionMode ? ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode) : 0;
			actionBar.setHomeAsUpIndicator(getIcon(selectionMode ? R.drawable.ic_action_close : AndroidUtils.getNavigationIconResId(app), colorId));

			int backgroundColorId = selectionMode ? getToolbarActiveColorId(nightMode) : getAppBarColorId(nightMode);
			actionBar.setBackgroundDrawable(new ColorDrawable(ColorUtilities.getColor(app, backgroundColorId)));
		}
	}

	public void performOperation(@NonNull OperationType type, @NonNull LocalItem... items) {
		LocalOperationTask task = new LocalOperationTask(app, type, this);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, items);
	}

	@Override
	public boolean isItemSelected(@NonNull LocalItem item) {
		return selectionHelper.isItemSelected(item);
	}

	@Override
	public void onItemSelected(@NonNull LocalItem item) {
		if (selectionMode) {
			boolean selected = !isItemSelected(item);
			selectionHelper.onItemsSelected(Collections.singleton(item), selected);
			adapter.updateItem(item);
		} else {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				LocalItemFragment.showInstance(manager, item, this);
			}
		}
	}

	@Override
	public void onItemOptionsSelected(@NonNull LocalItem item, @NonNull View view) {
		optionsHelper.onItemOptionsSelected(item, view, this);
	}

	@Override
	public void setMapsSortMode(@NonNull MapsSortMode sortMode) {
		settings.LOCAL_MAPS_SORT_MODE.set(sortMode);
		updateAdapter();
	}

	@Override
	public void onDeletionConfirmed(@NonNull LocalItem localItem) {
		performOperation(DELETE_OPERATION, localItem);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == IMPORT_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
			if (intent != null) {
				importHelper.handleImport(intent);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	@Override
	public void onOperationStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void onOperationProgress(@NonNull OperationType type, @NonNull LocalItem... values) {
		if (type == DELETE_OPERATION) {
			for (LocalItem item : values) {
				group.getItems().remove(item);
			}
		}
		if (isAdded()) {
			updateAdapter();
		}
	}

	@Override
	public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
		updateProgressVisibility(false);

		if (!Algorithms.isEmpty(result)) {
			app.showToastMessage(result);
		}

		DownloadActivity activity = getDownloadActivity();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			if (Algorithms.equalsToAny(type, RESTORE_OPERATION, BACKUP_OPERATION, CLEAR_TILES_OPERATION)) {
				activity.reloadLocalIndexes();
			} else {
				activity.onUpdatedIndexesList();
			}
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		optionsHelper.reloadItemsToUpdate();
	}

	@Override
	public void downloadHasFinished() {
		optionsHelper.reloadItemsToUpdate();
	}

	@Override
	public void onMapSourceUpdated() {
		reloadLocalIndexes();
	}

	private void reloadLocalIndexes() {
		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			activity.reloadLocalIndexes();
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull LocalGroup group, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			LocalItemsFragment fragment = new LocalItemsFragment();
			fragment.group = group;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}