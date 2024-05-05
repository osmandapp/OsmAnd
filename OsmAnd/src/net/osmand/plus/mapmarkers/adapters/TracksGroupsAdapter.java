package net.osmand.plus.mapmarkers.adapters;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.IndexConstants;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.R;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TracksGroupsAdapter extends GroupsAdapter {

	private final List<GpxDataItem> gpxFiles;

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
			markersGroupViewHolder.name.setText(gpx.getFile().getName().replace(IndexConstants.GPX_FILE_EXT, "").replace("/", " ").replace("_", " "));
			GPXTrackAnalysis analysis = gpx.getAnalysis();
			markersGroupViewHolder.numberCount.setText(analysis != null ? String.valueOf(analysis.getWptPoints()) : "");
			String description = getDescription(gpx);
			markersGroupViewHolder.description.setVisibility(description == null ? View.GONE : View.VISIBLE);
			markersGroupViewHolder.description.setText(description);
		}
	}

	@Override
	public int getItemCount() {
		return gpxFiles.size() + 1;
	}

	@Nullable
	private String getDescription(GpxDataItem item) {
		GPXTrackAnalysis analysis = item.getAnalysis();
		Set<String> categories = analysis != null ? analysis.getWptCategoryNamesSet() : null;
		if (categories != null && !categories.isEmpty() && !(categories.size() == 1 && categories.contains(""))) {
			StringBuilder sb = new StringBuilder();
			Iterator<String> it = categories.iterator();
			while (it.hasNext()) {
				String category = it.next();
				if (!category.isEmpty()) {
					sb.append(category);
					if (it.hasNext()) {
						sb.append(", ");
					}
				}
			}
			return sb.toString();
		}
		return null;
	}

	private GpxDataItem getItem(int position) {
		return gpxFiles.get(position - 1);
	}
}
