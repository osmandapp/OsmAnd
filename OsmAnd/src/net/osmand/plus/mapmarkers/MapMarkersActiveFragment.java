package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.mapmarkers.adapters.MapMarkersActiveAdapter;
import net.osmand.plus.mapmarkers.adapters.MapMarkersActiveAdapter.MapMarkersActiveAdapterListener;

public class MapMarkersActiveFragment extends Fragment implements OsmAndCompassListener, OsmAndLocationListener {

	private MapMarkersActiveAdapter adapter;
	private Location location;
	private float heading;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final RecyclerView recyclerView = new RecyclerView(getContext());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		final MapActivity mapActivity = (MapActivity) getActivity();

		adapter = new MapMarkersActiveAdapter(mapActivity);
		adapter.setAdapterListener(new MapMarkersActiveAdapterListener() {
			@Override
			public void onItemClick(View view) {
				int pos = recyclerView.indexOfChild(view);
				MapMarker marker = adapter.getItem(pos);
				mapActivity.getMyApplication().getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
						15, marker.getPointDescription(mapActivity), true, marker);
				MapActivity.launchMapActivityMoveToTop(mapActivity);
				((DialogFragment) getParentFragment()).dismiss();
			}
		});
		recyclerView.setAdapter(adapter);

		return recyclerView;
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.setScreenOrientation(DashLocationFragment.getScreenOrientation(getActivity()));
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
	}

	@Override
	public void updateLocation(Location location) {
		this.location = location;
		updateLocation();
	}

	@Override
	public void updateCompassValue(float heading) {
		this.heading = heading;
		updateLocation();
	}

	private OsmandApplication getMyApplication() {
		if (getActivity() != null) {
			return ((MapActivity) getActivity()).getMyApplication();
		}
		return null;
	}

	private void updateLocation() {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			if (location == null) {
				location = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
			}
			MapViewTrackingUtilities utilities = mapActivity.getMapViewTrackingUtilities();
			boolean useCenter = !(utilities.isMapLinkedToLocation() && location != null);

			adapter.setUseCenter(useCenter);
			adapter.setLocation(useCenter ? mapActivity.getMapLocation() : new LatLon(location.getLatitude(), location.getLongitude()));
			adapter.setHeading(useCenter ? -mapActivity.getMapRotate() : heading);
			adapter.notifyDataSetChanged();
		}
	}

	private void startLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocation();
		}
	}

	private void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
		}
	}
}
