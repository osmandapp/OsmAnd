package net.osmand.plus.search.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.search.NearbyPlacesAdapter.NearbyItemClickListener;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.SearchVisibilityListener;
import net.osmand.plus.search.listitems.NearbyPlacesCard;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.fragments.HistoryItemsFragment;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;

import java.util.List;

public class QuickSearchHistoryListFragment extends QuickSearchListFragment implements
		SearchVisibilityListener, NearbyItemClickListener {

	public static final int TITLE = R.string.shared_string_explore;

	private boolean selectionMode;
	private NearbyPlacesCard nearbyPlacesCard;

	public void onNearbyItemClicked(@NonNull Amenity amenity) {
		SearchUICore core = app.getSearchUICore().getCore();
		SearchPhrase phrase = SearchPhrase.emptyPhrase(core.getSearchSettings());
		showResult(SearchCoreFactory.createSearchResult(amenity, phrase, MapPoiTypes.getDefault()));
	}

	@Override
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
		getListAdapter().setSelectionMode(selectionMode, position);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setOnItemLongClickListener((parent, view, position, id) -> {
			int index = position - ((ListView) parent).getHeaderViewsCount();
			QuickSearchDialogFragment dialogFragment = getDialogFragment();
			FragmentManager fragmentManager = dialogFragment.getFragmentManager();
			if (fragmentManager != null && index >= 0 && index < getListAdapter().getCount()) {
				QuickSearchListItem item = getListAdapter().getItem(index);
				if (item != null && item.getSearchResult().object instanceof HistoryEntry) {
					HistoryEntry entry = (HistoryEntry) item.getSearchResult().object;
					HistoryItemsFragment.showInstance(fragmentManager, entry.getSource(), dialogFragment);
				}
			}
			return true;
		});
		getListAdapter().setSelectionListener(new QuickSearchListAdapter.OnSelectionListener() {
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
		setupNearByCard(view);
	}

	private void setupNearByCard(@NonNull View view) {
		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), !app.getSettings().isLightContent());
		nearbyPlacesCard = new NearbyPlacesCard(requireMapActivity(), this);
		getListView().addHeaderView(nearbyPlacesCard, null, false);
		getListView().addHeaderView(themedInflater.inflate(R.layout.recently_visited_header, getListView(), false));
	}

	@Override
	public void onVisibilityChanged(boolean visible) {
		if (visible) {
			nearbyPlacesCard.onResume();
		} else {
			nearbyPlacesCard.onPause();
		}
	}
}
