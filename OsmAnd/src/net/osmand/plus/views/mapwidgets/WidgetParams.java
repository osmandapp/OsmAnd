package net.osmand.plus.views.mapwidgets;

import android.content.Context;

import net.osmand.plus.R;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.configure.settings.ElevationProfileWidgetSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.MapMarkersBarWidgetSettingFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.RadiusRulerWidgetSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.DEFAULT_ORDER;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.LEFT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.RIGHT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

public enum WidgetParams {

	// Left Panel
	NEXT_TURN("next_turn", R.string.map_widget_next_turn, 0, R.drawable.widget_next_turn_day, R.drawable.widget_next_turn_night, LEFT),
	SMALL_NEXT_TURN("next_turn_small", R.string.map_widget_next_turn_small, 0, R.drawable.widget_next_turn_small_day, R.drawable.widget_next_turn_small_night, LEFT),
	SECOND_NEXT_TURN("next_next_turn", R.string.map_widget_next_next_turn, 0, R.drawable.widget_second_next_turn_day, R.drawable.widget_second_next_turn_night, LEFT),
	// Top panel
	COORDINATES("coordinates", R.string.coordinates_widget, R.string.coordinates_widget_desc, R.drawable.widget_coordinates_longitude_west_day, R.drawable.widget_coordinates_longitude_west_night, TOP),
	STREET_NAME("street_name", R.string.street_name, R.string.street_name_widget_desc, R.drawable.widget_street_name_day, R.drawable.widget_street_name_night, TOP),
	MARKERS_TOP_BAR("map_markers_top", R.string.map_markers_bar, R.string.map_markers_bar_widget_desc, R.drawable.widget_markers_topbar_day, R.drawable.widget_markers_topbar_night, TOP),
	LANES("lanes", R.string.show_lanes, R.string.lanes_widgets_desc, R.drawable.widget_lanes_day, R.drawable.widget_lanes_night, TOP),
	// Right panel
	DISTANCE_TO_DESTINATION("distance", R.string.map_widget_distance_to_destination, 0, R.drawable.widget_target_day, R.drawable.widget_target_night, RIGHT),
	INTERMEDIATE_DESTINATION("intermediate_distance", R.string.map_widget_intermediate_distance, 0, R.drawable.widget_intermediate_day, R.drawable.widget_intermediate_night, RIGHT),
	INTERMEDIATE_ARRIVAL_TIME("intermediate_arrival_time", R.string.access_intermediate_arrival_time, 0, R.drawable.widget_intermediate_time_day, R.drawable.widget_intermediate_time_night, RIGHT),
	INTERMEDIATE_TIME_TO_GO("intermediate_time_time_to_go", R.string.map_widget_intermediate_time, 0, R.drawable.widget_intermediate_time_day, R.drawable.widget_intermediate_time_night, RIGHT),
	ARRIVAL_TIME("arrival_time", R.string.access_arrival_time, R.string.arrival_time_widget_desc, R.drawable.widget_time_day, R.drawable.widget_time_night, RIGHT),
	TIME_TO_GO("time_to_go", R.string.map_widget_time, R.string.time_to_go_desc, R.drawable.widget_time_to_distance_day, R.drawable.widget_time_to_distance_night, RIGHT),
	SIDE_MARKER_1("map_marker_1st", R.string.map_marker_1st, 0, R.drawable.widget_marker_day, R.drawable.widget_marker_night, RIGHT),
	SIDE_MARKER_2("map_marker_2nd", R.string.map_marker_2nd, 0, R.drawable.widget_marker_day, R.drawable.widget_marker_night, RIGHT),
	RELATIVE_BEARING("relative_bearing", R.string.map_widget_bearing, 0, R.drawable.widget_relative_bearing_day, R.drawable.widget_relative_bearing_night, RIGHT),
	MAGNETIC_BEARING("magnetic_bearing", R.string.map_widget_magnetic_bearing, R.string.magnetic_bearing_widget_desc, R.drawable.widget_bearing_day, R.drawable.widget_bearing_night, RIGHT),
	CURRENT_SPEED("speed", R.string.map_widget_current_speed, R.string.current_speed_widget_desc, R.drawable.widget_speed_day, R.drawable.widget_speed_night, RIGHT),
	MAX_SPEED("max_speed", R.string.map_widget_max_speed, R.string.max_speed_widget_desc, R.drawable.widget_max_speed_day, R.drawable.widget_max_speed_night, RIGHT),
	ALTITUDE("altitude", R.string.map_widget_altitude, R.string.altitude_widget_desc, R.drawable.widget_altitude_day, R.drawable.widget_altitude_night, RIGHT),
	GPS_INFO("gps_info", R.string.map_widget_gps_info, R.string.gps_info_widget_desc, R.drawable.widget_gps_info_day, R.drawable.widget_gps_info_night, RIGHT),
	TRIP_RECORDING_DISTANCE("monitoring", R.string.map_widget_trip_recording_distance, 0, R.drawable.widget_trip_recording_day, R.drawable.widget_trip_recording_night, RIGHT),
	TRIP_RECORDING_TIME("trip_recording_time", R.string.map_widget_trip_recording_duration, 0, R.drawable.widget_track_recording_duration_day, R.drawable.widget_track_recording_duration_night, RIGHT),
	TRIP_RECORDING_UPHILL("trip_recording_uphill", R.string.map_widget_trip_recording_uphill, 0, R.drawable.widget_track_recording_uphill_day, R.drawable.widget_track_recording_uphill_night, RIGHT),
	TRIP_RECORDING_DOWNHILL("trip_recording_downhill", R.string.map_widget_trip_recording_downhill, 0, R.drawable.widget_track_recording_downhill_day, R.drawable.widget_track_recording_downhill_night, RIGHT),
	CURRENT_TIME("plain_time", R.string.map_widget_plain_time, R.string.current_time_widget_desc, R.drawable.widget_time_day, R.drawable.widget_time_night, RIGHT),
	BATTERY("battery", R.string.map_widget_battery, R.string.battery_widget_desc, R.drawable.widget_battery_day, R.drawable.widget_battery_night, RIGHT),
	RADIUS_RULER("ruler", R.string.map_widget_ruler_control, R.string.radius_rules_widget_desc, R.drawable.widget_ruler_circle_day, R.drawable.widget_ruler_circle_night, RIGHT),
	FPS("fps", R.string.map_widget_fps_info, R.string.fps_widget_desc, R.drawable.widget_fps_day, R.drawable.widget_fps_night, RIGHT),
	AV_NOTES_ON_REQUEST("av_notes_on_request", R.string.av_def_action_choose, R.string.av_notes_choose_action_widget_desc, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, RIGHT),
	AV_NOTES_RECORD_AUDIO("av_notes_record_audio", R.string.av_def_action_audio, 0, R.drawable.widget_av_audio_day, R.drawable.widget_av_audio_night, RIGHT),
	AV_NOTES_RECORD_VIDEO("av_notes_record_video", R.string.av_def_action_video, 0, R.drawable.widget_av_video_day, R.drawable.widget_av_video_night, RIGHT),
	AV_NOTES_TAKE_PHOTO("av_notes_take_photo", R.string.av_def_action_picture, 0, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, RIGHT),
	MAPILLARY("mapillary", R.string.mapillary, R.string.mapillary_widget_desc, R.drawable.widget_mapillary_day, R.drawable.widget_mapillary_night, RIGHT),
	PARKING("parking", R.string.map_widget_parking, R.string.parking_widget_desc, R.drawable.widget_parking_day, R.drawable.widget_parking_night, RIGHT),
	// Bottom panel
	ELEVATION_PROFILE("elevation_profile", R.string.elevation_profile, R.string.elevation_profile_widget_desc, R.drawable.widget_route_elevation_day, R.drawable.widget_route_elevation_night, BOTTOM);

	public static final String INTERMEDIATE_TIME_WIDGET_LEGACY = "intermediate_time";
	public static final String NAVIGATION_TIME_WIDGET_LEGACY = "time";
	public static final String BEARING_WIDGET_LEGACY = "bearing";
	public static final String AV_NOTES_WIDGET_LEGACY = "audionotes";

	@NonNull
	public final String id;
	@StringRes
	public final int titleId;
	@StringRes
	public final int descId;
	@DrawableRes
	public final int dayIconId;
	@DrawableRes
	public final int nightIconId;
	@NonNull
	public final WidgetsPanel defaultPanel;

	WidgetParams(@NonNull String id,
	             @StringRes int titleId,
	             @StringRes int descId,
	             @DrawableRes int dayIconId,
	             @DrawableRes int nightIconId,
	             @NonNull WidgetsPanel defaultPanel) {
		this.id = id;
		this.titleId = titleId;
		this.descId = descId;
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

	@Nullable
	public String getDocsUrl() {
		if (this == COORDINATES) {
			return "https://docs.osmand.net/docs/user/widgets/info-widgets#coordinates-widget";
		} else if (this == STREET_NAME) {
			return "https://docs.osmand.net/docs/user/widgets/nav-widgets#street-name";
		} else if (this == MARKERS_TOP_BAR || this == SIDE_MARKER_1 || this == SIDE_MARKER_2) {
			return "https://docs.osmand.net/docs/user/widgets/markers/";
		} else if (this == LANES) {
			return "https://docs.osmand.net/docs/user/widgets/nav-widgets/#lanes";
		} else if (this == CURRENT_SPEED) {
			return "https://docs.osmand.net/docs/user/widgets/info-widgets/#speed";
		} else if (this == MAX_SPEED) {
			return "https://docs.osmand.net/docs/user/widgets/nav-widgets/#speed-limit";
		} else if (this == ALTITUDE) {
			return "https://docs.osmand.net/docs/user/widgets/info-widgets/#altitude";
		} else if (this == GPS_INFO) {
			return "https://docs.osmand.net/docs/user/widgets/info-widgets/#gps-info-android";
		} else if (this == CURRENT_TIME) {
			return "https://docs.osmand.net/docs/user/widgets/info-widgets/#current-time";
		} else if (this == BATTERY) {
			return "https://docs.osmand.net/docs/user/widgets/info-widgets/#battery-level";
		} else if (this == RADIUS_RULER) {
			return "https://docs.osmand.net/docs/user/widgets/radius-ruler/";
		} else if (this == FPS) {
			return "https://docs.osmand.net/docs/user/widgets/info-widgets/#-fps-info-android";
		} else if (this == MAPILLARY) {
			return "https://docs.osmand.net/docs/user/widgets/info-widgets/#-mapillary-widget";
		} else if (this == PARKING) {
			return "https://docs.osmand.net/docs/user/widgets/info-widgets/#-parking-widget";
		}
		return null;
	}

	@Nullable
	public String getSecondaryDescriptionId(@NonNull Context context) {
		if (this == COORDINATES) {
			String configureProfile = context.getString(R.string.configure_profile);
			String generalSettings = context.getString(R.string.general_settings_2);
			String coordinatesFormat = context.getString(R.string.coordinates_format);
			return context.getString(R.string.coordinates_widget_secondary_desc, configureProfile,
					generalSettings, coordinatesFormat);
		} else if (this == FPS) {
			return WidgetGroup.getPartOfPluginDesc(context, OsmandDevelopmentPlugin.class);
		} else if (this == MAPILLARY) {
			return WidgetGroup.getPartOfPluginDesc(context, MapillaryPlugin.class);
		} else if (this == PARKING) {
			return WidgetGroup.getPartOfPluginDesc(context, ParkingPositionPlugin.class);
		}
		return null;
	}

	@DrawableRes
	public int getSecondaryIconId() {
		if (this == COORDINATES) {
			return R.drawable.ic_action_help;
		} else if (this == FPS || this == MAPILLARY || this == PARKING) {
			return R.drawable.ic_extension_dark;
		}
		return 0;
	}

	public int getDefaultOrder() {
		return defaultPanel.getOriginalWidgetOrder(id);
	}

	@NonNull
	public WidgetsPanel getPanel(@NonNull OsmandSettings settings) {
		if (defaultPanel == TOP || defaultPanel == BOTTOM) {
			return defaultPanel;
		} else if (defaultPanel == LEFT) {
			return RIGHT.getWidgetOrder(id, settings) != DEFAULT_ORDER ? RIGHT : LEFT;
		} else if (defaultPanel == RIGHT) {
			return LEFT.getWidgetOrder(id, settings) != DEFAULT_ORDER ? LEFT : RIGHT;
		}
		throw new IllegalStateException("Unsupported panel");
	}

	@Nullable
	public WidgetSettingsBaseFragment getSettingsFragment() {
		if (this == ELEVATION_PROFILE) {
			return new ElevationProfileWidgetSettingsFragment();
		} else if (this == MARKERS_TOP_BAR) {
			return new MapMarkersBarWidgetSettingFragment();
		} else if (this == RADIUS_RULER) {
			return new RadiusRulerWidgetSettingsFragment();
		}
		return null;
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