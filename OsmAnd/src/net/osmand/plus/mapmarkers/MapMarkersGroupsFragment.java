package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.mapmarkers.adapters.MapMarkersGroupsAdapter;
import net.osmand.util.MapUtils;

public class MapMarkersGroupsFragment extends Fragment implements OsmAndCompassListener, OsmAndLocationListener {

    public static final String TAG = "MapMarkersGroupsFragment";

    private MapMarkersGroupsAdapter adapter;
	private Float heading;
	private Location location;
	private boolean locationUpdateStarted;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        final MapActivity mapActivity = (MapActivity) getActivity();

        adapter = new MapMarkersGroupsAdapter(mapActivity);
		recyclerView.setAdapter(adapter);
        return recyclerView;
    }

    void updateAdapter() {
        if (adapter != null) {
			adapter.createDisplayGroups();
            adapter.notifyDataSetChanged();
        }
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

	void startLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && !locationUpdateStarted) {
			locationUpdateStarted = true;
			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
	}

	void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && locationUpdateStarted) {
			locationUpdateStarted = false;
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
		}
	}

	@Override
	public void updateLocation(Location location) {
		boolean newLocation = this.location == null && location != null;
		boolean locationChanged = this.location != null && location != null
				&& this.location.getLatitude() != location.getLatitude()
				&& this.location.getLongitude() != location.getLongitude();
		if (newLocation || locationChanged) {
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

	private OsmandApplication getMyApplication() {
		if (getActivity() != null) {
			return ((MapActivity) getActivity()).getMyApplication();
		}
		return null;
	}

	private void updateLocationUi() {
		final MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null && adapter != null) {
			mapActivity.getMyApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					if (location == null) {
						location = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
					}
					MapViewTrackingUtilities utilities = mapActivity.getMapViewTrackingUtilities();
					boolean useCenter = !(utilities.isMapLinkedToLocation() && location != null);

					adapter.setUseCenter(useCenter);
					adapter.setLocation(useCenter ? mapActivity.getMapLocation() : new LatLon(location.getLatitude(), location.getLongitude()));
					adapter.setHeading(useCenter ? -mapActivity.getMapRotate() : heading != null ? heading : 99);
					adapter.notifyDataSetChanged();
				}
			});
		}
	}
}
