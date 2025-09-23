package net.osmand.plus.mapcontextmenu.builders;

import android.graphics.drawable.Drawable;
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
				: selectedGpxPoint.getSelectedGpxFile().getTrackAnalysis(mapActivity.getApp());
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

		buildInfoRow(view, getThemedIcon(R.drawable.ic_action_polygom_dark), app.getString(R.string.distance),
				OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));

		String timeSpan = Algorithms.formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled());
		String timeMoving = Algorithms.formatDuration((int) (analysis.getTimeMoving() / 1000), app.accessibilityEnabled());
		String timeSpanTitle = app.getString(R.string.duration) + " / " + app.getString(R.string.moving_time);
		buildInfoRow(view, getThemedIcon(R.drawable.ic_action_time_span), timeSpanTitle,
				timeSpan + " / " + timeMoving);

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

			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_altitude_average), app.getString(R.string.average_altitude),
					OsmAndFormatter.getFormattedAlt(analysis.getAvgElevation(), app));

			String min = OsmAndFormatter.getFormattedAlt(analysis.getMinElevation(), app);
			String max = OsmAndFormatter.getFormattedAlt(analysis.getMaxElevation(), app);
			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_altitude_range), app.getString(R.string.altitude_range),
					min + " - " + max);

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

			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_speed), app.getString(R.string.average_speed),
					OsmAndFormatter.getFormattedSpeed(analysis.getAvgSpeed(), app));

			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_max_speed), app.getString(R.string.max_speed),
					OsmAndFormatter.getFormattedSpeed(analysis.getMaxSpeed(), app));
		}
	}

	public void buildPointRows(View view) {
		buildCategoryView(view, app.getString(R.string.plugin_distance_point));

		buildInfoRow(view, getThemedIcon(R.drawable.ic_action_polygom_dark), app.getString(R.string.distance),
				OsmAndFormatter.getFormattedDistance((float) selectedPoint.getDistance(), app));

		if (selectedPoint.getTime() != 0) {
			DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
			buildRow(view, getThemedIcon(R.drawable.ic_action_time_start), null, app.getString(R.string.shared_string_time),
					format.format(selectedPoint.getTime()), 0, null,
					false, null, false, 0, false, false, false, null, true);
		}
		if (!Double.isNaN(selectedPoint.getEle())) {
			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_altitude), app.getString(R.string.altitude),
					OsmAndFormatter.getFormattedAlt(selectedPoint.getEle(), app));
		}
		if (!Double.isNaN(selectedPoint.getSpeed())) {
			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_speed), app.getString(R.string.shared_string_speed),
					OsmAndFormatter.getFormattedSpeed((float) selectedPoint.getSpeed(), app));
		}
		if (!Float.isNaN(selectedGpxPoint.getBearing())) {
			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_relative_bearing), app.getString(R.string.shared_string_bearing),
					OsmAndFormatter.getFormattedAzimuth(selectedGpxPoint.getBearing(), app));
		}
	}

	@Nullable
	private TrackDisplayGroup getTrackGroup() {
		List<GpxDisplayGroup> gpxDisplayGroups = selectedGpxPoint.getSelectedGpxFile().getSplitGroups(app);
		if (Algorithms.isEmpty(gpxDisplayGroups)) {
			return null;
		}

		return gpxDisplayGroups.stream()
				.filter(TrackDisplayGroup.class::isInstance)
				.map(group -> (TrackDisplayGroup) group)
				.findFirst()
				.orElse(null);
	}

	private GpxDisplayItem findDisplayItem() {
		TrackDisplayGroup trackGroup = getTrackGroup();
		if (trackGroup == null) return null;

		WptPt refPoint = selectedGpxPoint.getPrevPoint();
		if (refPoint == null) refPoint = selectedGpxPoint.getNextPoint();
		if (refPoint == null) refPoint = selectedPoint;

		for (TrkSegment segment : selectedGpxPoint.getSelectedGpxFile().getPointsToDisplay()) {
			List<WptPt> points = segment.getPoints();
			int currentIndex = points.indexOf(refPoint);
			if (currentIndex == -1) continue;

			for (GpxDisplayItem item : trackGroup.getDisplayItems()) {
				int startIdx = points.indexOf(item.locationStart);
				int endIdx = points.indexOf(item.locationEnd);
				if (startIdx != -1 && endIdx != -1 && currentIndex >= startIdx && currentIndex < endIdx) {
					return item;
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

		buildInfoRow(view, getThemedIcon(R.drawable.ic_action_track_16), app.getString(R.string.distance),
				OsmAndFormatter.getFormattedDistance(segmentAnalysis.getTotalDistance(), app));

		int diffElevationUp = (int) segmentAnalysis.getDiffElevationUp();
		if (diffElevationUp > 0) {
			String asc = OsmAndFormatter.getFormattedAlt(diffElevationUp, app);
			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_altitude_descent_ascent_16), app.getString(R.string.ascent), asc);
		}

		int diffElevationDown = (int) segmentAnalysis.getDiffElevationDown();
		if (diffElevationDown > 0) {
			String desc = OsmAndFormatter.getFormattedAlt(diffElevationDown, app);
			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_altitude_descent_ascent_16), app.getString(R.string.descent), desc);
		}

		if (segmentAnalysis.getAvgElevation() != 0) {
			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_altitude_average_16), app.getString(R.string.average_elevation),
					OsmAndFormatter.getFormattedAlt(segmentAnalysis.getAvgElevation(), app));
		}

		if (segmentAnalysis.hasSpeedData()) {
			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_time_moving_16), app.getString(R.string.moving_time),
					Algorithms.formatDuration((int) (segmentAnalysis.getTimeMoving() / 1000), app.accessibilityEnabled()));

			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_speed_16), app.getString(R.string.average_speed),
					OsmAndFormatter.getFormattedSpeed(segmentAnalysis.getAvgSpeed(), app));

			String maxSpeed = OsmAndFormatter.getFormattedSpeed(segmentAnalysis.getMaxSpeed(), app);
			String minSpeed = OsmAndFormatter.getFormattedSpeed(segmentAnalysis.getMinSpeed(), app);
			String maxMinSpeed;
			if (maxSpeed.contains(" ")) {
				maxMinSpeed = maxSpeed.substring(0, maxSpeed.indexOf(" ")).concat("/").concat(minSpeed);
			} else {
				maxMinSpeed = maxSpeed.substring(0, maxSpeed.indexOf("-")).concat("/").concat(minSpeed);
			}

			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_max_speed_16), app.getString(R.string.max_min),
					maxMinSpeed);
		}

		if (segmentAnalysis.getTimeSpan() > 0) {
			DateFormat tf = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
			DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);

			Date start = new Date(segmentAnalysis.getStartTime());
			String startValue = app.getString(R.string.ltr_or_rtl_combine_via_dash, tf.format(start), df.format(start));
			Date end = new Date(segmentAnalysis.getEndTime());
			String endValue = app.getString(R.string.ltr_or_rtl_combine_via_dash, tf.format(end), df.format(end));

			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_time_start_16), app.getString(R.string.shared_string_start_time), startValue);
			buildInfoRow(view, getThemedIcon(R.drawable.ic_action_time_end_16), app.getString(R.string.shared_string_end_time), endValue);
		}
	}

	private void buildInfoRow(View view, Drawable icon, String textPrefix, String text){
		buildRow(view, icon, null, textPrefix,
				text, 0, null,
				false, null, false, 0, false, false, false, null, false);
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
