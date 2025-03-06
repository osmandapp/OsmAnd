package net.osmand.plus.search.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.ExploreTopPlacePoint;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.search.NearbyPlacesAdapter;
import net.osmand.plus.search.NearbyPlacesAdapter.NearbyItemClickListener;
import net.osmand.plus.search.listitems.NearbyPlacesCard;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.fragments.HistoryItemsFragment;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

public class QuickSearchHistoryListFragment extends QuickSearchListFragment implements NearbyPlacesAdapter.NearbyItemClickListener, IMapLocationListener,
		OsmandMapTileView.ManualZoomListener {

	public static final int TITLE = R.string.shared_string_explore;

	private boolean selectionMode;
	private NearbyPlacesCard nearbyPlacesCard;
	private QuadRect visiblePlacesRect = new QuadRect();
	private long lastPointListRectUpdate = 0;

	public void onNearbyItemClicked(@NonNull ExploreTopPlacePoint point) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			getMyApplication().getExplorePlacesProvider().showPointInContextMenu(mapActivity, point);
			getDialogFragment().hideToolbar();
			getDialogFragment().hide();
		}
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
		nearbyPlacesCard = new NearbyPlacesCard(requireMapActivity(), this);
		getListView().addHeaderView(nearbyPlacesCard, null, false);
		getListView().addHeaderView(themedInflater.inflate(R.layout.recently_visited_header, getListView(), false));
	}

	@Override
	public void onResume() {
		super.onResume();
		nearbyPlacesCard.onResume();
		app.getOsmandMap().getMapView().addMapLocationListener(this);
		app.getOsmandMap().getMapView().addManualZoomChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		nearbyPlacesCard.onPause();
		app.getOsmandMap().getMapView().removeMapLocationListener(this);
		app.getOsmandMap().getMapView().removeManualZoomListener(this);
	}

	@Override
	public void locationChanged(double v, double v1, Object o) {
		updatePointsList();
	}

	@Override
	public void onManualZoomChange() {
		updatePointsList();
	}

	private void updatePointsList() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			long now = System.currentTimeMillis();
			RotatedTileBox tileBox = mapActivity.getMapView().getCurrentRotatedTileBox();
			QuadRect rect = tileBox.getLatLonBounds();
			RotatedTileBox extended = tileBox.copy();
			extended.increasePixelDimensions(tileBox.getPixWidth() / 4, tileBox.getPixHeight() / 4);
			QuadRect extendedRect = extended.getLatLonBounds();
			if (!extendedRect.contains(visiblePlacesRect) && now - lastPointListRectUpdate > 1000) {
				lastPointListRectUpdate = now;
				visiblePlacesRect = rect;
				nearbyPlacesCard.update();
			}
		}
	}
}
