package net.osmand.plus.mapmarkers.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.util.List;

public class MapMarkersHistoryAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder> {

	private OsmandApplication app;
	private List<MapMarker> markers;
	private MapMarkersHistoryAdapterListener listener;

	public MapMarkersHistoryAdapter(OsmandApplication app) {
		this.app = app;
		markers = app.getMapMarkersHelper().getMapMarkersHistory();
	}

	public void setAdapterListener(MapMarkersHistoryAdapterListener listener) {
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
	public void onBindViewHolder(final MapMarkerItemViewHolder holder, int pos) {
		IconsCache iconsCache = app.getIconsCache();
		MapMarker marker = markers.get(pos);

		holder.iconReorder.setVisibility(View.GONE);

		int color = MapMarker.getColorId(marker.colorIndex);
		holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, color));

		holder.title.setText(marker.getName(app));

		holder.optionsBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_refresh_dark));
		holder.optionsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int position = holder.getAdapterPosition();
				if (position < 0) {
					return;
				}
				MapMarker marker = markers.get(position);
				app.getMapMarkersHelper().removeMapMarkerHistory(marker);
				app.getMapMarkersHelper().addMapMarker(marker, 0);
				notifyItemRemoved(position);
			}
		});
	}

	@Override
	public int getItemCount() {
		return markers.size();
	}

	public MapMarker getItem(int position) {
		return markers.get(position);
	}

	public interface MapMarkersHistoryAdapterListener {

		void onItemClick(View view);
	}
}
