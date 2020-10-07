package net.osmand.plus.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class GpxTrackAdapter extends RecyclerView.Adapter<GpxTrackAdapter.TrackViewHolder> {

	private OsmandApplication app;
	private LayoutInflater themedInflater;
	private UiUtilities iconsCache;
	private List<GPXInfo> gpxInfoList;
	private OnItemClickListener onItemClickListener;

	private boolean showFolderName;
	private boolean showCurrentGpx;

	public GpxTrackAdapter(Context ctx, List<GPXInfo> gpxInfoList, boolean showCurrentGpx, boolean showFolderName) {
		app = (OsmandApplication) ctx.getApplicationContext();
		themedInflater = UiUtilities.getInflater(ctx, app.getDaynightHelper().isNightModeForMapControls());
		iconsCache = app.getUIUtilities();
		this.gpxInfoList = gpxInfoList;
		this.showFolderName = showFolderName;
		this.showCurrentGpx = showCurrentGpx;
	}

	public List<GPXInfo> getGpxInfoList() {
		return gpxInfoList;
	}

	public void setGpxInfoList(List<GPXInfo> gpxInfoList) {
		this.gpxInfoList = gpxInfoList;
	}

	public void setShowCurrentGpx(boolean showCurrentGpx) {
		this.showCurrentGpx = showCurrentGpx;
	}

	public void setShowFolderName(boolean showFolderName) {
		this.showFolderName = showFolderName;
	}

	@NonNull
	@Override
	public GpxTrackAdapter.TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = themedInflater.inflate(R.layout.gpx_track_select_item, parent, false);
		ImageView distanceIcon = view.findViewById(R.id.distance_icon);
		distanceIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_distance_16));
		ImageView pointsIcon = view.findViewById(R.id.points_icon);
		pointsIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_waypoint_16));
		ImageView timeIcon = view.findViewById(R.id.time_icon);
		timeIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_16));
		return new TrackViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final GpxTrackAdapter.TrackViewHolder holder, final int position) {
		boolean currentlyRecordingTrack = (showCurrentGpx && position == 0);
		if (currentlyRecordingTrack) {
			holder.icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_track_recordable));
		} else {
			holder.icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_polygom_dark));
		}
		final int adapterPosition = holder.getAdapterPosition();
		GPXInfo info = gpxInfoList.get(adapterPosition);
		GpxDataItem dataItem = getDataItem(info);
		String itemTitle = GpxUiHelper.getGpxTitle(info.getFileName());
		if (!showFolderName) {
			itemTitle = Algorithms.getFileWithoutDirs(itemTitle);
		}
		updateGpxInfoView(holder, itemTitle, info, dataItem, currentlyRecordingTrack, app);
		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onItemClickListener != null) {
					onItemClickListener.onItemClick(adapterPosition);
				}
			}
		});
	}

	@Override
	public int getItemCount() {
		return gpxInfoList.size();
	}

	private void updateGpxInfoView(TrackViewHolder holder, String itemTitle, GPXInfo info,
	                               GpxDataItem dataItem, boolean currentlyRecordingTrack,
	                               OsmandApplication app) {
		holder.name.setText(itemTitle.replace("/", " • ").trim());
		GPXUtilities.GPXTrackAnalysis analysis = null;
		if (currentlyRecordingTrack) {
			analysis = app.getSavingTrackHelper().getCurrentTrack().getTrackAnalysis(app);
		} else if (dataItem != null) {
			analysis = dataItem.getAnalysis();
		}
		if (analysis == null) {
			holder.readSection.setVisibility(View.GONE);
			holder.unknownSection.setVisibility(View.VISIBLE);
			String date = "";
			String size = "";
			if (info.getFileSize() >= 0) {
				size = AndroidUtils.formatSize(app, info.getFileSize());
			}
			DateFormat df = app.getResourceManager().getDateFormat();
			long fd = info.getLastModified();
			if (fd > 0) {
				date = (df.format(new Date(fd)));
			}
			holder.dateAndSize.setText(String.format(app.getString(R.string.ltr_or_rtl_combine_via_bold_point), date, size));
		} else {
			holder.readSection.setVisibility(View.VISIBLE);
			holder.unknownSection.setVisibility(View.GONE);
			holder.pointsCount.setText(String.valueOf(analysis.wptPoints));
			holder.distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
			if (analysis.isTimeSpecified()) {
				holder.time.setText(Algorithms.formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()));
			} else {
				holder.time.setText("");
			}
		}
	}

	private GpxDataItem getDataItem(final GPXInfo info) {
		GpxDbHelper.GpxDataItemCallback gpxDataItemCallback = new GpxDbHelper.GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				if (item != null && gpxInfoList != null && info != null) {
					notifyItemChanged(gpxInfoList.indexOf(info));
				}
			}
		};
		return app.getGpxDbHelper().getItem(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), info.getFileName())
				, gpxDataItemCallback);
	}

	public void setAdapterListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	static class TrackViewHolder extends RecyclerView.ViewHolder {

		ImageView icon;
		TextView name;
		TextView distance;
		TextView pointsCount;
		TextView time;
		LinearLayout readSection;
		LinearLayout unknownSection;
		TextView dateAndSize;

		TrackViewHolder(View itemView) {
			super(itemView);
			icon = itemView.findViewById(R.id.icon);
			name = itemView.findViewById(R.id.name);
			distance = itemView.findViewById(R.id.distance);
			pointsCount = itemView.findViewById(R.id.points_count);
			time = itemView.findViewById(R.id.time);
			readSection = itemView.findViewById(R.id.read_section);
			unknownSection = itemView.findViewById(R.id.unknown_section);
			dateAndSize = itemView.findViewById(R.id.date_and_size_details);
		}
	}

	public interface OnItemClickListener {

		void onItemClick(int position);

	}
}
