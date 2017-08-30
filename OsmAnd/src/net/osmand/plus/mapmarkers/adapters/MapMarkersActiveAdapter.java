package net.osmand.plus.mapmarkers.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.views.DirectionDrawable;

import java.util.List;

public class MapMarkersActiveAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder> {

	private MapActivity mapActivity;
	private List<MapMarker> markers;
	private MapMarkersActiveAdapterListener listener;

	private LatLon location;
	private Float heading;
	private boolean useCenter;

	public MapMarkersActiveAdapter(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		markers = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers();
	}

	public void setAdapterListener(MapMarkersActiveAdapterListener listener) {
		this.listener = listener;
	}

	@Override
	public MapMarkerItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onItemClick(view);
			}
		});
		return new MapMarkerItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(MapMarkerItemViewHolder holder, int pos) {
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		MapMarker marker = markers.get(pos);
		calculateLocationParams();

		holder.iconReorder.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_reorder));
		holder.iconReorder.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				return false;
			}
		});

		int color = MapMarker.getColorId(marker.colorIndex);
		holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, color));

		holder.title.setText(marker.getName(mapActivity));

		float[] mes = new float[2];
		if (location != null && marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), location.getLatitude(), location.getLongitude(), mes);
		}
		DirectionDrawable dd = new DirectionDrawable(mapActivity, holder.iconDirection.getWidth(), holder.iconDirection.getHeight());
		dd.setImage(R.drawable.ic_direction_arrow, useCenter ? R.color.color_distance : R.color.color_myloc_distance);
		if (location == null || heading == null || marker.point == null) {
			dd.setAngle(0);
		} else {
			dd.setAngle(mes[1] - heading + 180 + DashLocationFragment.getScreenOrientation(mapActivity));
		}
		holder.iconDirection.setImageDrawable(dd);

		holder.distance.setTextColor(ContextCompat.getColor(mapActivity, useCenter ? R.color.color_distance : R.color.color_myloc_distance));
		holder.distance.setText(OsmAndFormatter.getFormattedDistance((int) mes[0], mapActivity.getMyApplication()));
	}

	@Override
	public int getItemCount() {
		return markers.size();
	}

	private void calculateLocationParams() {
		MapViewTrackingUtilities utilities = mapActivity.getMapViewTrackingUtilities();

		Float utHeading = utilities.getHeading();
		float mapRotation = mapActivity.getMapRotate();
		LatLon mapLoc = mapActivity.getMapLocation();
		Location lastLoc = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
		boolean mapLinked = utilities.isMapLinkedToLocation() && lastLoc != null;
		LatLon myLoc = lastLoc == null ? null : new LatLon(lastLoc.getLatitude(), lastLoc.getLongitude());
		useCenter = !mapLinked;
		location = (useCenter ? mapLoc : myLoc);
		heading = useCenter ? -mapRotation : utHeading;
	}

	public interface MapMarkersActiveAdapterListener {

		void onItemClick(View view);
	}
}
