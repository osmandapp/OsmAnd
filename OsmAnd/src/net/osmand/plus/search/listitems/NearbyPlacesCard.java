package net.osmand.plus.search.listitems;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.nearbyplaces.NearbyPlacesFragment;
import net.osmand.plus.nearbyplaces.NearbyPlacesHelper;
import net.osmand.plus.nearbyplaces.NearbyPlacesListener;
import net.osmand.plus.search.NearbyPlacesAdapter;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.wiki.WikiCoreHelper;

import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class NearbyPlacesCard extends FrameLayout implements NearbyPlacesListener {

	private boolean collapsed;
	private ImageView explicitIndicator;
	private View titleContainer;
	private View showAllBtnContainer;
	private RecyclerView nearByList;
	private MaterialProgressBar progressBar;
	private NearbyPlacesAdapter adapter;
	private OsmandApplication app;
	private NearbyPlacesAdapter.NearbyItemClickListener clickListener;

	public NearbyPlacesCard(@NonNull Context context, @NonNull NearbyPlacesAdapter.NearbyItemClickListener clickListener) {
		super(context);
		app = (OsmandApplication) context.getApplicationContext();
		this.clickListener = clickListener;
		init();
	}

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.nearby_places_card, this, true);
		progressBar = findViewById(R.id.progress_bar);
		nearByList = findViewById(R.id.nearByList);
		explicitIndicator = findViewById(R.id.explicit_indicator);
		titleContainer = findViewById(R.id.nearby_title_container);
		showAllBtnContainer = findViewById(R.id.show_all_button);

		setupRecyclerView();
		setupShowAllNearbyPlacesBtn();
		setupExpandNearbyPlacesIndicator();
		updateExpandState();
	}

	private void setupShowAllNearbyPlacesBtn() {
		showAllBtnContainer = findViewById(R.id.show_all_button);
		findViewById(R.id.show_all_btn).setOnClickListener(v -> {
			MapActivity activity = getMapActivity();
			if (activity != null) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					NearbyPlacesFragment.Companion.showInstance(activity.getSupportFragmentManager());
					QuickSearchDialogFragment dialogFragment = mapActivity.getFragmentsHelper().getQuickSearchDialogFragment();
					if (dialogFragment != null) {
						dialogFragment.hide();
					}
				}
			}
		});
	}

	private void setupRecyclerView() {
		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
		nearByList.setLayoutManager(layoutManager);
		nearByList.setItemAnimator(null);
		adapter = new NearbyPlacesAdapter(app, NearbyPlacesHelper.INSTANCE.getDataCollection(), false, clickListener);
		nearByList.setAdapter(adapter);
	}

	private void updateExpandState() {
		int iconRes = collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up;
		explicitIndicator.setImageDrawable(app.getUIUtilities().getIcon(iconRes, !app.getSettings().isLightContent()));
		AndroidUiHelper.updateVisibility(nearByList, !collapsed);
		AndroidUiHelper.updateVisibility(showAllBtnContainer, !collapsed && getNearbyAdapter().getItemCount() > 0);
	}

	public void updateNearbyItems() {
		AndroidUiHelper.updateVisibility(progressBar, false);
		adapter.setItems(NearbyPlacesHelper.INSTANCE.getDataCollection());
		adapter.notifyDataSetChanged();
		updateExpandState();
	}

	private NearbyPlacesAdapter getNearbyAdapter() {
		if (adapter == null) {
			List<WikiCoreHelper.OsmandApiFeatureData> nearbyData = NearbyPlacesHelper.INSTANCE.getDataCollection();
			adapter = new NearbyPlacesAdapter(app, nearbyData, false, clickListener);
		}
		return adapter;
	}

	@Nullable
	private MapActivity getMapActivity() {
		return app.getOsmandMap().getMapView().getMapActivity();
	}

	@Override
	public void onNearbyPlacesUpdated() {
		updateNearbyItems();
	}

	public void onResume() {
		NearbyPlacesHelper.INSTANCE.addListener(this);
	}

	public void onPause() {
		NearbyPlacesHelper.INSTANCE.removeListener(this);
	}

	private void onNearbyPlacesCollapseChanged() {
		updateExpandState();
		if (!collapsed) {
			AndroidUiHelper.updateVisibility(progressBar, true);
			NearbyPlacesHelper.INSTANCE.startLoadingNearestPhotos();
		}
		app.getSettings().EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED.set(collapsed);
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