package net.osmand.plus.download.local.dialogs.livegroup;

import static net.osmand.plus.download.DownloadActivity.LOCAL_TAB_NUMBER;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.download.local.dialogs.LocalItemsAdapter.*;
import static net.osmand.plus.helpers.FileNameTranslationHelper.getBasename;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;
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
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.local.dialogs.HeaderGroup;
import net.osmand.plus.download.local.dialogs.LocalBaseFragment;
import net.osmand.plus.download.local.dialogs.LocalCategoriesFragment;
import net.osmand.plus.download.local.dialogs.LocalItemInfoCard;
import net.osmand.plus.download.local.dialogs.LocalItemsAdapter;
import net.osmand.plus.download.local.dialogs.LocalItemsFragment;
import net.osmand.plus.download.local.dialogs.MapsComparator;
import net.osmand.plus.download.local.dialogs.MemoryInfo;
import net.osmand.plus.download.local.dialogs.SortMapsBottomSheet;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.settings.enums.MapsSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiveGroupItemsFragment extends LocalBaseFragment implements LiveGroupItemListener {

	public static final String TAG = LiveGroupItemsFragment.class.getSimpleName();

	private static final String ITEM_TYPE_KEY = "item_type_key";

	private LocalItemType type;
	private ImportHelper importHelper;
	private LiveGroupMenuProvider itemMenuProvider;
	private LiveGroupCategoryMenuProvider groupMenuProvider;

	private LocalItemsAdapter adapter;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
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
		importHelper = new ImportHelper(app);
		groupMenuProvider = new LiveGroupCategoryMenuProvider(activity, this);
		itemMenuProvider = new LiveGroupMenuProvider(activity, this);
		itemMenuProvider.setColorId(ColorUtilities.getDefaultIconColorId(nightMode));

		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				FragmentManager manager = activity.getSupportFragmentManager();
				if (!manager.isStateSaved()) {
					manager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
		adapter = new LocalItemsAdapter(view.getContext(), null,  this, nightMode);

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
		if (type == LocalItemType.LIVE_UPDATES) {
			Map<String, LiveGroupItem> liveGroupItemMap = new HashMap<>();
			List<Object> itemsToDelete = new ArrayList<>();

			for (Object item : items) {
				if (item instanceof LocalItem) {
					LocalItem localItem = (LocalItem) item;
					String basename = getBasename(app, localItem.getFileName());

					String withoutNumber = basename.replaceAll("(_\\d*)*$", "");
					String groupName = getNameToDisplay(withoutNumber, app);
					LiveGroupItem groupItem = liveGroupItemMap.get(groupName);
					if (groupItem == null) {
						groupItem = new LiveGroupItem(groupName);
						liveGroupItemMap.put(groupName, groupItem);
					}
					groupItem.addLocalItem(localItem);
					itemsToDelete.add(item);
				}
			}
			items.removeAll(itemsToDelete);
			items.addAll(liveGroupItemMap.values());
		}
		addMemoryInfo(items);
		adapter.setItems(items);
	}

	@NonNull
	private List<Object> getSortedItems() {
		List<LocalItem> activeItems = new ArrayList<>();
		List<LocalItem> backupedItems = new ArrayList<>();

		LocalGroup group = getGroup();
		if (group != null) {
			for (LocalItem item : group.getItems()) {
				if (item.isBackuped(app)) {
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

	private void sortItems(@NonNull List<LocalItem> items) {
		if (type.isMapsSortingSupported()) {
			MapsSortMode sortMode = settings.LOCAL_MAPS_SORT_MODE.get();
			Collections.sort(items, new MapsComparator(app, sortMode));
		} else {
			Collator collator = OsmAndCollator.primaryCollator();
			Collections.sort(items, (o1, o2) -> collator.compare(o1.getName(app).toString(), o2.getName(app).toString()));
		}
	}

	private void addMemoryInfo(@NonNull List<Object> items) {
		LocalGroup group = getGroup();
		LocalCategory category = getCategory();
		if (group != null && category != null) {
			List<MemoryInfo.MemoryItem> memoryItems = new ArrayList<>();

			if (!Algorithms.isEmpty(group.getItems())) {
				long size = group.getSize();
				String title = group.getName(app);
				String text = getString(R.string.ltr_or_rtl_combine_via_dash, title, AndroidUtils.formatSize(app, size));
				memoryItems.add(new MemoryInfo.MemoryItem(text, size, ColorUtilities.getActiveColor(app, nightMode)));
			}

			String title = category.getName(app);
			int color = ColorUtilities.getColor(app, category.getType().getColorId());
			String text = getString(R.string.ltr_or_rtl_combine_via_dash, title, AndroidUtils.formatSize(app, category.getSize()));
			memoryItems.add(new MemoryInfo.MemoryItem(text, category.getSize() - group.getSize(), color));

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
				actionBar.setTitle(group.getName(app));
			}

			int colorId = 0;
			actionBar.setHomeAsUpIndicator(getIcon(AndroidUtils.getNavigationIconResId(app), colorId));

			int backgroundColorId = getAppBarColorId(nightMode);
			actionBar.setBackgroundDrawable(new ColorDrawable(ColorUtilities.getColor(app, backgroundColorId)));
		}
	}

	@Override
	public void onItemSelected(@NonNull LiveGroupItem item) {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			LiveGroupItemFragment.showInstance(manager, item, this);
		}
	}

	@Override
	public void onItemOptionsSelected(@NonNull LiveGroupItem item, @NonNull View view) {
		itemMenuProvider.setLiveGroupItem(item);
		itemMenuProvider.showMenu(view);
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

	public static class LiveGroupItem implements LocalItemInfoCard.LocalItemInterface {

		public String name;
		public List<LocalItem> localItems = new ArrayList<>();

		LiveGroupItem(String name){
			this.name = name;
		}

		public void addLocalItem(LocalItem localItem){
			localItems.add(localItem);
		}

		@Override
		public LocalItemType getLocalItemType() {
			return LocalItemType.LIVE_UPDATES;
		}

		@Override
		public long getLocalItemCreated() {
			return localItems.isEmpty() ? 0 : localItems.get(0).getLastModified();
		}

		@Override
		public long getLocalItemSize() {
			long totalSize = 0;
			for(LocalItem item : localItems){
				totalSize += item.getSize();
			}
			return totalSize;
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull LocalItemType type, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putSerializable(ITEM_TYPE_KEY, type);

			LiveGroupItemsFragment fragment = new LiveGroupItemsFragment();
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}