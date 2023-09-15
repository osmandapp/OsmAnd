package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.DownloadActivity.LOCAL_TAB_NUMBER;
import static net.osmand.plus.download.ui.DownloadResourceGroupFragment.RELOAD_ID;
import static net.osmand.plus.download.ui.DownloadResourceGroupFragment.SEARCH_ID;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItemsHolder;
import net.osmand.plus.download.local.LocalItemsLoaderTask;
import net.osmand.plus.download.local.LocalItemsLoaderTask.LoadItemsListener;
import net.osmand.plus.download.local.dialogs.CategoriesAdapter.LocalTypeListener;
import net.osmand.plus.download.local.dialogs.MemoryInfo.MemoryItem;
import net.osmand.plus.download.ui.SearchDialogFragment;
import net.osmand.plus.utils.ColorUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class LocalCategoriesFragment extends LocalBaseFragment implements DownloadEvents, LocalTypeListener, LoadItemsListener {

	private LocalItemsHolder itemsHolder;
	private LocalItemsLoaderTask asyncLoader;

	private CategoriesAdapter adapter;

	@Nullable
	@Override
	public LocalItemsHolder getItemsHolder() {
		return itemsHolder;
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
		adapter = new CategoriesAdapter(this, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(adapter);
	}

	private void updateAdapter() {
		if (itemsHolder != null) {
			List<Object> items = new ArrayList<>();
			List<MemoryItem> memoryItems = new ArrayList<>();
			List<LocalCategory> categories = new ArrayList<>(itemsHolder.getCategories().values());

			for (LocalCategory category : categories) {
				items.add(category);

				CategoryType type = category.getType();
				memoryItems.add(new MemoryItem(category.getName(app), category.getSize(), type.getColorId()));

				List<LocalGroup> groups = new ArrayList<>(category.getGroups().values());
				Collections.sort(groups, (o1, o2) -> -Long.compare(o1.getSize(), o2.getSize()));

				for (LocalGroup group : groups) {
					items.add(group);
				}
			}
			MemoryInfo memoryInfo = new MemoryInfo(memoryItems);
			if (memoryInfo.hasData()) {
				items.add(0, memoryInfo);
			}
			adapter.setItems(items, memoryInfo);
		}
	}

	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();

		int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);

		MenuItem itemReload = menu.add(0, RELOAD_ID, 1, R.string.shared_string_refresh);
		itemReload.setIcon(getIcon(R.drawable.ic_action_refresh_dark, colorId));
		itemReload.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		itemReload.setOnMenuItemClickListener(item -> {
			DownloadActivity activity = getDownloadActivity();
			if (activity != null) {
				activity.reloadLocalIndexes();
			}
			return true;
		});

		MenuItem itemSearch = menu.add(0, SEARCH_ID, 1, R.string.shared_string_search);
		itemSearch.setIcon(getIcon(R.drawable.ic_action_search_dark, colorId));
		itemSearch.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		itemSearch.setOnMenuItemClickListener(item -> {
			DownloadActivity activity = getDownloadActivity();
			if (activity != null) {
				activity.showDialog(activity, SearchDialogFragment.createInstance(""));
			}
			return true;
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		if (itemsHolder == null) {
			reloadData();
		}
		DownloadActivity activity = getDownloadActivity();
		ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
		if (actionBar != null) {
			actionBar.setTitle(R.string.maps_and_resources);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			activity.updateToolbar();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (asyncLoader != null && asyncLoader.getStatus() == AsyncTask.Status.RUNNING) {
			asyncLoader.cancel(false);
		}
	}

	private void reloadData() {
		LocalItemsLoaderTask task = asyncLoader;
		if (task == null || task.getStatus() == AsyncTask.Status.FINISHED || task.isCancelled()) {
			asyncLoader = new LocalItemsLoaderTask(app, this);
			asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	@Override
	public void loadItemsStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void loadItemsFinished(@NonNull LocalItemsHolder holder) {
		itemsHolder = holder;

		updateAdapter();
		updateProgressVisibility(false);
	}

	@Override
	public void onUpdatedIndexesList() {
		reloadData();
	}

	@Override
	public void downloadHasFinished() {
		reloadData();
	}

	@Override
	public void onGroupSelected(@NonNull LocalGroup group) {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			LocalItemsFragment.showInstance(manager, group, this);
		}
	}
}