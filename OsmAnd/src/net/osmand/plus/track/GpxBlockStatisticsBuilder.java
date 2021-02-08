package net.osmand.plus.track;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

public class GpxBlockStatisticsBuilder {

	private final OsmandApplication app;
	private RecyclerView blocksView;
	private final SelectedGpxFile selectedGpxFile;
	private final TrackDisplayHelper displayHelper;
	private final GpxDisplayItemType[] filterTypes = {GpxDisplayItemType.TRACK_SEGMENT};

	private BlockStatisticsAdapter bsAdapter;
	private final List<StatBlock> items = new ArrayList<>();

	private final Handler handler = new Handler();
	private Runnable updatingStats;

	public GpxBlockStatisticsBuilder(OsmandApplication app, SelectedGpxFile selectedGpxFile, TrackDisplayHelper displayHelper) {
		this.app = app;
		this.selectedGpxFile = selectedGpxFile;
		this.displayHelper = displayHelper;
	}

	public void setBlocksView(RecyclerView blocksView) {
		this.blocksView = blocksView;
	}

	private GPXTrackAnalysis getAnalysis() {
		return selectedGpxFile.getTrackAnalysis(app);
	}

	public void initStatBlocks(@Nullable SegmentActionsListener actionsListener, @ColorInt int activeColor, boolean nightMode) {
		initItems();
		if (Algorithms.isEmpty(items)) {
			AndroidUiHelper.updateVisibility(blocksView, false);
		} else {
			bsAdapter = new BlockStatisticsAdapter(actionsListener, activeColor, nightMode);
			bsAdapter.setItems(items);
			blocksView.setLayoutManager(new LinearLayoutManager(app, LinearLayoutManager.HORIZONTAL, false));
			blocksView.setAdapter(bsAdapter);
		}
	}

	public void stopUpdatingStatBlocks() {
		handler.removeCallbacks(updatingStats);
	}

	public void runUpdatingStatBlocks() {
		updatingStats = new Runnable() {
			@Override
			public void run() {
				Log.d("BlockStatisticsBuilder", "run: working");
				if (bsAdapter != null) {
					initItems();
					bsAdapter.setItems(items);
				}
				int interval = app.getSettings().SAVE_GLOBAL_TRACK_INTERVAL.get();
				handler.postDelayed(this, Math.max(1000, interval));
			}
		};
		handler.post(updatingStats);
	}

	public void initItems() {
		GPXTrackAnalysis analysis = getAnalysis();
		float totalDistance = analysis.totalDistance;
		float timeSpan = analysis.timeSpan;
		String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
		String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
		String avg = OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app);
		String max = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);

		items.clear();
		prepareData(analysis, app.getString(R.string.distance), OsmAndFormatter.getFormattedDistance(totalDistance, app),
				R.drawable.ic_action_track_16, R.color.icon_color_default_light, GPXDataSetType.ALTITUDE, GPXDataSetType.SPEED, ItemType.ITEM_DISTANCE);
		prepareData(analysis, app.getString(R.string.altitude_ascent), asc,
				R.drawable.ic_action_arrow_up_16, R.color.gpx_chart_red, GPXDataSetType.SLOPE, null, ItemType.ITEM_ALTITUDE);
		prepareData(analysis, app.getString(R.string.altitude_descent), desc,
				R.drawable.ic_action_arrow_down_16, R.color.gpx_pale_green, GPXDataSetType.ALTITUDE, GPXDataSetType.SLOPE, ItemType.ITEM_ALTITUDE);
		prepareData(analysis, app.getString(R.string.average_speed), avg,
				R.drawable.ic_action_speed_16, R.color.icon_color_default_light, GPXDataSetType.SPEED, null, ItemType.ITEM_SPEED);
		prepareData(analysis, app.getString(R.string.max_speed), max,
				R.drawable.ic_action_max_speed_16, R.color.icon_color_default_light, GPXDataSetType.SPEED, null, ItemType.ITEM_SPEED);
		prepareData(analysis, app.getString(R.string.shared_string_time_span),
				Algorithms.formatDuration((int) (timeSpan / 1000), app.accessibilityEnabled()),
				R.drawable.ic_action_time_span_16, R.color.icon_color_default_light, GPXDataSetType.SPEED, null, ItemType.ITEM_TIME);
	}

	public void prepareData(GPXTrackAnalysis analysis, String title, String value,
							@DrawableRes int imageResId, @ColorRes int imageColorId,
							GPXDataSetType firstType, GPXDataSetType secondType, ItemType itemType) {
		StatBlock statBlock = new StatBlock(title, value, imageResId, imageColorId, firstType, secondType, itemType);
		switch (statBlock.itemType) {
			case ITEM_DISTANCE: {
				if (analysis.totalDistance != 0f) {
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
				if (analysis.hasSpeedData) {
					items.add(statBlock);
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

		@ColorInt
		private final int activeColor;
		private final List<StatBlock> statBlocks = new ArrayList<>();
		private final boolean nightMode;
		private final SegmentActionsListener actionsListener;

		public BlockStatisticsAdapter(@Nullable SegmentActionsListener actionsListener, @ColorInt int activeColor, boolean nightMode) {
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
		public void onBindViewHolder(final BlockStatisticsViewHolder holder, int position) {
			final StatBlock item = statBlocks.get(position);
			holder.valueText.setText(item.value);
			holder.titleText.setText(item.title);
			if (handler.hasCallbacks(updatingStats)) {
				holder.titleText.setWidth(app.getResources().getDimensionPixelSize(R.dimen.map_route_buttons_width));
			}
			holder.valueText.setTextColor(activeColor);
			holder.titleText.setTextColor(app.getResources().getColor(R.color.text_color_secondary_light));
			if (actionsListener != null && displayHelper != null) {
				holder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						List<GpxDisplayGroup> groups = displayHelper.getDisplayGroups(filterTypes);
						GpxDisplayGroup group = null;
						for (GpxDisplayGroup g : groups) {
							if (g.isGeneralTrack()) {
								group = g;
							}
						}
						if (group == null && !groups.isEmpty()) {
							group = groups.get(0);
						}
						if (group != null) {
							GpxDisplayItem displayItem = group.getModifiableList().get(0);
							if (displayItem != null && displayItem.analysis != null) {
								ArrayList<GPXDataSetType> list = new ArrayList<>();
								if (displayItem.analysis.hasElevationData || displayItem.analysis.isSpeedSpecified() || displayItem.analysis.hasSpeedData) {
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
					}
				});
			}
			setImageDrawable(holder.imageView, item.imageResId, item.imageColorId);
			AndroidUtils.setBackgroundColor(app, holder.divider, nightMode, R.color.divider_color_light, R.color.divider_color_dark);
			AndroidUiHelper.updateVisibility(holder.divider, position != statBlocks.size() - 1);
		}

		public void setItems(List<StatBlock> statBlocks) {
			this.statBlocks.clear();
			this.statBlocks.addAll(statBlocks);
			notifyItemRangeChanged(0, getItemCount());
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
