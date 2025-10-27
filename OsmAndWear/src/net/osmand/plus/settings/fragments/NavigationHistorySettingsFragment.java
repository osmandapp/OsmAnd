package net.osmand.plus.settings.fragments;


import static net.osmand.plus.settings.fragments.HistoryAdapter.PREVIOUS_ROUTE_HEADER;
import static net.osmand.plus.settings.fragments.SearchHistorySettingsFragment.createHistoryPairsByDate;
import static net.osmand.plus.settings.fragments.SearchHistorySettingsFragment.sortSearchResults;

import android.os.AsyncTask;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.search.ShareHistoryAsyncTask;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.search.core.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigationHistorySettingsFragment extends HistoryItemsFragment {

	public static final String TAG = NavigationHistorySettingsFragment.class.getSimpleName();

	@Override
	protected void setupWarningCard(@NonNull View view) {
		super.setupWarningCard(view);
		TextView warning = warningCard.findViewById(R.id.title);
		warning.setText(getString(R.string.is_disabled, getString(R.string.navigation_history)));
	}

	@Override
	protected void updateHistoryItems() {
		clearItems();
		SearchHistoryHelper historyHelper = SearchHistoryHelper.getInstance(app);
		List<SearchResult> searchResults = historyHelper.getHistoryResults(HistorySource.NAVIGATION, true, true);
		sortSearchResults(searchResults);

		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		TargetPoint startBackup = targetPointsHelper.getPointToStartBackup();
		if (startBackup == null) {
			startBackup = targetPointsHelper.getMyLocationToStart();
		}
		TargetPoint destinationBackup = targetPointsHelper.getPointToNavigateBackup();
		if (startBackup != null && destinationBackup != null) {
			items.add(PREVIOUS_ROUTE_HEADER);
			items.add(destinationBackup);
			itemsGroups.put(PREVIOUS_ROUTE_HEADER, new ArrayList<>(Collections.singleton(destinationBackup)));
		}

		Map<Integer, List<SearchResult>> historyGroups = new HashMap<>();
		List<Pair<Long, SearchResult>> pairs = createHistoryPairsByDate(searchResults);

		HistoryAdapter.createHistoryGroups(pairs, historyGroups, items);
		for (Map.Entry<Integer, List<SearchResult>> entry : historyGroups.entrySet()) {
			itemsGroups.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);

		View toolbarContainer = view.findViewById(R.id.toolbar);

		TextView title = toolbarContainer.findViewById(R.id.toolbar_title);
		title.setText(R.string.navigation_history);

		toolbarContainer.findViewById(R.id.toolbar_switch_container).setOnClickListener(view1 -> {
			boolean checked = !settings.NAVIGATION_HISTORY.get();
			settings.NAVIGATION_HISTORY.set(checked);
			updateToolbarSwitch(toolbarContainer);
			updateDisabledItems();

			Fragment target = getTargetFragment();
			if (target instanceof OnPreferenceChanged) {
				((OnPreferenceChanged) target).onPreferenceChanged(settings.SEARCH_HISTORY.getId());
			}
		});
	}

	@Override
	protected void shareItems() {
		List<HistoryEntry> historyEntries = new ArrayList<>();
		for (Object item : selectedItems) {
			if (item instanceof SearchResult) {
				HistoryEntry historyEntry = HistorySettingsFragment.getHistoryEntry((SearchResult) item);
				if (historyEntry != null) {
					historyEntries.add(historyEntry);
				}
			}
		}
		ShareHistoryAsyncTask exportTask = new ShareHistoryAsyncTask(app, historyEntries, null);
		exportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	protected void deleteSelectedItems() {
		boolean clearBackupPoints = false;
		SearchHistoryHelper historyHelper = SearchHistoryHelper.getInstance(app);
		for (Object item : selectedItems) {
			if (item instanceof SearchResult) {
				SearchResult searchResult = (SearchResult) item;
				historyHelper.remove(searchResult.object);
			} else if (item instanceof TargetPoint) {
				clearBackupPoints = true;
			}
		}
		if (clearBackupPoints) {
			app.getTargetPointsHelper().clearBackupPoints();
		}
	}

	@Override
	protected boolean isHistoryEnabled() {
		return settings.NAVIGATION_HISTORY.get();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			NavigationHistorySettingsFragment fragment = new NavigationHistorySettingsFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}


