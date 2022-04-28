package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.LEFT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.RIGHT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

public enum WidgetParams {

	// Left Panel
	NEXT_TURN("next_turn", R.string.map_widget_next_turn, R.drawable.widget_next_turn_day, R.drawable.widget_next_turn_night, LEFT),
	SMALL_NEXT_TURN("next_turn_small", R.string.map_widget_next_turn_small, R.drawable.widget_next_turn_small_day, R.drawable.widget_next_turn_small_night, LEFT),
	SECOND_NEXT_TURN("next_next_turn", R.string.map_widget_next_next_turn, R.drawable.widget_second_next_turn_day, R.drawable.widget_second_next_turn_night, LEFT),
	// Top panel
	COORDINATES("coordinates", R.string.coordinates_widget, R.drawable.ic_action_coordinates_widget, TOP),
	STREET_NAME("street_name", R.string.street_name, R.drawable.widget_street_name_day, R.drawable.widget_street_name_night, TOP),
	MARKERS_TOP_BAR("map_markers_top", R.string.map_markers_bar, R.drawable.widget_markers_topbar_day, R.drawable.widget_markers_topbar_night, TOP),
	LANES("lanes", R.string.show_lanes, R.drawable.widget_lanes_day, R.drawable.widget_lanes_night, TOP),
	// Right panel
	DISTANCE_TO_DESTINATION("distance", R.string.map_widget_distance_to_destination, R.drawable.widget_target_day, R.drawable.widget_target_night, RIGHT),
	INTERMEDIATE_DESTINATION("intermediate_distance", R.string.map_widget_intermediate_distance, R.drawable.widget_intermediate_day, R.drawable.widget_intermediate_night, RIGHT),
	INTERMEDIATE_ARRIVAL_TIME("intermediate_arrival_time", R.string.access_intermediate_arrival_time, R.drawable.widget_intermediate_time_day, R.drawable.widget_intermediate_time_night, RIGHT),
	INTERMEDIATE_TIME_TO_GO("intermediate_time_time_to_go", R.string.map_widget_intermediate_time, R.drawable.widget_intermediate_time_day, R.drawable.widget_intermediate_time_night, RIGHT),
	ARRIVAL_TIME("arrival_time", R.string.access_arrival_time, R.drawable.widget_time_day, R.drawable.widget_time_night, RIGHT),
	TIME_TO_GO("time_to_go", R.string.map_widget_time, R.drawable.widget_time_to_distance_day, R.drawable.widget_time_to_distance_night, RIGHT),
	SIDE_MARKER_1("map_marker_1st", R.string.map_marker_1st, R.drawable.widget_marker_day, R.drawable.widget_marker_night, RIGHT),
	SIDE_MARKER_2("map_marker_2nd", R.string.map_marker_2nd, R.drawable.widget_marker_day, R.drawable.widget_marker_night, RIGHT),
	RELATIVE_BEARING("relative_bearing", R.string.map_widget_bearing, R.drawable.widget_relative_bearing_day, R.drawable.widget_relative_bearing_night, RIGHT),
	MAGNETIC_BEARING("magnetic_bearing", R.string.map_widget_magnetic_bearing, R.drawable.widget_bearing_day, R.drawable.widget_bearing_night, RIGHT),
	CURRENT_SPEED("speed", R.string.map_widget_current_speed, R.drawable.widget_speed_day, R.drawable.widget_speed_night, RIGHT),
	MAX_SPEED("max_speed", R.string.map_widget_max_speed, R.drawable.widget_max_speed_day, R.drawable.widget_max_speed_night, RIGHT),
	ALTITUDE("altitude", R.string.map_widget_altitude, R.drawable.widget_altitude_day, R.drawable.widget_altitude_night, RIGHT),
	GPS_INFO("gps_info", R.string.map_widget_gps_info, R.drawable.widget_gps_info_day, R.drawable.widget_gps_info_night, RIGHT),
	TRIP_RECORDING("monitoring", R.string.map_widget_monitoring, R.drawable.widget_monitoring_rec_small_day, R.drawable.widget_monitoring_rec_small_night, RIGHT),
	CURRENT_TIME("plain_time", R.string.map_widget_plain_time, R.drawable.widget_time_day, R.drawable.widget_time_night, RIGHT),
	BATTERY("battery", R.string.map_widget_battery, R.drawable.widget_battery_day, R.drawable.widget_battery_night, RIGHT),
	RADIUS_RULER("ruler", R.string.map_widget_ruler_control, R.drawable.widget_ruler_circle_day, R.drawable.widget_ruler_circle_night, RIGHT),
	FPS("fps", R.string.map_widget_fps_info, R.drawable.widget_fps_day, R.drawable.widget_fps_night, RIGHT),
	AV_NOTES_ON_REQUEST("av_notes_on_request", R.string.av_def_action_choose, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, RIGHT),
	AV_NOTES_RECORD_AUDIO("av_notes_record_audio", R.string.av_def_action_audio, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, RIGHT),
	AV_NOTES_RECORD_VIDEO("av_notes_record_video", R.string.av_def_action_video, R.drawable.widget_av_video_day, R.drawable.widget_av_video_night, RIGHT),
	AV_NOTES_TAKE_PHOTO("av_notes_take_photo", R.string.av_def_action_picture, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, RIGHT),
	MAPILLARY("mapillary", R.string.mapillary, R.drawable.widget_mapillary_day, R.drawable.widget_mapillary_night, RIGHT),
	PARKING("parking", R.string.map_widget_parking, R.drawable.widget_parking_day, R.drawable.widget_parking_night, RIGHT),
	// Bottom panel
	ELEVATION_PROFILE("elevation_profile", R.string.elevation_profile, 0, BOTTOM);

	public static final String INTERMEDIATE_TIME_WIDGET_LEGACY = "intermediate_time";
	public static final String NAVIGATION_TIME_WIDGET_LEGACY = "time";
	public static final String BEARING_WIDGET_LEGACY = "bearing";
	public static final String AV_NOTES_WIDGET_LEGACY = "audionotes";

	@NonNull
	public final String id;
	@StringRes
	public final int titleId;
	@DrawableRes
	public final int dayIconId;
	@DrawableRes
	public final int nightIconId;
	@NonNull
	public final WidgetsPanel defaultPanel;

	WidgetParams(@NonNull String id,
	             @StringRes int titleId,
	             @DrawableRes int iconId,
	             @NonNull WidgetsPanel defaultPanel) {
		this(id, titleId, iconId, iconId, defaultPanel);
	}

	WidgetParams(@NonNull String id,
	             @StringRes int titleId,
	             @DrawableRes int dayIconId,
	             @DrawableRes int nightIconId,
	             @NonNull WidgetsPanel defaultPanel) {
		this.id = id;
		this.titleId = titleId;
		this.dayIconId = dayIconId;
		this.nightIconId = nightIconId;
		this.defaultPanel = defaultPanel;
	}

	@Nullable
	public WidgetGroup getGroup() {
		for (WidgetGroup group : WidgetGroup.values()) {
			if (group.getWidgets().contains(this)) {
				return group;
			}
		}
		return null;
	}

	@DrawableRes
	public int getIconId(boolean night) {
		return night ? nightIconId : dayIconId;
	}

	public boolean isIconColored() {
		return dayIconId != nightIconId;
	}

	public int getDefaultOrder() {
		return defaultPanel.getOriginalWidgetOrder(id);
	}

	@Nullable
	public static WidgetParams getById(@NonNull String id) {
		for (WidgetParams widget : WidgetParams.values()) {
			if (widget.id.equals(id)) {
				return widget;
			}
		}
		return null;
	}
}