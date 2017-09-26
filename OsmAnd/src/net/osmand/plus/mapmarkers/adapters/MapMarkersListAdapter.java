package net.osmand.plus.mapmarkers.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.MapUtils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapMarkersListAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder>
		implements MapMarkersItemTouchHelperCallback.ItemTouchHelperAdapter {

	private MapActivity mapActivity;
	private List<MapMarker> markers;
	private MapMarkersListAdapterListener listener;

	private LatLon location;
	private boolean useCenter;

	public void setAdapterListener(MapMarkersListAdapterListener listener) {
		this.listener = listener;
	}

	public void setLocation(LatLon location) {
		this.location = location;
	}

	public void setUseCenter(boolean useCenter) {
		this.useCenter = useCenter;
	}

	public MapMarkersListAdapter(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		markers = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers();
	}

	@Override
	public MapMarkerItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
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
	public void onBindViewHolder(final MapMarkerItemViewHolder holder, int pos) {
		boolean night = !mapActivity.getMyApplication().getSettings().isLightContent();
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		MapMarker marker = markers.get(pos);

		holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, MapMarker.getColorId(marker.colorIndex)));
		holder.iconReorder.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_reorder));
		holder.iconDirection.setVisibility(View.GONE);
		holder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.bg_color_dark : R.color.bg_color_light));
		holder.title.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.color_white : R.color.color_black));
		holder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.actionbar_dark_color : R.color.dashboard_divider_light));
		holder.optionsBtn.setVisibility(View.GONE);
		holder.description.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.dash_search_icon_dark : R.color.icon_color));
		holder.checkBox.setVisibility(View.VISIBLE);
		holder.checkBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onItemClick(holder.itemView);
			}
		});
		holder.checkBox.setChecked(marker.selected);

		if (pos == 0 || pos == getItemCount() - 1) {
			holder.firstDescription.setVisibility(View.VISIBLE);
			if (pos == 0) {
				holder.topDivider.setVisibility(View.VISIBLE);
				holder.firstDescription.setText(mapActivity.getString(R.string.shared_string_control_start) + " • ");
			} else {
				holder.firstDescription.setText(mapActivity.getString(R.string.shared_string_finish) + " • ");
			}
		} else {
			holder.firstDescription.setVisibility(View.GONE);
			holder.topDivider.setVisibility(View.GONE);
		}

		if (pos == getItemCount() - 1) {
			holder.bottomShadow.setVisibility(View.VISIBLE);
			holder.divider.setVisibility(View.GONE);
		} else {
			holder.bottomShadow.setVisibility(View.GONE);
			holder.divider.setVisibility(View.VISIBLE);
		}

		holder.point.setVisibility(View.VISIBLE);

		holder.iconReorder.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
					listener.onDragStarted(holder);
				}
				return false;
			}
		});

		holder.title.setText(marker.getName(mapActivity));

		String descr;
		if ((descr = marker.groupName) != null) {
			if (descr.equals("")) {
				descr = mapActivity.getString(R.string.shared_string_favorites);
			}
		} else {
			Date date = new Date(marker.creationDate);
			String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(date);
			if (month.length() > 1) {
				month = Character.toUpperCase(month.charAt(0)) + month.substring(1);
			}
			String day = new SimpleDateFormat("dd", Locale.getDefault()).format(date);
			descr = month + " " + day;
		}
		holder.description.setText(descr);

		if (location != null) {
			holder.distance.setTextColor(ContextCompat.getColor(mapActivity, useCenter
					? R.color.color_distance : R.color.color_myloc_distance));
			float dist = (float) MapUtils.getDistance(location.getLatitude(), location.getLongitude(),
					marker.getLatitude(), marker.getLongitude());
			holder.distance.setText(OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication()));
		}
	}

	@Override
	public int getItemCount() {
		return markers.size();
	}

	public MapMarker getItem(int position) {
		return markers.get(position);
	}

	@Override
	public void onSwipeStarted() {

	}

	@Override
	public boolean onItemMove(int from, int to) {
		Collections.swap(markers, from, to);
		notifyItemMoved(from, to);
		return true;
	}

	@Override
	public void onItemSwiped(RecyclerView.ViewHolder holder) {

	}

	@Override
	public void onItemDismiss(RecyclerView.ViewHolder holder) {
		listener.onDragEnded(holder);
	}

	public interface MapMarkersListAdapterListener {

		void onItemClick(View view);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragEnded(RecyclerView.ViewHolder holder);
	}
}
