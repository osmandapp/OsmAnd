package net.osmand.plus.mapmarkers.adapters;

import android.support.design.widget.Snackbar;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;

import java.util.Collections;
import java.util.List;

public class MapMarkersActiveAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder>
		implements MapMarkersItemTouchHelperCallback.ItemTouchHelperAdapter {

	private MapActivity mapActivity;
	private List<MapMarker> markers;
	private MapMarkersActiveAdapterListener listener;

	private LatLon location;
	private Float heading;
	private boolean useCenter;
	private int screenOrientation;

	public MapMarkersActiveAdapter(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		markers = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers();
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
	public void onBindViewHolder(final MapMarkerItemViewHolder holder, int pos) {
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		MapMarker marker = markers.get(pos);

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

		int color = MapMarker.getColorId(marker.colorIndex);
		holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, color));

		holder.title.setText(marker.getName(mapActivity));

		holder.description.setText(marker.creationDate + "");

		holder.optionsBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_remove_dark));
		holder.optionsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final int position = holder.getAdapterPosition();
				if (position < 0) {
					return;
				}
				final MapMarker marker = markers.get(position);
				final boolean[] undone = new boolean[1];

				mapActivity.getMyApplication().getMapMarkersHelper().removeMapMarker(marker.index);
				notifyItemRemoved(position);

				Snackbar.make(holder.itemView, R.string.item_removed, Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_undo, new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								undone[0] = true;
								mapActivity.getMyApplication().getMapMarkersHelper().addMapMarker(marker, position);
								notifyItemInserted(position);
							}
						})
						.addCallback(new Snackbar.Callback() {
							@Override
							public void onDismissed(Snackbar transientBottomBar, int event) {
								if (!undone[0]) {
									mapActivity.getMyApplication().getMapMarkersHelper().addMapMarkerHistory(marker);
								}
							}
						}).show();
			}
		});

		DashLocationFragment.updateLocationView(useCenter, location,
				heading, holder.iconDirection, holder.distance,
				marker.getLatitude(), marker.getLongitude(),
				screenOrientation, mapActivity.getMyApplication(), mapActivity);
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

	@Override
	public boolean onItemMove(int from, int to) {
		Collections.swap(markers, from, to);
		notifyItemMoved(from, to);
		return true;
	}

	@Override
	public void onItemDismiss(RecyclerView.ViewHolder holder) {
		listener.onDragEnded(holder);
	}

	public interface MapMarkersActiveAdapterListener {

		void onItemClick(View view);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragEnded(RecyclerView.ViewHolder holder);
	}
}
