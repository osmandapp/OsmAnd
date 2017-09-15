package net.osmand.plus.mapmarkers.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.List;

public class MapMarkersGroupsAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder> {

    private MapActivity mapActivity;
    private List<MapMarkersHelper.MapMarker> markers;
    private boolean night;

    public MapMarkersGroupsAdapter(MapActivity mapActivity) {
        this.mapActivity = mapActivity;
        markers = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers();
        night = !mapActivity.getMyApplication().getSettings().isLightContent();
    }

    @Override
    public MapMarkerItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
        return new MapMarkerItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MapMarkerItemViewHolder mapMarkerItemViewHolder, int i) {
        IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
        MapMarkersHelper.MapMarker marker = markers.get(i);
    }

    @Override
    public int getItemCount() {
        return markers.size();
    }

    public MapMarkersHelper.MapMarker getItem(int position) {
        return markers.get(position);
    }

    public List<MapMarkersHelper.MapMarker> getItems() {
        return markers;
    }
}
