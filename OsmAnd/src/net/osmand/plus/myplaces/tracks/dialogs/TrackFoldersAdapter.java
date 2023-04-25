package net.osmand.plus.myplaces.tracks.dialogs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TrackTab;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.configmap.tracks.TracksSortMode;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.myplaces.tracks.dialogs.TrackFolderViewHolder.FolderSelectionListener;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class TrackFoldersAdapter extends RecyclerView.Adapter<ViewHolder> {

	public static final int TYPE_SORT_TRACKS = 0;
	public static final int TYPE_TRACK = 1;
	public static final int TYPE_FOLDER = 2;

	private final List<Object> items = new ArrayList<>();
	private final UpdateLocationViewCache locationViewCache;
	private final boolean nightMode;
	private final TrackTab trackTab = new TrackTab(TrackTabType.ALL);

	private SortTracksListener sortListener;
	private TrackSelectionListener trackSelectionListener;
	private FolderSelectionListener folderSelectionListener;


	public TrackFoldersAdapter(@NonNull OsmandApplication app, boolean nightMode) {
		this.nightMode = nightMode;
		this.locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(app);
		locationViewCache.arrowResId = R.drawable.ic_direction_arrow;
		locationViewCache.arrowColor = ColorUtilities.getActiveIconColorId(nightMode);
	}

	public void setSortTracksListener(@Nullable SortTracksListener sortListener) {
		this.sortListener = sortListener;
	}

	public void setTrackSelectionListener(@Nullable TrackSelectionListener selectionListener) {
		this.trackSelectionListener = selectionListener;
	}

	public void setFolderSelectionListener(@Nullable FolderSelectionListener selectionListener) {
		this.folderSelectionListener = selectionListener;
	}

	public void updateFolder(@NonNull TrackFolder folder) {
		items.clear();
		items.add(TYPE_SORT_TRACKS);
		items.addAll(folder.getSubFolders());
		items.addAll(folder.getTrackItems());
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case TYPE_TRACK:
				View view = inflater.inflate(R.layout.track_list_item, parent, false);
				return new TrackViewHolder(view, trackSelectionListener, locationViewCache, nightMode);
			case TYPE_FOLDER:
				view = inflater.inflate(R.layout.track_folder_list_item, parent, false);
				return new TrackFolderViewHolder(view, folderSelectionListener, nightMode);
			case TYPE_SORT_TRACKS:
				view = inflater.inflate(R.layout.sort_type_view, parent, false);
				return new SortTracksViewHolder(view, sortListener, nightMode);
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof TrackItem) {
			return TYPE_TRACK;
		} else if (object instanceof TrackFolder) {
			return TYPE_FOLDER;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (TYPE_SORT_TRACKS == item) {
				return TYPE_SORT_TRACKS;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof SortTracksViewHolder) {
			TracksSortMode sortMode = trackTab.getSortMode();
			boolean enabled = !Algorithms.isEmpty(trackTab.getTrackItems());
			((SortTracksViewHolder) holder).bindView(sortMode, enabled);
		} else if (holder instanceof TrackViewHolder) {
			TrackItem trackItem = (TrackItem) items.get(position);
			boolean hideDivider = position == getItemCount() - 1;
			TrackViewHolder viewHolder = (TrackViewHolder) holder;
			viewHolder.bindView(trackTab, trackItem, !hideDivider);
		} else if (holder instanceof TrackFolderViewHolder) {
			TrackFolder trackFolder = (TrackFolder) items.get(position);
			TrackFolderViewHolder viewHolder = (TrackFolderViewHolder) holder;
			viewHolder.bindView(trackFolder);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}
}
