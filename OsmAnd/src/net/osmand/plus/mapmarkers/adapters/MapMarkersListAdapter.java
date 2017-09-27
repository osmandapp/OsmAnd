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

public class MapMarkersListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
		implements MapMarkersItemTouchHelperCallback.ItemTouchHelperAdapter {

	private static final int USE_LOCATION_CARD_TYPE = 1;
	private static final int MARKER_ITEM_TYPE = 2;

	private MapActivity mapActivity;
	private List<MapMarker> markers;
	private boolean locationCardDisplayed = true;
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
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		if (viewType == USE_LOCATION_CARD_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.use_location_card, viewGroup, false);
			return new UseLocationCardViewHolder(view);
		} else if (viewType == MARKER_ITEM_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					listener.onItemClick(view);
				}
			});
			return new MapMarkerItemViewHolder(view);
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (locationCardDisplayed && position == 0) {
			return USE_LOCATION_CARD_TYPE;
		}
		return MARKER_ITEM_TYPE;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
		boolean night = !mapActivity.getMyApplication().getSettings().isLightContent();
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();

		if (holder instanceof UseLocationCardViewHolder) {
			final UseLocationCardViewHolder locationCardHolder = (UseLocationCardViewHolder) holder;
		} else if (holder instanceof MapMarkerItemViewHolder) {
			MapMarker marker = getItem(pos);
			final MapMarkerItemViewHolder itemHolder = (MapMarkerItemViewHolder) holder;

			itemHolder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, MapMarker.getColorId(marker.colorIndex)));
			itemHolder.iconReorder.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_reorder));
			itemHolder.iconDirection.setVisibility(View.GONE);
			itemHolder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.bg_color_dark : R.color.bg_color_light));
			itemHolder.title.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.color_white : R.color.color_black));
			itemHolder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.actionbar_dark_color : R.color.dashboard_divider_light));
			itemHolder.optionsBtn.setVisibility(View.GONE);
			itemHolder.description.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.dash_search_icon_dark : R.color.icon_color));
			itemHolder.checkBox.setVisibility(View.VISIBLE);
			itemHolder.checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					listener.onItemClick(itemHolder.itemView);
				}
			});
			itemHolder.checkBox.setChecked(marker.selected);

			int firstMarkerPos = locationCardDisplayed ? 1 : 0;
			int lastMarkerPos = getItemCount() - 1;

			itemHolder.topDivider.setVisibility(pos == firstMarkerPos ? View.VISIBLE : View.GONE);
			itemHolder.firstDescription.setVisibility((pos == firstMarkerPos || pos == lastMarkerPos) ? View.VISIBLE : View.GONE);
			itemHolder.bottomShadow.setVisibility(pos == lastMarkerPos ? View.VISIBLE : View.GONE);
			itemHolder.divider.setVisibility(pos == lastMarkerPos ? View.GONE : View.VISIBLE);
			
			if (pos == firstMarkerPos) {
				itemHolder.firstDescription.setText(mapActivity.getString(R.string.shared_string_control_start) + " • ");
			} else if (pos == lastMarkerPos) {
				itemHolder.firstDescription.setText(mapActivity.getString(R.string.shared_string_finish) + " • ");
			}

			itemHolder.point.setVisibility(View.VISIBLE);

			itemHolder.iconReorder.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
						listener.onDragStarted(itemHolder);
					}
					return false;
				}
			});

			itemHolder.title.setText(marker.getName(mapActivity));

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
			itemHolder.description.setText(descr);

			if (location != null) {
				itemHolder.distance.setTextColor(ContextCompat.getColor(mapActivity, useCenter
						? R.color.color_distance : R.color.color_myloc_distance));
				float dist = (float) MapUtils.getDistance(location.getLatitude(), location.getLongitude(),
						marker.getLatitude(), marker.getLongitude());
				itemHolder.distance.setText(OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication()));
			}
		}
	}

	@Override
	public int getItemCount() {
		return locationCardDisplayed ? markers.size() + 1 : markers.size();
	}

	public MapMarker getItem(int position) {
		return markers.get(locationCardDisplayed ? position - 1 : position);
	}

	@Override
	public void onSwipeStarted() {

	}

	@Override
	public boolean onItemMove(int from, int to) {
		if (locationCardDisplayed && to == 0) {
			return false;
		}
		Collections.swap(markers, locationCardDisplayed ? from - 1 : from, locationCardDisplayed ? to - 1 : to);
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

	private class UseLocationCardViewHolder extends RecyclerView.ViewHolder {

		final View useLocationBtn;
		final View doNotUseLocationBtn;

		UseLocationCardViewHolder(View view) {
			super(view);
			useLocationBtn = view.findViewById(R.id.use_location_button);
			doNotUseLocationBtn = view.findViewById(R.id.do_not_use_location_button);
		}
	}
}
