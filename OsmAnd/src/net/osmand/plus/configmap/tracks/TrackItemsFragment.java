package net.osmand.plus.configmap.tracks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.track.BaseTracksTabsFragment;
import net.osmand.util.MapUtils;

import java.util.Set;

public class TrackItemsFragment extends BaseOsmAndFragment implements OsmAndCompassListener, OsmAndLocationListener, TrackItemsContainer {

	public static final String TAG = TrackItemsFragment.class.getSimpleName();

	private static final String TRACK_TAB_NAME_KEY = "track_tab_name_key";

	private String trackTabName;
	private TracksAdapter adapter;
	private RecyclerView recyclerView;

	private Location location;
	private Float heading;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;


	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.recycler_view_fragment, container, false);
		view.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light));

		recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setItemAnimator(null);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		TrackTab trackTab = getTrackTab();
		if (trackTab != null) {
			setupAdapter(trackTab);
		}
		return view;
	}

	private void setupAdapter(@NonNull TrackTab trackTab) {
		BaseTracksTabsFragment fragment = (BaseTracksTabsFragment) requireParentFragment();
		adapter = new TracksAdapter(requireContext(), trackTab, fragment, nightMode);
		adapter.setSelectionMode(fragment.selectionMode());
		recyclerView.setAdapter(adapter);
	}

	@Nullable
	public TrackTab getTrackTab() {
		BaseTracksTabsFragment fragment = (BaseTracksTabsFragment) requireParentFragment();
		return fragment.getTab(trackTabName);
	}

	public void setTrackTab(@NonNull TrackTab trackTab) {
		this.trackTabName = trackTab.getTypeName();
	}

	@Override
	public void updateItems(@NonNull Set<TrackItem> trackItems) {
		if (adapter != null) {
			adapter.updateItems(trackItems);
		}
	}

	@Override
	public void updateContent() {
		TrackTab trackTab = getTrackTab();
		if (adapter != null) {
			adapter.setTrackTab(trackTab);
		} else if (trackTab != null) {
			setupAdapter(trackTab);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(TRACK_TAB_NAME_KEY, trackTabName);
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private void updateLocationUi() {
		if (compassUpdateAllowed && adapter != null) {
			app.runInUIThread(() -> {
				if (location == null) {
					location = app.getLocationProvider().getLastKnownLocation();
				}
				adapter.notifyDataSetChanged();
			});
		}
	}

	public void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
			locationProvider.addCompassListener(this);
			locationProvider.addLocationListener(this);
			updateLocationUi();
		}
	}

	public void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeLocationListener(this);
			locationProvider.removeCompassListener(this);
			locationProvider.addCompassListener(locationProvider.getNavigationInfo());
		}
	}
}