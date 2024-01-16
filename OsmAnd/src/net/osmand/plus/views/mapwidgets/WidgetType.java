package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.MapWidgetInfo.DELIMITER;
import static net.osmand.plus.views.mapwidgets.WidgetGroup.ALTITUDE;
import static net.osmand.plus.views.mapwidgets.WidgetGroup.ANT_PLUS;
import static net.osmand.plus.views.mapwidgets.WidgetGroup.GLIDE;
import static net.osmand.plus.views.mapwidgets.WidgetGroup.SUNRISE_SUNSET;
import static net.osmand.plus.views.mapwidgets.WidgetGroup.WEATHER;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.LEFT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.RIGHT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.configure.settings.AverageGlideWidgetSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.AverageSpeedWidgetSettingFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.BaseSimpleWidgetSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.ElevationProfileWidgetSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.MapMarkerSideWidgetSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.MapMarkersBarWidgetSettingFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.RadiusRulerWidgetSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.SensorWidgetSettingFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.SunriseSunsetSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.TimeToNavigationPointSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum WidgetType {

	// Left Panel
	NEXT_TURN("next_turn", R.string.map_widget_next_turn, R.string.next_turn_widget_desc, R.drawable.widget_next_turn_day, R.drawable.widget_next_turn_night, 0, WidgetGroup.ROUTE_MANEUVERS, LEFT),
	SMALL_NEXT_TURN("next_turn_small", R.string.map_widget_next_turn_small, R.string.next_turn_widget_desc, R.drawable.widget_next_turn_small_day, R.drawable.widget_next_turn_small_night, 0, WidgetGroup.ROUTE_MANEUVERS, LEFT),
	SECOND_NEXT_TURN("next_next_turn", R.string.map_widget_next_next_turn, R.string.second_next_turn_widget_desc, R.drawable.widget_second_next_turn_day, R.drawable.widget_second_next_turn_night, 0, WidgetGroup.ROUTE_MANEUVERS, LEFT),

	// Top panel
	COORDINATES_MAP_CENTER("coordinates_map_center", R.string.coordinates_widget_map_center, R.string.coordinates_widget_map_center_desc, R.drawable.widget_coordinates_map_center_day, R.drawable.widget_coordinates_map_center_night, R.string.docs_widget_coordinates, WidgetGroup.COORDINATES_WIDGET, TOP),
	COORDINATES_CURRENT_LOCATION("coordinates_current_location", R.string.coordinates_widget_current_location, R.string.coordinates_widget_current_location_desc, R.drawable.widget_coordinates_location_day, R.drawable.widget_coordinates_location_night, R.string.docs_widget_coordinates, WidgetGroup.COORDINATES_WIDGET, TOP),
	STREET_NAME("street_name", R.string.street_name, R.string.street_name_widget_desc, R.drawable.widget_street_name_day, R.drawable.widget_street_name_night, R.string.docs_widget_street_name, null, TOP),
	MARKERS_TOP_BAR("map_markers_top", R.string.map_markers_bar, R.string.map_markers_bar_widget_desc, R.drawable.widget_markers_topbar_day, R.drawable.widget_markers_topbar_night, R.string.docs_widget_markers, null, TOP),
	LANES("lanes", R.string.show_lanes, R.string.lanes_widgets_desc, R.drawable.widget_lanes_day, R.drawable.widget_lanes_night, R.string.docs_widget_lanes, null, TOP),

	// Right panel
	DISTANCE_TO_DESTINATION("distance", R.string.map_widget_distance_to_destination, R.string.distance_to_destination_widget_desc, R.drawable.widget_target_day, R.drawable.widget_target_night, 0, WidgetGroup.NAVIGATION_POINTS, RIGHT),
	INTERMEDIATE_DESTINATION("intermediate_distance", R.string.map_widget_distance_to_intermediate, R.string.distance_to_intermediate_widget_desc, R.drawable.widget_intermediate_day, R.drawable.widget_intermediate_night, 0, WidgetGroup.NAVIGATION_POINTS, RIGHT),
	TIME_TO_INTERMEDIATE("time_to_intermediate", R.string.map_widget_time_to_intermediate, R.string.time_to_intermediate_widget_desc, R.drawable.widget_intermediate_time_day, R.drawable.widget_intermediate_time_night, 0, WidgetGroup.NAVIGATION_POINTS, RIGHT),
	TIME_TO_DESTINATION("time_to_destination", R.string.map_widget_time_to_destination, R.string.time_to_destination_widget_desc, R.drawable.widget_time_to_distance_day, R.drawable.widget_time_to_distance_night, 0, WidgetGroup.NAVIGATION_POINTS, RIGHT),

	SIDE_MARKER_1("map_marker_1st", R.string.marker_1st, R.string.first_marker_widget_desc, R.drawable.widget_marker_day, R.drawable.widget_marker_night, 0, WidgetGroup.MAP_MARKERS, RIGHT),
	SIDE_MARKER_2("map_marker_2nd", R.string.marker_2nd, R.string.second_marker_widget_desc, R.drawable.widget_marker_day, R.drawable.widget_marker_night, 0, WidgetGroup.MAP_MARKERS, RIGHT),

	RELATIVE_BEARING("relative_bearing", R.string.map_widget_bearing, R.string.relative_bearing_widget_desc, R.drawable.widget_relative_bearing_day, R.drawable.widget_relative_bearing_night, 0, WidgetGroup.BEARING, RIGHT),
	MAGNETIC_BEARING("magnetic_bearing", R.string.map_widget_magnetic_bearing, R.string.magnetic_bearing_widget_desc, R.drawable.widget_bearing_day, R.drawable.widget_bearing_night, 0, WidgetGroup.BEARING, RIGHT),
	TRUE_BEARING("true_bearing", R.string.map_widget_true_bearing, R.string.true_bearing_wdiget_desc, R.drawable.widget_true_bearing_day, R.drawable.widget_true_bearing_night, 0, WidgetGroup.BEARING, RIGHT),
	CURRENT_SPEED("speed", R.string.map_widget_current_speed, R.string.current_speed_widget_desc, R.drawable.widget_speed_day, R.drawable.widget_speed_night, R.string.docs_widget_current_speed, null, RIGHT),
	AVERAGE_SPEED("average_speed", R.string.map_widget_average_speed, R.string.average_speed_widget_desc, R.drawable.widget_average_speed_day, R.drawable.widget_average_speed_night, 0, null, RIGHT),
	MAX_SPEED("max_speed", R.string.map_widget_max_speed, R.string.max_speed_widget_desc, R.drawable.widget_max_speed_day, R.drawable.widget_max_speed_night, R.string.docs_widget_max_speed, null, RIGHT),
	ALTITUDE_MY_LOCATION("altitude", R.string.map_widget_altitude_current_location, R.string.altitude_widget_desc, R.drawable.widget_altitude_location_day, R.drawable.widget_altitude_location_night, R.string.docs_widget_altitude, WidgetGroup.ALTITUDE, RIGHT),
	ALTITUDE_MAP_CENTER("altitude_map_center", R.string.map_widget_altitude_map_center, R.string.map_widget_altitude_map_center_desc, R.drawable.widget_altitude_map_center_day, R.drawable.widget_altitude_map_center_night, 0, WidgetGroup.ALTITUDE, RIGHT),
	GPS_INFO("gps_info", R.string.map_widget_gps_info, R.string.gps_info_widget_desc, R.drawable.widget_gps_info_day, R.drawable.widget_gps_info_night, R.string.docs_widget_gps_info, null, RIGHT),

	TRIP_RECORDING_DISTANCE("monitoring", R.string.map_widget_trip_recording_distance, R.string.trip_recording_distance_widget_desc, R.drawable.widget_trip_recording_day, R.drawable.widget_trip_recording_night, 0, WidgetGroup.TRIP_RECORDING, RIGHT),
	TRIP_RECORDING_TIME("trip_recording_time", R.string.map_widget_trip_recording_duration, R.string.trip_recording_duration_widget_desc, R.drawable.widget_track_recording_duration_day, R.drawable.widget_track_recording_duration_night, 0, WidgetGroup.TRIP_RECORDING, RIGHT),
	TRIP_RECORDING_UPHILL("trip_recording_uphill", R.string.map_widget_trip_recording_uphill, R.string.trip_recording_uphill_widget_desc, R.drawable.widget_track_recording_uphill_day, R.drawable.widget_track_recording_uphill_night, 0, WidgetGroup.TRIP_RECORDING, RIGHT),
	TRIP_RECORDING_DOWNHILL("trip_recording_downhill", R.string.map_widget_trip_recording_downhill, R.string.trip_recording_downhill_widget_desc, R.drawable.widget_track_recording_downhill_day, R.drawable.widget_track_recording_downhill_night, 0, WidgetGroup.TRIP_RECORDING, RIGHT),

	CURRENT_TIME("plain_time", R.string.map_widget_plain_time, R.string.current_time_widget_desc, R.drawable.widget_time_day, R.drawable.widget_time_night, R.string.docs_widget_current_time, null, RIGHT),
	BATTERY("battery", R.string.map_widget_battery, R.string.battery_widget_desc, R.drawable.widget_battery_day, R.drawable.widget_battery_night, R.string.docs_widget_battery, null, RIGHT),

	RADIUS_RULER("ruler", R.string.map_widget_ruler_control, R.string.radius_rules_widget_desc, R.drawable.widget_ruler_circle_day, R.drawable.widget_ruler_circle_night, R.string.docs_widget_radius_ruler, null, RIGHT),

	DEV_FPS("fps", R.string.map_widget_rendering_fps, R.string.map_widget_rendering_fps_desc, R.drawable.widget_fps_day, R.drawable.widget_fps_night, R.string.docs_widget_fps, WidgetGroup.DEVELOPER_OPTIONS, RIGHT),
	DEV_MEMORY("memory", R.string.widget_available_ram, R.string.widget_available_ram_desc, R.drawable.widget_developer_ram_day, R.drawable.widget_developer_ram_night, R.string.docs_widget_fps, WidgetGroup.DEVELOPER_OPTIONS, RIGHT),
	DEV_CAMERA_TILT("dev_camera_tilt", R.string.map_widget_camera_tilt, R.string.map_widget_camera_tilt_desc, R.drawable.widget_developer_camera_tilt_day, R.drawable.widget_developer_camera_tilt_night, 0, WidgetGroup.DEVELOPER_OPTIONS, RIGHT),
	DEV_CAMERA_DISTANCE("dev_camera_distance", R.string.map_widget_camera_distance, R.string.map_widget_camera_distance_desc, R.drawable.widget_developer_camera_distance_day, R.drawable.widget_developer_camera_distance_night, 0, WidgetGroup.DEVELOPER_OPTIONS, RIGHT),
	DEV_ZOOM_LEVEL("dev_zoom_level", R.string.map_widget_zoom_level, R.string.map_widget_zoom_level_desc, R.drawable.widget_developer_map_zoom_day, R.drawable.widget_developer_map_zoom_night, 0, WidgetGroup.DEVELOPER_OPTIONS, RIGHT),
	DEV_TARGET_DISTANCE("dev_target_distance", R.string.map_widget_target_distance, R.string.map_widget_target_distance_desc, R.drawable.widget_developer_target_distance_day, R.drawable.widget_developer_target_distance_night, 0, WidgetGroup.DEVELOPER_OPTIONS, RIGHT),

	AV_NOTES_ON_REQUEST("av_notes_on_request", R.string.av_def_action_choose, R.string.av_notes_choose_action_widget_desc, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, 0, WidgetGroup.AUDIO_VIDEO_NOTES, RIGHT),
	AV_NOTES_RECORD_AUDIO("av_notes_record_audio", R.string.av_def_action_audio, R.string.av_notes_audio_widget_desc, R.drawable.widget_av_audio_day, R.drawable.widget_av_audio_night, 0, WidgetGroup.AUDIO_VIDEO_NOTES, RIGHT),
	AV_NOTES_RECORD_VIDEO("av_notes_record_video", R.string.av_def_action_video, R.string.av_notes_video_widget_desc, R.drawable.widget_av_video_day, R.drawable.widget_av_video_night, 0, WidgetGroup.AUDIO_VIDEO_NOTES, RIGHT),
	AV_NOTES_TAKE_PHOTO("av_notes_take_photo", R.string.av_def_action_picture, R.string.av_notes_photo_widget_desc, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, 0, WidgetGroup.AUDIO_VIDEO_NOTES, RIGHT),

	MAPILLARY("mapillary", R.string.mapillary, R.string.mapillary_widget_desc, R.drawable.widget_mapillary_day, R.drawable.widget_mapillary_night, R.string.docs_widget_mapillary, null, RIGHT),

	PARKING("parking", R.string.map_widget_parking, R.string.parking_widget_desc, R.drawable.widget_parking_day, R.drawable.widget_parking_night, R.string.docs_widget_parking, null, RIGHT),

	AIDL_WIDGET("aidl_widget", R.string.map_widget_parking, R.string.parking_widget_desc, R.drawable.widget_parking_day, R.drawable.widget_parking_night, R.string.docs_widget_parking, null, RIGHT),

	TEMPERATURE("temperature_sensor", R.string.shared_string_temperature, R.string.sensor_temperature_desc, R.drawable.widget_weather_temperature_day, R.drawable.widget_weather_temperature_night, 0, ANT_PLUS, RIGHT),
	HEART_RATE("ant_heart_rate", R.string.map_widget_ant_heart_rate, R.string.map_widget_ant_heart_rate_desc, R.drawable.widget_sensor_heart_rate_day, R.drawable.widget_sensor_heart_rate_night, 0, ANT_PLUS, RIGHT),
	BICYCLE_POWER("ant_bicycle_power", R.string.map_widget_ant_bicycle_power, R.string.map_widget_ant_bicycle_power_desc, R.drawable.widget_sensor_bicycle_power_day, R.drawable.widget_sensor_bicycle_power_night, 0, ANT_PLUS, RIGHT),
	BICYCLE_CADENCE("ant_bicycle_cadence", R.string.map_widget_ant_bicycle_cadence, R.string.map_widget_ant_bicycle_cadence_desc, R.drawable.widget_sensor_cadence_day, R.drawable.widget_sensor_cadence_night, 0, ANT_PLUS, RIGHT),
	BICYCLE_SPEED("ant_bicycle_speed", R.string.map_widget_ant_bicycle_speed, R.string.map_widget_ant_bicycle_speed_desc, R.drawable.widget_sensor_speed_day, R.drawable.widget_sensor_speed_night, 0, ANT_PLUS, RIGHT),
	BICYCLE_DISTANCE("ant_bicycle_distance", R.string.map_widget_ant_bicycle_dist, R.string.map_widget_ant_bicycle_dist_desc, R.drawable.widget_sensor_distance_day, R.drawable.widget_sensor_distance_night, 0, ANT_PLUS, RIGHT),
	RSSI("rssi", R.string.map_widget_rssi, R.string.rssi_widget_desc, R.drawable.widget_sensor_speed_day, R.drawable.widget_sensor_speed_night, R.string.docs_widget_rssi, null, RIGHT),

	WEATHER_TEMPERATURE_WIDGET("weather_temp", R.string.map_settings_weather_temp, R.string.temperature_widget_desc, R.drawable.widget_weather_temperature_day, R.drawable.widget_weather_temperature_night, 0, WEATHER, RIGHT),
	WEATHER_PRECIPITATION_WIDGET("weather_precip", R.string.map_settings_weather_precip, R.string.precipitation_widget_desc, R.drawable.widget_weather_precipitation_day, R.drawable.widget_weather_precipitation_night, 0, WEATHER, RIGHT),
	WEATHER_WIND_WIDGET("weather_wind", R.string.map_settings_weather_wind, R.string.wind_widget_desc, R.drawable.widget_weather_wind_day, R.drawable.widget_weather_wind_night, 0, WEATHER, RIGHT),
	WEATHER_CLOUDS_WIDGET("weather_cloud", R.string.map_settings_weather_cloud, R.string.clouds_widget_desc, R.drawable.widget_weather_clouds_day, R.drawable.widget_weather_clouds_night, 0, WEATHER, RIGHT),
	WEATHER_AIR_PRESSURE_WIDGET("weather_pressure", R.string.map_settings_weather_air_pressure, R.string.air_pressure_widget_desc, R.drawable.widget_weather_air_pressure_day, R.drawable.widget_weather_air_pressure_night, 0, WEATHER, RIGHT),

	SUN_POSITION("day_night_mode_sun_position", R.string.map_widget_sun_position, R.string.map_widget_sun_position_desc, R.drawable.widget_sunset_day, R.drawable.widget_sunset_night, 0, SUNRISE_SUNSET, RIGHT),
	SUNRISE("day_night_mode_sunrise", R.string.shared_string_sunrise, R.string.map_widget_sunrise_desc, R.drawable.widget_sunrise_day, R.drawable.widget_sunrise_night, 0, SUNRISE_SUNSET, RIGHT),
	SUNSET("day_night_mode_sunset", R.string.shared_string_sunset, R.string.map_widget_sunset_desc, R.drawable.widget_sunset_day, R.drawable.widget_sunset_night, 0, SUNRISE_SUNSET, RIGHT),

	GLIDE_TARGET("glide_ratio_to_target", R.string.glide_ratio_to_target, R.string.map_widget_glide_target_desc, R.drawable.widget_glide_ratio_to_target_day, R.drawable.widget_glide_ratio_to_target_night, 0, GLIDE, RIGHT),
	GLIDE_AVERAGE("average_glide_ratio", R.string.average_glide_ratio, R.string.map_widget_glide_average_desc, R.drawable.widget_glide_ratio_average_day, R.drawable.widget_glide_ratio_average_night, 0, GLIDE, RIGHT),

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
	private final WidgetGroup group;
	@NonNull
	public final WidgetsPanel defaultPanel;

	WidgetType(@NonNull String id,
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

	public WidgetGroup getGroup() {
		if (group == ALTITUDE && !ALTITUDE_MAP_CENTER.isAllowed()) {
			return null;
		}
		return group;
	}

	public boolean isAllowed() {
		if (this == ALTITUDE_MAP_CENTER) {
			SRTMPlugin plugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
			return plugin != null && plugin.is3DMapsEnabled();
		}
		return true;
	}

	@StringRes
	public int getGroupDescriptionId() {
		if (this == MAGNETIC_BEARING) {
			return R.string.magnetic_bearing_widget_desc;
		} else if (this == AV_NOTES_ON_REQUEST) {
			return R.string.av_notes_choose_action_widget_desc;
		}
		return 0;
	}

	@Nullable
	public String getSecondaryDescription(@NonNull Context context) {
		if (this == COORDINATES_CURRENT_LOCATION || this == COORDINATES_MAP_CENTER) {
			String configureProfile = context.getString(R.string.configure_profile);
			String generalSettings = context.getString(R.string.general_settings_2);
			String coordinatesFormat = context.getString(R.string.coordinates_format);
			return context.getString(R.string.coordinates_widget_secondary_desc, configureProfile,
					generalSettings, coordinatesFormat);
		} else if (this == DEV_FPS || this == DEV_MEMORY) {
			return WidgetGroup.getPartOfPluginDesc(context, OsmandDevelopmentPlugin.class);
		} else if (this == MAPILLARY) {
			return WidgetGroup.getPartOfPluginDesc(context, MapillaryPlugin.class);
		} else if (this == PARKING) {
			return WidgetGroup.getPartOfPluginDesc(context, ParkingPositionPlugin.class);
		} else if (group != null) {
			if (group == WEATHER) {
				return context.getString(R.string.weather_widgets_secondary_desc);
			} else {
				return group.getSecondaryDescription(context);
			}
		}
		return null;
	}

	@DrawableRes
	public int getSecondaryIconId() {
		if (this == COORDINATES_CURRENT_LOCATION || this == COORDINATES_MAP_CENTER) {
			return R.drawable.ic_action_help;
		} else if (this == DEV_FPS || this == DEV_MEMORY || this == MAPILLARY || this == PARKING) {
			return R.drawable.ic_extension_dark;
		} else if (group != null) {
			return group.getSecondaryIconId();
		}
		return 0;
	}

	public boolean isPurchased(@NonNull Context ctx) {
		if (getProWidgets().contains(this)) {
			OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
			return InAppPurchaseUtils.isProWidgetsAvailable(app);
		}
		return true;
	}

	public int getDefaultOrder() {
		return defaultPanel.getOriginalWidgetOrder(id);
	}

	@NonNull
	public WidgetsPanel getPanel(@NonNull OsmandSettings settings) {
		return getPanel(id, settings);
	}

	@NonNull
	public WidgetsPanel getPanel(@NonNull String widgetId, @NonNull OsmandSettings settings) {
		return getPanel(widgetId, settings.getApplicationMode(), settings);
	}

	@NonNull
	public WidgetsPanel getPanel(@NonNull String widgetId, @NonNull ApplicationMode mode, @NonNull OsmandSettings settings) {
		WidgetsPanel widgetsPanel = findWidgetPanel(widgetId, settings, mode);
		if (widgetsPanel != null) {
			return widgetsPanel;
		}
		return defaultPanel;
	}

	@Nullable
	public static WidgetsPanel findWidgetPanel(@NonNull String widgetId, @NonNull OsmandSettings settings, @Nullable ApplicationMode mode) {
		ApplicationMode appMode = mode == null ? settings.getApplicationMode() : mode;
		ArrayList<WidgetsPanel> setPanels = new ArrayList<>();
		ArrayList<WidgetsPanel> unsetPanels = new ArrayList<>();
		for (WidgetsPanel widgetsPanel : WidgetsPanel.values()) {
			if (widgetsPanel.getOrderPreference(settings).isSetForMode(appMode)) {
				setPanels.add(widgetsPanel);
			} else {
				unsetPanels.add(widgetsPanel);
			}
		}
		for (WidgetsPanel panel : setPanels) {
			if (panel.contains(widgetId, settings, appMode)) {
				return panel;
			}
		}
		for (WidgetsPanel panel : unsetPanels) {
			if (panel.contains(widgetId, settings, appMode)) {
				return panel;
			}
		}
		return null;
	}

	@Nullable
	public WidgetSettingsBaseFragment getSettingsFragment(@NonNull Context ctx, @Nullable MapWidgetInfo widgetInfo) {
		if (this == ELEVATION_PROFILE) {
			return isPurchased(ctx) ? new ElevationProfileWidgetSettingsFragment() : null;
		} else if (this == MARKERS_TOP_BAR) {
			return new MapMarkersBarWidgetSettingFragment();
		} else if (this == RADIUS_RULER) {
			return new RadiusRulerWidgetSettingsFragment();
		} else if (this == TIME_TO_INTERMEDIATE || this == TIME_TO_DESTINATION) {
			return new TimeToNavigationPointSettingsFragment();
		} else if (this == SIDE_MARKER_1 || this == SIDE_MARKER_2) {
			return new MapMarkerSideWidgetSettingsFragment();
		} else if (this == AVERAGE_SPEED) {
			return new AverageSpeedWidgetSettingFragment();
		} else if (this == SUNRISE || this == SUNSET || this == SUN_POSITION) {
			return new SunriseSunsetSettingsFragment();
		} else if (this == HEART_RATE ||
				this == BICYCLE_POWER ||
				this == BICYCLE_CADENCE ||
				this == BICYCLE_SPEED ||
				this == BICYCLE_DISTANCE ||
				this == RSSI ||
				this == TEMPERATURE) {
			return new SensorWidgetSettingFragment();
		} else if (this == GLIDE_AVERAGE) {
			return new AverageGlideWidgetSettingsFragment();
		}

		if (widgetInfo instanceof SimpleWidgetInfo) {
			SimpleWidget simpleWidget = (SimpleWidget) widgetInfo.widget;
			if (simpleWidget.isVerticalWidget()) {
				BaseSimpleWidgetSettingsFragment settingsFragment = new BaseSimpleWidgetSettingsFragment();
				settingsFragment.setWidgetType(this);
				return settingsFragment;
			}
		}
		return null;
	}

	public boolean isMainWidgetOfGroup() {
		return group != null && this == group.getMainWidget();
	}

	@Nullable
	public static WidgetType getById(@NonNull String id) {
		for (WidgetType widget : values()) {
			String defaultId = getDefaultWidgetId(id);
			if (widget.id.equals(defaultId)) {
				return widget;
			}
		}
		return null;
	}

	@NonNull
	public static List<WidgetType> getProWidgets() {
		return Collections.singletonList(ELEVATION_PROFILE);
	}

	public static boolean isOriginalWidget(@NonNull String widgetId) {
		return widgetId.equals(getDefaultWidgetId(widgetId));
	}

	public static boolean isComplexWidget(@NonNull String widgetId) {
		return CollectionUtils.equalsToAny(getDefaultWidgetId(widgetId), (Object[])getComplexWidgetIds());
	}

	@NonNull
	public static String[] getComplexWidgetIds() {
		return new String[] {COORDINATES_MAP_CENTER.id, COORDINATES_CURRENT_LOCATION.id,
				MARKERS_TOP_BAR.id, ELEVATION_PROFILE.id, STREET_NAME.id, LANES.id};
	}

	@NonNull
	public static String getDefaultWidgetId(@NonNull String widgetId) {
		int index = widgetId.indexOf(DELIMITER);
		return index != -1 ? widgetId.substring(0, index) : widgetId;
	}

	@NonNull
	public static String getDuplicateWidgetId(@NonNull WidgetType widgetType) {
		return getDuplicateWidgetId(widgetType.id);
	}

	@NonNull
	public static String getDuplicateWidgetId(@NonNull String widgetId) {
		return getDefaultWidgetId(widgetId) + DELIMITER + System.currentTimeMillis();
	}
}