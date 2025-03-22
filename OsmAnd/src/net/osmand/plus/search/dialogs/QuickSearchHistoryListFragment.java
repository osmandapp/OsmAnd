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
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.search.NearbyPlacesAdapter;
import net.osmand.plus.search.listitems.NearbyPlacesCard;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.fragments.HistoryItemsFragment;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView.MapZoomChangeListener;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;

import java.util.List;

public class QuickSearchHistoryListFragment extends QuickSearchListFragment implements NearbyPlacesAdapter.NearbyItemClickListener, IMapLocationListener,
		MapZoomChangeListener, DownloadIndexesThread.DownloadEvents {

	public static final int TITLE = R.string.shared_string_explore;

	private boolean selectionMode;
	private NearbyPlacesCard nearbyPlacesCard;
	private QuadRect visiblePlacesRect = new QuadRect();
	private long lastPointListRectUpdate = 0;

	@Override
	public void onUpdatedIndexesList() {
		if(nearbyPlacesCard != null) {
			nearbyPlacesCard.onUpdatedIndexesList();
		}
	}

	@Override
	public void downloadHasFinished() {
		if(nearbyPlacesCard != null) {
			nearbyPlacesCard.downloadHasFinished();
		}
	}

	@Override
	public void downloadInProgress() {
		if(nearbyPlacesCard != null) {
			nearbyPlacesCard.downloadInProgress();
		}
	}

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
			QuickSearchDialogFragment dialogFragment = getDialogFragment();
			FragmentManager fragmentManager = dialogFragment.getFragmentManager();
			if (fragmentManager != null) {
				QuickSearchListItem item = getListAdapter().getItem(position);
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
		nearbyPlacesCard = new NearbyPlacesCard(requireMapActivity(), this, ((QuickSearchDialogFragment) getParentFragment()).isNightMode());
		getListView().addHeaderView(nearbyPlacesCard, null, false);
		getListView().addHeaderView(themedInflater.inflate(R.layout.recently_visited_header, getListView(), false));
	}

	@Override
	public void onResume() {
		super.onResume();
		nearbyPlacesCard.onResume();
		app.getOsmandMap().getMapView().addMapLocationListener(this);
		app.getOsmandMap().getMapView().addMapZoomChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		nearbyPlacesCard.onPause();
		app.getOsmandMap().getMapView().removeMapLocationListener(this);
		app.getOsmandMap().getMapView().removeMapZoomChangeListener(this);
	}

	@Override
	public void locationChanged(double v, double v1, Object o) {
		updatePointsList();
	}

	@Override
	public void onMapZoomChanged(boolean manual) {
		if (manual) {
			updatePointsList();
		}
	}

	private void updatePointsList() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			long now = System.currentTimeMillis();
			RotatedTileBox tileBox = mapActivity.getMapView().getRotatedTileBox();
			QuadRect rect = tileBox.getLatLonBounds();
			tileBox.increasePixelDimensions(tileBox.getPixWidth() / 4, tileBox.getPixHeight() / 4);
			QuadRect extendedRect = tileBox.getLatLonBounds();
			if (!extendedRect.contains(visiblePlacesRect) && now - lastPointListRectUpdate > 1000) {
				lastPointListRectUpdate = now;
				visiblePlacesRect = rect;
				nearbyPlacesCard.update();
			}
		}
	}
}
