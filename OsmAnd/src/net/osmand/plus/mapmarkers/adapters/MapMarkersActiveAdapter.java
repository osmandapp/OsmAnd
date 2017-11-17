package net.osmand.plus.mapmarkers.adapters;

import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapMarkersActiveAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder>
		implements MapMarkersItemTouchHelperCallback.ItemTouchHelperAdapter {

	private MapActivity mapActivity;
	private List<MapMarker> markers;
	private MapMarkersActiveAdapterListener listener;
	private Snackbar snackbar;
	private boolean showDirectionEnabled;

	private LatLon location;
	private Float heading;
	private boolean useCenter;
	private int screenOrientation;
	private boolean night;

	public MapMarkersActiveAdapter(MapActivity mapActivity) {
		setHasStableIds(true);
		this.mapActivity = mapActivity;
		markers = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers();
		night = !mapActivity.getMyApplication().getSettings().isLightContent();
		showDirectionEnabled = mapActivity.getMyApplication().getSettings().MAP_MARKERS_MODE.get() != OsmandSettings.MapMarkersMode.NONE;
	}

	public void setShowDirectionEnabled(boolean showDirectionEnabled) {
		this.showDirectionEnabled = showDirectionEnabled;
	}

	public void setAdapterListener(MapMarkersActiveAdapterListener listener) {
		this.listener = listener;
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
	public void onBindViewHolder(final MapMarkerItemViewHolder holder, final int pos) {
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		MapMarker marker = markers.get(pos);

		ImageView markerImageViewToUpdate;
		int drawableResToUpdate;
		int markerColor = MapMarker.getColorId(marker.colorIndex);
		LatLon markerLatLon = new LatLon(marker.getLatitude(), marker.getLongitude());
		int displayedWidgets = mapActivity.getMyApplication().getSettings().DISPLAYED_MARKERS_WIDGETS_COUNT.get();
		if (showDirectionEnabled && pos < displayedWidgets) {
			holder.iconDirection.setVisibility(View.GONE);

			holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_arrow_marker_diretion, markerColor));
			holder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.list_divider_dark : R.color.markers_top_bar_background));
			holder.title.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_white));
			holder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.map_markers_on_map_divider_color));
			holder.optionsBtn.setBackgroundDrawable(mapActivity.getResources().getDrawable(R.drawable.marker_circle_background_on_map_with_inset));
			holder.optionsBtn.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_marker_passed, R.color.color_white));
			holder.iconReorder.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_reorder, R.color.dashboard_subheader_text_dark));
			holder.description.setTextColor(ContextCompat.getColor(mapActivity, R.color.map_markers_on_map_color));

			drawableResToUpdate = R.drawable.ic_arrow_marker_diretion;
			markerImageViewToUpdate = holder.icon;
		} else {
			holder.iconDirection.setVisibility(View.VISIBLE);

			holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, markerColor));
			holder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.bg_color_dark : R.color.bg_color_light));
			holder.title.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.color_white : R.color.color_black));
			holder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, night ? R.color.actionbar_dark_color : R.color.dashboard_divider_light));
			holder.optionsBtn.setBackgroundDrawable(mapActivity.getResources().getDrawable(night ? R.drawable.marker_circle_background_dark_with_inset : R.drawable.marker_circle_background_light_with_inset));
			holder.optionsBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_marker_passed));
			holder.iconReorder.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_reorder));
			holder.description.setTextColor(ContextCompat.getColor(mapActivity, night ? R.color.dash_search_icon_dark : R.color.icon_color));

			drawableResToUpdate = R.drawable.ic_direction_arrow;
			markerImageViewToUpdate = holder.iconDirection;
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
			month = month.replaceAll("\\.", "");
			String day = new SimpleDateFormat("d", Locale.getDefault()).format(date);
			descr = month + " " + day;
		}
		holder.description.setText(descr);

		holder.optionsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final int position = holder.getAdapterPosition();
				if (position < 0) {
					return;
				}
				final MapMarker marker = markers.get(position);

				mapActivity.getMyApplication().getMapMarkersHelper().moveMapMarkerToHistory(marker);
				changeMarkers();
				notifyDataSetChanged();

				snackbar = Snackbar.make(holder.itemView, mapActivity.getString(R.string.marker_moved_to_history), Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_undo, new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								mapActivity.getMyApplication().getMapMarkersHelper().restoreMarkerFromHistory(marker, position);
								changeMarkers();
								notifyDataSetChanged();
							}
						});
				View snackBarView = snackbar.getView();
				TextView tv = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_action);
				tv.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_dialog_buttons_dark));
				snackbar.show();
			}
		});

		DashLocationFragment.updateLocationView(useCenter, location,
				heading, markerImageViewToUpdate, drawableResToUpdate, pos < 2 ? markerColor : 0,
				holder.distance, markerLatLon,
				screenOrientation, mapActivity.getMyApplication(), mapActivity, true);
	}

	@Override
	public int getItemCount() {
		return markers.size();
	}

	public MapMarker getItem(int position) {
		return markers.get(position);
	}

	public List<MapMarker> getItems() {
		return markers;
	}

	public void changeMarkers() {
		markers = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers();
	}

	public void hideSnackbar() {
		if (snackbar != null && snackbar.isShown()) {
			snackbar.dismiss();
		}
	}

	@Override
	public void onSwipeStarted() {
		listener.onSwipeStarted();
	}

	@Override
	public boolean onItemMove(int from, int to) {
		Collections.swap(markers, from, to);
		notifyItemMoved(from, to);
		return true;
	}

	@Override
	public void onItemSwiped(RecyclerView.ViewHolder holder) {
		final int pos = holder.getAdapterPosition();
		final MapMarker marker = getItem(pos);
		mapActivity.getMyApplication().getMapMarkersHelper().moveMapMarkerToHistory(marker);
		MapMarkersHelper.MapMarkersGroup group = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkerGroupByName(marker.groupName);
		if (group != null) {
			mapActivity.getMyApplication().getMapMarkersHelper().updateGroup(group);
		}
		changeMarkers();
		notifyDataSetChanged();
		snackbar = Snackbar.make(holder.itemView, R.string.marker_moved_to_history, Snackbar.LENGTH_LONG)
				.setAction(R.string.shared_string_undo, new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						mapActivity.getMyApplication().getMapMarkersHelper().restoreMarkerFromHistory(marker, pos);
						changeMarkers();
						notifyDataSetChanged();
					}
				});
		View snackBarView = snackbar.getView();
		TextView tv = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_action);
		tv.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_dialog_buttons_dark));
		snackbar.show();
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).hashCode();
	}

	@Override
	public void onItemDismiss(RecyclerView.ViewHolder holder) {
		listener.onDragOrSwipeEnded(holder);
	}

	public interface MapMarkersActiveAdapterListener {

		void onItemClick(View view);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragOrSwipeEnded(RecyclerView.ViewHolder holder);

		void onSwipeStarted();
	}
}
