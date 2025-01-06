package net.osmand.plus.search.dialogs;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.QuadRect;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.search.GetNearbyImagesTask;
import net.osmand.plus.search.NearbyAdapter;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.fragments.HistoryItemsFragment;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import net.osmand.wiki.WikiCoreHelper;

import java.util.Collections;
import java.util.List;

public class QuickSearchHistoryListFragment extends QuickSearchListFragment implements NearbyAdapter.NearbyItemClickListener {

	public static final int TITLE = R.string.shared_string_explore;

	private boolean selectionMode;
	private View nearByContainer;
	private RecyclerView nearByList;
	private NearbyAdapter adapter = null;

	public void onNearbyItemClicked(@NonNull WikiCoreHelper.OsmandApiFeatureData item) {

	}

	private final GetNearbyImagesTask.GetImageCardsListener imageCardListener = new GetNearbyImagesTask.GetImageCardsListener() {
		@Override
		public void onTaskStarted() {

		}

		@Override
		public void onFinish(@NonNull List<? extends WikiCoreHelper.OsmandApiFeatureData> result) {
			updateNearbyItems(result);
		}
	};

	private void updateNearbyItems(@NonNull List<? extends WikiCoreHelper.OsmandApiFeatureData> result) {
		if (nearByContainer != null) {
			nearByContainer.setVisibility(result.isEmpty() ? View.GONE : View.VISIBLE);
		}
		if (adapter != null) {
			adapter.setItems(result);
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
		adapter = new NearbyAdapter(app, Collections.emptyList(), this);
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
		startLoadingNearestPhotos();
		setupNearByCard(view);
	}

	private void setupNearByCard(@NonNull View view) {
		nearByContainer = view.findViewById(R.id.nearBy_container);
		nearByList = view.findViewById(R.id.nearByList);
		LinearLayoutManager layoutManager = new LinearLayoutManager(app);
		layoutManager.setOrientation(RecyclerView.HORIZONTAL);
		nearByList.setLayoutManager(layoutManager);
		nearByList.setItemAnimator(null);
		nearByList.setAdapter(adapter);
	}

	private void startLoadingNearestPhotos() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		QuadRect mapRect = mapView.getCurrentRotatedTileBox().getLatLonBounds();
		String preferredLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		if (Algorithms.isEmpty(preferredLang)) {
			preferredLang = app.getLanguage();
		}
		(new GetNearbyImagesTask(mapRect, mapView.getZoom(), preferredLang, imageCardListener)).execute();
	}
}
