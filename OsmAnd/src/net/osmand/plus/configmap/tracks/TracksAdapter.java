package net.osmand.plus.configmap.tracks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.NoVisibleTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.RecentlyVisibleViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GPXInfo;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class TracksAdapter extends RecyclerView.Adapter<ViewHolder> {

	public static final int TYPE_TRACK = 0;
	public static final int TYPE_NO_TRACKS = 1;
	public static final int TYPE_NO_VISIBLE_TRACKS = 2;
	public static final int TYPE_RECENTLY_VISIBLE_TRACKS = 3;

	private final OsmandApplication app;
	private final TrackTab trackTab;
	private final List<Object> items = new ArrayList<>();
	private final TracksFragment fragment;

	private final boolean nightMode;

	public TracksAdapter(@NonNull OsmandApplication app, @NonNull TrackTab trackTab, @NonNull TracksFragment fragment, boolean nightMode) {
		this.app = app;
		this.trackTab = trackTab;
		this.fragment = fragment;
		this.nightMode = nightMode;
		this.items.addAll(trackTab.items);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case TYPE_TRACK:
				View view = inflater.inflate(R.layout.track_list_item, parent, false);
				return new TrackViewHolder(view, fragment, nightMode);
			case TYPE_NO_TRACKS:
				view = inflater.inflate(R.layout.empty_state, parent, false);
				return new EmptyTracksViewHolder(view, fragment, nightMode);
			case TYPE_NO_VISIBLE_TRACKS:
				view = inflater.inflate(R.layout.empty_state, parent, false);
				return new NoVisibleTracksViewHolder(view, fragment, nightMode);
			case TYPE_RECENTLY_VISIBLE_TRACKS:
				view = inflater.inflate(R.layout.list_header_switch_item, parent, false);
				return new RecentlyVisibleViewHolder(view, fragment, nightMode);
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}

	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof GPXInfo) {
			return TYPE_TRACK;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (TYPE_NO_TRACKS == item) {
				return TYPE_NO_TRACKS;
			} else if (TYPE_NO_VISIBLE_TRACKS == item) {
				return TYPE_NO_VISIBLE_TRACKS;
			} else if (TYPE_RECENTLY_VISIBLE_TRACKS == item) {
				return TYPE_RECENTLY_VISIBLE_TRACKS;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof TrackViewHolder) {
			GPXInfo gpxInfo = (GPXInfo) items.get(position);
			TrackViewHolder viewHolder = (TrackViewHolder) holder;
			boolean lastItem = position == getItemCount() - 1;
			String folderName = getFolderName(gpxInfo);
			viewHolder.bindView(gpxInfo, folderName, lastItem);
			bindInfoView(gpxInfo, viewHolder);
		} else if (holder instanceof NoVisibleTracksViewHolder) {
			((NoVisibleTracksViewHolder) holder).bindView();
		} else if (holder instanceof EmptyTracksViewHolder) {
			((EmptyTracksViewHolder) holder).bindView();
		} else if (holder instanceof RecentlyVisibleViewHolder) {
			((RecentlyVisibleViewHolder) holder).bindView();
		}
	}


	@Nullable
	private String getFolderName(@NonNull GPXInfo gpxInfo) {
		String folderName = null;
		File file = gpxInfo.getFile();
		if (trackTab.type.shouldShowFolder() && file != null) {
			String[] path = file.getAbsolutePath().split(File.separator);
			folderName = path.length > 1 ? path[path.length - 2] : null;
		}
		return folderName;
	}

	private void bindInfoView(@NonNull GPXInfo gpxInfo, @NonNull TrackViewHolder holder) {
		File file = gpxInfo.getFile();
		if (file == null) {
			return;
		}
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(file, new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return fragment.isAdded();
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				if (item != null) {
					holder.bindInfoRow(gpxInfo, item);
				}
			}
		});
		if (dataItem != null) {
			holder.bindInfoRow(gpxInfo, dataItem);
		}
	}


	public void onGpxInfosSelected(@NonNull Set<GPXInfo> gpxInfos) {
		for (GPXInfo gpxInfo : gpxInfos) {
			updateItem(gpxInfo);
		}
		updateItem(TYPE_RECENTLY_VISIBLE_TRACKS);
	}

	private void updateItem(@NonNull Object object) {
		int index = items.indexOf(object);
		if (index != -1) {
			notifyItemChanged(index);
		}
	}

	@Override
	public int getItemCount() {
		return trackTab.items.size();
	}
}