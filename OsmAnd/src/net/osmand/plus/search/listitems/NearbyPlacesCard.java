package net.osmand.plus.search.listitems;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.NearbyPlacePoint;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.exploreplaces.ExplorePlacesFragment;
import net.osmand.plus.search.NearbyPlacesAdapter;

import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class NearbyPlacesCard extends FrameLayout implements ExplorePlacesProvider.ExplorePlacesListener {

	private boolean collapsed;
	private ImageView explicitIndicator;
	private View titleContainer;
	private RecyclerView nearByList;
	private MaterialProgressBar progressBar;
	private NearbyPlacesAdapter adapter;
	private OsmandApplication app;
	private NearbyPlacesAdapter.NearbyItemClickListener clickListener;
	private MapActivity mapActivity;
	private View noInternetCard;
	private View emptyView;
	private View cardContent;
	private boolean isLoadingItems;
	private QuadRect visiblePlacesRect;

	public NearbyPlacesCard(@NonNull MapActivity mapActivity, @NonNull NearbyPlacesAdapter.NearbyItemClickListener clickListener) {
		super(mapActivity);
		app = (OsmandApplication) mapActivity.getApplicationContext();
		this.mapActivity = mapActivity;
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
			ExplorePlacesFragment.Companion.showInstance(mapActivity.getSupportFragmentManager(), visiblePlacesRect);
		});
	}

	private void setupRecyclerView() {
		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
		nearByList.setLayoutManager(layoutManager);
		nearByList.setItemAnimator(null);
		visiblePlacesRect = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getLatLonBounds();
		adapter = new NearbyPlacesAdapter(app, app.getExplorePlacesProvider().getDataCollection(visiblePlacesRect), false, clickListener);
		nearByList.setAdapter(adapter);
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
			List<NearbyPlacePoint> nearbyData = app.getExplorePlacesProvider().getDataCollection(visiblePlacesRect);
			adapter = new NearbyPlacesAdapter(app, nearbyData, false, clickListener);
		}
		return adapter;
	}

	@Override
	public void onNewExplorePlacesDownloaded() {
		isLoadingItems = false;
		AndroidUiHelper.updateVisibility(progressBar, false);
		adapter.setItems(app.getExplorePlacesProvider().getDataCollection(visiblePlacesRect));
		adapter.notifyDataSetChanged();
		updateExpandState();
	}

	public void onResume() {
		app.getExplorePlacesProvider().addListener(this);
	}

	public void onPause() {
		app.getExplorePlacesProvider().removeListener(this);
	}

	private void onNearbyPlacesCollapseChanged() {
		if (!collapsed && app.getSettings().isInternetConnectionAvailable()) {
			startLoadingNearbyPlaces();
		}
		updateExpandState();
		app.getSettings().EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.set(collapsed);
	}

	private void startLoadingNearbyPlaces() {
		isLoadingItems = true;
		AndroidUiHelper.updateVisibility(progressBar, true);
		app.getExplorePlacesProvider().loadPlaces(
				app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getLatLonBounds(), this
		);
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
}