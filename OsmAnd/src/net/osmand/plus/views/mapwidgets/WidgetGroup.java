package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.R;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import static net.osmand.plus.views.mapwidgets.WidgetParams.ARRIVAL_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.AV_NOTES_ON_REQUEST;
import static net.osmand.plus.views.mapwidgets.WidgetParams.AV_NOTES_RECORD_AUDIO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.AV_NOTES_RECORD_VIDEO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.AV_NOTES_TAKE_PHOTO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.DISTANCE_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_ARRIVAL_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_TIME_TO_GO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.MAGNETIC_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetParams.NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetParams.RELATIVE_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SECOND_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SIDE_MARKER_1;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SIDE_MARKER_2;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SMALL_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetParams.TIME_TO_GO;

public enum WidgetGroup {

	ROUTE_MANEUVERS(R.string.route_maneuvers, R.string.route_maneuvers_desc, R.drawable.widget_lanes_day, R.drawable.widget_lanes_night, "https://docs.osmand.net/docs/user/widgets/nav-widgets#next-turns"),
	NAVIGATION_POINTS(R.string.navigation_points, R.string.navigation_points_desc, R.drawable.widget_navigation_day, R.drawable.widget_navigation_night, "https://docs.osmand.net/docs/user/widgets/nav-widgets/#navigation-points"),
	MAP_MARKERS(R.string.map_markers, R.string.map_markers_desc, R.drawable.widget_marker_day, R.drawable.widget_marker_day, "https://docs.osmand.net/docs/user/widgets/markers"),
	BEARING(R.string.shared_string_bearing, R.string.bearing_desc, R.drawable.widget_relative_bearing_day, R.drawable.widget_relative_bearing_night, "https://docs.osmand.net/docs/user/widgets/nav-widgets#bearing"),
	AUDIO_VIDEO_NOTES(R.string.map_widget_av_notes, R.string.audio_video_notes_desc, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, "https://docs.osmand.net/docs/user/widgets/info-widgets#-audio-video-notes-widget-android");

	@StringRes
	public final int titleId;
	@StringRes
	public final int descId;
	@DrawableRes
	public final int dayIconId;
	@DrawableRes
	public final int nightIconId;
	@NonNull
	public String docsUrl;

	WidgetGroup(@StringRes int titleId,
	            @StringRes int descId,
	            @DrawableRes int dayIconId,
	            @DrawableRes int nightIconId,
	            @NonNull String docsUrl) {
		this.titleId = titleId;
		this.descId = descId;
		this.dayIconId = dayIconId;
		this.nightIconId = nightIconId;
		this.docsUrl = docsUrl;
	}

	@NonNull
	public List<WidgetParams> getWidgets() {
		if (this == ROUTE_MANEUVERS) {
			return Arrays.asList(NEXT_TURN, SMALL_NEXT_TURN, SECOND_NEXT_TURN);
		} else if (this == NAVIGATION_POINTS) {
			return Arrays.asList(DISTANCE_TO_DESTINATION, INTERMEDIATE_DESTINATION, INTERMEDIATE_ARRIVAL_TIME,
					INTERMEDIATE_TIME_TO_GO, ARRIVAL_TIME, TIME_TO_GO);
		} else if (this == MAP_MARKERS) {
			return Arrays.asList(SIDE_MARKER_1, SIDE_MARKER_2);
		} else if (this == BEARING) {
			return Arrays.asList(RELATIVE_BEARING, MAGNETIC_BEARING);
		} else if (this == AUDIO_VIDEO_NOTES) {
			return Arrays.asList(AV_NOTES_ON_REQUEST, AV_NOTES_RECORD_AUDIO, AV_NOTES_RECORD_VIDEO, AV_NOTES_TAKE_PHOTO);
		} else {
			throw new IllegalStateException("Unsupported widgets group");
		}
	}

	@DrawableRes
	public int getIconId(boolean nightMode) {
		return nightMode ? nightIconId : dayIconId;
	}

	@StringRes
	public int getSecondaryDescriptionId() {
		if (this == BEARING) {
			return R.string.bearing_secondary_desc;
		} else if (this == AUDIO_VIDEO_NOTES) {
			return R.string.av_notes_secondary_desc;
		}
		return 0;
	}

	@DrawableRes
	public int getSecondaryIconId() {
		if (this == BEARING) {
			return R.drawable.ic_action_help;
		} else if (this == AUDIO_VIDEO_NOTES) {
			return R.drawable.ic_extension_dark;
		}
		return 0;
	}

	public int getOrder() {
		return getWidgets().get(0).getDefaultOrder();
	}
}