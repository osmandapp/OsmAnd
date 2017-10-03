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
import net.osmand.plus.OsmandApplication;
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

	private int startPos = -1;
	private int finishPos = -1;

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
		OsmandApplication app = mapActivity.getMyApplication();
		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		IconsCache iconsCache = app.getIconsCache();

		boolean locationItem = pos == 0;
		boolean lastMarkerItem = pos == getItemCount() - 1;
		boolean start = pos == startPos;
		boolean finish = pos == finishPos && startPos != finishPos;

		MapMarker marker = locationItem ? null : getItem(pos);

		holder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.bg_color_dark : R.color.bg_color_light));
		holder.title.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.color_white : R.color.color_black));
		holder.title.setText(locationItem ? mapActivity.getString(R.string.shared_string_my_location) : marker.getName(mapActivity));
		holder.iconDirection.setVisibility(View.GONE);
		holder.optionsBtn.setVisibility(View.GONE);
		holder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.actionbar_dark_color : R.color.dashboard_divider_light));
		holder.divider.setVisibility(lastMarkerItem ? View.GONE : View.VISIBLE);
		holder.checkBox.setVisibility(View.VISIBLE);
		holder.checkBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onItemClick(holder.itemView);
			}
		});
		holder.bottomShadow.setVisibility(lastMarkerItem ? View.VISIBLE : View.GONE);

		holder.firstDescription.setVisibility((start || finish) ? View.VISIBLE : View.GONE);
		if (start) {
			holder.firstDescription.setText(mapActivity.getString(R.string.shared_string_control_start) + (locationItem ? "" : " • "));
		} else if (finish) {
			holder.firstDescription.setText(mapActivity.getString(R.string.shared_string_finish) + " • ");
		}

		if (locationItem) {
			holder.topDivider.setVisibility(View.VISIBLE);
			holder.flagIconLeftSpace.setVisibility(View.VISIBLE);
			holder.icon.setImageDrawable(ContextCompat.getDrawable(mapActivity, R.drawable.map_pedestrian_location));
			holder.point.setVisibility(View.GONE);
			holder.checkBox.setChecked(app.getMapMarkersHelper().isStartFromMyLocation());
			holder.iconReorder.setVisibility(View.GONE);
			holder.description.setVisibility(View.GONE);
			holder.distance.setVisibility(View.GONE);
		} else {
			holder.topDivider.setVisibility(View.GONE);
			holder.flagIconLeftSpace.setVisibility(View.GONE);
			holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, MapMarker.getColorId(marker.colorIndex)));
			holder.point.setVisibility(View.VISIBLE);
			holder.checkBox.setChecked(marker.selected);

			holder.iconReorder.setVisibility(View.VISIBLE);
			holder.iconReorder.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_reorder));
			holder.iconReorder.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
						listener.onDragStarted(holder);
					}
					return false;
				}
			});

			holder.description.setVisibility(View.VISIBLE);
			holder.description.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.dash_search_icon_dark : R.color.icon_color));
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
				holder.distance.setVisibility(View.VISIBLE);
				holder.distance.setTextColor(ContextCompat.getColor(mapActivity, useCenter
						? R.color.color_distance : R.color.color_myloc_distance));
				float dist = (float) MapUtils.getDistance(location.getLatitude(), location.getLongitude(),
						marker.getLatitude(), marker.getLongitude());
				holder.distance.setText(OsmAndFormatter.getFormattedDistance(dist, app));
			}
		}
	}

	@Override
	public int getItemCount() {
		return markers.size() + 1;
	}

	public MapMarker getItem(int position) {
		return markers.get(position - 1);
	}

	@Override
	public void onSwipeStarted() {

	}

	@Override
	public boolean onItemMove(int from, int to) {
		if (to == 0) {
			return false;
		}
		Collections.swap(markers, from - 1, to - 1);
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

	public void updateStartAndFinish() {
		int startP = startPos;
		int finishP = finishPos;
		calculateStartAndFinishPos();
		notifyItemChanged(startP);
		notifyItemChanged(finishP);
		notifyItemChanged(startPos);
		notifyItemChanged(finishPos);
	}

	public void calculateStartAndFinishPos() {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean startCalculated = false;
		boolean finishCalculated = false;
		if (app.getMapMarkersHelper().isStartFromMyLocation() && app.getLocationProvider().getLastStaleKnownLocation() != null) {
			startPos = 0;
			startCalculated = true;
		} else {
			for (int i = 0; i < markers.size(); i++) {
				if (markers.get(i).selected) {
					startPos = i + 1;
					startCalculated = true;
					break;
				}
			}
		}
		for (int i = markers.size() - 1; i >= 0; i--) {
			if (markers.get(i).selected) {
				finishPos = i + 1;
				finishCalculated = true;
				break;
			}
		}
		if (!startCalculated) {
			startPos = -1;
		}
		if (!finishCalculated) {
			finishPos = -1;
		}
	}

	public interface MapMarkersListAdapterListener {

		void onItemClick(View view);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragEnded(RecyclerView.ViewHolder holder);
	}
}
