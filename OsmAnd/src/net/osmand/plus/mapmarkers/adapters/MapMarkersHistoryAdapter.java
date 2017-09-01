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

	public MapMarkersHistoryAdapter(OsmandApplication app) {
		this.app = app;
		markers = app.getMapMarkersHelper().getMapMarkersHistory();
	}

	@Override
	public MapMarkerItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
		return new MapMarkerItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(MapMarkerItemViewHolder holder, int pos) {
		IconsCache iconsCache = app.getIconsCache();
		MapMarker marker = markers.get(pos);

		holder.iconReorder.setVisibility(View.GONE);

		int color = MapMarker.getColorId(marker.colorIndex);
		holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, color));

		holder.title.setText(marker.getName(app));
	}

	@Override
	public int getItemCount() {
		return markers.size();
	}
}
