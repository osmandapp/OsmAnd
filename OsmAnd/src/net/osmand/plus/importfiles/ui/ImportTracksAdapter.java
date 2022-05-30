package net.osmand.plus.importfiles.ui;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.TrackBitmapDrawer.TrackBitmapDrawerListener;
import net.osmand.plus.myplaces.TrackBitmapDrawer.TracksDrawParams;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class ImportTracksAdapter extends RecyclerView.Adapter<ViewHolder> {

	private static final int TYPE_TRACK = 0;
	protected static final int TYPE_HEADER = 1;
	protected static final int TYPE_FOOTER = 2;

	private final OsmandApplication app;

	private final GPXFile gpxFile;
	private final List<Object> items = new ArrayList<>();
	private final List<TrackItem> trackItems = new ArrayList<>();

	private Set<TrackItem> selectedTracks;
	private ImportTracksListener listener;
	private TracksDrawParams drawParams;

	private String selectedFolder;
	private final String fileName;
	private final boolean nightMode;

	public ImportTracksAdapter(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                           @NonNull String fileName, boolean nightMode) {
		this.app = app;
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.nightMode = nightMode;
	}

	public void setSelectedFolder(@Nullable String selectedFolder) {
		this.selectedFolder = selectedFolder;
	}

	public void setListener(@Nullable ImportTracksListener listener) {
		this.listener = listener;
	}

	public void setDrawParams(@NonNull TracksDrawParams drawParams) {
		this.drawParams = drawParams;
	}

	public void updateItems(@NonNull List<TrackItem> trackItems, @NonNull Set<TrackItem> selectedTracks) {
		this.trackItems.clear();
		this.trackItems.addAll(trackItems);
		this.selectedTracks = selectedTracks;

		items.clear();
		items.add(TYPE_HEADER);
		items.addAll(trackItems);
		items.add(TYPE_FOOTER);

		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case TYPE_TRACK:
				View view = inflater.inflate(R.layout.import_track_item, parent, false);
				return new TrackViewHolder(view, drawParams, listener, nightMode);
			case TYPE_HEADER:
				view = inflater.inflate(R.layout.import_tracks_header, parent, false);
				return new HeaderViewHolder(view);
			case TYPE_FOOTER:
				view = inflater.inflate(R.layout.select_folder_card, parent, false);
				return new FoldersViewHolder(view, listener, nightMode);
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof HeaderViewHolder) {
			((HeaderViewHolder) holder).bindView(app, fileName, trackItems.size(), nightMode);
		} else if (holder instanceof TrackViewHolder) {
			TrackViewHolder viewHolder = (TrackViewHolder) holder;

			TrackItem item = (TrackItem) getItem(position);
			boolean checked = selectedTracks.contains(item);
			TrackBitmapDrawerListener listener = getBitmapDrawerListener(item, viewHolder);

			viewHolder.bindView(item, gpxFile.getPoints(), checked, listener);
		} else if (holder instanceof FoldersViewHolder) {
			FoldersViewHolder viewHolder = (FoldersViewHolder) holder;
			viewHolder.bindView(selectedFolder);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	private Object getItem(int position) {
		return items.get(position);
	}

	protected int getItemPosition(@NonNull Object object) {
		return items.indexOf(object);
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof TrackItem) {
			return TYPE_TRACK;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (TYPE_HEADER == item) {
				return TYPE_HEADER;
			} else if (TYPE_FOOTER == item) {
				return TYPE_FOOTER;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	private TrackBitmapDrawerListener getBitmapDrawerListener(@NonNull TrackItem item, @NonNull TrackViewHolder holder) {
		return new TrackBitmapDrawerListener() {
			@Override
			public void onTrackBitmapDrawing() {

			}

			@Override
			public void onTrackBitmapDrawn(boolean success) {
				if (!success) {
					item.bitmapDrawer.initAndDraw();
				}
			}

			@Override
			public boolean isTrackBitmapSelectionSupported() {
				return false;
			}

			@Override
			public void drawTrackBitmap(Bitmap bitmap) {
				item.bitmap = bitmap;
				notifyItemChanged(holder.getAdapterPosition());
			}
		};
	}

	public interface ImportTracksListener {

		void onAddFolderSelected();

		void onFoldersListSelected();

		void onFolderSelected(@NonNull String folderName);

		void onTrackItemSelected(@NonNull TrackItem item, boolean selected);

		void onTrackItemPointsSelected(@NonNull TrackItem item);

	}
}