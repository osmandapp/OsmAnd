package net.osmand.plus.myplaces.tracks.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.configmap.tracks.viewholders.EmptyFolderLoadingTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.EmptySmartFolderLoadingTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder.EmptyTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.myplaces.tracks.EmptySmartFolderListener;
import net.osmand.plus.myplaces.tracks.VisibleTracksGroup;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.EmptyFolderViewHolder;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.EmptySmartFolderViewHolder;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.FolderStatsViewHolder;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.RecordingTrackViewHolder;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.RecordingTrackViewHolder.RecordingTrackListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.SmartFolderViewHolder;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TrackFolderViewHolder;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TracksGroupViewHolder.TrackGroupsListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.VisibleTracksViewHolder;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.filters.TrackFolderAnalysis;
import net.osmand.shared.gpx.data.TracksGroup;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TrackFoldersAdapter extends RecyclerView.Adapter<ViewHolder> {

	// values are used to sort items in TracksComparator
	public static final int TYPE_SORT_TRACKS = 0;
	public static final int TYPE_RECORDING_TRACK = 1;
	public static final int TYPE_VISIBLE_TRACKS = 2;
	public static final int TYPE_FOLDER = 3;
	public static final int TYPE_TRACK = 4;
	public static final int TYPE_EMPTY_FOLDER = 5;
	public static final int TYPE_FOLDER_STATS = 6;
	public static final int TYPE_EMPTY_TRACKS = 7;
	public static final int TYPE_SMART_FOLDER = 8;
	public static final int TYPE_EMPTY_SMART_FOLDER = 9;
	public static final int TYPE_EMPTY_SMART_FOLDER_LOADING = 10;
	public static final int TYPE_EMPTY_FOLDER_LOADING = 11;

	private final OsmandApplication app;
	private final UpdateLocationViewCache locationViewCache;
	private final List<Object> items = new ArrayList<>();

	@Nullable
	private SortTracksListener sortListener;
	@Nullable
	private TrackGroupsListener trackGroupsListener;
	@Nullable
	private TrackSelectionListener trackSelectionListener;
	@Nullable
	private RecordingTrackListener recordingTrackListener;
	@Nullable
	private EmptyTracksListener emptyTracksListener;
	@Nullable
	private EmptySmartFolderListener emptySmartFolderListener;

	private TracksSortMode sortMode = TracksSortMode.getDefaultSortMode();
	private boolean nightMode;
	private boolean selectionMode;
	private boolean shouldShowFolder;

	public TrackFoldersAdapter(@NonNull Context context, boolean nightMode) {
		this.app = (OsmandApplication) context.getApplicationContext();
		this.nightMode = nightMode;
		locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(context);
		locationViewCache.arrowResId = R.drawable.ic_direction_arrow;
		locationViewCache.arrowColor = ColorUtilities.getActiveIconColorId(nightMode);
	}

	public void setItems(@NonNull List<Object> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	public void setSortMode(@NonNull TracksSortMode sortMode) {
		this.sortMode = sortMode;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	public void setShouldShowFolder(boolean shouldShowFolder) {
		this.shouldShowFolder = shouldShowFolder;
	}

	public void setSortTracksListener(@Nullable SortTracksListener sortListener) {
		this.sortListener = sortListener;
	}

	public void setTrackSelectionListener(@Nullable TrackSelectionListener selectionListener) {
		this.trackSelectionListener = selectionListener;
	}

	public void setTrackGroupsListener(@Nullable TrackGroupsListener trackGroupsListener) {
		this.trackGroupsListener = trackGroupsListener;
	}

	public void setRecordingTrackListener(@Nullable RecordingTrackListener recordingTrackListener) {
		this.recordingTrackListener = recordingTrackListener;
	}

	public void setEmptyTracksListener(@Nullable EmptyTracksListener emptyTracksListener) {
		this.emptyTracksListener = emptyTracksListener;
	}

	public void setEmptySmartFolderListener(@Nullable EmptySmartFolderListener emptySmartFolderListener) {
		this.emptySmartFolderListener = emptySmartFolderListener;
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
				return new TrackFolderViewHolder(view, trackGroupsListener, nightMode, selectionMode);
			case TYPE_SMART_FOLDER:
				view = inflater.inflate(R.layout.track_list_item, parent, false);
				return new SmartFolderViewHolder(view, trackGroupsListener, nightMode, selectionMode);
			case TYPE_VISIBLE_TRACKS:
				view = inflater.inflate(R.layout.track_list_item, parent, false);
				return new VisibleTracksViewHolder(view, trackGroupsListener, nightMode, selectionMode);
			case TYPE_SORT_TRACKS:
				view = inflater.inflate(R.layout.sort_type_view, parent, false);
				return new SortTracksViewHolder(view, sortListener, nightMode);
			case TYPE_RECORDING_TRACK:
				view = inflater.inflate(R.layout.current_gpx_item, parent, false);
				return new RecordingTrackViewHolder(view, trackSelectionListener, recordingTrackListener, nightMode);
			case TYPE_EMPTY_FOLDER:
				view = inflater.inflate(R.layout.track_folder_empty_state, parent, false);
				return new EmptyFolderViewHolder(view, emptyTracksListener);
			case TYPE_EMPTY_SMART_FOLDER:
				view = inflater.inflate(R.layout.track_folder_empty_state, parent, false);
				return new EmptySmartFolderViewHolder(view, emptySmartFolderListener);
			case TYPE_EMPTY_SMART_FOLDER_LOADING:
				view = inflater.inflate(R.layout.track_smart_folder_empty_loading_state, parent, false);
				return new EmptySmartFolderLoadingTracksViewHolder(view);
			case TYPE_EMPTY_FOLDER_LOADING:
				view = inflater.inflate(R.layout.track_smart_folder_empty_loading_state, parent, false);
				return new EmptyFolderLoadingTracksViewHolder(view);
			case TYPE_FOLDER_STATS:
				view = inflater.inflate(R.layout.folder_stats_item, parent, false);
				return new FolderStatsViewHolder(app, view);
			case TYPE_EMPTY_TRACKS:
				view = inflater.inflate(R.layout.track_folder_empty_state, parent, false);
				return new EmptyTracksViewHolder(view, emptyTracksListener);
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof TrackItem) {
			return ((TrackItem) object).isShowCurrentTrack() ? TYPE_RECORDING_TRACK : TYPE_TRACK;
		} else if (object instanceof TrackFolder) {
			return TYPE_FOLDER;
		} else if (object instanceof SmartFolder) {
			return TYPE_SMART_FOLDER;
		} else if (object instanceof VisibleTracksGroup) {
			return TYPE_VISIBLE_TRACKS;
		} else if (object instanceof TrackFolderAnalysis) {
			return TYPE_FOLDER_STATS;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (TYPE_SORT_TRACKS == item) {
				return TYPE_SORT_TRACKS;
			} else if (TYPE_EMPTY_FOLDER == item) {
				return TYPE_EMPTY_FOLDER;
			} else if (TYPE_EMPTY_SMART_FOLDER_LOADING == item) {
				return TYPE_EMPTY_SMART_FOLDER_LOADING;
			} else if (TYPE_EMPTY_FOLDER_LOADING == item) {
				return TYPE_EMPTY_FOLDER_LOADING;
			} else if (TYPE_EMPTY_TRACKS == item) {
				return TYPE_EMPTY_TRACKS;
			} else if (TYPE_EMPTY_SMART_FOLDER == item) {
				return TYPE_EMPTY_SMART_FOLDER;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	private boolean isLastItem(int position) {
		int itemCount = getItemCount();
		boolean isStatsLastItem = items.get(itemCount - 1) instanceof TrackFolderAnalysis;
		int offset = (isStatsLastItem && itemCount >= 2) ? 2 : 1;
		return position != itemCount - offset;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		boolean lastItem = isLastItem(position);

		if (holder instanceof SortTracksViewHolder) {
			SortTracksViewHolder viewHolder = (SortTracksViewHolder) holder;
			viewHolder.bindView(hasTrackItems(), null);
		} else if (holder instanceof TrackViewHolder) {
			TrackItem trackItem = (TrackItem) items.get(position);

			TrackViewHolder viewHolder = (TrackViewHolder) holder;
			viewHolder.bindView(sortMode, trackItem, lastItem, shouldShowFolder, selectionMode);
		} else if (holder instanceof TrackFolderViewHolder) {
			TrackFolder trackFolder = (TrackFolder) items.get(position);

			TrackFolderViewHolder viewHolder = (TrackFolderViewHolder) holder;
			viewHolder.bindView(trackFolder, lastItem);
		} else if (holder instanceof VisibleTracksViewHolder) {
			VisibleTracksGroup tracksGroup = (VisibleTracksGroup) items.get(position);

			VisibleTracksViewHolder viewHolder = (VisibleTracksViewHolder) holder;
			viewHolder.bindView(tracksGroup, lastItem);
		} else if (holder instanceof RecordingTrackViewHolder) {
			TrackItem trackItem = (TrackItem) items.get(position);

			RecordingTrackViewHolder viewHolder = (RecordingTrackViewHolder) holder;
			viewHolder.bindView(trackItem);
		} else if (holder instanceof EmptyFolderViewHolder) {
			EmptyFolderViewHolder viewHolder = (EmptyFolderViewHolder) holder;
			viewHolder.bindView();
		} else if (holder instanceof EmptySmartFolderViewHolder) {
			EmptySmartFolderViewHolder viewHolder = (EmptySmartFolderViewHolder) holder;
			viewHolder.bindView();
		} else if (holder instanceof FolderStatsViewHolder) {
			TrackFolderAnalysis folderAnalysis = (TrackFolderAnalysis) items.get(position);
			FolderStatsViewHolder viewHolder = (FolderStatsViewHolder) holder;
			viewHolder.bindView(folderAnalysis);
		} else if (holder instanceof EmptyTracksViewHolder) {
			EmptyTracksViewHolder viewHolder = (EmptyTracksViewHolder) holder;
			viewHolder.bindView();
		} else if (holder instanceof EmptySmartFolderLoadingTracksViewHolder) {
			EmptySmartFolderLoadingTracksViewHolder viewHolder = (EmptySmartFolderLoadingTracksViewHolder) holder;
			viewHolder.bindView();
		} else if (holder instanceof EmptyFolderLoadingTracksViewHolder) {
			EmptyFolderLoadingTracksViewHolder viewHolder = (EmptyFolderLoadingTracksViewHolder) holder;
			viewHolder.bindView();
		} else if (holder instanceof SmartFolderViewHolder) {
			SmartFolderViewHolder viewHolder = (SmartFolderViewHolder) holder;
			viewHolder.bindView((SmartFolder) items.get(position), lastItem);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public int getItemPosition(@NonNull Object object) {
		return items.indexOf(object);
	}

	@Nullable
	public Object getItemByPosition(int position) {
		return items.size() > position ? items.get(position) : null;
	}

	public void updateItem(@NonNull Object object) {
		int index = getItemPosition(object);
		if (index != -1) {
			notifyItemChanged(index);
		}
	}

	public void onItemsSelected(@NonNull Set<?> items) {
		for (Object item : items) {
			updateItem(item);
		}
	}

	private boolean hasTrackItems() {
		for (Object o : items) {
			if (o instanceof TrackItem || o instanceof TracksGroup) {
				return true;
			}
		}
		return false;
	}
}
