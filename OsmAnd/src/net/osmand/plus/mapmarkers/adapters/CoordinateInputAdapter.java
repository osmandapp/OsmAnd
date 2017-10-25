package net.osmand.plus.mapmarkers.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.MapMarkersHelper.MAP_MARKERS_COLORS_COUNT;

public class CoordinateInputAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder> {

	private MapActivity mapActivity;
	private boolean nightTheme;
	private IconsCache iconsCache;
	private List<MapMarker> mapMarkers = new ArrayList<>();
	private LatLon location;
	private Float heading;
	private boolean useCenter;
	private int screenOrientation;

	public CoordinateInputAdapter (MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		nightTheme = !mapActivity.getMyApplication().getSettings().isLightContent();
		iconsCache = mapActivity.getMyApplication().getIconsCache();
	}

	public void setLocation(LatLon location) {
		this.location = location;
	}

	public void setHeading(Float heading) {
		this.heading = heading;
	}

	public void setUseCenter(boolean useCenter) {
		this.useCenter = useCenter;
	}

	public void setScreenOrientation(int screenOrientation) {
		this.screenOrientation = screenOrientation;
	}

	@Override
	public MapMarkerItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.map_marker_item_new, parent, false);
		return new MapMarkerItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(MapMarkerItemViewHolder holder, int position) {
		MapMarker mapMarker = getItem(position);
		holder.iconDirection.setVisibility(View.VISIBLE);
		holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, MapMarker.getColorId(mapMarker.colorIndex)));
		holder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, nightTheme ? R.color.bg_color_dark : R.color.bg_color_light));
		holder.title.setTextColor(ContextCompat.getColor(mapActivity, nightTheme ? R.color.color_white : R.color.color_black));
		holder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, nightTheme ? R.color.actionbar_dark_color : R.color.dashboard_divider_light));
		holder.optionsBtn.setBackgroundDrawable(mapActivity.getResources().getDrawable(nightTheme ? R.drawable.marker_circle_background_dark_with_inset : R.drawable.marker_circle_background_light_with_inset));
		holder.optionsBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_remove_dark));
		holder.iconReorder.setVisibility(View.GONE);
		holder.numberText.setVisibility(View.VISIBLE);
		holder.numberText.setText(Integer.toString(position + 1));
		holder.description.setVisibility(View.GONE);

		boolean fistItem = position == 0;
		boolean lastItem = position == getItemCount() - 1;
		holder.topDivider.setVisibility(fistItem ? View.VISIBLE : View.GONE);
		holder.divider.setVisibility((getItemCount() > 1 && !lastItem) ? View.VISIBLE : View.GONE);
		holder.bottomShadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);

		holder.title.setText(mapMarker.getName(mapActivity));

		DashLocationFragment.updateLocationView(useCenter, location,
				heading, holder.iconDirection, R.drawable.ic_direction_arrow,
				holder.distance, new LatLon(mapMarker.getLatitude(), mapMarker.getLongitude()),
				screenOrientation, mapActivity.getMyApplication(), mapActivity, true);
	}

	@Override
	public int getItemCount() {
		return mapMarkers.size();
	}

	public MapMarker getItem(int position) {
		return mapMarkers.get(position);
	}

	public void addMapMarker(String latitude, String longitude, String name) {
		PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, name);
		LatLon latLon = new LatLon(30.537020, 50.443477);
		int colorIndex = mapMarkers.size() > 0 ? mapMarkers.get(mapMarkers.size() - 1).colorIndex : -1;
		if (colorIndex == -1) {
			colorIndex = 0;
		} else {
			colorIndex = (colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
		}
		MapMarker mapMarker = new MapMarker(latLon, pointDescription, colorIndex, false, 0);
		mapMarkers.add(mapMarker);
		notifyDataSetChanged();
	}
}
