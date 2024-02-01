package net.osmand.plus.track;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.IndexConstants;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.ChipsAdapter.OnSelectChipListener;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GpxTrackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int TRACK_INFO_VIEW_TYPE = 0;
	private static final int TRACK_CATEGORY_VIEW_TYPE = 1;

	private final OsmandApplication app;
	private final LayoutInflater themedInflater;
	private final UiUtilities iconsCache;

	private List<GPXInfo> gpxInfoList;
	private Map<String, List<GPXInfo>> gpxInfoCategories = new HashMap<>();

	private OnItemClickListener onItemClickListener;
	private OnSelectChipListener onSelectChipListener;

	private String selectedCategory;
	private boolean showFolderName;
	private boolean showCurrentGpx;
	private boolean showCategories;

	public GpxTrackAdapter(@NonNull Context ctx, @NonNull List<GPXInfo> gpxInfoList) {
		app = (OsmandApplication) ctx.getApplicationContext();
		themedInflater = UiUtilities.getInflater(ctx, app.getDaynightHelper().isNightModeForMapControls());
		iconsCache = app.getUIUtilities();
		this.gpxInfoList = gpxInfoList;
	}

	@NonNull
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

	public void setShowCategories(boolean showCategories) {
		this.showCategories = showCategories;
	}

	public void setSelectedCategory(String selectedCategory) {
		this.selectedCategory = selectedCategory;
	}

	public void setOnSelectChipListener(@Nullable OnSelectChipListener onSelectChipListener) {
		this.onSelectChipListener = onSelectChipListener;
	}

	public void setGpxInfoCategories(@NonNull Map<String, List<GPXInfo>> gpxInfoCategories) {
		this.gpxInfoCategories = gpxInfoCategories;
	}

	@Override
	public int getItemViewType(int position) {
		return showCategories && position == 0 ? TRACK_CATEGORY_VIEW_TYPE : TRACK_INFO_VIEW_TYPE;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		if (viewType == TRACK_INFO_VIEW_TYPE) {
			View view = themedInflater.inflate(R.layout.gpx_track_select_item, parent, false);
			ImageView distanceIcon = view.findViewById(R.id.distance_icon);
			distanceIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_distance_16));
			ImageView pointsIcon = view.findViewById(R.id.points_icon);
			pointsIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_waypoint_16));
			ImageView timeIcon = view.findViewById(R.id.time_icon);
			timeIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_16));
			return new TrackViewHolder(view);
		} else {
			View view = themedInflater.inflate(R.layout.gpx_track_select_category_item, parent, false);
			return new TrackCategoriesViewHolder(view, gpxInfoCategories, selectedCategory, onSelectChipListener);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder.getItemViewType() == TRACK_INFO_VIEW_TYPE) {
			TrackViewHolder trackViewHolder = (TrackViewHolder) holder;
			int listPosition = mapToListPosition(position);
			boolean currentlyRecordingTrack = (showCurrentGpx && isFirstListItem(position));
			if (currentlyRecordingTrack) {
				trackViewHolder.icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_track_recordable));
			} else {
				trackViewHolder.icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_polygom_dark));
			}
			GPXInfo info = gpxInfoList.get(listPosition);
			GpxDataItem dataItem = getDataItem(info);
			String itemTitle = GpxUiHelper.getGpxTitle(info.getFileName());
			if (!showFolderName) {
				itemTitle = Algorithms.getFileWithoutDirs(itemTitle);
			}
			updateGpxInfoView(trackViewHolder, itemTitle, info, dataItem, currentlyRecordingTrack, app);
			trackViewHolder.itemView.setOnClickListener(v -> {
				if (onItemClickListener != null) {
					onItemClickListener.onItemClick(listPosition);
				}
			});
		} else {
			TrackCategoriesViewHolder viewHolder = (TrackCategoriesViewHolder) holder;
			viewHolder.chipsView.notifyDataSetChanged();
		}
	}


	@Override
	public int getItemCount() {
		return gpxInfoList.size() + (showCategories ? 1 : 0);
	}

	private int mapToListPosition(int position) {
		return showCategories ? position - 1 : position;
	}

	private boolean isFirstListItem(int position) {
		return position == 0 && !showCategories || position == 1 && showCategories;
	}

	private void updateGpxInfoView(TrackViewHolder holder, String itemTitle, GPXInfo info, GpxDataItem dataItem, boolean currentlyRecordingTrack, OsmandApplication app) {
		holder.title.setText(itemTitle.replace("/", " • ").trim());
		GPXTrackAnalysis analysis = null;
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
			DateFormat df = OsmAndFormatter.getDateFormat(app);
			long lastModified = info.getLastModified();
			if (lastModified > 0) {
				date = (df.format(new Date(lastModified)));
			}
			holder.dateAndSize.setText(String.format(app.getString(R.string.ltr_or_rtl_combine_via_bold_point), date, size));
		} else {
			holder.readSection.setVisibility(View.VISIBLE);
			holder.unknownSection.setVisibility(View.GONE);
			holder.pointsCount.setText(String.valueOf(analysis.getWptPoints()));
			holder.distance.setText(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
			if (analysis.isTimeSpecified()) {
				holder.time.setText(Algorithms.formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled()));
			} else {
				holder.time.setText("");
			}
		}
	}

	private GpxDataItem getDataItem(@NonNull GPXInfo info) {
		GpxDataItemCallback callback = item -> {
			if (gpxInfoList != null) {
				notifyItemChanged(gpxInfoList.indexOf(info));
			}
		};
		return app.getGpxDbHelper().getItem(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), info.getFileName()), callback);
	}

	public void setAdapterListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	static class TrackViewHolder extends RecyclerView.ViewHolder {

		private final ImageView icon;
		private final TextView title;
		private final TextView distance;
		private final TextView pointsCount;
		private final TextView time;
		private final LinearLayout readSection;
		private final LinearLayout unknownSection;
		private final TextView dateAndSize;

		TrackViewHolder(View itemView) {
			super(itemView);
			icon = itemView.findViewById(R.id.icon);
			title = itemView.findViewById(R.id.title);
			distance = itemView.findViewById(R.id.distance);
			pointsCount = itemView.findViewById(R.id.points_count);
			time = itemView.findViewById(R.id.time);
			readSection = itemView.findViewById(R.id.read_section);
			unknownSection = itemView.findViewById(R.id.unknown_section);
			dateAndSize = itemView.findViewById(R.id.date_and_size_details);
		}
	}

	static class TrackCategoriesViewHolder extends RecyclerView.ViewHolder {

		private final HorizontalChipsView chipsView;

		TrackCategoriesViewHolder(@NonNull View itemView, @NonNull Map<String, List<GPXInfo>> categories,
		                          @NonNull String selectedCategory, @Nullable OnSelectChipListener listener) {
			super(itemView);
			chipsView = itemView.findViewById(R.id.track_categories);
			chipsView.setItems(getChipItems(categories));
			chipsView.setSelected(chipsView.getChipById(selectedCategory));
			chipsView.setOnSelectChipListener(chip -> {
				chipsView.smoothScrollTo(chip);
				if (listener != null) {
					listener.onSelectChip(chip);
				}
				return true;
			});
		}

		@NonNull
		private List<ChipItem> getChipItems(@NonNull Map<String, List<GPXInfo>> categories) {
			List<ChipItem> items = new ArrayList<>();
			for (String title : categories.keySet()) {
				ChipItem item = new ChipItem(title);
				item.title = title;
				item.contentDescription = title;
				items.add(item);
			}
			return items;
		}
	}

	public interface OnItemClickListener {

		void onItemClick(int position);

	}
}