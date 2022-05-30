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
import net.osmand.plus.views.mapwidgets.configure.settings.TimeToNavigationPointSettingsFragment.TimeToDestinationSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.TimeToNavigationPointSettingsFragment.TimeToIntermediateSettingsFragment;
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
	NEXT_TURN("next_turn", R.string.map_widget_next_turn, 0, R.drawable.widget_next_turn_day, R.drawable.widget_next_turn_night, 0, WidgetGroup.ROUTE_MANEUVERS, LEFT),
	SMALL_NEXT_TURN("next_turn_small", R.string.map_widget_next_turn_small, 0, R.drawable.widget_next_turn_small_day, R.drawable.widget_next_turn_small_night, 0, WidgetGroup.ROUTE_MANEUVERS, LEFT),
	SECOND_NEXT_TURN("next_next_turn", R.string.map_widget_next_next_turn, 0, R.drawable.widget_second_next_turn_day, R.drawable.widget_second_next_turn_night, 0, WidgetGroup.ROUTE_MANEUVERS, LEFT),
	// Top panel
	COORDINATES("coordinates", R.string.coordinates_widget, R.string.coordinates_widget_desc, R.drawable.widget_coordinates_longitude_west_day, R.drawable.widget_coordinates_longitude_west_night, R.string.docs_widget_coordinates, null, TOP),
	STREET_NAME("street_name", R.string.street_name, R.string.street_name_widget_desc, R.drawable.widget_street_name_day, R.drawable.widget_street_name_night, R.string.docs_widget_street_name, null, TOP),
	MARKERS_TOP_BAR("map_markers_top", R.string.map_markers_bar, R.string.map_markers_bar_widget_desc, R.drawable.widget_markers_topbar_day, R.drawable.widget_markers_topbar_night, R.string.docs_widget_markers, null, TOP),
	LANES("lanes", R.string.show_lanes, R.string.lanes_widgets_desc, R.drawable.widget_lanes_day, R.drawable.widget_lanes_night, R.string.docs_widget_lanes, null, TOP),
	// Right panel
	DISTANCE_TO_DESTINATION("distance", R.string.map_widget_distance_to_destination, 0, R.drawable.widget_target_day, R.drawable.widget_target_night, 0, WidgetGroup.NAVIGATION_POINTS, RIGHT),
	INTERMEDIATE_DESTINATION("intermediate_distance", R.string.map_widget_distance_to_intermediate, 0, R.drawable.widget_intermediate_day, R.drawable.widget_intermediate_night, 0, WidgetGroup.NAVIGATION_POINTS, RIGHT),
	TIME_TO_INTERMEDIATE("time_to_intermediate", R.string.map_widget_time_to_intermediate, 0, R.drawable.widget_intermediate_time_day, R.drawable.widget_intermediate_time_night, 0, WidgetGroup.NAVIGATION_POINTS, RIGHT),
	TIME_TO_DESTINATION("time_to_destination", R.string.map_widget_time_to_destination, 0, R.drawable.widget_time_to_distance_day, R.drawable.widget_time_to_distance_night, 0, WidgetGroup.NAVIGATION_POINTS, RIGHT),
	SIDE_MARKER_1("map_marker_1st", R.string.map_marker_1st, 0, R.drawable.widget_marker_day, R.drawable.widget_marker_night, 0, WidgetGroup.MAP_MARKERS, RIGHT),
	SIDE_MARKER_2("map_marker_2nd", R.string.map_marker_2nd, 0, R.drawable.widget_marker_day, R.drawable.widget_marker_night, 0, WidgetGroup.MAP_MARKERS, RIGHT),
	RELATIVE_BEARING("relative_bearing", R.string.map_widget_bearing, 0, R.drawable.widget_relative_bearing_day, R.drawable.widget_relative_bearing_night, 0, WidgetGroup.BEARING, RIGHT),
	MAGNETIC_BEARING("magnetic_bearing", R.string.map_widget_magnetic_bearing, R.string.magnetic_bearing_widget_desc, R.drawable.widget_bearing_day, R.drawable.widget_bearing_night, 0, WidgetGroup.BEARING, RIGHT),
	TRUE_BEARING("true_bearing", R.string.map_widget_true_bearing, 0, R.drawable.widget_true_bearing_day, R.drawable.widget_true_bearing_night, 0, WidgetGroup.BEARING, RIGHT),
	CURRENT_SPEED("speed", R.string.map_widget_current_speed, R.string.current_speed_widget_desc, R.drawable.widget_speed_day, R.drawable.widget_speed_night, R.string.docs_widget_current_speed, null, RIGHT),
	MAX_SPEED("max_speed", R.string.map_widget_max_speed, R.string.max_speed_widget_desc, R.drawable.widget_max_speed_day, R.drawable.widget_max_speed_night, R.string.docs_widget_max_speed, null, RIGHT),
	ALTITUDE("altitude", R.string.map_widget_altitude, R.string.altitude_widget_desc, R.drawable.widget_altitude_day, R.drawable.widget_altitude_night, R.string.docs_widget_altitude, null, RIGHT),
	GPS_INFO("gps_info", R.string.map_widget_gps_info, R.string.gps_info_widget_desc, R.drawable.widget_gps_info_day, R.drawable.widget_gps_info_night, R.string.docs_widget_gps_info, null, RIGHT),
	TRIP_RECORDING_DISTANCE("monitoring", R.string.map_widget_trip_recording_distance, 0, R.drawable.widget_trip_recording_day, R.drawable.widget_trip_recording_night, 0, WidgetGroup.TRIP_RECORDING, RIGHT),
	TRIP_RECORDING_TIME("trip_recording_time", R.string.map_widget_trip_recording_duration, 0, R.drawable.widget_track_recording_duration_day, R.drawable.widget_track_recording_duration_night, 0, WidgetGroup.TRIP_RECORDING, RIGHT),
	TRIP_RECORDING_UPHILL("trip_recording_uphill", R.string.map_widget_trip_recording_uphill, 0, R.drawable.widget_track_recording_uphill_day, R.drawable.widget_track_recording_uphill_night, 0, WidgetGroup.TRIP_RECORDING, RIGHT),
	TRIP_RECORDING_DOWNHILL("trip_recording_downhill", R.string.map_widget_trip_recording_downhill, 0, R.drawable.widget_track_recording_downhill_day, R.drawable.widget_track_recording_downhill_night, 0, WidgetGroup.TRIP_RECORDING, RIGHT),
	CURRENT_TIME("plain_time", R.string.map_widget_plain_time, R.string.current_time_widget_desc, R.drawable.widget_time_day, R.drawable.widget_time_night, R.string.docs_widget_current_time, null, RIGHT),
	BATTERY("battery", R.string.map_widget_battery, R.string.battery_widget_desc, R.drawable.widget_battery_day, R.drawable.widget_battery_night, R.string.docs_widget_battery, null, RIGHT),
	RADIUS_RULER("ruler", R.string.map_widget_ruler_control, R.string.radius_rules_widget_desc, R.drawable.widget_ruler_circle_day, R.drawable.widget_ruler_circle_night, R.string.docs_widget_radius_ruler, null, RIGHT),
	FPS("fps", R.string.map_widget_fps_info, R.string.fps_widget_desc, R.drawable.widget_fps_day, R.drawable.widget_fps_night, R.string.docs_widget_fps, null, RIGHT),
	AV_NOTES_ON_REQUEST("av_notes_on_request", R.string.av_def_action_choose, R.string.av_notes_choose_action_widget_desc, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, 0, WidgetGroup.AUDIO_VIDEO_NOTES, RIGHT),
	AV_NOTES_RECORD_AUDIO("av_notes_record_audio", R.string.av_def_action_audio, 0, R.drawable.widget_av_audio_day, R.drawable.widget_av_audio_night, 0, WidgetGroup.AUDIO_VIDEO_NOTES, RIGHT),
	AV_NOTES_RECORD_VIDEO("av_notes_record_video", R.string.av_def_action_video, 0, R.drawable.widget_av_video_day, R.drawable.widget_av_video_night, 0, WidgetGroup.AUDIO_VIDEO_NOTES, RIGHT),
	AV_NOTES_TAKE_PHOTO("av_notes_take_photo", R.string.av_def_action_picture, 0, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, 0, WidgetGroup.AUDIO_VIDEO_NOTES, RIGHT),
	MAPILLARY("mapillary", R.string.mapillary, R.string.mapillary_widget_desc, R.drawable.widget_mapillary_day, R.drawable.widget_mapillary_night, R.string.docs_widget_mapillary, null, RIGHT),
	PARKING("parking", R.string.map_widget_parking, R.string.parking_widget_desc, R.drawable.widget_parking_day, R.drawable.widget_parking_night, R.string.docs_widget_parking, null, RIGHT),
	// Bottom panel
	ELEVATION_PROFILE("elevation_profile", R.string.elevation_profile, R.string.elevation_profile_widget_desc, R.drawable.widget_route_elevation_day, R.drawable.widget_route_elevation_night, 0, null, BOTTOM);

	public static final String INTERMEDIATE_TIME_WIDGET_LEGACY = "intermediate_time";
	public static final String NAVIGATION_TIME_WIDGET_LEGACY = "time";
	public static final String BEARING_WIDGET_LEGACY = "bearing";
	public static final String AV_NOTES_WIDGET_LEGACY = "audionotes";
	public static final String INTERMEDIATE_ARRIVAL_TIME_LEGACY = "intermediate_arrival_time";
	public static final String INTERMEDIATE_TIME_TO_GO_LEGACY = "intermediate_time_time_to_go";
	public static final String ARRIVAL_TIME_LEGACY = "arrival_time";
	public static final String TIME_TO_GO_LEGACY = "time_to_go";

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
	@StringRes
	public final int docsUrlId;
	@Nullable
	public WidgetGroup group;
	@NonNull
	public final WidgetsPanel defaultPanel;

	WidgetParams(@NonNull String id,
	             @StringRes int titleId,
	             @StringRes int descId,
	             @DrawableRes int dayIconId,
	             @DrawableRes int nightIconId,
	             @StringRes int docsUrlId,
	             @Nullable WidgetGroup group,
	             @NonNull WidgetsPanel defaultPanel) {
		this.id = id;
		this.titleId = titleId;
		this.descId = descId;
		this.dayIconId = dayIconId;
		this.nightIconId = nightIconId;
		this.docsUrlId = docsUrlId;
		this.group = group;
		this.defaultPanel = defaultPanel;
	}

	@DrawableRes
	public int getIconId(boolean night) {
		return night ? nightIconId : dayIconId;
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
		} else if (this == TIME_TO_INTERMEDIATE) {
			return new TimeToIntermediateSettingsFragment();
		} else if (this == TIME_TO_DESTINATION) {
			return new TimeToDestinationSettingsFragment();
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