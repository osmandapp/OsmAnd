package net.osmand.plus.settings.fragments;

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
import net.osmand.plus.search.ShareHistoryAsyncTask;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.search.core.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchHistorySettingsFragment extends HistoryItemsFragment {

	public static final String TAG = SearchHistorySettingsFragment.class.getSimpleName();

	@Override
	protected void setupWarningCard(@NonNull View view) {
		super.setupWarningCard(view);
		TextView warning = warningCard.findViewById(R.id.title);
		warning.setText(getString(R.string.is_disabled, getString(R.string.shared_string_search_history)));
	}

	@Override
	protected void updateHistoryItems() {
		clearItems();
		SearchHistoryHelper historyHelper = SearchHistoryHelper.getInstance(app);
		List<SearchResult> searchResults = historyHelper.getHistoryResults(HistorySource.SEARCH, false, true);
		sortSearchResults(searchResults);

		Map<Integer, List<SearchResult>> historyGroups = new HashMap<>();
		List<Pair<Long, SearchResult>> pairs = createHistoryPairsByDate(searchResults);

		HistoryAdapter.createHistoryGroups(pairs, historyGroups, items);
		for (Map.Entry<Integer, List<SearchResult>> entry : historyGroups.entrySet()) {
			itemsGroups.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
	}

	protected static List<Pair<Long, SearchResult>> createHistoryPairsByDate(@NonNull List<SearchResult> searchResults) {
		List<Pair<Long, SearchResult>> pairs = new ArrayList<>();
		for (SearchResult searchResult : searchResults) {
			long lastAccessedTime = 0;
			HistoryEntry historyEntry = HistorySettingsFragment.getHistoryEntry(searchResult);
			if (historyEntry != null) {
				lastAccessedTime = historyEntry.getLastAccessTime();
			}
			pairs.add(new Pair<>(lastAccessedTime, searchResult));
		}
		return pairs;
	}

	protected static void sortSearchResults(@NonNull List<SearchResult> searchResults) {
		Collections.sort(searchResults, (o1, o2) -> {
			long lastTime1 = 0;
			HistoryEntry historyEntry1 = HistorySettingsFragment.getHistoryEntry(o1);
			if (historyEntry1 != null) {
				lastTime1 = historyEntry1.getLastAccessTime();
			}
			long lastTime2 = 0;
			HistoryEntry historyEntry2 = HistorySettingsFragment.getHistoryEntry(o2);
			if (historyEntry2 != null) {
				lastTime2 = historyEntry2.getLastAccessTime();
			}
			return Long.compare(lastTime2, lastTime1);
		});
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);

		View toolbarContainer = view.findViewById(R.id.toolbar);
		TextView title = toolbarContainer.findViewById(R.id.toolbar_title);
		title.setText(R.string.shared_string_search_history);

		toolbarContainer.findViewById(R.id.toolbar_switch_container).setOnClickListener(view1 -> {
			boolean checked = !settings.SEARCH_HISTORY.get();
			settings.SEARCH_HISTORY.set(checked);
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
		SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
		for (Object item : selectedItems) {
			if (item instanceof SearchResult) {
				SearchResult searchResult = (SearchResult) item;
				helper.remove(searchResult.object);
			}
		}
	}

	@Override
	protected boolean isHistoryEnabled() {
		return settings.SEARCH_HISTORY.get();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SearchHistorySettingsFragment fragment = new SearchHistorySettingsFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}

