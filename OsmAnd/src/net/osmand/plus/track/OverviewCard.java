package net.osmand.plus.track;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
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
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.LineGraphType;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.Arrays;
import java.util.List;

import static net.osmand.plus.myplaces.TrackActivityFragmentAdapter.isGpxFileSelected;
import static net.osmand.plus.track.OptionsCard.APPEARANCE_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.DIRECTIONS_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.EDIT_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.SHOW_ON_MAP_BUTTON_INDEX;

public class OverviewCard extends BaseCard {

	private RecyclerView rvOverview;
	private View showButton;
	private View appearanceButton;
	private View editButton;
	private View directionsButton;

	private final TrackDisplayHelper displayHelper;
	private final GPXFile gpxFile;
	private final GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[]{GpxDisplayItemType.TRACK_SEGMENT};
	private final SegmentActionsListener listener;

	public OverviewCard(@NonNull MapActivity mapActivity, @NonNull TrackDisplayHelper displayHelper,
						@NonNull SegmentActionsListener listener) {
		super(mapActivity);
		this.displayHelper = displayHelper;
		this.gpxFile = displayHelper.getGpx();
		this.listener = listener;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_overview_fragment;
	}

	@Override
	protected void updateContent() {
		int iconColorDef = R.color.icon_color_active_light;
		int iconColorPres = R.color.active_buttons_and_links_text_dark;
		boolean fileAvailable = gpxFile.path != null && !gpxFile.showCurrentTrack;

		showButton = view.findViewById(R.id.show_button);
		appearanceButton = view.findViewById(R.id.appearance_button);
		editButton = view.findViewById(R.id.edit_button);
		directionsButton = view.findViewById(R.id.directions_button);
		rvOverview = view.findViewById(R.id.recycler_overview);

		MapContextMenu menu = mapActivity.getContextMenu();
		TextView distanceText = (TextView) view.findViewById(R.id.distance);
		ImageView direction = (ImageView) view.findViewById(R.id.direction);
		UpdateLocationViewCache updateLocationViewCache = app.getUIUtilities().getUpdateLocationViewCache();
		app.getUIUtilities().updateLocationView(updateLocationViewCache, direction, distanceText, menu.getLatLon());

		initShowButton(iconColorDef, iconColorPres);
		initAppearanceButton(iconColorDef, iconColorPres);
		if (fileAvailable) {
			initEditButton(iconColorDef, iconColorPres);
			initDirectionsButton(iconColorDef, iconColorPres);
		}

		initStatBlocks();
	}

	void initStatBlocks() {
		GpxDisplayItem gpxItem = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes)).get(0);
		GPXTrackAnalysis analysis = gpxItem.analysis;
		boolean joinSegments = displayHelper.isJoinSegments();
		float totalDistance = !joinSegments && gpxItem.isGeneralTrack() ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;
		float timeSpan = !joinSegments && gpxItem.isGeneralTrack() ? analysis.timeSpanWithoutGaps : analysis.timeSpan;
		String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
		String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
		String avg = OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app);
		String max = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);

		StatBlock sDistance = new StatBlock(app.getResources().getString(R.string.distance), OsmAndFormatter.getFormattedDistance(totalDistance, app),
				R.drawable.ic_action_track_16, R.color.icon_color_default_light, LineGraphType.ALTITUDE, LineGraphType.SPEED);
		StatBlock sAscent = new StatBlock(app.getResources().getString(R.string.altitude_ascent), asc,
				R.drawable.ic_action_arrow_up_16, R.color.gpx_chart_red, LineGraphType.SLOPE, null);
		StatBlock sDescent = new StatBlock(app.getResources().getString(R.string.altitude_descent), desc,
				R.drawable.ic_action_arrow_down_16, R.color.gpx_pale_green, LineGraphType.ALTITUDE, LineGraphType.SLOPE);
		StatBlock sAvSpeed = new StatBlock(app.getResources().getString(R.string.average_speed), avg,
				R.drawable.ic_action_speed_16, R.color.icon_color_default_light, LineGraphType.SPEED, null);
		StatBlock sMaxSpeed = new StatBlock(app.getResources().getString(R.string.max_speed), max,
				R.drawable.ic_action_max_speed_16, R.color.icon_color_default_light, LineGraphType.SPEED, null);
		StatBlock sTimeSpan = new StatBlock(app.getResources().getString(R.string.shared_string_time_span),
				Algorithms.formatDuration((int) (timeSpan / 1000), app.accessibilityEnabled()),
				R.drawable.ic_action_time_span_16, R.color.icon_color_default_light, LineGraphType.SPEED, null);

		LinearLayoutManager llManager = new LinearLayoutManager(app);
		llManager.setOrientation(LinearLayoutManager.HORIZONTAL);
		rvOverview.setLayoutManager(llManager);
		rvOverview.setItemAnimator(new DefaultItemAnimator());
		List<StatBlock> items = Arrays.asList(sDistance, sAscent, sDescent, sAvSpeed, sMaxSpeed, sTimeSpan);
		final StatBlockAdapter siAdapter = new StatBlockAdapter(items);
		rvOverview.setAdapter(siAdapter);
		rvOverview.addItemDecoration(new HorizontalDividerDecoration(app));
	}

	private void initShowButton(final int iconColorDef, final int iconColorPres) {
		final AppCompatImageView image = showButton.findViewById(R.id.image);
		final AppCompatImageView filled = showButton.findViewById(R.id.filled);
		final int iconShowResId = R.drawable.ic_action_view;
		final int iconHideResId = R.drawable.ic_action_hide;
		final boolean[] gpxFileSelected = {isGpxFileSelected(app, gpxFile)};
		filled.setImageResource(R.drawable.bg_topbar_shield_exit_ref);
		filled.setAlpha(gpxFileSelected[0] ? 1f : 0.1f);
		setImageDrawable(image, gpxFileSelected[0] ? iconShowResId : iconHideResId,
				gpxFileSelected[0] ? iconColorPres : iconColorDef);
		showButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gpxFileSelected[0] = !gpxFileSelected[0];
				filled.setAlpha(gpxFileSelected[0] ? 1f : 0.1f);
				setImageDrawable(image, gpxFileSelected[0] ? iconShowResId : iconHideResId,
						gpxFileSelected[0] ? iconColorPres : iconColorDef);
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(OverviewCard.this, SHOW_ON_MAP_BUTTON_INDEX);
				}
			}
		});
	}

	private void initAppearanceButton(int iconColorDef, int iconColorPres) {
		initButton(appearanceButton, APPEARANCE_BUTTON_INDEX, R.drawable.ic_action_appearance, iconColorDef, iconColorPres);
	}

	private void initEditButton(int iconColorDef, int iconColorPres) {
		initButton(editButton, EDIT_BUTTON_INDEX, R.drawable.ic_action_edit_dark, iconColorDef, iconColorPres);
	}

	private void initDirectionsButton(int iconColorDef, int iconColorPres) {
		initButton(directionsButton, DIRECTIONS_BUTTON_INDEX, R.drawable.ic_action_gdirections_dark, iconColorDef, iconColorPres);
	}

	private void initButton(View item, final int buttonIndex, int iconResId, int iconColorDef, int iconColorPres) {
		final AppCompatImageView image = item.findViewById(R.id.image);
		final AppCompatImageView filled = item.findViewById(R.id.filled);
		filled.setImageResource(R.drawable.bg_topbar_shield_exit_ref);
		filled.setAlpha(0.1f);
		setImageDrawable(image, iconResId, iconColorDef);
		setOnTouchItem(item, image, filled, iconResId, iconColorDef, iconColorPres);
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(OverviewCard.this, buttonIndex);
				}
			}
		});
	}

	private void setImageDrawable(ImageView iv, @DrawableRes int resId, @ColorRes int color) {
		Drawable icon = app.getUIUtilities().getIcon(resId, color);
		iv.setImageDrawable(icon);
	}

	private void setOnTouchItem(View item, final ImageView image, final ImageView filled, @DrawableRes final int resId, @ColorRes final int colorDef, @ColorRes final int colorPres) {
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

	private class StatBlockAdapter extends RecyclerView.Adapter<StatBlockAdapter.StatBlockViewHolder> {
		private final List<StatBlock> StatBlocks;

		public StatBlockAdapter(List<StatBlock> StatBlocks) {
			this.StatBlocks = StatBlocks;
		}

		@Override
		public int getItemCount() {
			return StatBlocks.size();
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
			StatBlock item = StatBlocks.get(position);
			holder.bind(item);
		}

		class StatBlockViewHolder extends RecyclerView.ViewHolder {
			private final TextViewEx valueText;
			private final TextView titleText;
			private final AppCompatImageView imageView;

			StatBlockViewHolder(View view) {
				super(view);
				valueText = view.findViewById(R.id.value);
				titleText = view.findViewById(R.id.title);
				imageView = view.findViewById(R.id.image);
			}

			public void bind(final StatBlock overviewItem) {
				valueText.setText(overviewItem.value);
				valueText.setTextColor(app.getResources().getColor(R.color.active_color_primary_light));
				titleText.setText(overviewItem.title);
				titleText.setTextColor(app.getResources().getColor(R.color.text_color_secondary_light));
				setImageDrawable(imageView, overviewItem.imageResId, overviewItem.imageColorId);
				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						GpxDisplayItem gpxItem = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes)).get(0);
						GPXTrackAnalysis analysis = gpxItem.analysis;
						GpxDataItem gpxDataItem = displayHelper.getGpxDataItem();
						boolean calcWithoutGaps = gpxItem.isGeneralTrack() && gpxDataItem != null && !gpxDataItem.isJoinSegments();
						List<ILineDataSet> dataSets = GpxUiHelper.getDataSets(new LineChart(app), app, analysis, overviewItem.firstType, overviewItem.secondType, calcWithoutGaps);
						listener.openAnalyzeOnMap(gpxItem, dataSets, null);
					}
				});
			}
		}
	}

	private class HorizontalDividerDecoration extends RecyclerView.ItemDecoration {
		private final Drawable divider;

		public HorizontalDividerDecoration(Context context) {
			int[] ATTRS = new int[]{android.R.attr.listDivider};
			final TypedArray ta = context.obtainStyledAttributes(ATTRS);
			divider = ta.getDrawable(0);
//			DrawableCompat.setTint(divider, context.getResources().getColor(R.color.divider_color_light)); //todo change drawable color
			ta.recycle();
		}

		@Override
		public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
			drawHorizontal(c, parent);
		}

		public void drawHorizontal(Canvas c, RecyclerView parent) {
			final int marginV = parent.getContext().getResources().getDimensionPixelSize(R.dimen.map_small_button_margin);
			final int marginH = parent.getContext().getResources().getDimensionPixelSize(R.dimen.content_padding);
			final int childCount = parent.getChildCount();
			for (int i = 0; i < childCount; i++) {
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
		public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
			int marginV = parent.getContext().getResources().getDimensionPixelSize(R.dimen.map_small_button_margin);
			int marginH = parent.getContext().getResources().getDimensionPixelSize(R.dimen.content_padding);
			outRect.set(marginH - divider.getIntrinsicWidth(), marginV, marginH + divider.getIntrinsicWidth(), marginV);
		}
	}

	private static class StatBlock {
		private String title;
		private String value;
		private int imageResId;
		private int imageColorId;
		private LineGraphType firstType;
		private LineGraphType secondType;

		public StatBlock(String title, String value, @DrawableRes int imageResId, @ColorRes int imageColorId, LineGraphType firstType, LineGraphType secondType) {
			this.title = title;
			this.value = value;
			this.imageResId = imageResId;
			this.imageColorId = imageColorId;
			this.firstType = firstType;
			this.secondType = secondType;
		}

	}
}