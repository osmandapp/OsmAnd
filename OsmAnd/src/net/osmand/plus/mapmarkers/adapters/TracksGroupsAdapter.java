package net.osmand.plus.mapmarkers.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.R;

import java.util.List;

public class TracksGroupsAdapter extends GroupsAdapter {

	private List<GpxDataItem> gpxFiles;

	public TracksGroupsAdapter(Context context, List<GpxDataItem> gpxFiles) {
		super(context);
		this.gpxFiles = gpxFiles;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof MapMarkersGroupHeaderViewHolder) {
			MapMarkersGroupHeaderViewHolder markersGroupHeaderViewHolder = (MapMarkersGroupHeaderViewHolder) holder;
			markersGroupHeaderViewHolder.title.setText(app.getText(R.string.shared_string_tracks));
			markersGroupHeaderViewHolder.description.setText(app.getText(R.string.add_track_to_markers_descr));
		} else if (holder instanceof MapMarkersGroupViewHolder) {
			GpxDataItem gpx = getItem(position);
			MapMarkersGroupViewHolder markersGroupViewHolder = (MapMarkersGroupViewHolder) holder;
			markersGroupViewHolder.icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_polygom_dark));
			markersGroupViewHolder.name.setText(gpx.getFile().getName().replace(".gpx", "").replace("/", " ").replace("_", " "));
			markersGroupViewHolder.numberCount.setText(String.valueOf(gpx.getAnalysis().wptPoints));
		}
	}

	@Override
	public int getItemCount() {
		return gpxFiles.size() + 1;
	}

	private GpxDataItem getItem(int position) {
		return gpxFiles.get(position - 1);
	}
}
