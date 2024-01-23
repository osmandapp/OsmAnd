package net.osmand.plus.mapcontextmenu.builders;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.WptPt;
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


public class SelectedGpxMenuBuilder extends MenuBuilder {

	private final SelectedGpxPoint selectedGpxPoint;
	private final GPXTrackAnalysis analysis;
	private final WptPt selectedPoint;

	public SelectedGpxMenuBuilder(@NonNull MapActivity mapActivity, @NonNull SelectedGpxPoint selectedGpxPoint) {
		super(mapActivity);
		this.selectedGpxPoint = selectedGpxPoint;
		selectedPoint = selectedGpxPoint.getSelectedPoint();
		analysis = selectedGpxPoint.getSelectedGpxFile() == null
				? new GPXTrackAnalysis()
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
		buildOverviewRows(view);
		buildElevationRows(view);
		buildSpeedRows(view);
	}

	public void buildOverviewRows(View view) {
		buildCategoryView(view, app.getString(R.string.shared_string_overview));

		buildRow(view, getThemedIcon(R.drawable.ic_action_polygom_dark), null, app.getString(R.string.distance),
				OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app), 0, null,
				false, null, false, 0, false, false, false, null, false);

		String timeSpan = Algorithms.formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled());
		String timeMoving = Algorithms.formatDuration((int) (analysis.timeMoving / 1000), app.accessibilityEnabled());
		String timeSpanTitle = app.getString(R.string.shared_string_time_span) + " / " + app.getString(R.string.moving_time);
		buildRow(view, getThemedIcon(R.drawable.ic_action_time_span), null, timeSpanTitle,
				timeSpan + " / " + timeMoving, 0, null,
				false, null, false, 0, false, false, false, null, false);

		Date start = new Date(analysis.startTime);
		Date end = new Date(analysis.endTime);
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
					OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app), 0, null,
					false, null, false, 0, false, false, false, null, false);

			String min = OsmAndFormatter.getFormattedAlt(analysis.minElevation, app);
			String max = OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app);
			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_range), null, app.getString(R.string.altitude_range),
					min + " - " + max, 0, null,
					false, null, false, 0, false, false, false, null, false);

			String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
			String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_descent), null, app.getString(R.string.ascent_descent),
					asc + " / " + desc, 0, null,
					false, null, false, 0, false, false, false, null, true);

			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude_descent), null, app.getString(R.string.distance_moving),
					OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving, app), 0, null,
					false, null, false, 0, false, false, false, null, true);
		}
	}

	public void buildSpeedRows(View view) {
		if (analysis.isSpeedSpecified()) {
			buildCategoryView(view, app.getString(R.string.shared_string_speed));

			buildRow(view, getThemedIcon(R.drawable.ic_action_speed), null, app.getString(R.string.average_speed),
					OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app), 0, null,
					false, null, false, 0, false, false, false, null, false);

			buildRow(view, getThemedIcon(R.drawable.ic_action_max_speed), null, app.getString(R.string.max_speed),
					OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app), 0, null,
					false, null, false, 0, false, false, false, null, false);
		}
	}

	public void buildPointRows(View view) {
		buildCategoryView(view, app.getString(R.string.plugin_distance_point));

		buildRow(view, getThemedIcon(R.drawable.ic_action_polygom_dark), null, app.getString(R.string.distance),
				OsmAndFormatter.getFormattedDistance((float) selectedPoint.distance, app), 0, null,
				false, null, false, 0, false, false, false, null, false);

		if (selectedPoint.time != 0) {
			DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
			buildRow(view, getThemedIcon(R.drawable.ic_action_time_start), null, app.getString(R.string.shared_string_time),
					format.format(selectedPoint.time), 0, null,
					false, null, false, 0, false, false, false, null, true);
		}
		if (!Double.isNaN(selectedPoint.ele)) {
			buildRow(view, getThemedIcon(R.drawable.ic_action_altitude), null, app.getString(R.string.altitude),
					OsmAndFormatter.getFormattedAlt(selectedPoint.ele, app), 0, null,
					false, null, false, 0, false, false, false, null, false);
		}
		if (!Double.isNaN(selectedPoint.speed)) {
			buildRow(view, getThemedIcon(R.drawable.ic_action_speed), null, app.getString(R.string.shared_string_speed),
					OsmAndFormatter.getFormattedSpeed((float) selectedPoint.speed, app), 0, null,
					false, null, false, 0, false, false, false, null, false);
		}
		if (!Float.isNaN(selectedGpxPoint.getBearing())) {
			buildRow(view, getThemedIcon(R.drawable.ic_action_relative_bearing), null, app.getString(R.string.shared_string_bearing),
					OsmAndFormatter.getFormattedAzimuth(selectedGpxPoint.getBearing(), app), 0, null,
					false, null, false, 0, false, false, false, null, false);
		}
	}

	private void buildCategoryView(View view, String name) {
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
		int gpxSmallIconMargin = (int) ll.getResources().getDimension(R.dimen.gpx_small_icon_margin);
		int gpxSmallTextMargin = (int) ll.getResources().getDimension(R.dimen.gpx_small_text_margin);
		float gpxTextSize = ll.getResources().getDimension(R.dimen.default_desc_text_size);

		int textColor = ColorUtilities.getPrimaryTextColor(ll.getContext(), !light);

		buildIcon(ll, gpxSmallIconMargin, R.drawable.ic_action_distance_16);
		buildTextView(ll, gpxSmallTextMargin, gpxTextSize, textColor,
				OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));

		buildIcon(ll, gpxSmallIconMargin, R.drawable.ic_action_waypoint_16);
		buildTextView(ll, gpxSmallTextMargin, gpxTextSize, textColor, "" + analysis.wptPoints);

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
