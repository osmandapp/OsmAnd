package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.DownloadActivity.LOCAL_TAB_NUMBER;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
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
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.local.dialogs.LocalItemsAdapter.LocalItemListener;
import net.osmand.plus.download.local.dialogs.MemoryInfo.MemoryItem;
import net.osmand.plus.download.local.dialogs.SortMapsBottomSheet.MapsSortModeListener;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.settings.enums.LocalSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LocalItemsFragment extends LocalBaseFragment implements LocalItemListener, MapsSortModeListener {

	public static final String TAG = LocalItemsFragment.class.getSimpleName();

	private static final String ITEM_TYPE_KEY = "item_type_key";

	private LocalItemType type;
	private ImportHelper importHelper;
	private ItemMenuProvider itemMenuProvider;
	private GroupMenuProvider groupMenuProvider;
	private final ItemsSelectionHelper<BaseLocalItem> selectionHelper = new ItemsSelectionHelper<>();

	private LocalItemsAdapter adapter;
	private boolean selectionMode;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		if (selectionMode) {
			return ColorUtilities.getStatusBarActiveColorId(nightMode);
		} else {
			return ColorUtilities.getStatusBarColorId(nightMode);
		}
	}

	public boolean isSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		selectionHelper.clearSelectedItems();
		updateContent();
	}

	@NonNull
	public ItemsSelectionHelper<BaseLocalItem> getSelectionHelper() {
		return selectionHelper;
	}

	@Nullable
	public LocalGroup getGroup() {
		LocalCategory category = getCategory();
		return category != null ? category.getGroups().get(type) : null;
	}

	@Nullable
	public LocalCategory getCategory() {
		Map<CategoryType, LocalCategory> categories = getCategories();
		return categories != null ? categories.get(type.getCategoryType()) : null;
	}

	@Nullable
	@Override
	public Map<CategoryType, LocalCategory> getCategories() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof LocalCategoriesFragment) {
			return ((LocalCategoriesFragment) fragment).getCategories();
		}
		return null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			type = AndroidUtils.getSerializable(args, ITEM_TYPE_KEY, LocalItemType.class);
		}

		DownloadActivity activity = requireDownloadActivity();
		importHelper = app.getImportHelper();
		groupMenuProvider = new GroupMenuProvider(activity, this);
		itemMenuProvider = new ItemMenuProvider(activity, this);
		itemMenuProvider.setColorId(ColorUtilities.getDefaultIconColorId(nightMode));

		LocalGroup group = getGroup();
		if (savedInstanceState == null && group != null) {
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
		updateContent();

		return view;
	}

	private void setupRecyclerView(@NonNull View view) {
		adapter = new LocalItemsAdapter(view.getContext(), this, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(adapter);
	}

	public void updateContent() {
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
	private List<Object> getSortedItems() {
		List<BaseLocalItem> activeItems = new ArrayList<>();
		List<BaseLocalItem> backupedItems = new ArrayList<>();

		LocalGroup group = getGroup();
		if (group != null) {
			for (BaseLocalItem item : group.getItems()) {
				if (item instanceof LocalItem && ((LocalItem) item).isBackuped(app)) {
					backupedItems.add(item);
				} else {
					activeItems.add(item);
				}
			}
			sortItems(activeItems);
			sortItems(backupedItems);
		}

		List<Object> items = new ArrayList<>(activeItems);
		if (!Algorithms.isEmpty(backupedItems)) {
			items.add(new HeaderGroup(getString(R.string.local_indexes_cat_backup), backupedItems));
			items.addAll(backupedItems);
		}
		return items;
	}

	private void sortItems(@NonNull List<BaseLocalItem> items) {
		if (type.isSortingSupported()) {
			LocalSortMode sortMode = LocalItemUtils.getSortModePref(app, type).get();
			Collections.sort(items, new LocalItemsComparator(app, sortMode));
		} else {
			Collator collator = OsmAndCollator.primaryCollator();
			Collections.sort(items, (o1, o2) -> collator.compare(o1.getName(app).toString(), o2.getName(app).toString()));
		}
	}

	private void addMemoryInfo(@NonNull List<Object> items) {
		LocalGroup group = getGroup();
		LocalCategory category = getCategory();
		if (group != null && category != null) {
			List<MemoryItem> memoryItems = new ArrayList<>();

			if (!Algorithms.isEmpty(group.getItems())) {
				long size = group.getSize();
				String title = group.getName(app);
				String text = getString(R.string.ltr_or_rtl_combine_via_dash, title, AndroidUtils.formatSize(app, size));
				memoryItems.add(new MemoryItem(text, size, ColorUtilities.getActiveColor(app, nightMode)));
			}

			String title = category.getName(app);
			int color = ColorUtilities.getColor(app, category.getType().getColorId());
			String text = getString(R.string.ltr_or_rtl_combine_via_dash, title, AndroidUtils.formatSize(app, category.getSize()));
			memoryItems.add(new MemoryItem(text, category.getSize() - group.getSize(), color));

			items.add(0, new MemoryInfo(memoryItems));
		}
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		groupMenuProvider.onCreateMenu(menu, inflater);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateToolbar();
	}

	@Override
	public void onPause() {
		super.onPause();
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

			actionBar.setElevation(5.0f);
			LocalGroup group = getGroup();
			if (group != null) {
				actionBar.setTitle(selectionMode ? getString(R.string.shared_string_select) : group.getName(app));
			}

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
	public boolean isItemSelected(@NonNull BaseLocalItem item) {
		return selectionHelper.isItemSelected(item);
	}

	@Override
	public boolean itemUpdateAvailable(@NonNull LocalItem item) {
		return getItemsToUpdate().containsKey(item.getFile().getName());
	}

	@Override
	public void onItemSelected(@NonNull BaseLocalItem item) {
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
	public void onItemOptionsSelected(@NonNull BaseLocalItem item, @NonNull View view) {
		itemMenuProvider.setItem(item);
		itemMenuProvider.showMenu(view);
	}

	@Override
	public void setMapsSortMode(@NonNull LocalSortMode sortMode) {
		LocalItemUtils.getSortModePref(app, type).set(sortMode);
		updateAdapter();
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
	public void onOperationProgress(@NonNull OperationType type, @NonNull BaseLocalItem... values) {
		LocalGroup group = getGroup();
		if (type == DELETE_OPERATION && group != null) {
			for (BaseLocalItem item : values) {
				group.removeItem(app, item);
			}
		}
		if (isAdded()) {
			updateAdapter();
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull LocalItemType type, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putSerializable(ITEM_TYPE_KEY, type);

			LocalItemsFragment fragment = new LocalItemsFragment();
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}