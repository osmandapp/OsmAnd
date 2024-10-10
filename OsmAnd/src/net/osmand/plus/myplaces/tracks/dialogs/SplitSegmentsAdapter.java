package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.track.helpers.GpxDisplayGroup.getTrackDisplayGroup;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.TrackDisplayGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

class SplitSegmentsAdapter extends ArrayAdapter<GpxDisplayItem> {

	private final OsmandApplication app;
	private final FragmentActivity activity;

	private final Rect minMaxSpeedTextBounds = new Rect();
	private final GpxDisplayItem displayItem;
	private final boolean joinSegments;
	private int minMaxSpeedLayoutWidth;

	private final Paint minMaxSpeedPaint = new Paint();
	private ColorStateList defaultTextColor;

	SplitSegmentsAdapter(@NonNull FragmentActivity activity,
	                     @NonNull List<GpxDisplayItem> items,
	                     @NonNull GpxDisplayItem displayItem,
	                     boolean joinSegments) {
		super(activity, 0, items);
		this.activity = activity;
		this.app = (OsmandApplication) activity.getApplicationContext();
		this.displayItem = displayItem;
		this.joinSegments = joinSegments;

		minMaxSpeedPaint.setTextSize(app.getResources().getDimension(R.dimen.default_split_segments_data));
		minMaxSpeedPaint.setTypeface(FontCache.getMediumFont());
		minMaxSpeedPaint.setStyle(Paint.Style.FILL);
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		GpxDisplayItem currentGpxDisplayItem = getItem(position);
		TrackDisplayGroup trackGroup = null;
		if (currentGpxDisplayItem != null) {
			trackGroup = getTrackDisplayGroup(currentGpxDisplayItem.group);
		}
		if (convertView == null) {
			convertView = activity.getLayoutInflater().inflate(R.layout.gpx_split_segment_fragment, parent, false);
		}
		convertView.setOnClickListener(null);
		boolean nightMode = !app.getSettings().isLightContent();
		int activeColorId = ColorUtilities.getActiveColorId(nightMode);
		TextView overviewTextView = convertView.findViewById(R.id.overview_text);
		ImageView overviewImageView = convertView.findViewById(R.id.overview_image);
		if (position == 0) {
			overviewImageView.setImageDrawable(getIcon(R.drawable.ic_action_time_span_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
			if (defaultTextColor == null) {
				defaultTextColor = overviewTextView.getTextColors();
			}
			overviewTextView.setTextColor(defaultTextColor);
			overviewTextView.setText(app.getString(R.string.shared_string_overview));
			if (currentGpxDisplayItem != null) {
				String overview = getString(R.string.shared_string_overview);
				String points = String.valueOf(currentGpxDisplayItem.analysis.getPoints());
				overviewTextView.setText(getString(R.string.ltr_or_rtl_combine_with_brackets, overview, points));

				String timeSpan = getString(R.string.shared_string_time_span);
				String formattedDuration = Algorithms.formatDuration(currentGpxDisplayItem.analysis.getDurationInSeconds(), app.accessibilityEnabled());
				TextView tvDuration = convertView.findViewById(R.id.fragment_count_text);
				tvDuration.setText(getString(R.string.ltr_or_rtl_combine_via_colon, timeSpan, formattedDuration));
			}
		} else {
			if (currentGpxDisplayItem != null && currentGpxDisplayItem.analysis != null) {
				overviewTextView.setTextColor(app.getColor(activeColorId));
				if (trackGroup != null && trackGroup.isSplitDistance()) {
					overviewImageView.setImageDrawable(getIcon(R.drawable.ic_action_track_16, activeColorId));
					overviewTextView.setText("");
					double metricStart = currentGpxDisplayItem.analysis.getMetricEnd() - currentGpxDisplayItem.analysis.getTotalDistance();
					overviewTextView.append(OsmAndFormatter.getFormattedDistance((float) metricStart, app));
					overviewTextView.append(" - ");
					overviewTextView.append(OsmAndFormatter.getFormattedDistance((float) currentGpxDisplayItem.analysis.getMetricEnd(), app));
					overviewTextView.append("  (" + currentGpxDisplayItem.analysis.getPoints() + ")");
				} else if (trackGroup != null && trackGroup.isSplitTime()) {
					overviewImageView.setImageDrawable(getIcon(R.drawable.ic_action_time_span_16, activeColorId));
					overviewTextView.setText("");
					double metricStart = currentGpxDisplayItem.analysis.getMetricEnd() - (currentGpxDisplayItem.analysis.getTimeSpan() / 1000f);
					overviewTextView.append(OsmAndFormatter.getFormattedDuration((int) metricStart, app));
					overviewTextView.append(" - ");
					overviewTextView.append(OsmAndFormatter.getFormattedDuration((int) currentGpxDisplayItem.analysis.getMetricEnd(), app));
					overviewTextView.append("  (" + currentGpxDisplayItem.analysis.getPoints() + ")");
				}
				((TextView) convertView.findViewById(R.id.fragment_count_text)).setText(getString(R.string.of, position, getCount() - 1));
			}
		}

		((ImageView) convertView.findViewById(R.id.start_time_image))
				.setImageDrawable(getIcon(R.drawable.ic_action_time_start_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
		((ImageView) convertView.findViewById(R.id.end_time_image))
				.setImageDrawable(getIcon(R.drawable.ic_action_time_end_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
		((ImageView) convertView.findViewById(R.id.average_altitude_image))
				.setImageDrawable(getIcon(R.drawable.ic_action_altitude_average_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
		((ImageView) convertView.findViewById(R.id.altitude_range_image))
				.setImageDrawable(getIcon(R.drawable.ic_action_altitude_range_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
		((ImageView) convertView.findViewById(R.id.ascent_descent_image))
				.setImageDrawable(getIcon(R.drawable.ic_action_altitude_descent_ascent_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
		((ImageView) convertView.findViewById(R.id.moving_time_image))
				.setImageDrawable(getIcon(R.drawable.ic_action_time_moving_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
		((ImageView) convertView.findViewById(R.id.average_speed_image))
				.setImageDrawable(getIcon(R.drawable.ic_action_speed_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
		((ImageView) convertView.findViewById(R.id.max_speed_image))
				.setImageDrawable(getIcon(R.drawable.ic_action_max_speed_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));

		if (currentGpxDisplayItem != null) {
			GpxTrackAnalysis analysis = currentGpxDisplayItem.analysis;
			if (analysis != null) {
				ImageView distanceOrTimeSpanImageView = convertView.findViewById(R.id.distance_or_timespan_image);
				TextView distanceOrTimeSpanValue = convertView.findViewById(R.id.distance_or_time_span_value);
				TextView distanceOrTimeSpanText = convertView.findViewById(R.id.distance_or_time_span_text);
				if (position == 0) {
					distanceOrTimeSpanImageView.setImageDrawable(getIcon(R.drawable.ic_action_track_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
					float totalDistance = !joinSegments && displayItem.isGeneralTrack() ? analysis.getTotalDistanceWithoutGaps() : analysis.getTotalDistance();
					distanceOrTimeSpanValue.setText(OsmAndFormatter.getFormattedDistance(totalDistance, app));
					distanceOrTimeSpanText.setText(app.getString(R.string.distance));
				} else {
					if (trackGroup != null && trackGroup.isSplitDistance()) {
						distanceOrTimeSpanImageView.setImageDrawable(getIcon(R.drawable.ic_action_time_span_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
						if (analysis.getDurationInMs() > 0) {
							distanceOrTimeSpanValue.setText(Algorithms.formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled()));
						} else {
							distanceOrTimeSpanValue.setText("-");
						}
						distanceOrTimeSpanText.setText(app.getString(R.string.shared_string_time_span));
					} else if (trackGroup != null && trackGroup.isSplitTime()) {
						distanceOrTimeSpanImageView.setImageDrawable(getIcon(R.drawable.ic_action_track_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
						distanceOrTimeSpanValue.setText(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
						distanceOrTimeSpanText.setText(app.getString(R.string.distance));
					}
				}

				TextView startTimeValue = convertView.findViewById(R.id.start_time_value);
				TextView startDateValue = convertView.findViewById(R.id.start_date_value);
				TextView endTimeValue = convertView.findViewById(R.id.end_time_value);
				TextView endDateValue = convertView.findViewById(R.id.end_date_value);
				if (analysis.getTimeSpan() > 0) {
					DateFormat tf = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
					DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);

					Date start = new Date(analysis.getStartTime());
					startTimeValue.setText(tf.format(start));
					startDateValue.setText(df.format(start));

					Date end = new Date(analysis.getEndTime());
					endTimeValue.setText(tf.format(end));
					endDateValue.setText(df.format(end));
				} else {
					startTimeValue.setText("-");
					startDateValue.setText("-");
					endTimeValue.setText("-");
					endDateValue.setText("-");
				}

				View elevationDivider = convertView.findViewById(R.id.elevation_divider);
				View elevationSection = convertView.findViewById(R.id.elevation_layout);
				if (analysis.hasElevationData()) {
					elevationDivider.setVisibility(View.VISIBLE);
					elevationSection.setVisibility(View.VISIBLE);

					((TextView) convertView.findViewById(R.id.average_altitude_value))
							.setText(OsmAndFormatter.getFormattedAlt(analysis.getAvgElevation(), app));

					String min = OsmAndFormatter.getFormattedAlt(analysis.getMinElevation(), app);
					String max = OsmAndFormatter.getFormattedAlt(analysis.getMaxElevation(), app);
					String min_max_elevation = min.substring(0, min.indexOf(" ")).concat("/").concat(max);
					if (min_max_elevation.length() > 9) {
						(convertView.findViewById(R.id.min_altitude_value))
								.setVisibility(View.VISIBLE);
						(convertView.findViewById(R.id.max_altitude_value))
								.setVisibility(View.VISIBLE);
						((TextView) convertView.findViewById(R.id.min_altitude_value))
								.setText(min);
						((TextView) convertView.findViewById(R.id.max_altitude_value))
								.setText(max);
						(convertView.findViewById(R.id.min_max_altitude_value))
								.setVisibility(View.GONE);
					} else {
						(convertView.findViewById(R.id.min_max_altitude_value))
								.setVisibility(View.VISIBLE);
						((TextView) convertView.findViewById(R.id.min_max_altitude_value))
								.setText(min_max_elevation);
						(convertView.findViewById(R.id.min_altitude_value))
								.setVisibility(View.GONE);
						(convertView.findViewById(R.id.max_altitude_value))
								.setVisibility(View.GONE);
					}

					TextView ascentValue = convertView.findViewById(R.id.ascent_value);
					TextView descentValue = convertView.findViewById(R.id.descent_value);
					TextView ascentDescentValue = convertView.findViewById(R.id.ascent_descent_value);

					String asc = OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationUp(), app);
					String desc = OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationDown(), app);
					String asc_desc = asc.substring(0, asc.indexOf(" ")).concat("/").concat(desc);
					if (asc_desc.length() > 9) {
						ascentValue.setVisibility(View.VISIBLE);
						descentValue.setVisibility(View.VISIBLE);
						ascentValue.setText(asc);
						descentValue.setText(desc);
						ascentDescentValue.setVisibility(View.GONE);
					} else {
						ascentDescentValue.setVisibility(View.VISIBLE);
						ascentDescentValue.setText(asc_desc);
						ascentValue.setVisibility(View.GONE);
						descentValue.setVisibility(View.GONE);
					}

				} else {
					elevationDivider.setVisibility(View.GONE);
					elevationSection.setVisibility(View.GONE);
				}

				View speedDivider = convertView.findViewById(R.id.speed_divider);
				View speedSection = convertView.findViewById(R.id.speed_layout);
				if (analysis.hasSpeedData()) {
					speedDivider.setVisibility(View.VISIBLE);
					speedSection.setVisibility(View.VISIBLE);

					((TextView) convertView.findViewById(R.id.moving_time_value))
							.setText(Algorithms.formatDuration((int) (analysis.getTimeMoving() / 1000), app.accessibilityEnabled()));
					((TextView) convertView.findViewById(R.id.average_speed_value))
							.setText(OsmAndFormatter.getFormattedSpeed(analysis.getAvgSpeed(), app));

					String maxSpeed = OsmAndFormatter.getFormattedSpeed(analysis.getMaxSpeed(), app);
					String minSpeed = OsmAndFormatter.getFormattedSpeed(analysis.getMinSpeed(), app);
					String maxMinSpeed;
					if (maxSpeed.contains(" ")) {
						maxMinSpeed = maxSpeed.substring(0, maxSpeed.indexOf(" ")).concat("/").concat(minSpeed);
					} else {
						maxMinSpeed = maxSpeed.substring(0, maxSpeed.indexOf("-")).concat("/").concat(minSpeed);
					}

					if (minMaxSpeedLayoutWidth == 0) {
						DisplayMetrics metrics = new DisplayMetrics();
						activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
						int screenWidth = metrics.widthPixels;
						int widthWithoutSidePadding = screenWidth - AndroidUtils.dpToPx(activity, 32);
						int singleLayoutWidth = widthWithoutSidePadding / 3;
						int twoLayouts = 2 * (singleLayoutWidth + AndroidUtils.dpToPx(activity, 3));
						minMaxSpeedLayoutWidth = widthWithoutSidePadding - twoLayouts - AndroidUtils.dpToPx(activity, 28);
					}

					minMaxSpeedPaint.getTextBounds(maxMinSpeed, 0, maxMinSpeed.length(), minMaxSpeedTextBounds);
					int minMaxStringWidth = minMaxSpeedTextBounds.width();

					if (analysis.getMinSpeed() == 0) {
						(convertView.findViewById(R.id.max_speed_value))
								.setVisibility(View.VISIBLE);
						(convertView.findViewById(R.id.min_speed_value))
								.setVisibility(View.GONE);
						((TextView) convertView.findViewById(R.id.max_speed_value))
								.setText(maxSpeed);
						(convertView.findViewById(R.id.max_min_speed_value))
								.setVisibility(View.GONE);
						((TextView) convertView.findViewById(R.id.max_min_speed_text))
								.setText(app.getString(R.string.shared_string_max));
					} else if (minMaxStringWidth > minMaxSpeedLayoutWidth) {
						(convertView.findViewById(R.id.max_speed_value))
								.setVisibility(View.VISIBLE);
						(convertView.findViewById(R.id.min_speed_value))
								.setVisibility(View.VISIBLE);
						((TextView) convertView.findViewById(R.id.max_speed_value))
								.setText(maxSpeed);
						((TextView) convertView.findViewById(R.id.min_speed_value))
								.setText(minSpeed);
						(convertView.findViewById(R.id.max_min_speed_value))
								.setVisibility(View.GONE);
						((TextView) convertView.findViewById(R.id.max_min_speed_text))
								.setText(app.getString(R.string.max_min));
					} else {
						(convertView.findViewById(R.id.max_min_speed_value))
								.setVisibility(View.VISIBLE);
						((TextView) convertView.findViewById(R.id.max_min_speed_value))
								.setText(maxMinSpeed);
						(convertView.findViewById(R.id.max_speed_value))
								.setVisibility(View.GONE);
						(convertView.findViewById(R.id.min_speed_value))
								.setVisibility(View.GONE);
						((TextView) convertView.findViewById(R.id.max_min_speed_text))
								.setText(app.getString(R.string.max_min));
					}
				} else {
					speedDivider.setVisibility(View.GONE);
					speedSection.setVisibility(View.GONE);
				}
			}
		}
		return convertView;
	}

	@NonNull
	private Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return app.getUIUtilities().getIcon(id, colorId);
	}

	@NonNull
	private String getString(@StringRes int resId, Object... formatArgs) {
		return app.getString(resId, formatArgs);
	}
}
