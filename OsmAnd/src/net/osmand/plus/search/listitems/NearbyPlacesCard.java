package net.osmand.plus.search.listitems;

import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.NearbyPlacesAdapter;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class NearbyPlacesCard extends FrameLayout {

	private static final int DISPLAY_ITEMS = 25;
	private static final int SEARCH_POI_RADIUS = 15000;
	private static final Log log = LogFactory.getLog(NearbyPlacesCard.class);
	private boolean collapsed;
	private ImageView explicitIndicator;
	private View titleContainer;
	private RecyclerView nearByList;
	private MaterialProgressBar progressBar;
	private NearbyPlacesAdapter adapter;
	private OsmandApplication app;
	private NearbyPlacesAdapter.NearbyItemClickListener clickListener;
	private View noInternetCard;
	private View emptyView;
	private View cardContent;
	private boolean isLoadingItems;
	private SearchAmenitiesTask loadTask;
	private PoiUIFilter wikiFilter;

	public NearbyPlacesCard(@NonNull MapActivity mapActivity, @NonNull NearbyPlacesAdapter.NearbyItemClickListener clickListener) {
		super(mapActivity);
		app = (OsmandApplication) mapActivity.getApplicationContext();
		this.clickListener = clickListener;
		init();
	}

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.nearby_places_card, this, true);
		progressBar = findViewById(R.id.progress_bar);
		nearByList = findViewById(R.id.nearByList);
		explicitIndicator = findViewById(R.id.explicit_indicator);
		titleContainer = findViewById(R.id.nearby_title_container);
		noInternetCard = findViewById(R.id.no_internet);
		emptyView = findViewById(R.id.empty_nearby_places);
		cardContent = findViewById(R.id.card_content);
		noInternetCard.findViewById(R.id.try_again_button).setOnClickListener((v) -> {
			if (app.getSettings().isInternetConnectionAvailable(true)) {
				startLoadingNearbyPlaces();
				updateExpandState();
			}
		});

		setupRecyclerView();
		setupShowAllNearbyPlacesBtn();
		setupExpandNearbyPlacesIndicator();
		updateExpandState();
	}

	private void setupShowAllNearbyPlacesBtn() {
		findViewById(R.id.show_all_btn).setOnClickListener(v -> {
			MapActivity activity = getMapActivity();
			if (activity != null) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					QuickSearchDialogFragment dialogFragment = mapActivity.getFragmentsHelper().getQuickSearchDialogFragment();
					if (dialogFragment != null) {
						dialogFragment.showResult(wikiFilter);
					}
				}
			}
		});
	}

	@Nullable
	private MapActivity getMapActivity() {
		return app.getOsmandMap().getMapView().getMapActivity();
	}

	private void setupRecyclerView() {
		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
		nearByList.setLayoutManager(layoutManager);
		nearByList.setItemAnimator(null);
		adapter = new NearbyPlacesAdapter(getContext(), Collections.emptyList(), false, clickListener);
		nearByList.setAdapter(adapter);
	}

	public void update() {
		startLoadingNearbyPlaces();
	}

	private void updateExpandState() {
		int iconRes = collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up;
		explicitIndicator.setImageDrawable(app.getUIUtilities().getIcon(iconRes, !app.getSettings().isLightContent()));
		boolean internetAvailable = app.getSettings().isInternetConnectionAvailable();
		boolean nearbyPointFound = getNearbyAdapter().getItemCount() > 0;
		AndroidUiHelper.updateVisibility(cardContent, !collapsed && nearbyPointFound && internetAvailable);
		AndroidUiHelper.updateVisibility(noInternetCard, !collapsed && !internetAvailable);
		AndroidUiHelper.updateVisibility(emptyView, !collapsed && internetAvailable && !nearbyPointFound && !isLoadingItems);
	}

	private NearbyPlacesAdapter getNearbyAdapter() {
		if (adapter == null) {
			adapter = new NearbyPlacesAdapter(getContext(), Collections.emptyList(), false, clickListener);
		}
		return adapter;
	}

	public void onLoadingFinished() {
		loadTask = null;
		isLoadingItems = false;
		AndroidUiHelper.updateVisibility(progressBar, false);
		updateExpandState();
	}

	private void updateItems(List<Amenity> amenities) {
		adapter.setItems(amenities);
		adapter.notifyDataSetChanged();
		updateExpandState();
	}

	public void onResume() {
		if (!collapsed) {
			startLoadingNearbyPlaces();
		}
	}

	public void onPause() {
		AsyncTask task = loadTask;
		if (task != null) {
			task.cancel(false);
		}
	}

	private void onNearbyPlacesCollapseChanged() {
		if (!collapsed && app.getSettings().isInternetConnectionAvailable()) {
			startLoadingNearbyPlaces();
		}
		updateExpandState();
		app.getSettings().EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.set(collapsed);
	}

	private void startLoadingNearbyPlaces() {
		if (!isLoadingItems) {
			isLoadingItems = true;
			LatLon latLon = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
			loadTask = new SearchAmenitiesTask(getFilter(), latLon);
			loadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			AndroidUiHelper.updateVisibility(progressBar, true);
		}
	}

	private PoiUIFilter getFilter() {
		if (wikiFilter == null) {
			WikipediaPlugin plugin = PluginsHelper.getPlugin(WikipediaPlugin.class);
			wikiFilter = plugin.getTopWikiPoiFilter();
		}
		return wikiFilter;
	}

	private void setupExpandNearbyPlacesIndicator() {
		collapsed = app.getSettings().EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.get();
		explicitIndicator = findViewById(R.id.explicit_indicator);
		titleContainer = findViewById(R.id.nearby_title_container);
		titleContainer.setOnClickListener(v -> {
			collapsed = !collapsed;
			onNearbyPlacesCollapseChanged();
		});
		onNearbyPlacesCollapseChanged();
	}
	
	private class SearchAmenitiesTask extends AsyncTask<Void, Void, List<Amenity>> {

		private final LatLon latLon;
		private final PoiUIFilter filter;

		protected SearchAmenitiesTask(@NonNull PoiUIFilter filter, @NonNull LatLon latLon) {
			this.filter = filter;
			this.latLon = latLon;
		}

		@Override
		protected List<Amenity> doInBackground(Void... params) {
			QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), SEARCH_POI_RADIUS);
			List<Amenity> amenities = getAmenities(rect);
			return amenities.subList(0, Math.min(DISPLAY_ITEMS, amenities.size()));
		}

		@NonNull
		private List<Amenity> getAmenities(@NonNull QuadRect rect) {
			return filter.searchAmenities(rect.top, rect.left, rect.bottom, rect.right, -1, null);
		}

		@Override
		protected void onPostExecute(List<Amenity> amenities) {
			if (isCancelled()) {
				return;
			}
			updateItems(amenities);
			onLoadingFinished();
		}
	}
}