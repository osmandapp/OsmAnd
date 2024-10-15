package net.osmand.plus.views.mapwidgets;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.weather.WeatherPlugin;

import java.util.ArrayList;
import java.util.List;

public enum WidgetGroup {

	ROUTE_MANEUVERS(R.string.route_maneuvers, R.string.route_maneuvers_desc, R.drawable.widget_lanes_day, R.drawable.widget_lanes_night, R.string.docs_widget_route_maneuvers),
	NAVIGATION_POINTS(R.string.navigation_points, R.string.navigation_points_desc, R.drawable.widget_navigation_day, R.drawable.widget_navigation_night, R.string.docs_widget_navigation_points),
	COORDINATES_WIDGET(R.string.coordinates_widget, R.string.coordinates_widget_desc, R.drawable.widget_coordinates_longitude_west_day, R.drawable.widget_coordinates_longitude_west_night, R.string.docs_widget_coordinates),
	MAP_MARKERS(R.string.map_markers, R.string.map_markers_desc, R.drawable.widget_marker_day, R.drawable.widget_marker_night, R.string.docs_widget_markers),
	BEARING(R.string.shared_string_bearing, R.string.bearing_desc, R.drawable.widget_relative_bearing_day, R.drawable.widget_relative_bearing_night, R.string.docs_widget_bearing),
	TRIP_RECORDING(R.string.map_widget_monitoring, 0, R.drawable.widget_trip_recording_day, R.drawable.widget_trip_recording_night, R.string.docs_widget_trip_recording),
	AUDIO_VIDEO_NOTES(R.string.map_widget_av_notes, R.string.audio_video_notes_desc, R.drawable.widget_av_photo_day, R.drawable.widget_av_photo_night, R.string.docs_widget_av_notes),
	DEVELOPER_OPTIONS(R.string.developer_widgets, 0, R.drawable.widget_developer_day, R.drawable.widget_developer_night, 0),
	ALTITUDE(R.string.altitude, R.string.map_widget_altitude_desc, R.drawable.widget_altitude_day, R.drawable.widget_altitude_night, 0),
	ANT_PLUS(R.string.external_sensor_widgets, 0, R.drawable.widget_sensor_external_day, R.drawable.widget_sensor_external_night, 0),
	VEHICLE_METRICS(R.string.obd_widget_group, 0, R.drawable.widget_sensor_external_day, R.drawable.widget_sensor_external_night, 0),
	WEATHER(R.string.shared_string_weather, R.string.weather_widget_group_desc, R.drawable.widget_weather_umbrella_day, R.drawable.widget_weather_umbrella_night, 0),
	SUNRISE_SUNSET(R.string.map_widget_sun_position, R.string.map_widget_group_sunrise_sunset_desc, R.drawable.widget_sunset_day, R.drawable.widget_sunset_night, 0),
	GLIDE(R.string.map_widget_group_glide_ratio, R.string.map_widget_group_glide_desc, R.drawable.widget_glide_ratio_to_target_day, R.drawable.widget_glide_ratio_to_target_night, 0),
	ROUTE_GUIDANCE(R.string.route_guidance, R.string.route_guidance_desc, R.drawable.widget_lanes_day, R.drawable.widget_lanes_night, R.string.docs_widget_route_maneuvers);

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

	WidgetGroup(@StringRes int titleId,
	            @StringRes int descId,
	            @DrawableRes int dayIconId,
	            @DrawableRes int nightIconId,
	            @StringRes int docsUrlId) {
		this.titleId = titleId;
		this.descId = descId;
		this.dayIconId = dayIconId;
		this.nightIconId = nightIconId;
		this.docsUrlId = docsUrlId;
	}

	@NonNull
	public List<WidgetType> getWidgets() {
		List<WidgetType> widgets = new ArrayList<>();
		for (WidgetType widget : WidgetType.values()) {
			if (this == widget.getGroup() || this == widget.getVerticalGroup()) {
				widgets.add(widget);
			}
		}
		return widgets;
	}

	@NonNull
	public List<String> getWidgetsIds() {
		List<String> widgetsIds = new ArrayList<>();
		for (WidgetType widget : getWidgets()) {
			widgetsIds.add(widget.id);
		}
		return widgetsIds;
	}

	@Nullable
	public WidgetType getMainWidget() {
		switch (this) {
			case BEARING:
				return WidgetType.RELATIVE_BEARING;
			case TRIP_RECORDING:
				return WidgetType.TRIP_RECORDING_DISTANCE;
			case AUDIO_VIDEO_NOTES:
				return WidgetType.AV_NOTES_ON_REQUEST;
			default:
				return null;
		}
	}

	@DrawableRes
	public int getIconId(boolean nightMode) {
		return nightMode ? nightIconId : dayIconId;
	}

	@Nullable
	public String getSecondaryDescription(@NonNull Context context) {
		if (this == BEARING) {
			String configureProfile = context.getString(R.string.configure_profile);
			String generalSettings = context.getString(R.string.general_settings_2);
			String angularUnit = context.getString(R.string.angular_measeurement);
			return context.getString(R.string.bearing_secondary_desc, configureProfile, generalSettings, angularUnit);
		} else if (this == TRIP_RECORDING) {
			return getPartOfPluginDesc(context, OsmandMonitoringPlugin.class);
		} else if (this == AUDIO_VIDEO_NOTES) {
			return getPartOfPluginDesc(context, AudioVideoNotesPlugin.class);
		} else if (this == COORDINATES_WIDGET) {
			String configureProfile = context.getString(R.string.configure_profile);
			String generalSettings = context.getString(R.string.general_settings_2);
			String coordinatesFormat = context.getString(R.string.coordinates_format);
			return context.getString(R.string.coordinates_widget_secondary_desc, configureProfile,
					generalSettings, coordinatesFormat);
		} else if (this == DEVELOPER_OPTIONS) {
			return getPartOfPluginDesc(context, OsmandDevelopmentPlugin.class);
		} else if (this == ANT_PLUS) {
			return getPartOfPluginDesc(context, ExternalSensorsPlugin.class);
		} else if (this == WEATHER) {
			return getPartOfPluginDesc(context, WeatherPlugin.class);
		}
		return null;
	}

	@DrawableRes
	public int getSecondaryIconId() {
		if (this == BEARING || this == COORDINATES_WIDGET) {
			return R.drawable.ic_action_help;
		} else if (this == TRIP_RECORDING || this == AUDIO_VIDEO_NOTES || this == DEVELOPER_OPTIONS
				|| this == WEATHER || this == ANT_PLUS) {
			return R.drawable.ic_extension_dark;
		}
		return 0;
	}

	public int getOrder() {
		return getWidgets().get(0).ordinal();
	}

	@Nullable
	public static <T extends OsmandPlugin> String getPartOfPluginDesc(@NonNull Context context, @NonNull Class<T> clz) {
		OsmandPlugin plugin = PluginsHelper.getPlugin(clz);
		return plugin != null
				? context.getString(R.string.widget_secondary_desc_part_of, plugin.getName())
				: null;
	}
}