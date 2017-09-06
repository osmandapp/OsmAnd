package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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
import net.osmand.plus.mapmarkers.adapters.MapMarkersItemTouchHelperCallback;
import net.osmand.util.MapUtils;

public class MapMarkersActiveFragment extends Fragment implements OsmAndCompassListener, OsmAndLocationListener {

	private MapMarkersActiveAdapter adapter;
	private Location location;
	private Float heading;
	private boolean locationUpdateStarted;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final RecyclerView recyclerView = new RecyclerView(getContext());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		final MapActivity mapActivity = (MapActivity) getActivity();

		adapter = new MapMarkersActiveAdapter(mapActivity);
		final ItemTouchHelper touchHelper = new ItemTouchHelper(new MapMarkersItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(recyclerView);
		adapter.setAdapterListener(new MapMarkersActiveAdapterListener() {

			private int fromPosition;
			private int toPosition;

			@Override
			public void onItemClick(View view) {
				int pos = recyclerView.indexOfChild(view);
				MapMarker marker = adapter.getItem(pos);
				mapActivity.getMyApplication().getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
						15, marker.getPointDescription(mapActivity), true, marker);
				MapActivity.launchMapActivityMoveToTop(mapActivity);
				((DialogFragment) getParentFragment()).dismiss();
			}

			@Override
			public void onDragStarted(RecyclerView.ViewHolder holder) {
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragEnded(RecyclerView.ViewHolder holder) {
				toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
					mapActivity.getMyApplication().getMapMarkersHelper().saveMapMarkers(adapter.getItems(), null);
				}
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

	void updateAdapter() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private void updateLocationUi() {
		final MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
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
					adapter.setHeading(useCenter ? -mapActivity.getMapRotate() : heading);
					adapter.notifyDataSetChanged();
				}
			});
		}
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
}
