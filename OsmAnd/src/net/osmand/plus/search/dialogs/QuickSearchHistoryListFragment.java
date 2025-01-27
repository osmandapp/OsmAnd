package net.osmand.plus.search.dialogs;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.nearbyplaces.NearbyPlacesFragment;
import net.osmand.plus.nearbyplaces.NearbyPlacesHelper;
import net.osmand.plus.nearbyplaces.NearbyPlacesListener;
import net.osmand.plus.search.NearbyPlacesAdapter;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.fragments.HistoryItemsFragment;
import net.osmand.wiki.WikiCoreHelper;
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData;

import java.util.List;

public class QuickSearchHistoryListFragment extends QuickSearchListFragment implements NearbyPlacesAdapter.NearbyItemClickListener, NearbyPlacesListener {

	public static final int TITLE = R.string.shared_string_explore;

	private boolean selectionMode;
	private View nearByContainer;
	private RecyclerView nearByList;
	private NearbyPlacesAdapter adapter;
	private ImageView explicitIndicator;
	private View titleContainer;
	private boolean collapsed;
	private View showAllBtnContainer;
	private View progressBar;

	public void onNearbyItemClicked(@NonNull WikiCoreHelper.OsmandApiFeatureData item) {

	}

	private void updateNearbyItems() {
		if (progressBar != null) {
			AndroidUiHelper.updateVisibility(progressBar, false);
		}
		List<OsmandApiFeatureData> nearbyData = NearbyPlacesHelper.INSTANCE.getDataCollection();
		getNearbyAdapter().setItems(nearbyData);
		getNearbyAdapter().notifyDataSetChanged();
		updateExpandState();
	}

	private NearbyPlacesAdapter getNearbyAdapter() {
		if (adapter == null) {
			List<OsmandApiFeatureData> nearbyData = NearbyPlacesHelper.INSTANCE.getDataCollection();
			adapter = new NearbyPlacesAdapter(getMyApplication(), nearbyData, false, this);
		}
		return adapter;
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
		progressBar = view.findViewById(R.id.progress_bar);
		setupShowAllNearbyPlacesBtn(view);
		setupExpandNearbyPlacesIndicator(view);
		setupNearByCard(view);
		updateExpandState();
	}

	private void setupShowAllNearbyPlacesBtn(@NonNull View view) {
		showAllBtnContainer = view.findViewById(R.id.show_all_button);
		view.findViewById(R.id.show_all_btn).setOnClickListener(v -> {
			MapActivity activity = getMapActivity();
			if (activity != null) {
				NearbyPlacesFragment.Companion.showInstance(activity.getSupportFragmentManager());
				getDialogFragment().hide();
			}
		});
	}

	private void setupExpandNearbyPlacesIndicator(@NonNull View view) {
		collapsed = app.getSettings().EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.get();
		explicitIndicator = view.findViewById(R.id.explicit_indicator);
		titleContainer = view.findViewById(R.id.nearby_title_container);
		titleContainer.setOnClickListener(v -> {
			collapsed = !collapsed;
			onNearbyPlacesCollapseChanged();
		});
		onNearbyPlacesCollapseChanged();
	}

	private void onNearbyPlacesCollapseChanged() {
		updateExpandState();
		if (!collapsed) {
			AndroidUiHelper.updateVisibility(progressBar, true);
			NearbyPlacesHelper.INSTANCE.startLoadingNearestPhotos();
		}
		app.getSettings().EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.set(collapsed);
	}

	private void setupNearByCard(@NonNull View view) {
		nearByContainer = view.findViewById(R.id.nearBy_container);
		nearByList = view.findViewById(R.id.nearByList);
		LinearLayoutManager layoutManager = new LinearLayoutManager(app);
		layoutManager.setOrientation(RecyclerView.HORIZONTAL);
		nearByList.setLayoutManager(layoutManager);
		nearByList.setItemAnimator(null);
		nearByList.setAdapter(getNearbyAdapter());
	}

	@Override
	public void onResume() {
		super.onResume();
		NearbyPlacesHelper.INSTANCE.addListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		NearbyPlacesHelper.INSTANCE.removeListener(this);
	}

	@Override
	public void onNearbyPlacesUpdated() {
		updateNearbyItems();
	}

	private void updateExpandState() {
		int iconRes = collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up;
		explicitIndicator.setImageDrawable(app.getUIUtilities().getIcon(iconRes, !app.getSettings().isLightContent()));
		AndroidUiHelper.updateVisibility(nearByList, !collapsed);
		AndroidUiHelper.updateVisibility(showAllBtnContainer, !collapsed);
	}
}
