package net.osmand.plus.helpers;

import android.app.Activity;
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
import net.osmand.plus.GPXDatabase;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class GpxTrackAdapter extends RecyclerView.Adapter<GpxTrackAdapter.TrackViewHolder> {

	private LayoutInflater themedInflater;
	private List<GpxUiHelper.GPXInfo> myImageNameList;
	private OsmandApplication app;
	private boolean showCurrentGpx;
	private OnItemClickListener onItemClickListener;
	private UiUtilities iconsCache;


	GpxTrackAdapter(Activity activity, List<GpxUiHelper.GPXInfo> myImageNameList, boolean showCurrentGpx,
	                OnItemClickListener onItemClickListener) {
		this.showCurrentGpx = showCurrentGpx;
		this.onItemClickListener = onItemClickListener;
		app = (OsmandApplication) activity.getApplication();
		boolean nightMode = !app.getSettings().isLightContent();
		themedInflater = UiUtilities.getInflater(activity, nightMode);
		this.myImageNameList = myImageNameList;
		iconsCache = app.getUIUtilities();
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
	public void onBindViewHolder(@NonNull GpxTrackAdapter.TrackViewHolder holder, final int position) {
		boolean currentlyRecordingTrack = (showCurrentGpx && position == 0);
		if (currentlyRecordingTrack) {
			holder.icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_track_recordable));
		} else {
			holder.icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_polygom_dark));
		}
		GpxUiHelper.GPXInfo info = myImageNameList.get(position);
		GPXDatabase.GpxDataItem dataItem = getDataItem(info);
		String itemTitle = GpxUiHelper.getGpxTitle(info.getFileName());
		updateGpxInfoView(holder, itemTitle, info, dataItem, currentlyRecordingTrack, app);
		holder.bind(position, onItemClickListener);
	}

	@Override
	public int getItemCount() {
		return myImageNameList.size();
	}

	private void updateGpxInfoView(TrackViewHolder holder, String itemTitle, GpxUiHelper.GPXInfo info,
	                               GPXDatabase.GpxDataItem dataItem, boolean currentlyRecordingTrack,
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

	private GPXDatabase.GpxDataItem getDataItem(GpxUiHelper.GPXInfo info) {
		return app.getGpxDbHelper().getItem(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), info.getFileName()));
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

		void bind(final int position, final OnItemClickListener onItemClickListener) {
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onItemClickListener.onItemClick(position);
				}
			});
		}
	}

	public interface OnItemClickListener {

		void onItemClick(int position);

	}
}
