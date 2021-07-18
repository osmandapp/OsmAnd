package net.osmand.plus.track;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.PlatformUtil;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.myplaces.GPXTabItemType;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static net.osmand.plus.liveupdates.LiveUpdatesFragment.getDefaultIconColorId;

public class GpxBlockStatisticsBuilder {

	private static final Log LOG = PlatformUtil.getLog(GpxBlockStatisticsBuilder.class);
	private static final int BLOCKS_UPDATE_INTERVAL = 1000;

	private final OsmandApplication app;
	private final boolean nightMode;

	private RecyclerView blocksView;
	private final SelectedGpxFile selectedGpxFile;
	private GPXTrackAnalysis analysis;

	private BlockStatisticsAdapter adapter;
	private final List<StatBlock> items = new ArrayList<>();
	private boolean blocksClickable = true;
	private GPXTabItemType tabItem = null;

	private final Handler handler = new Handler();
	private Runnable updatingItems;
	private boolean updateRunning = false;

	public GpxBlockStatisticsBuilder(OsmandApplication app, SelectedGpxFile selectedGpxFile, boolean nightMode) {
		this.app = app;
		this.selectedGpxFile = selectedGpxFile;
		this.nightMode = nightMode;
	}

	public boolean isUpdateRunning() {
		return updateRunning;
	}

	public void setBlocksClickable(boolean blocksClickable) {
		this.blocksClickable = blocksClickable;
	}

	public void setBlocksView(final RecyclerView blocksView, boolean isParentExpandable) {
		this.blocksView = blocksView;
		if (isParentExpandable) {
			blocksView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					if (blocksView.getHeight() != 0) {
						ViewTreeObserver obs = blocksView.getViewTreeObserver();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							obs.removeOnGlobalLayoutListener(this);
						} else {
							obs.removeGlobalOnLayoutListener(this);
						}
						blocksView.setMinimumHeight(blocksView.getHeight());
					}
				}
			});
		}
	}

	public void setTabItem(GPXTabItemType tabItem) {
		this.tabItem = tabItem;
	}

	@Nullable
	public GpxDisplayItem getDisplayItem(GPXFile gpxFile) {
		return gpxFile.tracks.size() > 0 ? GpxUiHelper.makeGpxDisplayItem(app, gpxFile, false) : null;
	}

	private GPXFile getGPXFile() {
		return selectedGpxFile.getGpxFile();
	}

	public void initStatBlocks(@Nullable SegmentActionsListener actionsListener, @ColorInt int activeColor,
	                           @Nullable GPXTrackAnalysis analysis) {
		initItems(analysis);
		adapter = new BlockStatisticsAdapter(getDisplayItem(getGPXFile()), actionsListener, activeColor);
		adapter.setItems(items);
		blocksView.setLayoutManager(new LinearLayoutManager(app, LinearLayoutManager.HORIZONTAL, false));
		blocksView.setAdapter(adapter);
		AndroidUiHelper.updateVisibility(blocksView, !Algorithms.isEmpty(items));
	}

	public void stopUpdatingStatBlocks() {
		handler.removeCallbacks(updatingItems);
		updateRunning = false;
	}

	public void runUpdatingStatBlocksIfNeeded() {
		if (!isUpdateRunning()) {
			updatingItems = new Runnable() {
				@Override
				public void run() {
					initItems();
					if (adapter != null) {
						adapter.setItems(items);
					}
					AndroidUiHelper.updateVisibility(blocksView, !Algorithms.isEmpty(items));
					int interval = app.getSettings().SAVE_GLOBAL_TRACK_INTERVAL.get();
					updateRunning = handler.postDelayed(this, Math.max(BLOCKS_UPDATE_INTERVAL, interval));
				}
			};
			updateRunning = handler.post(updatingItems);
		}
	}

	public void restartUpdatingStatBlocks() {
		stopUpdatingStatBlocks();
		runUpdatingStatBlocksIfNeeded();
	}

	public void initItems() {
		initItems(null);
	}

	public void initItems(@Nullable GPXTrackAnalysis initAnalysis) {
		GPXFile gpxFile = getGPXFile();
		if (app == null || gpxFile == null) {
			return;
		}
		boolean withoutGaps = false;
		if (initAnalysis == null) {
			withoutGaps = true;
			if (gpxFile.equals(app.getSavingTrackHelper().getCurrentGpx())) {
				GPXFile currentGpx = app.getSavingTrackHelper().getCurrentTrack().getGpxFile();
				analysis = currentGpx.getAnalysis(0);
				withoutGaps = !selectedGpxFile.isJoinSegments()
						&& (Algorithms.isEmpty(currentGpx.tracks) || currentGpx.tracks.get(0).generalTrack);
			} else {
				GpxDisplayItem gpxDisplayItem = getDisplayItem(gpxFile);
				if (gpxDisplayItem != null) {
					analysis = gpxDisplayItem.analysis;
					withoutGaps = !selectedGpxFile.isJoinSegments() && gpxDisplayItem.isGeneralTrack();
				}
			}
		} else {
			analysis = initAnalysis;
		}
		items.clear();
		if (analysis != null) {
			if (tabItem == null) {
				float totalDistance = withoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;
				String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
				String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
				String minElevation = OsmAndFormatter.getFormattedAlt(analysis.minElevation, app);
				String maxElevation = OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app);
				String avg = OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app);
				String maxSpeed = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);
				float timeSpan = withoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan;
				long timeMoving = withoutGaps ? analysis.timeMovingWithoutGaps : analysis.timeMoving;
				prepareDataDistance(totalDistance);
				prepareDataAscent(asc);
				prepareDataDescent(desc);
				prepareDataAltitudeRange(minElevation, maxElevation);
				prepareDataAverageSpeed(avg);
				prepareDataMaximumSpeed(maxSpeed);
				prepareDataTimeSpan(timeSpan);
				prepareDataTimeMoving(timeMoving);
			} else {
				switch (tabItem) {
					case GPX_TAB_ITEM_GENERAL: {
						float totalDistance = withoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;
						float timeSpan = withoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan;
						Date start = new Date(analysis.startTime);
						Date end = new Date(analysis.endTime);
						prepareDataDistance(totalDistance);
						prepareDataTimeSpan(timeSpan);
						prepareDataStartTime(start);
						prepareDataEndTime(end);
						break;
					}
					case GPX_TAB_ITEM_ALTITUDE: {
						String min = OsmAndFormatter.getFormattedAlt(analysis.minElevation, app);
						String max = OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app);
						String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
						String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
						prepareDataAverageAltitude();
						prepareDataAltitudeRange(min, max);
						prepareDataAscent(asc);
						prepareDataDescent(desc);
						break;
					}
					case GPX_TAB_ITEM_SPEED: {
						String avg = OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app);
						String max = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);
						long timeMoving = withoutGaps ? analysis.timeMovingWithoutGaps : analysis.timeMoving;
						float totalDistanceMoving = withoutGaps ? analysis.totalDistanceMovingWithoutGaps : analysis.totalDistanceMoving;
						prepareDataAverageSpeed(avg);
						prepareDataMaximumSpeed(max);
						prepareDataTimeMoving(timeMoving);
						prepareDataDistanceCorrected(totalDistanceMoving);
						break;
					}
				}
			}
		}
	}

	public void prepareDataDistance(float totalDistance) {
		prepareData(app.getString(R.string.distance), OsmAndFormatter.getFormattedDistance(totalDistance, app),
				R.drawable.ic_action_track_16, GPXDataSetType.ALTITUDE, GPXDataSetType.SPEED, ItemType.ITEM_DISTANCE);
	}

	public void prepareDataAverageAltitude() {
		prepareData(app.getString(R.string.average_altitude), OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app),
				R.drawable.ic_action_altitude_average_16, GPXDataSetType.ALTITUDE, null, ItemType.ITEM_ALTITUDE);
	}

	public void prepareDataAltitudeRange(String min, String max) {
		prepareData(app.getString(R.string.altitude_range), min + " - " + max,
				R.drawable.ic_action_altitude_range_16, GPXDataSetType.ALTITUDE, null, ItemType.ITEM_ALTITUDE);
	}

	public void prepareDataAscent(String asc) {
		prepareData(app.getString(R.string.altitude_ascent), asc,
				R.drawable.ic_action_arrow_up_16, R.color.gpx_chart_red,
				GPXDataSetType.SLOPE, null, ItemType.ITEM_ALTITUDE);
	}

	public void prepareDataDescent(String desc) {
		prepareData(app.getString(R.string.altitude_descent), desc,
				R.drawable.ic_action_arrow_down_16, R.color.gpx_pale_green,
				GPXDataSetType.ALTITUDE, GPXDataSetType.SLOPE, ItemType.ITEM_ALTITUDE);
	}

	public void prepareDataAverageSpeed(String avg) {
		prepareData(app.getString(R.string.average_speed), avg,
				R.drawable.ic_action_speed_16, GPXDataSetType.SPEED, null, ItemType.ITEM_SPEED);
	}

	public void prepareDataMaximumSpeed(String max) {
		prepareData(app.getString(R.string.max_speed), max,
				R.drawable.ic_action_max_speed_16, GPXDataSetType.SPEED, null, ItemType.ITEM_SPEED);
	}

	public void prepareDataTimeMoving(long timeMoving) {
		prepareData(app.getString(R.string.moving_time), Algorithms.formatDuration((int) (timeMoving / 1000), app.accessibilityEnabled()),
				R.drawable.ic_action_time_moving_16, GPXDataSetType.SPEED, null, ItemType.ITEM_TIME_MOVING);
	}

	public void prepareDataDistanceCorrected(float totalDistanceMoving) {
		prepareData(app.getString(R.string.distance_moving),
				OsmAndFormatter.getFormattedDistance(totalDistanceMoving, app),
				R.drawable.ic_action_polygom_dark, GPXDataSetType.SPEED, null, ItemType.ITEM_DISTANCE_MOVING);
	}

	public void prepareDataTimeSpan(float timeSpan) {
		prepareData(app.getString(R.string.shared_string_time_span),
				Algorithms.formatDuration((int) (timeSpan / 1000), app.accessibilityEnabled()),
				R.drawable.ic_action_time_span_16, GPXDataSetType.SPEED, null, ItemType.ITEM_TIME_SPAN);
	}

	public void prepareDataStartTime(Date start) {
		DateFormat dtf = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		prepareData(app.getString(R.string.shared_string_start_time), dtf.format(start),
				R.drawable.ic_action_time_start_16, GPXDataSetType.SPEED, null, ItemType.ITEM_TIME);
	}

	public void prepareDataEndTime(Date end) {
		DateFormat dtf = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		prepareData(app.getString(R.string.shared_string_end_time), dtf.format(end),
				R.drawable.ic_action_time_end_16, GPXDataSetType.SPEED, null, ItemType.ITEM_TIME);
	}

	public void prepareData(String title, String value, @DrawableRes int imageResId,
							GPXDataSetType firstType, GPXDataSetType secondType, ItemType itemType) {
		prepareData(title, value, imageResId, getDefaultIconColorId(nightMode), firstType, secondType, itemType);
	}

	public void prepareData(String title, String value, @DrawableRes int imageResId, @ColorRes int imageColorId,
							GPXDataSetType firstType, GPXDataSetType secondType, ItemType itemType) {
		if (analysis == null) {
			return;
		}
		StatBlock statBlock = new StatBlock(title, value, imageResId, imageColorId, firstType, secondType, itemType);
		switch (statBlock.itemType) {
			case ITEM_DISTANCE: {
				if (analysis.totalDistance != 0f) {
					items.add(statBlock);
				}
				break;
			}
			case ITEM_DISTANCE_MOVING: {
				if (analysis.totalDistanceMoving != 0f) {
					items.add(statBlock);
				}
				break;
			}
			case ITEM_ALTITUDE: {
				if (analysis.hasElevationData) {
					items.add(statBlock);
				}
				break;
			}
			case ITEM_SPEED: {
				if (analysis.isSpeedSpecified()) {
					items.add(statBlock);
				}
				break;
			}
			case ITEM_TIME: {
				if (analysis.timeSpan > 0) {
					items.add(statBlock);
				}
				break;
			}
			case ITEM_TIME_SPAN: {
				if (analysis.hasSpeedData) {
					items.add(statBlock);
				}
				break;
			}
			case ITEM_TIME_MOVING: {
				if (analysis.isTimeMoving()) {
					items.add(statBlock);
				}
				break;
			}
		}
	}

	public static class StatBlock {
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
		ITEM_DISTANCE_MOVING,
		ITEM_ALTITUDE,
		ITEM_SPEED,
		ITEM_TIME,
		ITEM_TIME_SPAN,
		ITEM_TIME_MOVING;
	}

	private class BlockStatisticsAdapter extends RecyclerView.Adapter<BlockStatisticsViewHolder> {

		private final List<StatBlock> items = new ArrayList<>();
		private final GpxDisplayItem displayItem;
		private final SegmentActionsListener actionsListener;
		@ColorInt
		private final int activeColor;
		private final int minWidthPx;
		private final int maxWidthPx;
		private final int textSize;

		public BlockStatisticsAdapter(GpxDisplayItem displayItem, SegmentActionsListener actionsListener,
									  @ColorInt int activeColor) {
			this.displayItem = displayItem;
			this.actionsListener = actionsListener;
			this.activeColor = activeColor;
			minWidthPx = AndroidUtils.dpToPx(app, 60f);
			maxWidthPx = AndroidUtils.dpToPx(app, 120f);
			textSize = app.getResources().getDimensionPixelSize(R.dimen.default_desc_text_size);
		}

		@Override
		public int getItemCount() {
			return items.size();
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
			final StatBlock item = items.get(position);
			holder.valueText.setText(item.value);
			holder.valueText.setTextColor(activeColor);
			holder.titleText.setText(item.title);
			holder.titleText.setTextColor(ContextCompat.getColor(app, R.color.text_color_secondary_light));
			float letterSpacing = 0.00f;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				letterSpacing = Math.max(holder.valueText.getLetterSpacing(), holder.titleText.getLetterSpacing());
			}
			holder.titleText.setMinWidth(calculateWidthWithin(letterSpacing, item.title, item.value));
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					GPXTrackAnalysis analysis = displayItem != null ? displayItem.analysis : null;
					if (blocksClickable && analysis != null && actionsListener != null) {
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
			Drawable icon = app.getUIUtilities().getIcon(item.imageResId, item.imageColorId);
			holder.imageView.setImageDrawable(icon);
			AndroidUtils.setBackgroundColor(app, holder.divider, nightMode, R.color.divider_color_light, R.color.divider_color_dark);
			AndroidUiHelper.updateVisibility(holder.divider, position != items.size() - 1);
		}

		public void setItems(List<StatBlock> items) {
			this.items.clear();
			this.items.addAll(items);
			notifyDataSetChanged();
		}

		public int calculateWidthWithin(float letterSpacing, String... texts) {
			int textWidth = AndroidUtils.getTextMaxWidth(textSize, Arrays.asList(texts));
			if (letterSpacing != 0.00f) {
				textWidth += Math.ceil(textWidth * letterSpacing);
			}
			return Math.min(maxWidthPx, Math.max(minWidthPx, textWidth));
		}
	}

	private static class BlockStatisticsViewHolder extends RecyclerView.ViewHolder {

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
