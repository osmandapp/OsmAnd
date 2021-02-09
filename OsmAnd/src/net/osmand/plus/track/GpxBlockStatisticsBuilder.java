package net.osmand.plus.track;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class GpxBlockStatisticsBuilder {

	private final OsmandApplication app;
	private RecyclerView blocksView;
	private final SelectedGpxFile selectedGpxFile;

	public GpxBlockStatisticsBuilder(OsmandApplication app, SelectedGpxFile selectedGpxFile) {
		this.app = app;
		this.selectedGpxFile = selectedGpxFile;
	}

	public void setBlocksView(RecyclerView blocksView) {
		this.blocksView = blocksView;
	}

	private GpxDisplayItem getDisplayItem(GPXFile gpxFile) {
		return GpxUiHelper.makeGpxDisplayItem(app, gpxFile);
	}

	private GPXFile getGPXFile() {
		return selectedGpxFile.getGpxFile();
	}

	public void initStatBlocks(SegmentActionsListener actionsListener, @ColorInt int activeColor, boolean nightMode) {
		List<StatBlock> items = new ArrayList<>();
		GPXFile gpxFile = getGPXFile();
		GpxDisplayItem gpxDisplayItem = null;
		GPXTrackAnalysis analysis = null;
		if (gpxFile.tracks.size() > 0) {
			gpxDisplayItem = getDisplayItem(gpxFile);
			analysis = gpxDisplayItem.analysis;
		}
		if (gpxDisplayItem != null && analysis != null) {
			boolean withoutGaps = !selectedGpxFile.isJoinSegments() && gpxDisplayItem.isGeneralTrack();
			float totalDistance = withoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;
			float timeSpan = withoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan;
			String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
			String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
			String avg = OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app);
			String max = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);

			prepareData(analysis, items, app.getString(R.string.distance), OsmAndFormatter.getFormattedDistance(totalDistance, app),
					R.drawable.ic_action_track_16, R.color.icon_color_default_light, GPXDataSetType.ALTITUDE, GPXDataSetType.SPEED, ItemType.ITEM_DISTANCE);
			prepareData(analysis, items, app.getString(R.string.altitude_ascent), asc,
					R.drawable.ic_action_arrow_up_16, R.color.gpx_chart_red, GPXDataSetType.SLOPE, null, ItemType.ITEM_ALTITUDE);
			prepareData(analysis, items, app.getString(R.string.altitude_descent), desc,
					R.drawable.ic_action_arrow_down_16, R.color.gpx_pale_green, GPXDataSetType.ALTITUDE, GPXDataSetType.SLOPE, ItemType.ITEM_ALTITUDE);
			prepareData(analysis, items, app.getString(R.string.average_speed), avg,
					R.drawable.ic_action_speed_16, R.color.icon_color_default_light, GPXDataSetType.SPEED, null, ItemType.ITEM_SPEED);
			prepareData(analysis, items, app.getString(R.string.max_speed), max,
					R.drawable.ic_action_max_speed_16, R.color.icon_color_default_light, GPXDataSetType.SPEED, null, ItemType.ITEM_SPEED);
			prepareData(analysis, items, app.getString(R.string.shared_string_time_span),
					Algorithms.formatDuration((int) (timeSpan / 1000), app.accessibilityEnabled()),
					R.drawable.ic_action_time_span_16, R.color.icon_color_default_light, GPXDataSetType.SPEED, null, ItemType.ITEM_TIME);
		}
		boolean isNotEmpty = !Algorithms.isEmpty(items);
		AndroidUiHelper.updateVisibility(blocksView, isNotEmpty);
		if (isNotEmpty && gpxDisplayItem != null) {
			final BlockStatisticsAdapter sbAdapter = new BlockStatisticsAdapter(items, gpxDisplayItem, actionsListener, activeColor, nightMode);
			blocksView.setLayoutManager(new LinearLayoutManager(app, LinearLayoutManager.HORIZONTAL, false));
			blocksView.setAdapter(sbAdapter);
		}
	}

	public void prepareData(GPXTrackAnalysis analysis, List<StatBlock> listItems, String title,
							String value, @DrawableRes int imageResId, @ColorRes int imageColorId,
							GPXDataSetType firstType, GPXDataSetType secondType, ItemType itemType) {
		StatBlock statBlock = new StatBlock(title, value, imageResId, imageColorId, firstType, secondType, itemType);
		switch (statBlock.itemType) {
			case ITEM_DISTANCE: {
				if (analysis.totalDistance != 0f) {
					listItems.add(statBlock);
				}
				break;
			}
			case ITEM_ALTITUDE: {
				if (analysis.hasElevationData) {
					listItems.add(statBlock);
				}
				break;
			}
			case ITEM_SPEED: {
				if (analysis.isSpeedSpecified()) {
					listItems.add(statBlock);
				}
				break;
			}
			case ITEM_TIME: {
				if (analysis.hasSpeedData) {
					listItems.add(statBlock);
				}
				break;
			}
		}
	}

	private void setImageDrawable(ImageView iv, @DrawableRes Integer resId, @ColorRes int color) {
		Drawable icon = resId != null ? app.getUIUtilities().getIcon(resId, color)
				: UiUtilities.tintDrawable(iv.getDrawable(), getResolvedColor(color));
		iv.setImageDrawable(icon);
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(app, colorId);
	}

	public class StatBlock {
		private final String title;
		private final String value;
		private final int imageResId;
		private final int imageColorId;
		private final GPXDataSetType firstType;
		private final GPXDataSetType secondType;
		private final ItemType itemType;

		public StatBlock(String title, String value, @DrawableRes int imageResId, @ColorRes int imageColorId,
						 GPXDataSetType firstType, GPXDataSetType secondType, ItemType itemType) {
			this.title = title;
			this.value = value;
			this.imageResId = imageResId;
			this.imageColorId = imageColorId;
			this.firstType = firstType;
			this.secondType = secondType;
			this.itemType = itemType;
		}
	}

	public enum ItemType {
		ITEM_DISTANCE,
		ITEM_ALTITUDE,
		ITEM_SPEED,
		ITEM_TIME;
	}

	private class BlockStatisticsAdapter extends RecyclerView.Adapter<BlockStatisticsViewHolder> {

		private final List<StatBlock> statBlocks;
		private final GpxDisplayItem displayItem;
		private final SegmentActionsListener actionsListener;
		@ColorInt
		private final int activeColor;
		private final boolean nightMode;

		public BlockStatisticsAdapter(List<StatBlock> statBlocks, GpxDisplayItem displayItem,
									  SegmentActionsListener actionsListener, @ColorInt int activeColor, boolean nightMode) {
			this.statBlocks = statBlocks;
			this.displayItem = displayItem;
			this.actionsListener = actionsListener;
			this.activeColor = activeColor;
			this.nightMode = nightMode;
		}

		@Override
		public int getItemCount() {
			return statBlocks.size();
		}

		@NonNull
		@Override
		public BlockStatisticsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View itemView = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_gpx_stat_block, parent, false);
			return new BlockStatisticsViewHolder(itemView);
		}

		@Override
		public void onBindViewHolder(BlockStatisticsViewHolder holder, int position) {
			final StatBlock item = statBlocks.get(position);
			holder.valueText.setText(item.value);
			holder.titleText.setText(item.title);
			holder.valueText.setTextColor(activeColor);
			holder.titleText.setTextColor(app.getResources().getColor(R.color.text_color_secondary_light));
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					GPXTrackAnalysis analysis = displayItem.analysis;
					if (displayItem != null && analysis != null) {
						ArrayList<GPXDataSetType> list = new ArrayList<>();
						if (analysis.hasElevationData || analysis.isSpeedSpecified() || analysis.hasSpeedData) {
							if (item.firstType != null) {
								list.add(item.firstType);
							}
							if (item.secondType != null) {
								list.add(item.secondType);
							}
						}
						displayItem.chartTypes = list.size() > 0 ? list.toArray(new GPXDataSetType[0]) : null;
						displayItem.locationOnMap = displayItem.locationStart;
						actionsListener.openAnalyzeOnMap(displayItem);
					}
				}
			});
			setImageDrawable(holder.imageView, item.imageResId, item.imageColorId);
			AndroidUtils.setBackgroundColor(app, holder.divider, nightMode, R.color.divider_color_light, R.color.divider_color_dark);
			AndroidUiHelper.updateVisibility(holder.divider, position != statBlocks.size() - 1);
		}
	}

	private class BlockStatisticsViewHolder extends RecyclerView.ViewHolder {

		private final TextViewEx valueText;
		private final TextView titleText;
		private final AppCompatImageView imageView;
		private final View divider;

		public BlockStatisticsViewHolder(View view) {
			super(view);
			valueText = view.findViewById(R.id.value);
			titleText = view.findViewById(R.id.title);
			imageView = view.findViewById(R.id.image);
			divider = view.findViewById(R.id.divider);
		}
	}
}
