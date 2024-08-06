package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.Location;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.adapters.MapMarkersActiveAdapter;
import net.osmand.plus.mapmarkers.adapters.MapMarkersActiveAdapter.MapMarkersActiveAdapterListener;
import net.osmand.plus.mapmarkers.adapters.MapMarkersItemTouchHelperCallback;
import net.osmand.plus.widgets.EmptyStateRecyclerView;
import net.osmand.util.MapUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MapMarkersActiveFragment extends Fragment implements OsmAndCompassListener, OsmAndLocationListener {

	private MapMarkersActiveAdapter adapter;
	private Location location;
	private Float heading;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) getActivity();
		View mainView = inflater.inflate(R.layout.fragment_map_markers_active, container, false);
		EmptyStateRecyclerView recyclerView = mainView.findViewById(R.id.list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

		adapter = new MapMarkersActiveAdapter(mapActivity);
		ItemTouchHelper touchHelper = new ItemTouchHelper(new MapMarkersItemTouchHelperCallback(mapActivity, adapter));
		touchHelper.attachToRecyclerView(recyclerView);
		adapter.setAdapterListener(new MapMarkersActiveAdapterListener() {

			private int fromPosition;
			private int toPosition;

			@Override
			public void onItemClick(View view) {
				int pos = recyclerView.getChildAdapterPosition(view);
				if (pos == RecyclerView.NO_POSITION) {
					return;
				}

				MapMarker marker = adapter.getItem(pos);
				OsmandApplication app = mapActivity.getMyApplication();
				if (app.getSettings().SELECT_MARKER_ON_SINGLE_TAP.get()) {
					app.getMapMarkersHelper().moveMarkerToTop(marker);
					updateAdapter();
				} else {
					FavouritePoint fav = marker.favouritePoint == null
							? app.getFavoritesHelper().getVisibleFavByLatLon(marker.point)
							: marker.favouritePoint;
					if (fav != null) {
						showMap(marker.point, fav.getPointDescription(mapActivity), fav);
						return;
					}

					WptPt pt = marker.wptPt == null
							? app.getSelectedGpxHelper().getVisibleWayPointByLatLon(marker.point)
							: marker.wptPt;
					if (pt != null) {
						showMap(marker.point, new WptLocationPoint(pt).getPointDescription(mapActivity), pt);
						return;
					}

					Amenity mapObj = mapActivity.getMapLayers().getMapMarkersLayer().getMapObjectByMarker(marker);
					PointDescription desc = mapObj == null
							? marker.getPointDescription(mapActivity)
							: mapActivity.getMapLayers().getPoiMapLayer().getObjectName(mapObj);
					showMap(marker.point, desc, mapObj == null ? marker : mapObj);
				}
			}

			private void showMap(LatLon latLon, PointDescription desc, Object objToShow) {
				mapActivity.getMyApplication().getSettings().setMapLocationToShow(latLon.getLatitude(),
						latLon.getLongitude(), 15, desc, true, objToShow);
				MapActivity.launchMapActivityMoveToTop(mapActivity);
				((DialogFragment) getParentFragment()).dismiss();
			}

			@Override
			public void onDragStarted(RecyclerView.ViewHolder holder) {
				compassUpdateAllowed = false;
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragOrSwipeEnded(RecyclerView.ViewHolder holder) {
				compassUpdateAllowed = true;
				toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
					hideSnackbar();
					mapActivity.getMyApplication().getMapMarkersHelper().saveGroups(false);
					adapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onSwipeStarted() {
				compassUpdateAllowed = false;
			}
		});

		View emptyView = mainView.findViewById(R.id.empty_view);
		ImageView emptyImageView = emptyView.findViewById(R.id.empty_state_image_view);
		emptyImageView.setImageResource(mapActivity.getMyApplication().getSettings().isLightContent()
				? R.drawable.ic_empty_state_marker_list_day : R.drawable.ic_empty_state_marker_list_night);
		recyclerView.setEmptyView(emptyView);
		recyclerView.setAdapter(adapter);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});
		return mainView;
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

	private OsmandApplication getMyApplication() {
		if (getActivity() != null) {
			return ((MapActivity) getActivity()).getMyApplication();
		}
		return null;
	}

	void setShowDirectionEnabled(boolean showDirectionEnabled) {
		if (adapter != null) {
			adapter.setShowDirectionEnabled(showDirectionEnabled);
		}
	}

	void updateAdapter() {
		if (adapter != null) {
			adapter.changeMarkers();
			adapter.notifyDataSetChanged();
		}
	}

	void hideSnackbar() {
		if (adapter != null) {
			adapter.hideSnackbar();
		}
	}

	private void updateLocationUi() {
		if (!compassUpdateAllowed) {
			return;
		}
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null && adapter != null) {
			mapActivity.getMyApplication().runInUIThread(() -> {
				if (location == null) {
					location = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
				}
				adapter.notifyDataSetChanged();
			});
		}
	}

	void startLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && !locationUpdateStarted) {
			locationUpdateStarted = true;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
			locationProvider.addCompassListener(this);
			locationProvider.addLocationListener(this);
			updateLocationUi();
		}
	}

	void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && locationUpdateStarted) {
			locationUpdateStarted = false;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeLocationListener(this);
			locationProvider.removeCompassListener(this);
			locationProvider.addCompassListener(locationProvider.getNavigationInfo());
		}
	}
}