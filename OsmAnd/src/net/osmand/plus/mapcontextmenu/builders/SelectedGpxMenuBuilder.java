package net.osmand.plus.mapcontextmenu.builders;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.TrackDisplayGroup;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class SelectedGpxMenuBuilder extends MenuBuilder {

	private final SelectedGpxPoint selectedGpxPoint;
	private final GpxTrackAnalysis analysis;
	private final WptPt selectedPoint;

	public SelectedGpxMenuBuilder(@NonNull MapActivity mapActivity, @NonNull SelectedGpxPoint selectedGpxPoint) {
		super(mapActivity);
		this.selectedGpxPoint = selectedGpxPoint;
		selectedPoint = selectedGpxPoint.getSelectedPoint();
		analysis = selectedGpxPoint.getSelectedGpxFile() == null
				? new GpxTrackAnalysis()
				: selectedGpxPoint.getSelectedGpxFile().getTrackAnalysis(mapActivity.getMyApplication());
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	protected boolean needBuildCoordinatesRow() {
		return true;
	}

	@Override
	public void buildInternal(View view) {
		buildPointRows(view);
		buildUphillDownhill(view);
		buildOverviewRows(view);
		buildElevationRows(view);
		buildSpeedRows(view);
	}

	public void buildOverviewRows(View view) {
		buildCategoryView(view, app.getString(R.string.shared_string_overview));

		buildRow(view, getThemedIcon(R.drawable.ic_action_polygom_dark), null, app.getString(R.string.distance),
				OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app), 0, null,
				false, null, false, 0, false, false, false, null, false);

		String timeSpan = Algorithms.formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled());
		String timeMoving = Algorithms.formatDuration((int) (analysis.getTimeMoving() / 1000), app.accessibilityEnabled());
		String timeSpanTitle = app.getString(R.string.shared_string_time_span) + " / " + app.getString(R.string.moving_time);
		buildRow(view, getThemedIcon(R.drawable.ic_action_time_span), null, timeSpanTitle,
				timeSpan + " / " + timeMoving, 0, null,
				false, null, false, 0, false, false, false, null, false);

		Date start = new Date(analysis.getStartTime());
		Date end = new Date(analysis.getEndTime());
		DateFormat startFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		DateFormat endFormat;
		if (OsmAndFormatter.isSameDay(start, end)) {
			endFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
		} else {
			endFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		}
		String startEndTimeTitle = app.getString(R.string.shared_string_start_time) + " - " + app.getString(R.string.shared_string_end_time);
		buildRow(view, getThemedIcon(R.drawable.ic_action_time_start), null, startEndTimeTitle,
				startFormat.format(start) + " - " + endFormat.format(end), 0, null,
				false, null, false, 0, false, false, false, null, true);
	}

	public void buildElevationRows(View view) {
		if (analysis.isElevationSpecified()) {
			buildCategoryView(view, app.getString(R.string.altitude));

			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_average), null, app.getString(R.string.average_altitude),
					OsmAndFormatter.getFormattedAlt(analysis.getAvgElevation(), app), 0, null,
					false, null, false, 0, false, false, false, null, false);

			String min = OsmAndFormatter.getFormattedAlt(analysis.getMinElevation(), app);
			String max = OsmAndFormatter.getFormattedAlt(analysis.getMaxElevation(), app);
			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_range), null, app.getString(R.string.altitude_range),
					min + " - " + max, 0, null,
					false, null, false, 0, false, false, false, null, false);

			String asc = OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationUp(), app);
			String desc = OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationDown(), app);
			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_descent), null, app.getString(R.string.ascent_descent),
					asc + " / " + desc, 0, null,
					false, null, false, 0, false, false, false, null, true);

			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_descent), null, app.getString(R.string.distance_moving),
					OsmAndFormatter.getFormattedDistance(analysis.getTotalDistanceMoving(), app), 0, null,
					false, null, false, 0, false, false, false, null, true);
		}
	}

	public void buildSpeedRows(View view) {
		if (analysis.isSpeedSpecified()) {
			buildCategoryView(view, app.getString(R.string.shared_string_speed));

			buildRow(view, getThemedIcon(R.drawable.ic_action_speed), null, app.getString(R.string.average_speed),
					OsmAndFormatter.getFormattedSpeed(analysis.getAvgSpeed(), app), 0, null,
					false, null, false, 0, false, false, false, null, false);

			buildRow(view, getThemedIcon(R.drawable.ic_action_max_speed), null, app.getString(R.string.max_speed),
					OsmAndFormatter.getFormattedSpeed(analysis.getMaxSpeed(), app), 0, null,
					false, null, false, 0, false, false, false, null, false);
		}
	}

	public void buildPointRows(View view) {
		buildCategoryView(view, app.getString(R.string.plugin_distance_point));

		buildRow(view, getThemedIcon(R.drawable.ic_action_polygom_dark), null, app.getString(R.string.distance),
				OsmAndFormatter.getFormattedDistance((float) selectedPoint.getDistance(), app), 0, null,
				false, null, false, 0, false, false, false, null, false);

		if (selectedPoint.getTime() != 0) {
			DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
			buildRow(view, getThemedIcon(R.drawable.ic_action_time_start), null, app.getString(R.string.shared_string_time),
					format.format(selectedPoint.getTime()), 0, null,
					false, null, false, 0, false, false, false, null, true);
		}
		if (!Double.isNaN(selectedPoint.getEle())) {
			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude), null, app.getString(R.string.altitude),
					OsmAndFormatter.getFormattedAlt(selectedPoint.getEle(), app), 0, null,
					false, null, false, 0, false, false, false, null, false);
		}
		if (!Double.isNaN(selectedPoint.getSpeed())) {
			buildRow(view, getThemedIcon(R.drawable.ic_action_speed), null, app.getString(R.string.shared_string_speed),
					OsmAndFormatter.getFormattedSpeed((float) selectedPoint.getSpeed(), app), 0, null,
					false, null, false, 0, false, false, false, null, false);
		}
		if (!Float.isNaN(selectedGpxPoint.getBearing())) {
			buildRow(view, getThemedIcon(R.drawable.ic_action_relative_bearing), null, app.getString(R.string.shared_string_bearing),
					OsmAndFormatter.getFormattedAzimuth(selectedGpxPoint.getBearing(), app), 0, null,
					false, null, false, 0, false, false, false, null, false);
		}
	}

	@Nullable
	private TrackDisplayGroup getTrackGroup() {
		List<GpxDisplayGroup> gpxDisplayGroups = selectedGpxPoint.getSelectedGpxFile().getSplitGroups(app);
		if (Algorithms.isEmpty(gpxDisplayGroups)) {
			return null;
		}

		TrackDisplayGroup trackDisplayGroup = null;
		for (GpxDisplayGroup group : gpxDisplayGroups) {
			if (group instanceof TrackDisplayGroup) {
				trackDisplayGroup = (TrackDisplayGroup) group;
				break;
			}
		}

		return trackDisplayGroup;
	}

	@Nullable
	private GpxDisplayItem findDisplayItem() {
		TrackDisplayGroup trackDisplayGroup = getTrackGroup();
		if (trackDisplayGroup == null) {
			return null;
		}

		List<TrkSegment> segments = selectedGpxPoint.getSelectedGpxFile().getPointsToDisplay();
		for (TrkSegment segment : segments) {

			List<WptPt> wptPts = segment.getPoints();
			int currentPointIndex;
			if (selectedGpxPoint.getNextPoint() != null) {
				currentPointIndex = wptPts.indexOf(selectedGpxPoint.getNextPoint());
			} else if (selectedGpxPoint.getPrevPoint() != null) {
				currentPointIndex = wptPts.indexOf(selectedGpxPoint.getPrevPoint());
			} else {
				currentPointIndex = wptPts.indexOf(selectedPoint);
			}

			if (currentPointIndex == -1) {
				return null;
			}
			for (GpxDisplayItem gpxDisplayItem : trackDisplayGroup.getDisplayItems()) {
				int startItemPointIndex = wptPts.indexOf(gpxDisplayItem.locationStart);
				int endItemPointIndex = wptPts.indexOf(gpxDisplayItem.locationEnd);
				boolean hasStartEndIndex = startItemPointIndex != -1 && endItemPointIndex != -1;

				if (hasStartEndIndex && currentPointIndex >= startItemPointIndex
						&& currentPointIndex < endItemPointIndex) {
					return gpxDisplayItem;
				}
			}
		}
		return null;
	}

	private void buildUphillDownhill(View view) {
		GpxDisplayItem currentSegment = findDisplayItem();
		if (currentSegment == null || currentSegment.analysis == null) {
			return;
		}

		buildCategoryView(view, app.getString(R.string.uphill_downhill_split));
		GpxTrackAnalysis segmentAnalysis = currentSegment.analysis;

		buildRow(view, getThemedIcon(R.drawable.ic_action_track_16), null, app.getString(R.string.distance),
				OsmAndFormatter.getFormattedDistance(segmentAnalysis.getTotalDistance(), app), 0, null,
				false, null, false, 0, false, false, false, null, false);

		int diffElevationUp = (int) segmentAnalysis.getDiffElevationUp();
		if (diffElevationUp > 0) {
			String asc = OsmAndFormatter.getFormattedAlt(diffElevationUp, app);
			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_descent_ascent_16), null, app.getString(R.string.ascent),
					asc, 0, null,
					false, null, false, 0, false, false, false, null, false);
		}

		int diffElevationDown = (int) segmentAnalysis.getDiffElevationDown();
		if (diffElevationDown > 0) {
			String desc = OsmAndFormatter.getFormattedAlt(diffElevationDown, app);
			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_descent_ascent_16), null, app.getString(R.string.descent),
					desc, 0, null,
					false, null, false, 0, false, false, false, null, false);
		}

		if (segmentAnalysis.getAvgElevation() != 0) {
			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_average_16), null, app.getString(R.string.average_elevation),
					OsmAndFormatter.getFormattedAlt(segmentAnalysis.getAvgElevation(), app), 0, null,
					false, null, false, 0, false, false, false, null, false);
		}

		if (segmentAnalysis.hasSpeedData()) {
			buildRow(view, getThemedIcon(R.drawable.ic_action_time_moving_16), null, app.getString(R.string.moving_time),
					Algorithms.formatDuration((int) (segmentAnalysis.getTimeMoving() / 1000), app.accessibilityEnabled()), 0, null,
					false, null, false, 0, false, false, false, null, false);

			buildRow(view, getThemedIcon(R.drawable.ic_action_speed_16), null, app.getString(R.string.average_speed),
					OsmAndFormatter.getFormattedSpeed(segmentAnalysis.getAvgSpeed(), app), 0, null,
					false, null, false, 0, false, false, false, null, false);

			String maxSpeed = OsmAndFormatter.getFormattedSpeed(segmentAnalysis.getMaxSpeed(), app);
			String minSpeed = OsmAndFormatter.getFormattedSpeed(segmentAnalysis.getMinSpeed(), app);
			String maxMinSpeed;
			if (maxSpeed.contains(" ")) {
				maxMinSpeed = maxSpeed.substring(0, maxSpeed.indexOf(" ")).concat("/").concat(minSpeed);
			} else {
				maxMinSpeed = maxSpeed.substring(0, maxSpeed.indexOf("-")).concat("/").concat(minSpeed);
			}

			buildRow(view, getThemedIcon(R.drawable.ic_action_max_speed_16), null, app.getString(R.string.max_min),
					maxMinSpeed, 0, null,
					false, null, false, 0, false, false, false, null, false);
		}

		if (segmentAnalysis.getTimeSpan() > 0) {
			DateFormat tf = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
			DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);

			Date start = new Date(segmentAnalysis.getStartTime());
			String startValue = app.getString(R.string.ltr_or_rtl_combine_via_dash, tf.format(start), df.format(start));
			Date end = new Date(segmentAnalysis.getEndTime());
			String endValue = app.getString(R.string.ltr_or_rtl_combine_via_dash, tf.format(end), df.format(end));

			buildRow(view, getThemedIcon(R.drawable.ic_action_time_start_16), null, app.getString(R.string.shared_string_start_time),
					startValue, 0, null,
					false, null, false, 0, false, false, false, null, false);
			buildRow(view, getThemedIcon(R.drawable.ic_action_time_end_16), null, app.getString(R.string.shared_string_end_time),
					endValue, 0, null,
					false, null, false, 0, false, false, false, null, false);
		}
	}

	private void buildCategoryView(View view, String name) {
		boolean light = isLightContent();

		if (!isFirstRow()) {
			buildRowDivider(view);
		}

		View categoryView = UiUtilities.getInflater(view.getContext(), !light).inflate(R.layout.preference_category_with_descr, (ViewGroup) view, false);

		AndroidUiHelper.updateVisibility(categoryView.findViewById(android.R.id.icon), false);
		AndroidUiHelper.updateVisibility(categoryView.findViewById(android.R.id.summary), false);

		TextView title = categoryView.findViewById(android.R.id.title);
		title.setText(name);

		((LinearLayout) view).addView(categoryView);
	}

	@Override
	public boolean hasCustomAddressLine() {
		return true;
	}

	public void buildCustomAddressLine(LinearLayout ll) {
		boolean light = isLightContent();
		int gpxSmallIconMargin = (int) ll.getResources().getDimension(R.dimen.gpx_small_icon_margin);
		int gpxSmallTextMargin = (int) ll.getResources().getDimension(R.dimen.gpx_small_text_margin);
		float gpxTextSize = ll.getResources().getDimension(R.dimen.default_desc_text_size);

		int textColor = ColorUtilities.getPrimaryTextColor(ll.getContext(), !light);

		buildIcon(ll, gpxSmallIconMargin, R.drawable.ic_action_distance_16);
		buildTextView(ll, gpxSmallTextMargin, gpxTextSize, textColor,
				OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));

		buildIcon(ll, gpxSmallIconMargin, R.drawable.ic_action_waypoint_16);
		buildTextView(ll, gpxSmallTextMargin, gpxTextSize, textColor, "" + analysis.getWptPoints());

		buildIcon(ll, gpxSmallIconMargin, R.drawable.ic_action_time_16);
		buildTextView(ll, gpxSmallTextMargin, gpxTextSize, textColor,
				Algorithms.formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled()) + "");
	}

	private void buildIcon(LinearLayout ll, int gpxSmallIconMargin, int iconId) {
		ImageView icon = new ImageView(ll.getContext());
		LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llIconParams, 0, 0, gpxSmallIconMargin, 0);
		llIconParams.gravity = Gravity.CENTER_VERTICAL;
		icon.setLayoutParams(llIconParams);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(iconId));
		ll.addView(icon);
	}

	private void buildTextView(LinearLayout ll, int gpxSmallTextMargin, float gpxTextSize, int textColor, String text) {
		TextView textView = new TextView(ll.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llTextParams, 0, 0, gpxSmallTextMargin, 0);
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, gpxTextSize);
		textView.setTextColor(textColor);
		textView.setText(text);
		ll.addView(textView);
	}
}
