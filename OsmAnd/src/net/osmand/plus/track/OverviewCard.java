package net.osmand.plus.track;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.myplaces.TrackActivityFragmentAdapter.isGpxFileSelected;
import static net.osmand.plus.track.OptionsCard.APPEARANCE_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.DIRECTIONS_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.EDIT_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.SHOW_ON_MAP_BUTTON_INDEX;
import static net.osmand.plus.track.OverviewCard.StatBlock.ItemType.*;

public class OverviewCard extends BaseCard {

	private RecyclerView rvOverview;
	private View showButton;
	private View appearanceButton;
	private View editButton;
	private View directionsButton;

	private final TrackDisplayHelper displayHelper;
	private final GPXFile gpxFile;
	private final GpxDisplayItemType[] filterTypes = {GpxDisplayItemType.TRACK_SEGMENT};
	private final SegmentActionsListener listener;
	private boolean gpxFileSelected;
	private GpxDisplayItem gpxItem;

	public OverviewCard(@NonNull MapActivity mapActivity, @NonNull TrackDisplayHelper displayHelper,
						@NonNull SegmentActionsListener listener) {
		super(mapActivity);
		this.displayHelper = displayHelper;
		this.listener = listener;
		gpxFile = displayHelper.getGpx();
		gpxFileSelected = isGpxFileSelected(app, gpxFile);
		gpxItem = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes)).get(0);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_overview_fragment;
	}

	@Override
	protected void updateContent() {
		int iconColorDef = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		int iconColorPres = R.color.active_buttons_and_links_text_dark;
		boolean fileAvailable = gpxFile.path != null && !gpxFile.showCurrentTrack;

		showButton = view.findViewById(R.id.show_button);
		appearanceButton = view.findViewById(R.id.appearance_button);
		editButton = view.findViewById(R.id.edit_button);
		directionsButton = view.findViewById(R.id.directions_button);
		rvOverview = view.findViewById(R.id.recycler_overview);

		initShowButton(iconColorDef, iconColorPres);
		initAppearanceButton(iconColorDef, iconColorPres);
		if (fileAvailable) {
			initEditButton(iconColorDef, iconColorPres);
			initDirectionsButton(iconColorDef, iconColorPres);
		}
		initStatBlocks();
	}

	void initStatBlocks() {
		GPXTrackAnalysis analysis = gpxItem.analysis;
		if (analysis != null) {
			boolean joinSegments = displayHelper.isJoinSegments();
			float totalDistance = !joinSegments && gpxItem.isGeneralTrack() ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;
			float timeSpan = !joinSegments && gpxItem.isGeneralTrack() ? analysis.timeSpanWithoutGaps : analysis.timeSpan;
			String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
			String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
			String avg = OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app);
			String max = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);
			List<StatBlock> items = new ArrayList<>();

			StatBlock.prepareData(analysis, items, app.getString(R.string.distance), OsmAndFormatter.getFormattedDistance(totalDistance, app),
					R.drawable.ic_action_track_16, R.color.icon_color_default_light, GPXDataSetType.ALTITUDE, GPXDataSetType.SPEED, ITEM_DISTANCE);
			StatBlock.prepareData(analysis, items, app.getString(R.string.altitude_ascent), asc,
					R.drawable.ic_action_arrow_up_16, R.color.gpx_chart_red, GPXDataSetType.SLOPE, null, ITEM_ALTITUDE);
			StatBlock.prepareData(analysis, items, app.getString(R.string.altitude_descent), desc,
					R.drawable.ic_action_arrow_down_16, R.color.gpx_pale_green, GPXDataSetType.ALTITUDE, GPXDataSetType.SLOPE, ITEM_ALTITUDE);
			StatBlock.prepareData(analysis, items, app.getString(R.string.average_speed), avg,
					R.drawable.ic_action_speed_16, R.color.icon_color_default_light, GPXDataSetType.SPEED, null, ITEM_SPEED);
			StatBlock.prepareData(analysis, items, app.getString(R.string.max_speed), max,
					R.drawable.ic_action_max_speed_16, R.color.icon_color_default_light, GPXDataSetType.SPEED, null, ITEM_SPEED);
			StatBlock.prepareData(analysis, items, app.getString(R.string.shared_string_time_span),
					Algorithms.formatDuration((int) (timeSpan / 1000), app.accessibilityEnabled()),
					R.drawable.ic_action_time_span_16, R.color.icon_color_default_light, GPXDataSetType.SPEED, null, ITEM_TIME);

			final StatBlockAdapter sbAdapter = new StatBlockAdapter(items);
			rvOverview.setLayoutManager(new LinearLayoutManager(app, LinearLayoutManager.HORIZONTAL, false));
//			rvOverview.addItemDecoration(new HorizontalDividerDecoration(app, nightMode));
			rvOverview.setAdapter(sbAdapter);
		} else {
			AndroidUiHelper.updateVisibility(rvOverview, false);
		}
	}

	private void initShowButton(final int iconColorDef, final int iconColorPres) {
		initButton(showButton, SHOW_ON_MAP_BUTTON_INDEX, null, iconColorDef, iconColorPres, new StatBlockCallback() {
			@Override
			public void run(AppCompatImageView image) {
				gpxFileSelected = isGpxFileSelected(app, gpxFile);
				setImageDrawable(image, gpxFileSelected ? R.drawable.ic_action_view : R.drawable.ic_action_hide, iconColorDef);
				image.requestLayout();
			}
		});
	}

	private void initAppearanceButton(@ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		initButton(appearanceButton, APPEARANCE_BUTTON_INDEX, R.drawable.ic_action_appearance, iconColorDef, iconColorPres, null);
	}

	private void initEditButton(@ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		initButton(editButton, EDIT_BUTTON_INDEX, R.drawable.ic_action_edit_dark, iconColorDef, iconColorPres, null);
	}

	private void initDirectionsButton(@ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		initButton(directionsButton, DIRECTIONS_BUTTON_INDEX, R.drawable.ic_action_gdirections_dark, iconColorDef, iconColorPres, null);
	}

	private void initButton(View item, final int buttonIndex, @DrawableRes Integer iconResId,
							@ColorRes int iconColorDef, @ColorRes int iconColorPres, @Nullable final StatBlockCallback callback) {
		final AppCompatImageView icon = item.findViewById(R.id.image);
		final AppCompatImageView filled = item.findViewById(R.id.filled);
		filled.setImageResource(nightMode ? R.drawable.bg_plugin_logo_enabled_dark : R.drawable.bg_topbar_shield_exit_ref);
		filled.setAlpha(0.1f);
		if (callback != null) {
			callback.run(icon);
		} else {
			setImageDrawable(icon, iconResId, iconColorDef);
		}
		setOnTouchItem(item, icon, filled, iconResId, iconColorDef, iconColorPres);
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(OverviewCard.this, buttonIndex);
					if (callback != null) {
						callback.run(icon);
					}
				}
			}
		});
	}

	private void setImageDrawable(ImageView iv, @DrawableRes Integer resId, @ColorRes int color) {
		Drawable icon = resId != null ? app.getUIUtilities().getIcon(resId, color)
				: UiUtilities.tintDrawable(iv.getDrawable(), getResolvedColor(color));
		iv.setImageDrawable(icon);
	}

	private void setOnTouchItem(View item, final ImageView image, final ImageView filled, @DrawableRes final Integer resId, @ColorRes final int colorDef, @ColorRes final int colorPres) {
		item.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN: {
						filled.setAlpha(1f);
						setImageDrawable(image, resId, colorPres);
						break;
					}
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL: {
						filled.setAlpha(0.1f);
						setImageDrawable(image, resId, colorDef);
						break;
					}
				}
				return false;
			}
		});
	}

	private interface StatBlockCallback {
		void run(AppCompatImageView image);
	}

	private class StatBlockAdapter extends RecyclerView.Adapter<StatBlockViewHolder> {

		private final List<StatBlock> statBlocks;

		public StatBlockAdapter(List<StatBlock> StatBlocks) {
			this.statBlocks = StatBlocks;
		}

		@Override
		public int getItemCount() {
			return statBlocks.size();
		}

		@NonNull
		@Override
		public StatBlockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View itemView = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_gpx_stat_block, parent, false);
			return new StatBlockViewHolder(itemView);
		}

		@Override
		public void onBindViewHolder(StatBlockViewHolder holder, int position) {
			final StatBlock item = statBlocks.get(position);
			final int textColor = nightMode ?
					app.getResources().getColor(R.color.active_color_primary_dark)
					: app.getResources().getColor(R.color.active_color_primary_light);

			holder.valueText.setText(item.value);
			holder.titleText.setText(item.title);
			holder.valueText.setTextColor(textColor);
			holder.titleText.setTextColor(app.getResources().getColor(R.color.text_color_secondary_light));
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (gpxItem != null && gpxItem.analysis != null) {
						ArrayList<GPXDataSetType> list = new ArrayList<>();
						if (gpxItem.analysis.hasElevationData || gpxItem.analysis.isSpeedSpecified() || gpxItem.analysis.hasSpeedData) {
							if (item.firstType != null) {
								list.add(item.firstType);
							}
							if (item.secondType != null) {
								list.add(item.secondType);
							}
						}
						gpxItem.chartTypes = list.size() > 0 ? list.toArray(new GPXDataSetType[0]) : null;
						gpxItem.locationOnMap = gpxItem.locationStart;

						listener.openAnalyzeOnMap(gpxItem);
					}
				}
			});
			setImageDrawable(holder.imageView, item.imageResId, item.imageColorId);
			AndroidUtils.setBackgroundColor(view.getContext(), holder.divider, nightMode, R.color.divider_color_light, R.color.divider_color_dark);
			if (position == statBlocks.size() - 1) {
				AndroidUiHelper.setVisibility(View.GONE, holder.divider);
			}
		}
	}

	private static class StatBlockViewHolder extends RecyclerView.ViewHolder {

		private final TextViewEx valueText;
		private final TextView titleText;
		private final AppCompatImageView imageView;
		private final View divider;

		StatBlockViewHolder(View view) {
			super(view);
			valueText = view.findViewById(R.id.value);
			titleText = view.findViewById(R.id.title);
			imageView = view.findViewById(R.id.image);
			divider = view.findViewById(R.id.divider);
		}
	}

	private static class HorizontalDividerDecoration extends ItemDecoration {

		private final Drawable divider;
		private final int marginV;
		private final int marginH;

		public HorizontalDividerDecoration(Context context, boolean nightMode) {
			divider = new ColorDrawable(ContextCompat.getColor(context, nightMode ? R.color.divider_color_dark : R.color.divider_color_light));
			marginV = context.getResources().getDimensionPixelSize(R.dimen.map_small_button_margin);
			marginH = context.getResources().getDimensionPixelSize(R.dimen.content_padding);
		}

		@Override
		public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
			drawHorizontal(c, parent);
		}

		public void drawHorizontal(Canvas c, RecyclerView parent) {
			for (int i = 0; i < parent.getChildCount() - 1; i++) {
				final View child = parent.getChildAt(i);
				final int left = child.getRight() - divider.getIntrinsicWidth() + marginH;
				final int right = left + divider.getIntrinsicHeight();
				final int top = child.getTop() + marginV;
				final int bottom = child.getBottom() - marginV;
				divider.setBounds(left, top, right, bottom);
				divider.draw(c);
			}
		}

		@Override
		public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
			outRect.set(marginH - divider.getIntrinsicWidth(), 0, marginH + divider.getIntrinsicWidth(), 0);
		}
	}

	protected static class StatBlock {

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

		public static void prepareData(GPXTrackAnalysis analysis, List<StatBlock> listItems, String title,
									   String value, @DrawableRes int imageResId, @ColorRes int imageColorId,
									   GPXDataSetType firstType, GPXDataSetType secondType, ItemType itemType) {
			StatBlock statBlock = new StatBlock(title, value, imageResId, imageColorId, firstType, secondType, itemType);
			switch (statBlock.itemType) {
				case ITEM_DISTANCE: {
					listItems.add(statBlock);
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

		public enum ItemType {
			ITEM_DISTANCE,
			ITEM_ALTITUDE,
			ITEM_SPEED,
			ITEM_TIME;
		}
	}
}