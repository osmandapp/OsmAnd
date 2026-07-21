package net.osmand.plus.search.dialogs;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.search.history.HistoryEntry;
import net.osmand.plus.search.NearbyPlacesAdapter.NearbyItemClickListener;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.SearchVisibilityListener;
import net.osmand.plus.search.listitems.NearbyPlacesCard;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.fragments.HistoryItemsFragment;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;

import java.util.List;


public class QuickSearchHistoryListFragment extends QuickSearchListFragment implements SearchVisibilityListener,
		NearbyItemClickListener, DownloadIndexesThread.DownloadEvents {

	public static final int TITLE = R.string.shared_string_explore;

	private boolean selectionMode;
	private NearbyPlacesCard nearbyPlacesCard;
	private boolean historyCollapsed;
	private View historyTitleContainer;
	private ImageView historyCollapseIndicator;

	@Override
	public void onUpdatedIndexesList() {
		if (nearbyPlacesCard != null) {
			nearbyPlacesCard.onUpdatedIndexesList();
		}
	}

	@Override
	public void downloadHasFinished() {
		if (nearbyPlacesCard != null) {
			nearbyPlacesCard.downloadHasFinished();
		}
	}

	@Override
	public void downloadInProgress() {
		if (nearbyPlacesCard != null) {
			nearbyPlacesCard.downloadInProgress();
		}
	}

	public void onNearbyItemClicked(@NonNull Amenity amenity) {
		SearchUICore core = app.getSearchUICore().getCore();
		SearchPhrase phrase = SearchPhrase.emptyPhrase(core.getSearchSettings());
		showResult(SearchCoreFactory.createSearchResult(amenity, phrase, MapPoiTypes.getDefault()));
	}

	@Override
	@NonNull
	public SearchListFragmentType getType() {
		return SearchListFragmentType.HISTORY;
	}

	@LayoutRes
	protected int getLayoutId() {
		return R.layout.search_explore_fragment_layout;
	}

	public boolean isSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(boolean selectionMode, int position) {
		this.selectionMode = selectionMode;
		QuickSearchListAdapter adapter = getListAdapter();
		if (adapter != null) {
			adapter.setSelectionMode(selectionMode, position);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		QuickSearchListAdapter adapter = getListAdapter();
		if (adapter != null) {
			adapter.setExploreHistoryCard(true);
			adapter.setSelectionListener(new QuickSearchListAdapter.OnSelectionListener() {
				@Override
				public void onUpdateSelectionMode(List<QuickSearchListItem> selectedItems) {
					getDialogFragment().updateSelectionMode(selectedItems);
				}

				@Override
				public void reloadData() {
					getDialogFragment().reloadHistory();
				}
			});
		}
		historyCollapsed = app.getSettings().EXPLORE_HISTORY_ROW_COLLAPSED.get();
		updateHistoryCollapseIndicator();
		getListView().setOnItemLongClickListener((parent, view, position, id) -> {
			int index = position - ((ListView) parent).getHeaderViewsCount();
			QuickSearchDialogFragment dialogFragment = getDialogFragment();
			FragmentManager fragmentManager = dialogFragment.getFragmentManager();
			if (fragmentManager != null && index >= 0 && index < getListAdapter().getCount()) {
				QuickSearchListItem item = getListAdapter().getItem(index);
				if (item != null) {
					HistoryEntry entry = getEntryFromSearchItem(item);
					if (entry != null) {
						HistoryItemsFragment.showInstance(fragmentManager, entry.getSource(), dialogFragment);
					}
				}
			}
			return true;
		});
	}

	@Nullable
	private HistoryEntry getEntryFromSearchItem(@NonNull QuickSearchListItem item) {
		SearchResult searchResult = item.getSearchResult();
		if (searchResult != null) {
			if (searchResult.object instanceof HistoryEntry historyEntry) {
				return historyEntry;
			}
			if (searchResult.relatedObject instanceof HistoryEntry historyEntry) {
				return historyEntry;
			}
		}
		return null;
	}

	@Override
	public void onListItemClick(@NonNull ListView listView, @NonNull View view, int position, long id) {
		if (selectionMode) {
			CheckBox ch = view.findViewById(R.id.toggle_item);
			ch.setChecked(!ch.isChecked());
			getListAdapter().toggleCheckbox(position - listView.getHeaderViewsCount(), ch);
		} else {
			super.onListItemClick(listView, view, position, id);
		}
	}

	@Override
	public void updateListAdapter(List<QuickSearchListItem> listItems, boolean append, boolean addShadows) {
		super.updateListAdapter(listItems, append, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		QuickSearchDialogFragment dialogFragment = (QuickSearchDialogFragment) getParentFragment();
		nearbyPlacesCard = new NearbyPlacesCard(requireMapActivity(), this, nightMode, !dialogFragment.isSearchHidden());
		getListView().addHeaderView(nearbyPlacesCard, null, false);
		View historyHeader = inflate(R.layout.recently_visited_header, getListView(), false);
		historyTitleContainer = historyHeader.findViewById(R.id.history_title_container);
		historyCollapseIndicator = historyHeader.findViewById(R.id.explicit_indicator);
		historyHeader.setOnClickListener(v -> {
			historyCollapsed = !historyCollapsed;
			app.getSettings().EXPLORE_HISTORY_ROW_COLLAPSED.set(historyCollapsed);
			updateHistoryCollapseIndicator();
			getDialogFragment().reloadHistory();
		});
		updateHistoryCollapseIndicator();
		getListView().addHeaderView(historyHeader, null, false);
	}

	public boolean isHistoryExpanded() {
		return !historyCollapsed;
	}

	private void updateHistoryCollapseIndicator() {
		if (historyTitleContainer != null) {
			historyTitleContainer.setBackgroundResource(historyCollapsed
					? R.drawable.bg_list_card_round
					: R.drawable.bg_list_card_top_round);
		}
		if (historyCollapseIndicator != null) {
			int iconRes = historyCollapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up;
			historyCollapseIndicator.setImageDrawable(app.getUIUtilities().getIcon(iconRes, nightMode));
		}
	}

	@Override
	public void onVisibilityChanged(boolean visible) {
		if (visible) {
			nearbyPlacesCard.onResume();
		} else {
			nearbyPlacesCard.onPause();
		}
	}

	@Override
	protected boolean needToSeparateTopItemsInList() {
		return false;
	}
}
