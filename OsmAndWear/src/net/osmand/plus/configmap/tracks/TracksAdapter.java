package net.osmand.plus.configmap.tracks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder.EmptyTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.FolderViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.NoVisibleTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.RecentlyVisibleViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.track.BaseTracksTabsFragment;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Set;

public class TracksAdapter extends RecyclerView.Adapter<ViewHolder> {

	// values are used to sort items in TracksComparator
	public static final int TYPE_SORT_TRACKS = 0;
	public static final int TYPE_NO_TRACKS = 1;
	public static final int TYPE_NO_VISIBLE_TRACKS = 2;
	public static final int TYPE_RECENTLY_VISIBLE_TRACKS = 3;
	public static final int TYPE_TRACK = 4;
	public static final int TYPE_FOLDER = 5;

	private final UpdateLocationViewCache locationViewCache;
	private boolean selectionMode = true;
	private boolean selectTrackMode;
	private TrackTab trackTab;
	private final BaseTracksTabsFragment fragment;
	protected final boolean nightMode;
	private EmptyTracksListener emptyTracksListener;
	private TrackSelectionListener trackSelectionListener;
	private SortTracksListener sortTracksListener;

	public TracksAdapter(@NonNull Context context, @NonNull TrackTab trackTab, @NonNull BaseTracksTabsFragment fragment, boolean nightMode) {
		this.trackTab = trackTab;
		this.fragment = fragment;
		this.nightMode = nightMode;
		this.locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(context);
		locationViewCache.arrowResId = R.drawable.ic_direction_arrow;
		locationViewCache.arrowColor = ColorUtilities.getActiveIconColorId(nightMode);
		emptyTracksListener = fragment;
		trackSelectionListener = fragment;
		sortTracksListener = fragment;
		selectTrackMode = fragment.selectTrackMode();
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	public void setSelectTrackMode(boolean selectTrackMode) {
		this.selectTrackMode = selectTrackMode;
	}

	public void setEmptyTracksListener(@Nullable EmptyTracksListener emptyTracksListener) {
		this.emptyTracksListener = emptyTracksListener;
	}

	public void setTrackSelectionListener(@Nullable TrackSelectionListener trackSelectionListener) {
		this.trackSelectionListener = trackSelectionListener;
	}

	public void setSortTracksListener(@Nullable SortTracksListener sortTracksListener) {
		this.sortTracksListener = sortTracksListener;
	}

	@NonNull
	public TrackTab getTrackTab() {
		return trackTab;
	}

	public void setTrackTab(TrackTab trackTab) {
		this.trackTab = trackTab;
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
				view = inflater.inflate(R.layout.track_list_item, parent, false);
				return new FolderViewHolder(view, trackSelectionListener, nightMode);
			case TYPE_NO_TRACKS:
				view = inflater.inflate(R.layout.track_folder_empty_state, parent, false);
				return new EmptyTracksViewHolder(view, emptyTracksListener);
			case TYPE_NO_VISIBLE_TRACKS:
				view = inflater.inflate(R.layout.track_folder_empty_state, parent, false);
				return new NoVisibleTracksViewHolder(view, fragment);
			case TYPE_RECENTLY_VISIBLE_TRACKS:
				view = inflater.inflate(R.layout.list_header_switch_item, parent, false);
				return new RecentlyVisibleViewHolder(view, fragment, nightMode);
			case TYPE_SORT_TRACKS:
				return createSortTracksViewHolder(parent, inflater);
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}
	}

	@NonNull
	protected SortTracksViewHolder createSortTracksViewHolder(@NonNull ViewGroup parent, LayoutInflater inflater) {
		View view = inflater.inflate(R.layout.sort_type_view, parent, false);
		return new SortTracksViewHolder(view, sortTracksListener, nightMode);
	}

	@Override
	public int getItemViewType(int position) {
		Object object = getItems().get(position);
		if (object instanceof TrackItem) {
			return TYPE_TRACK;
		} else if (object instanceof TrackFolder) {
			return TYPE_FOLDER;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (TYPE_NO_TRACKS == item) {
				return TYPE_NO_TRACKS;
			} else if (TYPE_NO_VISIBLE_TRACKS == item) {
				return TYPE_NO_VISIBLE_TRACKS;
			} else if (TYPE_RECENTLY_VISIBLE_TRACKS == item) {
				return TYPE_RECENTLY_VISIBLE_TRACKS;
			} else if (TYPE_SORT_TRACKS == item) {
				return TYPE_SORT_TRACKS;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	protected List<Object> getItems() {
		return trackTab.items;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof TrackViewHolder) {
			TrackItem item = (TrackItem) getItems().get(position);
			boolean shouldShowFolder = trackTab.type.shouldShowFolder();
			TrackViewHolder viewHolder = (TrackViewHolder) holder;
			viewHolder.bindView(trackTab.getSortMode(), item, !hideDivider(position), shouldShowFolder, selectionMode, selectTrackMode);
		} else if (holder instanceof FolderViewHolder) {
			TrackFolder folder = (TrackFolder) getItems().get(position);
			FolderViewHolder viewHolder = (FolderViewHolder) holder;
			viewHolder.bindView(folder, !hideDivider(position));
		} else if (holder instanceof NoVisibleTracksViewHolder) {
			((NoVisibleTracksViewHolder) holder).bindView();
		} else if (holder instanceof EmptyTracksViewHolder) {
			((EmptyTracksViewHolder) holder).bindView();
		} else if (holder instanceof RecentlyVisibleViewHolder) {
			((RecentlyVisibleViewHolder) holder).bindView(selectionMode);
		} else if (holder instanceof SortTracksViewHolder) {
			boolean enabled = !Algorithms.isEmpty(trackTab.getTrackItems()) || (selectTrackMode && !Algorithms.isEmpty(trackTab.getTrackFolders()));
			((SortTracksViewHolder) holder).bindView(enabled, null);
		}
	}

	private boolean hideDivider(int position) {
		return position == getItemCount() - 1
				|| Algorithms.objectEquals(getItems().get(position + 1), TYPE_RECENTLY_VISIBLE_TRACKS);
	}

	public void updateItems(@NonNull Set<TrackItem> trackItems) {
		for (TrackItem trackItem : trackItems) {
			updateItem(trackItem);
		}
		updateItem(TYPE_RECENTLY_VISIBLE_TRACKS);
	}

	private void updateItem(@NonNull Object object) {
		int index = getItems().indexOf(object);
		if (index != -1) {
			notifyItemChanged(index);
		}
	}

	@Override
	public int getItemCount() {
		return getItems().size();
	}

	public interface ItemVisibilityCallback{
		boolean shouldShowItem(TrackItem trackItem);
	}
}