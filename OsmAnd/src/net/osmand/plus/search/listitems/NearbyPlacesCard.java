package net.osmand.plus.search.listitems;

import static net.osmand.plus.download.DownloadActivityType.WIKIPEDIA_FILE;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;

import net.osmand.data.Amenity;
import net.osmand.data.DataSourceType;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.NearbyPlacesAdapter;
import net.osmand.plus.search.NearbyPlacesAdapter.NearbyItemClickListener;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.wikipedia.WikipediaPlugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class NearbyPlacesCard extends FrameLayout implements DownloadItemsAdapter.OnItemClickListener {

	private static final int DISPLAY_ITEMS = 25;
	private static final int SEARCH_POI_RADIUS = 15000;

	private static final Log log = LogFactory.getLog(NearbyPlacesCard.class);

	private final OsmandApplication app;
	private final WikipediaPlugin plugin = PluginsHelper.requirePlugin(WikipediaPlugin.class);
	private PoiUIFilter wikiFilter;

	private SearchAmenitiesTask searchAmenitiesTask;

	private boolean collapsed;
	private ImageView explicitIndicator;
	private View titleContainer;
	private RecyclerView nearByList;
	private MaterialProgressBar progressBar;
	private NearbyPlacesAdapter adapter;
	private NearbyPlacesAdapter.NearbyItemClickListener clickListener;
	private View noInternetCard;
	private View emptyView;
	private View noCardsFound;
	private View showAllBtn;
	private ShimmerFrameLayout cardContent;
	private boolean isLoadingItems;

	private RecyclerView downloadRecyclerView;
	private View downloadMapsCard;
	private DownloadItemsAdapter downloadItemsAdapter;
	private List<Object> items = new ArrayList<>();
	private DownloadIndexesThread downloadThread;
	private boolean nightMode;

	public NearbyPlacesCard(@NonNull MapActivity activity,
	                        @NonNull NearbyItemClickListener listener, boolean nightMode, boolean loadItemsOnInit) {
		super(activity);
		app = (OsmandApplication) activity.getApplicationContext();
		this.clickListener = listener;
		this.nightMode = nightMode;
		init(loadItemsOnInit);
	}

	private void init(boolean loadItemsOnInit) {
		downloadThread = app.getDownloadThread();
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		inflater.inflate(R.layout.nearby_places_card, this, true);
		progressBar = findViewById(R.id.progress_bar);
		nearByList = findViewById(R.id.nearByList);
		explicitIndicator = findViewById(R.id.explicit_indicator);
		titleContainer = findViewById(R.id.nearby_title_container);
		noInternetCard = findViewById(R.id.no_internet);
		emptyView = findViewById(R.id.empty_nearby_places);
		noCardsFound = findViewById(R.id.no_cards_found);
		showAllBtn = findViewById(R.id.show_all_button);
		cardContent = findViewById(R.id.card_content);
		downloadMapsCard = findViewById(R.id.download_maps_card);
		downloadRecyclerView = emptyView.findViewById(R.id.download_recycler_view);
		downloadRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		downloadItemsAdapter = new DownloadItemsAdapter(app, this, nightMode); // Pass 'this' as the listener
		downloadRecyclerView.setAdapter(downloadItemsAdapter);
		noInternetCard.findViewById(R.id.try_again_button).setOnClickListener((v) -> {
			if (app.getSettings().isInternetConnectionAvailable(true)) {
				startLoadingNearbyPlaces();
				updateExpandState();
			}
		});

		setupRecyclerView();
		setupShowAllNearbyPlacesBtn();
		setupExpandNearbyPlacesIndicator(loadItemsOnInit);
		updateExpandState();
	}

	private void setupShowAllNearbyPlacesBtn() {
		showAllBtn.setOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				QuickSearchDialogFragment dialogFragment = mapActivity.getFragmentsHelper().getQuickSearchDialogFragment();
				if (dialogFragment != null && getWikiFilter() != null) {
					dialogFragment.showResult(getWikiFilter());
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
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		explicitIndicator.setImageDrawable(app.getUIUtilities().getIcon(iconRes, nightMode));
		boolean nearbyPointFound = getNearbyAdapter().hasData();
		AndroidUiHelper.updateVisibility(cardContent, !collapsed && isDataSourceAvailable());
		AndroidUiHelper.updateVisibility(showAllBtn, !collapsed && nearbyPointFound);
		AndroidUiHelper.updateVisibility(noInternetCard, !collapsed && !isDataSourceAvailable());
		AndroidUiHelper.updateVisibility(emptyView, !collapsed && isDataSourceAvailable() && !nearbyPointFound && !isLoadingItems);
		if (!collapsed && !nearbyPointFound && !isLoadingItems) {
			populateDownloadItems();
		}
	}

	private NearbyPlacesAdapter getNearbyAdapter() {
		if (adapter == null) {
			adapter = new NearbyPlacesAdapter(getContext(), Collections.emptyList(), false, clickListener);
		}
		return adapter;
	}

	public void onLoadingFinished() {
		searchAmenitiesTask = null;
		adapter.setLoading(false);
		isLoadingItems = false;
		cardContent.stopShimmer();
		cardContent.hideShimmer();
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
		if (searchAmenitiesTask != null) {
			searchAmenitiesTask.cancel(false);
		}
	}

	private boolean isDataSourceAvailable() {
		OsmandSettings settings = app.getSettings();
		boolean dataSourceOnline = settings.WIKI_DATA_SOURCE_TYPE.get() == DataSourceType.ONLINE;
		return settings.isInternetConnectionAvailable() || !dataSourceOnline;
	}

	private void onNearbyPlacesCollapseChanged() {
		if (!collapsed && isDataSourceAvailable()) {
			startLoadingNearbyPlaces();
		}
		updateExpandState();
		app.getSettings().EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.set(collapsed);
	}

	private void startLoadingNearbyPlaces() {
		if (!isLoadingItems && getWikiFilter() != null) {
			isLoadingItems = true;
			adapter.setLoading(true);
			LatLon latLon = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
			searchAmenitiesTask = new SearchAmenitiesTask(getWikiFilter(), latLon, SEARCH_POI_RADIUS, amenities -> {
				amenities = amenities.subList(0, Math.min(DISPLAY_ITEMS, amenities.size()));
				updateItems(amenities);
				onLoadingFinished();
				return false;
			});
			OsmAndTaskManager.executeTask(searchAmenitiesTask);
			AndroidUiHelper.updateVisibility(progressBar, true);
			if(!getNearbyAdapter().hasData()) {
				cardContent.startShimmer();
			}
		}
	}

	@Nullable
	private PoiUIFilter getWikiFilter() {
		if (wikiFilter == null) {
			wikiFilter = plugin.getTopWikiPoiFilter();
		}
		return wikiFilter;
	}

	private void setupExpandNearbyPlacesIndicator(boolean loadItemsOnInit) {
		collapsed = app.getSettings().EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.get();
		explicitIndicator = findViewById(R.id.explicit_indicator);
		titleContainer = findViewById(R.id.nearby_title_container);
		titleContainer.setOnClickListener(v -> {
			collapsed = !collapsed;
			onNearbyPlacesCollapseChanged();
		});
		if (loadItemsOnInit) {
			onNearbyPlacesCollapseChanged();
		}
	}

	private void populateDownloadItems() {
		boolean haveWikiMapsToDownload = false;
		items.clear();
		if (downloadThread.shouldDownloadIndexes()) {
			items.add(DownloadItemsAdapter.DOWNLOADING_WIKI_MAPS_TYPE);
			downloadThread.runReloadIndexFiles();
			downloadItemsAdapter.setItems(items);
		} else {
			try {
				MapViewTrackingUtilities utilities = app.getMapViewTrackingUtilities();
				items.addAll(DownloadResources.findIndexItemsAt(app, utilities.getMapLocation(), WIKIPEDIA_FILE,
						false, -1, true));
				haveWikiMapsToDownload = !items.isEmpty();
				if (haveWikiMapsToDownload) {
					downloadItemsAdapter.setItems(items);
				} else {
					downloadItemsAdapter.setItems(Collections.emptyList());
				}
			} catch (IOException e) {
				log.error(e);
				downloadItemsAdapter.setItems(Collections.emptyList());
			}
		}
		AndroidUiHelper.updateVisibility(downloadMapsCard, downloadThread.shouldDownloadIndexes() || haveWikiMapsToDownload);
		AndroidUiHelper.updateVisibility(noCardsFound, !downloadThread.shouldDownloadIndexes() && !haveWikiMapsToDownload);
	}

	@Override
	public void onItemClick(IndexItem item) {
		if (downloadThread.isDownloading(item)) {
			downloadThread.cancelDownload(item);
		} else {
			new DownloadValidationManager(app).startDownload(getMapActivity(), item);
		}
		populateDownloadItems();
	}

	public void onUpdatedIndexesList() {
		populateDownloadItems();
		downloadItemsAdapter.notifyDataSetChanged();
	}

	public void downloadInProgress() {
		downloadItemsAdapter.notifyDataSetChanged();
	}

	public void downloadHasFinished() {
		onNearbyPlacesCollapseChanged();
		downloadItemsAdapter.notifyDataSetChanged();
	}
}