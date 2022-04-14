package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
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
	INTERMEDIATE_DISTANCE("intermediate_distance", R.string.map_widget_intermediate_distance, R.drawable.ic_action_intermediate, RIGHT),
	INTERMEDIATE_TIME("intermediate_time", 0, 0, RIGHT),
	DISTANCE_TO_DESTINATION("distance", R.string.map_widget_distance_to_destination, R.drawable.ic_action_target, RIGHT),
	NAVIGATION_TIME("time", 0, 0, RIGHT),
	SIDE_MARKER_1("map_marker_1st", R.string.map_marker_1st, R.drawable.ic_action_flag, RIGHT),
	BEARING("bearing", 0, 0, RIGHT),
	SIDE_MARKER_2("map_marker_2nd", R.string.map_marker_2nd, R.drawable.ic_action_flag, RIGHT),
	CURRENT_SPEED("speed", R.string.map_widget_current_speed, R.drawable.ic_action_speed, RIGHT),
	MAX_SPEED("max_speed", R.string.map_widget_max_speed, R.drawable.ic_action_speed_limit, RIGHT),
	ALTITUDE("altitude", R.string.map_widget_altitude, R.drawable.ic_action_altitude, RIGHT),
	GPS_INFO("gps_info", R.string.map_widget_gps_info, R.drawable.ic_action_gps_info, RIGHT),
	TRIP_RECORDING("monitoring", R.string.map_widget_monitoring, R.drawable.ic_action_play_dark, RIGHT),
	AUDIO_VIDEO_NOTES("audionotes", 0, 0, RIGHT),
	MAPILLARY("mapillary", R.string.mapillary, R.drawable.ic_action_mapillary, RIGHT),
	PARKING("parking", R.string.map_widget_parking, R.drawable.ic_action_parking_dark, RIGHT),
	PLAIN_TIME("plain_time", R.string.map_widget_plain_time, R.drawable.ic_action_time, RIGHT),
	BATTERY("battery", R.string.map_widget_battery, R.drawable.ic_action_battery, RIGHT),
	RADIUS_RULER("ruler", 0, 0, RIGHT),
	FPS("fps", R.string.map_widget_fps_info, R.drawable.ic_action_fps, RIGHT),
	// Bottom panel
	ELEVATION_PROFILE("elevation_profile", 0, 0, BOTTOM);

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

	WidgetParams(@NonNull String id, @StringRes int titleId, @DrawableRes int iconId, @NonNull WidgetsPanel defaultPanel) {
		this(id, titleId, iconId, iconId, defaultPanel);
	}

	WidgetParams(@NonNull String id, @StringRes int titleId, @DrawableRes int dayIconId,
	             @DrawableRes int nightIconId, @NonNull WidgetsPanel defaultPanel) {
		this.id = id;
		this.titleId = titleId;
		this.dayIconId = dayIconId;
		this.nightIconId = nightIconId;
		this.defaultPanel = defaultPanel;
	}
}